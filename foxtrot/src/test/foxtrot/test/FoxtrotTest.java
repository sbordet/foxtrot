/**
 * Copyright (c) 2002, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.test;

import java.util.ArrayList;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.SwingUtilities;
import javax.swing.JButton;

import foxtrot.Job;
import foxtrot.Task;
import foxtrot.Worker;

/**
 * Tests for the basic Foxtrot functionality.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision$
 */
public class FoxtrotTest extends FoxtrotTestCase
{
   public FoxtrotTest(String s)
   {
      super(s);
   }

   public void testThreads() throws Exception
   {
      invokeTest(new Runnable()
      {
         public void run()
         {
            Worker.post(new Job()
            {
               public Object run()
               {
                  // Check that I'm NOT in the AWT Event Dispatch Thread
                  if (SwingUtilities.isEventDispatchThread()) fail();

                  // Check that I'm really in the Foxtrot Worker Thread
                  if (Thread.currentThread().getName().indexOf("Foxtrot") < 0) fail();

                  return null;
               }
            });
         }
      });
   }

   public void testBlocking() throws Exception
   {
      invokeTest(new Runnable()
      {
         public void run()
         {
            final long sleep = 1000;

            long start = System.currentTimeMillis();
            Worker.post(new Job()
            {
               public Object run()
               {
                  sleep(sleep);
                  return null;
               }
            });
            long end = System.currentTimeMillis();

            if (end - start < sleep) fail();
         }
      });
   }

