package foxtrot;

import foxtrot.workers.MultiWorkerThread;

public class ConcurrentWorker extends AbstractSyncWorker
{
   private static ConcurrentWorker instance = new ConcurrentWorker();

   /**
    * Cannot be instantiated, use static methods only.
    */
   private ConcurrentWorker()
   {
   }

   /**
    * @see Worker#getWorkerThread
    */
   public static WorkerThread getWorkerThread()
   {
      return instance.workerThread();
   }

   /**
    * @see Worker#setWorkerThread
    */
   public static void setWorkerThread(WorkerThread workerThread)
   {
      instance.workerThread(workerThread);
   }

   WorkerThread createDefaultWorkerThread()
   {
      return new MultiWorkerThread();
   }

   public static EventPump getEventPump()
   {
      return instance.eventPump();
   }

   public static void setEventPump(EventPump eventPump)
   {
      instance.eventPump(eventPump);
   }
   
   public static Object post(Task task) throws Exception
   {
      return instance.post(task, getWorkerThread(), getEventPump());
   }
}
