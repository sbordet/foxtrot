package foxtrot;

import foxtrot.workers.MultiWorkerThread;

/**
 *
 * @version $Revision$
 */
public class AsyncWorker extends AbstractWorker
{
   private static AsyncWorker instance = new AsyncWorker();

   /**
    * Cannot be instantiated, use static methods only.
    */
   private AsyncWorker()
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

   public static void post(AsyncTask task)
   {
      WorkerThread workerThread = getWorkerThread();
      workerThread.postTask(task);
   }
}
