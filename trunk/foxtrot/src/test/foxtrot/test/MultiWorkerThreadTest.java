package foxtrot.test;

import foxtrot.workers.MultiWorkerThread;
import foxtrot.Job;

/**
 *
 * @version $Revision$
 */
public class MultiWorkerThreadTest extends FoxtrotTestCase
{
   public MultiWorkerThreadTest(String s)
   {
      super(s);
   }

   public void testThreads() throws Exception
   {
      invokeTest(new Runnable()
      {
         public void run()
         {
            final MutableHolder thread = new MutableHolder(null);
            MultiWorkerThread worker = new MultiWorkerThread()
            {
               public void run()
               {
                  thread.set(Thread.currentThread());
                  super.run();
               }
            };
            worker.start();

            final MutableHolder runner = new MutableHolder(null);
            worker.postTask(new Job()
            {
               public Object run()
               {
                  runner.set(Thread.currentThread());
                  return null;
               }
            });

            sleep(1000);

            if (thread.get() == runner.get()) fail();
            String threadName = ((Thread)thread.get()).getName();
            String runnerName = ((Thread)runner.get()).getName();
            if (!runnerName.startsWith(threadName) || runnerName.equals(threadName)) fail();
         }
      });
   }

   public void testLongBeforeShort() throws Exception
   {
      invokeTest(new Runnable()
      {
         public void run()
         {
            MultiWorkerThread worker = new MultiWorkerThread();
            worker.start();

            // A long Task followed by a short one.
            final long longDelay = 5000;
            final MutableInteger longer = new MutableInteger(0);
            worker.postTask(new Job()
            {
               public Object run()
               {
                  longer.set(1);
                  sleep(longDelay);
                  longer.set(2);
                  return null;
               }
            });
            final long shortDelay = 2000;
            final MutableInteger shorter = new MutableInteger(0);
            worker.postTask(new Job()
            {
               public Object run()
               {
                  shorter.set(1);
                  sleep(shortDelay);
                  shorter.set(2);
                  return null;
               }
            });

            sleep(shortDelay / 2);
            if (shorter.get() != 1) fail();
            if (longer.get() != 1) fail();

            sleep(shortDelay);
            if (shorter.get() != 2) fail();
            if (longer.get() != 1) fail();

            sleep(longDelay);
            if (longer.get() != 2) fail();
         }
      });
   }
}
