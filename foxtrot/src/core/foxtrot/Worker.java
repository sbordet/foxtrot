/**
 * Copyright (c) 2002, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.swing.SwingUtilities;

/**
 * The class that execute time-consuming {@link Task}s and {@link Job}s. <p>
 * It is normally used in event listeners that must execute time-consuming operations without
 * freezing the Swing GUI. <br>
 * Usage example (simplified from the Foxtrot examples):
 * <pre>
 * JButton button = new JButton("Take a nap!");
 * button.addActionListener(new ActionListener()
 * {
 *    public void actionPerformed(ActionEvent e)
 *    {
 *       try
 *       {
 *          Worker.post(new Task()
 *          {
 *             public Object run() throws Exception
 *             {
 *                 Thread.sleep(10000);
 *                 return null;
 *             }
 *          });
 *       }
 *       catch (Exception ignored) {}
 *    }
 * });
 * </pre>
 *
 * While normally not necessary, it is possible to customize the two core components of this
 * class, the {@link EventPump} and the {@link WorkerThread}, explicitely via API or by
 * setting the system properties <code>foxtrot.event.pump</code> and
 * <code>foxtrot.worker.thread</code>, respectively, to a full qualified name of a class
 * implementing, respectively, the above interfaces.
 *
 * @see Task
 * @see Job
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision$
 */
public class Worker
{
   private static EventPump eventPump;
   private static WorkerThread workerThread;

   static final boolean debug = true;

   /**
    * Cannot be instantiated, use static methods only.
    */
   private Worker()
   {
   }

   /**
    * Enqueues the given Task to be executed in the worker thread. <br>
    * If this method is called from the Event Dispatch Thread, it blocks until the task has been executed,
    * either by finishing normally or throwing an exception. <br>
    * If this method is called by the worker thread, and thus is called from another Task,
    * it executes the new Task immediately and then returns the control to the calling Task. <br>
    * While executing Tasks, it dequeues AWT events from the AWT Event Queue; even in case of AWT events
    * that throw RuntimeExceptions or Errors, this method will not return until the first Task
    * (posted from the Event Dispatch Thread) is finished.
    * @throws IllegalStateException if is not called from the Event Dispatch Thread nor from another Task.
    * @see #post(Job job)
    */
   public static Object post(Task task) throws Exception
   {
      initializeWorkerThread();
      initializeEventPump();

      boolean isEventThread = SwingUtilities.isEventDispatchThread();
      if (!isEventThread && !workerThread.isWorkerThread())
      {
         throw new IllegalStateException("This method can be called only from the AWT Event Dispatch Thread or from the another Task");
      }

      // It is possible that the worker thread is stopped when an applet is destroyed.
      // Here we restart it in case has been stopped.
      // Useful also if the WorkerThread has been replaced but not started by the user
      if (!workerThread.isAlive()) workerThread.start();

      if (isEventThread)
      {
         workerThread.postTask(task);

         // The following line blocks until the task has been executed, or it is interrupted
         eventPump.pumpEvents(task);
      }
      else
      {
         workerThread.runTask(task);
      }

      return task.getResultOrThrow();
   }

   /**
    * Enqueues the given Job to be executed in the worker thread. <br>
    * This method behaves exactly like {@link #post(Task task)}, but it does not throw checked exceptions.
    * @throws IllegalStateException if is not called from the Event Dispatch Thread nor from another Job or Task.
    * @see #post(Task task)
    */
   public static Object post(Job job)
   {
      try
      {
         return post((Task)job);
      }
      catch (RuntimeException x)
      {
         throw x;
      }
      catch (Exception x)
      {
         // If it happens, it's a bug
         if (debug)
         {
            System.err.println("[Foxtrot] ERROR: checked exception thrown by a Job");
            x.printStackTrace();
         }

         // I should throw an UndeclaredThrowableException, but that is
         // available only in JDK 1.3+, so here I use RuntimeException
         throw new RuntimeException(x.toString());
      }
      catch (Error x)
      {
         throw x;
      }
   }

   /**
    * Returns the EventPump used to pump events from the AWT Event Queue.
    */
   public static EventPump getEventPump()
   {
      return eventPump;
   }

   /**
    * Sets the EventPump to be used to pump events from the AWT Event Queue. <br>
    * After calling this method, subsequent invocation of {@link #post(Task)}
    * or {@link #post(Job)} will use the newly installed EventPump.
    */
   public static void setEventPump(EventPump eventPump)
   {
      if (eventPump == null) throw new IllegalArgumentException("EventPump cannot be null");
      Worker.eventPump = eventPump;
   }

   /**
    * Returns the WorkerThread used to run {@link Task}s or {@link Job}s in a thread
    * that is not the Event Dispatch Thread.
    */
   public static WorkerThread getWorkerThread()
   {
      return workerThread;
   }

   /**
    * Sets the WorkerThread used to run {@link Task}s or {@link Job}s in a thread
    * that is not the Event Dispatch Thread. <br>
    * After calling this method, subsequent invocation of {@link #post(Task)}
    * or {@link #post(Job)} will use the newly installed WorkerThread.
    */
   public static void setWorkerThread(WorkerThread workerThread)
   {
      if (workerThread == null) throw new IllegalArgumentException("WorkerThread cannot be null");
      Worker.workerThread = workerThread;
   }

   private static void initializeWorkerThread()
   {
      if (workerThread != null) return;

      // First look into the system property
      String workerThreadClassName = (String)AccessController.doPrivileged(new PrivilegedAction()
      {
         public Object run()
         {
            return System.getProperty("foxtrot.worker.thread");
         }
      });

      if (workerThreadClassName == null)
      {
         workerThread = new SingleWorkerThread();
      }
      else
      {
         ClassLoader loader = Worker.class.getClassLoader();
         if (loader == null) loader = ClassLoader.getSystemClassLoader();
         try
         {
            workerThread = (WorkerThread)loader.loadClass(workerThreadClassName).newInstance();
         }
         catch (Throwable x)
         {
            workerThread = new SingleWorkerThread();
         }
      }
   }

   private static void initializeEventPump()
   {
      if (eventPump != null) return;

      // First look into the system property
      String eventPumpClassName = (String)AccessController.doPrivileged(new PrivilegedAction()
      {
         public Object run()
         {
            return System.getProperty("foxtrot.event.pump");
         }
      });

      if (eventPumpClassName != null)
      {
         ClassLoader loader = Worker.class.getClassLoader();
         if (loader == null) loader = ClassLoader.getSystemClassLoader();
         try
         {
            eventPump = (EventPump)loader.loadClass(eventPumpClassName).newInstance();
            return;
         }
         catch (Throwable ignored)
         {
            // Fall through
         }
      }

      if (JDKVersion.isJDK14() || JDKVersion.isJDK13())
      {
         eventPump = new ConditionalEventPump();
      }
      else if (JDKVersion.isJDK12())
      {
         // Not sure it will work.
         eventPump = new ConditionalEventPump();
      }
      else
      {
         // No JDK 1.1 support for now
         throw new IllegalStateException("JDK 1.1 is not supported");
      }
   }
}
