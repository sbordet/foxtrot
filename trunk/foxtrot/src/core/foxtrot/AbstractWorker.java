package foxtrot;

/**
 *
 * @version $Revision$
 */
abstract class AbstractWorker
{
   static final boolean debug = false;

   private WorkerThread workerThread;

   AbstractWorker()
   {
   }

   /**
    * Returns the WorkerThread used to run {@link foxtrot.Task}s subclasses in a thread
    * that is not the Event Dispatch Thread.
    * @see #workerThread(WorkerThread)
    */
   WorkerThread workerThread()
   {
      if (workerThread == null)
         workerThread(createDefaultWorkerThread());
      return workerThread;
   }

   /**
    * Sets the WorkerThread used to run {@link foxtrot.Task}s subclasses in a thread
    * that is not the Event Dispatch Thread.
    * @see #workerThread
    * @throws java.lang.IllegalArgumentException If workerThread is null
    */
   void workerThread(WorkerThread workerThread)
   {
      if (workerThread == null) throw new IllegalArgumentException("WorkerThread cannot be null");
      this.workerThread = workerThread;
      if (debug) System.out.println("[Worker] Initialized WorkerThread: " + workerThread);
   }

   /**
    * Creates a default WorkerThread instance for this Worker.
    */
   abstract WorkerThread createDefaultWorkerThread();
}
