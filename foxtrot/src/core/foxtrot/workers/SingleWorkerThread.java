/**
 * Copyright (c) 2002, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.workers;

import foxtrot.AbstractWorkerThread;
import foxtrot.Task;

/**
 * Full implementation of {@link foxtrot.WorkerThread} that uses a single worker thread to run
 * {@link foxtrot.Task}s subclasses. <br>
 * Tasks execution is serialized: tasks are enqueued and executed one after the other.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision$
 */
public class SingleWorkerThread extends AbstractWorkerThread implements Runnable
{
   private static final boolean debug = false;

   private Thread thread;
   private Link current;

   public void start()
   {
      if (isAlive()) return;
      if (debug) System.out.println("[SingleWorkerThread] Starting");

      stop();

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

   public void stop()
   {
      if (thread != null)
      {
         if (debug) System.out.println("[SingleWorkerThread] Ending " + thread);
         thread.interrupt();
      }
   }

   public boolean isAlive()
   {
      if (thread == null) return false;
      return thread.isAlive() && !isThreadInterrupted();
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
            if (debug) System.out.println("[SingleWorkerThread] Task queue not empty, enqueueing task:" + t);

            // Append the given task at the end of the queue
            Link item = current;
            while (item.next != null) item = item.next;
            item.next = new Link(t);
         }
         else
         {
            if (debug) System.out.println("[SingleWorkerThread] Task queue empty, adding task:" + t);

            // Add the given task and notify waiting
            current = new Link(t);
            notifyAll();
         }
      }
   }

   /**
    * Removes and returns the first available {@link foxtrot.Task} from the internal queue.
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
            if (debug) System.out.println("[SingleWorkerThread] Task queue empty, waiting for tasks");

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
    * @see java.lang.Thread#isInterrupted
    */
   protected boolean isThreadInterrupted()
   {
      return thread.isInterrupted();
   }

   /**
    * The worker thread dequeues one {@link foxtrot.Task} from the internal queue via {@link #takeTask}
    * and then runs it calling {@link #runTask}
    */
   public void run()
   {
      if (debug) System.out.println("[SingleWorkerThread] Started " + thread);

      while (!isThreadInterrupted())
      {
         try
         {
            Task t = takeTask();
            if (debug) System.out.println("[SingleWorkerThread] Dequeued Task " + t);
            run(t);
         }
         catch (InterruptedException x)
         {
            if (debug) System.out.println("[SingleWorkerThread] Interrupted " + thread);
            Thread.currentThread().interrupt();
            break;
         }
      }
   }

   protected void run(Task task)
   {
      runTask(task);
   }

   private static class Link
   {
      private Link next;
      private final Task task;

      private Link(Task task)
      {
         this.task = task;
      }
   }
}