   public void testDequeuing() throws Exception
   {
      invokeTest(new Runnable()
      {
         public void run()
         {
            final MutableInteger check = new MutableInteger(0);
            final long sleep = 1000;

            // This event will be dequeued only after Worker.post()
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  check.set(1);
               }
            });

            sleep(2 * sleep);

            // Check that the text is still the original one
            if (check.get() != 0) fail();

            Worker.post(new Job()
            {
               public Object run()
               {
                  sleep(sleep);
                  return null;
               }
            });

            // Check that the event posted with invokeLater has been dequeued
            if (check.get() != 1) fail();
         }
      });
   }

   public void testTaskException() throws Exception
   {
      invokeTest(new Runnable()
      {
         public void run()
         {
            try
            {
               Worker.post(new Task()
               {
                  public Object run() throws NumberFormatException
                  {
                     return new NumberFormatException();
                  }
               });
            }
            catch (NumberFormatException ignored)
            {
            }
            catch (Throwable x)
            {
               fail();
            }
         }
      });
   }

   public void testTaskError() throws Exception
   {
      invokeTest(new Runnable()
      {
         public void run()
         {
            try
            {
               Worker.post(new Job()
               {
                  public Object run()
                  {
                     return new NoClassDefFoundError();
                  }
               });
            }
            catch (NoClassDefFoundError ignored)
            {
            }
            catch (Throwable x)
            {
               fail();
            }
         }
      });
   }

   public void testAWTException() throws Exception
   {
      invokeTest(new Runnable()
      {
         public void run()
         {
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  throw new RuntimeException();
               }
            });

            final long sleep = 1000;
            long start = System.currentTimeMillis();
            Worker.post(new Job()
            {
               public Object run()
               {
                  sleep(sleep);
                  return null;
               }
            });
            long end = System.currentTimeMillis();

            // Must check that really elapsed all the time
            if (end - start < sleep) fail();
         }
      });
   }

   public void testAWTError() throws Exception
   {
      invokeTest(new Runnable()
      {
         public void run()
         {
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  throw new Error();
               }
            });

            final long sleep = 1000;
            long start = System.currentTimeMillis();
            Worker.post(new Job()
            {
               public Object run()
               {
                  sleep(sleep);
                  return null;
               }
            });
            long end = System.currentTimeMillis();

            // Must check that really elapsed all the time
            if (end - start < sleep) fail();
         }
      });
   }

   public void testPostFromTask() throws Exception
   {
      invokeTest(new Runnable()
      {
         public void run()
         {
            final MutableInteger counter = new MutableInteger(0);

            Worker.post(new Job()
            {
               public Object run()
               {
                  counter.set(counter.get() + 1);

                  Worker.post(new Job()
                  {
                     public Object run()
                     {
                        if (counter.get() != 1) fail();

                        counter.set(counter.get() + 1);
                        return null;
                     }
                  });

                  if (counter.get() != 2) fail();

                  counter.set(counter.get() + 1);

                  return null;
               }
            });

            if (counter.get() != 3) fail();
         }
      });
   }

   public void testTaskReuse() throws Exception
   {
      invokeTest(new Runnable()
      {
         public void run()
         {
            final MutableInteger count = new MutableInteger(0);

            Job job = new Job()
            {
               public Object run()
               {
                  count.set(count.get() + 1);
                  return null;
               }
            };

            int times = 2;
            for (int i = 0; i < times; ++i)
            {
               Worker.post(job);
            }

            if (count.get() != times) fail();
         }
      });
   }

   public void testPostFromInvokeLater() throws Exception
   {
      // TODO: understand better this beast and how to test it
      invokeTest(new Runnable()
      {
         public void run()
         {
            MutableInteger counter = new MutableInteger(0);
            postFromInvokeLater(counter);
         }
      });
   }

   private void postFromInvokeLater(final MutableInteger counter)
   {
      long start = System.currentTimeMillis();
      final int deep = counter.get() + 1;

      Job job = new Job()
      {
         public Object run()
         {
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  counter.set(counter.get() + 1);
                  if (counter.get() < 5) postFromInvokeLater(counter);
               }
            });

            sleep(1000 * deep);

            return null;
         }
      };

      Worker.post(job);

      long end = System.currentTimeMillis();
      System.out.println("Time: " + (end - start) + " for job " + job);
   }

   public void testTaskQueueing() throws Exception
   {
      invokeTest(new Runnable()
      {
         public void run()
         {
            final int count = 10;
            final MutableInteger counter = new MutableInteger(0);

            Worker.post(new Job()
            {
               public Object run()
               {
                  for (int i = 0; i < 10; ++i)
                  {
                     SwingUtilities.invokeLater(new Runnable()
                     {
                        public void run()
                        {
                           Worker.post(new Job()
                           {
                              public Object run()
                              {
                                 counter.set(counter.get() + 1);
                                 return null;
                              }
                           });
                        }
                     });
                  }

                  sleep(1000);

                  return null;
               }
            });

            if (counter.get() != count) fail();
         }
      });
   }

   public void testPerformance() throws Exception
   {
      invokeTest(new Runnable()
      {
         public void run()
         {
            JButton button = new JButton();
            int count = 100;
            final long sleep = 100;

            ActionListener listener = new ActionListener()
            {
               public void actionPerformed(ActionEvent e)
               {
                  Worker.post(new Job()
                  {
                     public Object run()
                     {
                        sleep(sleep);
                        return null;
                     }
                  });
               }
            };
            button.addActionListener(listener);

            long start = System.currentTimeMillis();
            for (int i = 0; i < count; ++i) button.doClick();
            long end = System.currentTimeMillis();
            long workerElapsed = end - start;
            System.out.println("Worker.post(Job) performance: " + count + " calls in " + workerElapsed + " ms");

            button.removeActionListener(listener);

            listener = new ActionListener()
            {
               public void actionPerformed(ActionEvent e)
               {
                  sleep(sleep);
               }
            };
            button.addActionListener(listener);

            start = System.currentTimeMillis();
            for (int i = 0; i < count; ++i)
            {
               button.doClick();
            }
            end = System.currentTimeMillis();
            long plainElapsed = end - start;
            System.out.println("Plain Listener performance: " + count + " calls in " + plainElapsed + " ms");

            int percentage = 5;
            if ((workerElapsed - plainElapsed) * 100 > plainElapsed * percentage) fail();
         }
      });
   }

   public void testMemoryLeaks() throws Exception
   {
      invokeTest(new Runnable()
      {
         public void run()
         {
            ArrayList list = new ArrayList();

            int times = 1024;
            for (int i = 0; i < times; ++i)
            {
               try
               {
                  Job job = new FatJob();
                  list.add(job);
                  Worker.post(job);
               }
               catch (OutOfMemoryError x)
               {
                  list.clear();
                  break;
               }
            }

            // Try again, without mantaining jobs alive
            int j = 0;
            for (; j < times; ++j)
            {
               Job job = new FatJob();
               Worker.post(job);
            }

            if (j < times) fail();
         }
      });
   }

   private static class FatJob extends Job
   {
      // An heavy data member to explode the heap
      private byte[] fatty = new byte[1024 * 1024];

      public Object run()
      {
         return null;
      }
   }


}
