package foxtrot.test;

import javax.swing.SwingUtilities;

import foxtrot.AsyncTask;
import foxtrot.AsyncWorker;
import foxtrot.Task;

/**
 *
 * @version $Revision$
 */
public class AsyncWorkerTest extends FoxtrotTestCase
{
   public AsyncWorkerTest(String s)
   {
      super(s);
   }

   public void testPostAndForget() throws Exception
   {
      invokeTest(new Runnable()
      {
         public void run()
         {
            AsyncWorker.getWorkerThread().postTask(new Task()
            {
               public Object run() throws Exception
               {
                  return null;
               }
            });
         }
      });
   }

   public void testUsage() throws Exception
   {
      invokeTest(new Runnable()
      {
         public void run()
         {
            AsyncWorker.post(new AsyncTask()
            {
               private static final String VALUE = "1000";

               public Object run() throws Exception
               {
                  Thread.sleep(1000);
                  return VALUE;
               }

               public void finish()
               {
                  try
                  {
                     String value = (String)getResultOrThrow();
                     if (!VALUE.equals(value)) fail();
                  }
                  catch (Exception x)
                  {
                     fail();
                  }
               }
            });
         }
      });
   }

   public void testThreads() throws Exception
   {
      invokeTest(new Runnable()
      {
         public void run()
         {
            AsyncWorker.post(new AsyncTask()
            {
               public Object run() throws Exception
               {
                  // Check that I'm NOT in the AWT Event Dispatch Thread
                  if (SwingUtilities.isEventDispatchThread()) fail("Must not be in the Event Dispatch Thread");

                  // Check that I'm really in the Foxtrot Worker Thread
                  if (Thread.currentThread().getName().indexOf("Foxtrot") < 0) fail("Must be in the Foxtrot Worker Thread");

                  return null;
               }

               public void finish()
               {
                  // Check that I'm in the AWT Event Dispatch Thread
                  if (!SwingUtilities.isEventDispatchThread()) fail("Must be in the Event Dispatch Thread");
               }
            });
         }
      });
   }
   
   public void testPostThrowException() throws Exception
   {
      invokeTest(new Runnable()
      {
         public void run()
         {
            final RuntimeException ex = new RuntimeException();
            AsyncTask task = new AsyncTask()
            {
               public Object run() throws Exception
               {
                  throw ex;
               }
               
               public void finish()
               {
                  try
                  {
                     getResultOrThrow();
                     fail("Expected exception");
                  }
                  catch (RuntimeException x)
                  {
                     assertSame(x, ex);
                  }
                  catch (Exception x)
                  {
                     fail("Did not expect checked exception");
                  }
               }
            };
         }
      });
   }
}
