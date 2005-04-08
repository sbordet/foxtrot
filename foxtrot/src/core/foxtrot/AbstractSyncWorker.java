/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot;

import javax.swing.SwingUtilities;

import foxtrot.pumps.JDK13QueueEventPump;
import foxtrot.pumps.SunJDK140ConditionalEventPump;
import foxtrot.pumps.SunJDK141ConditionalEventPump;

/**
 * Base class for Foxtrot workers that have synchronous behavior.
 * @version $Revision$
 */
abstract class AbstractSyncWorker extends AbstractWorker
{
   private EventPump eventPump;

   AbstractSyncWorker()
   {
   }

   /**
    * Returns the EventPump for this worker, creating it if not already set. <br />
    * Uses a C-style getter method to avoid clash with the static getter method
    * present in subclasses.
    * @see #createDefaultEventPump
    */
   EventPump eventPump()
   {
      if (eventPump == null)
         eventPump(createDefaultEventPump());
      return eventPump;
   }

   /**
    * Sets the EventPump for this worker. <br />
    * Uses a C-style setter method to avoid clash with the static setter method
    * present in subclasses.
    * @throws IllegalArgumentException if eventPump is null
    */
   void eventPump(EventPump eventPump)
   {
      if (eventPump == null) throw new IllegalArgumentException("EventPump cannot be null");
      this.eventPump = eventPump;
      if (debug) System.out.println("[AbstractSyncWorker] Initialized EventPump: " + eventPump);
   }

   /**
    * Creates and returns the default EventPump for this worker
    */
   EventPump createDefaultEventPump()
   {
      if (JREVersion.isJRE141())
      {
         return new SunJDK141ConditionalEventPump();
      }
      else if (JREVersion.isJRE140())
      {
         return new SunJDK140ConditionalEventPump();
      }
      else if (JREVersion.isJRE13() || JREVersion.isJRE12())
      {
         return new JDK13QueueEventPump();
      }
      else
      {
         throw new Error("The current JRE is not supported");
      }
   }
   
   /**
    * Executes the given Task using the given workerThread and eventPump.
    * This method blocks (while dequeuing AWT events) until the Task is finished,
    * either by returning a result or by throwing.
    */
   Object post(Task task, WorkerThread workerThread, EventPump eventPump) throws Exception
   {
      boolean isEventThread = SwingUtilities.isEventDispatchThread();
      if (!isEventThread && !workerThread.isWorkerThread())
      {
         throw new IllegalStateException("Worker.post() can be called only from the AWT Event Dispatch Thread or from a worker thread");
      }

      if (isEventThread)
      {
         workerThread.postTask(task);

         // The following line blocks until the task has been executed
         eventPump.pumpEvents(task);
      }
      else
      {
         // Executes the Task in this thread
         workerThread.runTask(task);
      }

      try
      {
         return task.getResultOrThrow();
      }
      finally
      {
         task.reset();
      }
   }
   
   /**
    * Executes the given Job using the given workerThread and eventPump.
    * This method has the same behavior of {@link post(Task)}
    */
   Object post(Job job, WorkerThread workerThread, EventPump eventPump)
   {
      try
      {
         return post((Task)job, workerThread, eventPump);
      }
      catch (RuntimeException x)
      {
         throw x;
      }
      catch (Exception x)
      {
         // If it happens, it's a bug in the compiler
         if (debug)
         {
            System.err.println("[Worker] PANIC: checked exception thrown by a Job !");
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
}
