package foxtrot;

import javax.swing.SwingUtilities;

import foxtrot.pumps.JDK13QueueEventPump;
import foxtrot.pumps.SunJDK140ConditionalEventPump;
import foxtrot.pumps.SunJDK141ConditionalEventPump;

abstract class AbstractSyncWorker extends AbstractWorker
{
   private EventPump eventPump;

   AbstractSyncWorker()
   {
   }

   EventPump eventPump()
   {
      if (eventPump == null)
         eventPump(createDefaultEventPump());
      return eventPump;
   }

   void eventPump(EventPump eventPump)
   {
      if (eventPump == null) throw new IllegalArgumentException("EventPump cannot be null");
      this.eventPump = eventPump;
      if (debug) System.out.println("[Worker] Initialized EventPump: " + eventPump);
   }

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
   
   Object post(Task task, WorkerThread workerThread, EventPump eventPump) throws Exception
   {
      boolean isEventThread = SwingUtilities.isEventDispatchThread();
      if (!isEventThread && !workerThread.isWorkerThread())
      {
         throw new IllegalStateException("Worker.post() can be called only from the AWT Event Dispatch Thread or from another Task");
      }

      if (isEventThread)
      {
         workerThread.postTask(task);

         // The following line blocks until the task has been executed
         eventPump.pumpEvents(task);
      }
      else
      {
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
