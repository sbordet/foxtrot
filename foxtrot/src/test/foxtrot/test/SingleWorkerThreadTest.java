package foxtrot.test;

import foxtrot.workers.SingleWorkerThread;
import foxtrot.Task;

/**
 *
 * @version $Revision$
 */
public class SingleWorkerThreadTest extends FoxtrotTestCase
{
   public SingleWorkerThreadTest(String s)
   {
      super(s);
   }

   private class TestSingleWorkerThread extends SingleWorkerThread
   {
      public void stop()
      {
         super.stop();
      }
   }

   public void testStart() throws Exception
   {
      TestSingleWorkerThread worker = new TestSingleWorkerThread();
      if (worker.isAlive()) fail();
      worker.start();
      if (!worker.isAlive()) fail();
      // This sleep will ensure that the JUnit JVM does not exit before the WorkerThread has done its job
      Thread.sleep(500);
   }

   public void testStop() throws Exception
   {
      TestSingleWorkerThread worker = new TestSingleWorkerThread();
      worker.start();
      worker.stop();
      if (worker.isAlive()) fail();
      Thread.sleep(500);
   }

   public void testStartStopStart() throws Exception
   {
      TestSingleWorkerThread worker = new TestSingleWorkerThread();
      worker.start();
      worker.stop();
      worker.start();
      if (!worker.isAlive()) fail();
      Thread.sleep(500);
   }

   public void testStopBeforeStart() throws Exception
   {
      TestSingleWorkerThread worker = new TestSingleWorkerThread();
      worker.stop();
      Thread.sleep(500);
   }

   public void testStartStart() throws Exception
   {
      final MutableHolder thread = new MutableHolder(null);
      TestSingleWorkerThread worker = new TestSingleWorkerThread()
      {
         public void run()
         {
            thread.set(Thread.currentThread());
            super.run();
         }
      };
      worker.start();
      Thread.sleep(500);
      Thread foxtrot = (Thread)thread.get();
      worker.start();
      Thread.sleep(500);
      if (foxtrot != thread.get()) fail();
      Thread.sleep(500);
   }

   public void testNoStartAllowsPost() throws Exception
   {
      TestSingleWorkerThread worker = new TestSingleWorkerThread();
      final MutableInteger pass = new MutableInteger(0);
      worker.postTask(new Task()
      {
         public Object run() throws Exception
         {
            pass.set(1);
            return null;
         }
      });
      Thread.sleep(500);
      if (pass.get() != 1) fail();
   }

   public void testPost() throws Exception
   {
      TestSingleWorkerThread worker = new TestSingleWorkerThread();
      worker.start();
      final MutableInteger pass = new MutableInteger(0);
      worker.postTask(new Task()
      {
         public Object run() throws Exception
         {
            pass.set(1);
            return null;
         }
      });
      Thread.sleep(100);
      if (pass.get() != 1) fail();
      Thread.sleep(500);
   }

   public void testManyPosts() throws Exception
   {
      TestSingleWorkerThread worker = new TestSingleWorkerThread();
      worker.start();
      worker.postTask(new Task()
      {
         public Object run() throws Exception
         {
            Thread.sleep(500);
            return null;
         }
      });
      final MutableInteger pass = new MutableInteger(0);
      worker.postTask(new Task()
      {
         public Object run() throws Exception
         {
            pass.set(pass.get() + 1);
            return null;
         }
      });
      worker.postTask(new Task()
      {
         public Object run() throws Exception
         {
            pass.set(pass.get() + 1);
            return null;
         }
      });
      Thread.sleep(1000);
      if (pass.get() != 2) fail();
      Thread.sleep(500);
   }

