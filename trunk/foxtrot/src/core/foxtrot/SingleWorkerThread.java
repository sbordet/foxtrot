package foxtrot;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;

import javax.swing.SwingUtilities;

/**
 * Full implementation of {@link WorkerThread} that uses a single worker thread to run
 * {@link Task}s and {@link Job}s. <br>
 * Tasks execution is serialized: tasks are enqueued and executed one after the other.
 *
 * @author <a href="mailto:simone.bordet@hp.com">Simone Bordet</a>
 * @version $Revision$
 */
public class SingleWorkerThread extends AbstractWorkerThread implements Runnable
{
   private Thread thread;
   private Link current;

   public void start()
   {
      if (Worker.debug) System.out.println("[Foxtrot] Starting Worker Thread");

      if (thread != null && thread.isAlive()) thread.interrupt();

      thread = new Thread(this, getThreadName());
      // Daemon, since the JVM should shut down on Event Dispatch Thread termination
      thread.setDaemon(true);
      thread.start();
   }

   /**
    * Returns the name of the worker thread used by this WorkerThread.
    */
   protected String getThreadName()
   {
      return "Foxtrot Single Worker Thread";
   }

   private void stop()
   {
      if (Worker.debug) System.out.println("[Foxtrot] Ending Worker Thread");

      thread.interrupt();
   }

   public boolean isAlive()
   {
      if (thread == null) return false;
      return thread.isAlive();
   }

   public boolean isWorkerThread()
   {
      return Thread.currentThread() == thread;
   }

   /**
    * Posts the given Task onto an internal queue.
    * @see #takeTask
    */
   public void postTask(Task t)
   {
      // Synchronized since the variable current is accessed from two threads.
      // See takeTask()
      synchronized (this)
      {
         if (hasTasks())
         {
            if (Worker.debug) System.out.println("[Foxtrot] Worker Thread queue not empty, enqueueing task:" + t);

            // Append the given task at the end of the queue
            Link item = current;
            while (item.next != null) item = item.next;
            item.next = new Link(t);
         }
         else
         {
            if (Worker.debug) System.out.println("[Foxtrot] Worker Thread queue empty, adding task:" + t);

            // Add the given task and notify waiting
            current = new Link(t);
            notifyAll();
         }
      }
   }

   /**
    * Removes and returns the first available {@link Task} from the internal queue.
    * If no Tasks are available, this method blocks until a Task is posted via
    * {@link #postTask}
    */
   protected Task takeTask() throws InterruptedException
   {
      // Synchronized since the variable current is accessed from two threads.
      // See postTask()
      synchronized (this)
      {
         while (!hasTasks())
         {
            if (Worker.debug) System.out.println("[Foxtrot] Worker Thread queue empty, waiting for tasks");

            wait();
         }

         // Taking the current task, removing it from the queue
         Task t = current.task;
         current = current.next;
         return t;
      }
   }

   private boolean hasTasks()
   {
      synchronized (this)
      {
         return current != null;
      }
   }

   /**
    * Returns whether the worker thread has been interrupted or not.
    * @see Thread#isInterrupted
    */
   protected boolean isThreadInterrupted()
   {
      return thread.isInterrupted();
   }

   /**
    * The worker thread dequeues one {@link Task} from the internal queue via {@link #takeTask}
    * and then runs it calling {@link #runTask}
    */
   public void run()
   {
      if (Worker.debug) System.out.println("[Foxtrot] Worker Thread started");

      while (!isThreadInterrupted())
      {
         try
         {
            Task t = takeTask();
            runTask(t);
         }
         catch (InterruptedException x)
         {
            if (Worker.debug) System.out.println("[Foxtrot] Worker Thread interrupted, shutting down");
            break;
         }
      }
   }

   private static class Link
   {
      private Link next;
      private Task task;

      private Link(Task task)
      {
         this.task = task;
      }
   }
}
