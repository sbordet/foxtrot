/**
 * Copyright (c) 2002, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.test;

import java.lang.reflect.Method;

import javax.swing.SwingUtilities;

import foxtrot.ConditionalEventPump;
import foxtrot.EventPump;
import foxtrot.Task;

/**
 * Tests for EventPumps.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision$
 */
public class EventPumpTest extends FoxtrotTestCase
{
   public EventPumpTest(String s)
   {
      super(s);
   }

   public void testConditionalEventPump() throws Exception
   {
      ConditionalEventPump pump = new ConditionalEventPump();

      testPumpEventsBlocks(pump);
      testPumpEventsDequeues(pump);
      tesPumpEventsOnThrowException(pump);
      tesPumpEventsOnThrowError(pump);
   }

   /**
    * Verifies that EventPump.pumpEvents(Task) blocks until the Task is completed
    */
   private void testPumpEventsBlocks(final EventPump pump) throws Exception
   {
      // We force this thread ("main") to block until the test is finished
      invokeTest(new Runnable()
      {
         public void run()
         {
            final Task task = new Task()
            {
               public Object run() throws Exception
               {
                  return null;
               }
            };

            final long delay = 5000;

            // I enqueue another event to stop the task
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  sleep(delay);
                  setTaskCompleted(task);
               }
            });

            // Now I start the event pump, the events above must be dequeued.
            long start = System.currentTimeMillis();
            pump.pumpEvents(task);
            long stop = System.currentTimeMillis();

            if (stop - start <= delay) fail();
         }
      });
   }

   /**
    * Verifies that AWT events are dequeued by EventPump.pumpEvents(Task)
    */
   private void testPumpEventsDequeues(final EventPump pump) throws Exception
   {
      // We force this thread ("main") to block until the test is finished
      invokeTest(new Runnable()
      {
         public void run()
         {
            final MutableInteger count = new MutableInteger(0);

            // I enqueue an event, for now it waits since thread "main"
            // does not dequeue it (I used invokeAndWait)
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  count.set(count.get() + 1);
               }
            });

            final Task task = new Task()
            {
               public Object run() throws Exception
               {
                  return null;
               }
            };

            // I enqueue another event to stop the task
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  setTaskCompleted(task);
               }
            });

            // Now I start the event pump, the events above must be dequeued.
            pump.pumpEvents(task);

            if (count.get() != 1) fail();
         }
      });
   }

   /**
    * Verifies that EventPump.pumpEvents(Task) does not return in case of runtime exceptions
    */
   private void tesPumpEventsOnThrowException(final EventPump pump) throws Exception
   {
      // We force this thread ("main") to block until the test is finished
      invokeTest(new Runnable()
      {
         public void run()
         {
            // I enqueue an event, for now it waits since thread "main"
            // does not dequeue it (I used invokeAndWait)
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  throw new RuntimeException();
               }
            });

            final Task task = new Task()
            {
               public Object run() throws Exception
               {
                  return null;
               }
            };

            // I enqueue another event to stop the task
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  setTaskCompleted(task);
               }
            });

            try
            {
               // Now I start the event pump, the events above must be dequeued.
               pump.pumpEvents(task);
            }
            catch (RuntimeException x)
            {
               fail();
            }
         }
      });
   }

   /**
    * Verifies that EventPump.pumpEvents(Task) does not return in case of errors
    */
   private void tesPumpEventsOnThrowError(final EventPump pump) throws Exception
   {
      // We force this thread ("main") to block until the test is finished
      invokeTest(new Runnable()
      {
         public void run()
         {
            // I enqueue an event, for now it waits since thread "main"
            // does not dequeue it (I used invokeAndWait)
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  throw new Error();
               }
            });

            final Task task = new Task()
            {
               public Object run() throws Exception
               {
                  return null;
               }
            };

            // I enqueue another event to stop the task
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                  setTaskCompleted(task);
               }
            });

            try
            {
               // Now I start the event pump, the events above must be dequeued.
               pump.pumpEvents(task);
            }
            catch (Error x)
            {
               fail();
            }
         }
      });
   }

   private void setTaskCompleted(Task task)
   {
      try
      {
         Method completed = Task.class.getDeclaredMethod("completed", new Class[0]);
         completed.setAccessible(true);
         completed.invoke(task, new Object[0]);
      }
      catch (Throwable x)
      {
         x.printStackTrace();
         fail();
      }
   }
}