   public void testTaskIsExecutedAfterIgnoredInterruptInTask() throws Exception
   {
      TestSingleWorkerThread worker = new TestSingleWorkerThread();
      final MutableHolder thread = new MutableHolder(null);
      final MutableInteger pass = new MutableInteger(0);
      worker.postTask(new Task()
      {
         public Object run() throws Exception
         {
            thread.set(Thread.currentThread());

            try
            {
               Thread.sleep(1000);
            }
            catch (InterruptedException x)
            {
               // Swallow the exception and don't restore the interrupted status of the thread
               // Be sure we pass here
               pass.set(1);
            }

            return null;
         }
      });
      Thread.sleep(500);

      // Interrupt the WorkerThread.
      // The task will not restore the status of the thread, thereby allowing it to continue
      Thread foxtrot = (Thread)thread.get();
      foxtrot.interrupt();
      Thread.sleep(1000);
      if (pass.get() != 1) fail();

      // Be sure another Task can be posted and executed on the same worker thread as above
      worker.postTask(new Task()
      {
         public Object run() throws Exception
         {
            thread.set(Thread.currentThread());
            pass.set(2);
            return null;
         }
      });
      Thread.sleep(500);
      if (thread.get() != foxtrot) fail();
      if (pass.get() != 2) fail();
      Thread.sleep(500);
   }

   public void testTaskIsExecutedAfterNotIgnoredInterruptInTask() throws Exception
   {
      TestSingleWorkerThread worker = new TestSingleWorkerThread();
      final MutableHolder thread = new MutableHolder(null);
      final MutableInteger pass = new MutableInteger(0);
      worker.postTask(new Task()
      {
         public Object run() throws Exception
         {
            thread.set(Thread.currentThread());

            try
            {
               Thread.sleep(1000);
            }
            catch (InterruptedException x)
            {
               // Restore the interrupted status of the thread
               Thread.currentThread().interrupt();
               // Be sure we pass here
               pass.set(1);
            }

            return null;
         }
      });
      Thread.sleep(500);

      // Interrupt the WorkerThread.
      // The task will not restore the status of the thread, thereby allowing it to continue
      Thread foxtrot = (Thread)thread.get();
      foxtrot.interrupt();
      Thread.sleep(1000);
      if (pass.get() != 1) fail();

      // Be sure another Task can be posted and executed on a different worker thread
      worker.postTask(new Task()
      {
         public Object run() throws Exception
         {
            thread.set(Thread.currentThread());
            pass.set(2);
            return null;
         }
      });
      Thread.sleep(500);
      if (thread.get() == foxtrot) fail();
      if (pass.get() != 2) fail();
      Thread.sleep(500);
   }

   public void testTaskIsExecutedAfterInterruptInTask() throws Exception
   {
      TestSingleWorkerThread worker = new TestSingleWorkerThread();
      final MutableHolder thread = new MutableHolder(null);
      final MutableInteger pass = new MutableInteger(0);
      worker.postTask(new Task()
      {
         public Object run() throws Exception
         {
            thread.set(Thread.currentThread());

            // Allow InterruptedException to unwind the stack frames
            Thread.sleep(1000);

            return null;
         }
      });
      Thread.sleep(500);

      Thread foxtrot = (Thread)thread.get();
      foxtrot.interrupt();
      Thread.sleep(1000);

      // Be sure another Task can be posted and executed on a different worker thread
      worker.postTask(new Task()
      {
         public Object run() throws Exception
         {
            thread.set(Thread.currentThread());
            pass.set(1);
            return null;
         }
      });
      Thread.sleep(500);
      if (thread.get() == foxtrot) fail();
      if (pass.get() != 1) fail();
      Thread.sleep(500);
   }

   public void testPendingTasksAreExecutedAfterRestart() throws Exception
   {
      TestSingleWorkerThread worker = new TestSingleWorkerThread();
      worker.start();
      worker.postTask(new Task()
      {
         public Object run() throws Exception
         {
            Thread.sleep(500);
            return null;
         }
      });
      final MutableInteger pass = new MutableInteger(0);
      worker.postTask(new Task()
      {
         public Object run() throws Exception
         {
            pass.set(1);
            return null;
         }
      });
      Thread.sleep(250);
      worker.stop();
      Thread.sleep(1000);
      // Be sure 2nd Task not yet executed
      if (pass.get() != 0) fail();
      worker.start();
      Thread.sleep(500);
      if (pass.get() != 1) fail();
      Thread.sleep(500);
   }
}
