/**
 * Copyright (c) 2002, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

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

            // Check that the value is still the original one
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

                  // Nested Worker.post()
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
      invokeTest(new Runnable()
      {
         public void run()
         {
            int max = 5;
            MutableInteger counter = new MutableInteger(0);

            long start = System.currentTimeMillis();

            postFromInvokeLater(counter, max);

            long end = System.currentTimeMillis();

            // We used the default WorkerThread, be sure task times were summed
            long sum = 0;
            for (int i = 0; i < max; ++i) sum += i + 1;
            sum *= 1000;

            long epsilon = 100;
            long elapsed = end - start;
            if (elapsed > sum + epsilon) fail();
            if (elapsed < sum - epsilon) fail();
         }
      });
   }

   private void postFromInvokeLater(final MutableInteger counter, final int maxDeep)
   {
      final int deep = counter.get() + 1;

      Job job = new Job()
      {
         public Object run()
         {
            // Here I recurse on calling Worker.post(), that is: I am in event0, that calls
            // Worker.post(task1) that dequeues event1 that calls Worker.post(task2) that dequeues event2
            // that calls Worker.post(task3) and so on.
            // Since Worker.post() calls are synchronous, the Worker.post(task1) call returns
            // only when the task1 is finished AND event1 is finished; but event1 is finished
            // only when Worker.post(task2) returns; Worker.post(task2) returns only when task2
            // is finished AND event2 is finished; but event2 is finished only when Worker.post(task3)
            // returns; and so on.
            // The total execution time is dependent on the implementation of the WorkerThread:
            // if it enqueues tasks (like the default implementation) we have (roughly) that:
            // time(task1) = time(task3) + time(task2)
            // even if to execute only task1 taskes a very short time.
            // If the worker implementation uses parallel threads to execute tasks, then (roughly):
            // time(task1) = max(time(task3), time(task2)).
            // In general, it is a bad idea to use Foxtrot this way: you probably need an asynchronous
            // solution.
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  counter.set(deep);
                  if (deep < maxDeep) postFromInvokeLater(counter, maxDeep);
               }
            });

            sleep(1000 * deep);

            return null;
         }
      };

      // job1 sleeps 1 s, but Worker.post(job1) returns after event1 is finished.
      // event1 runs Worker.post(job2); job2 sleeps 2 s, but Worker.post(job2) returns after event2 is finished.
      // event2 runs Worker.post(job3); job3 sleeps 3 s, but Worker.post(job3) returns after event3 is finished.
      // event3 runs Worker.post(job4); job4 sleeps 4 s, but Worker.post(job4) returns after event4 is finished.
      // event4 runs Worker.post(job5); job5 sleeps 5 s.
      // Worker.post(job1) returns after 1+2+3+4+5 s since the default implementation enqueues tasks.
      Worker.post(job);
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
