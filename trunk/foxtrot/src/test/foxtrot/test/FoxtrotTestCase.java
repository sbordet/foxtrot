/**
 * Created by IntelliJ IDEA.
 * User: BORDET
 * Date: Nov 14, 2002
 * Time: 4:52:28 PM
 * To change this template use Options | File Templates.
 */

package foxtrot.test;

import javax.swing.SwingUtilities;

import junit.framework.TestCase;

public class FoxtrotTestCase extends TestCase
{
   private Throwable throwable;

   protected FoxtrotTestCase(String s)
   {
      super(s);
   }

   protected void invokeTest(final Runnable run) throws Exception
   {
      final Object lock = new Object();
      final MutableInteger barrier = new MutableInteger(0);

      synchronized (lock)
      {
         throwable = null;
      }

      SwingUtilities.invokeLater(new Runnable()
      {
         public void run()
         {
            synchronized (lock)
            {
               while (barrier.get() < 1)
               {
                  try
                  {
                     lock.wait(10);
                  }
                  catch (InterruptedException ignored)
                  {
                  }
               }
            }

            try
            {
               run.run();
            }
            catch (Throwable x)
            {
               synchronized (lock)
               {
                  throwable = x;
               }
            }

            synchronized (lock)
            {
               lock.notifyAll();
            }
         }
      });

      synchronized (lock)
      {
         barrier.set(1);
         lock.wait();

         if (throwable instanceof Error) throw (Error)throwable;
         if (throwable instanceof Exception) throw (Exception)throwable;
      }
   }

   protected void sleep(long ms)
   {
      try
      {
         Thread.sleep(ms);
      }
      catch (InterruptedException ignored)
      {
      }
   }
}
