/**
 * Copyright (c) 2002, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot;

import java.awt.EventQueue;
import java.awt.FoxtrotConditional;
import java.awt.Toolkit;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.swing.SwingUtilities;

/**
 * The class that execute time-consuming {@link Task}s. <p>
 * It is normally used in event listeners that must execute time-consuming operations without
 * freezing the Swing GUI. <br>
 * Usage example (simplified from the Foxtrot examples):
 * <pre>
 * JButton button = new JButton("Take a nap!");
 * button.addActionListener(new ActionListener()
 * {
 *    public void actionPerformed(ActionEvent e)
 *    {
 *       try
 *       {
 *          Worker.post(new Task()
 *          {
 *             public Object run() throws Exception
 *             {
 *                 Thread.sleep(10000);
 *                 return null;
 *             }
 *          });
 *       }
 *       catch (Exception ignored) {}
 *    }
 * });
 * <pre>
 *
 * @see Task
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision$
 */
public class Worker
{
	private static Link m_current;
	private static Thread m_thread;
	private static Object m_lock = new Object();
	private static EventQueue m_queue;

	private static final boolean m_debug = false;

	static
	{
		m_queue = Toolkit.getDefaultToolkit().getSystemEventQueue();

		m_thread = new Thread(new Runner(), "Foxtrot Worker Thread");
		// Daemon, since if someone loads this class without using it,
		// the JVM should shut down on main thread's termination
		m_thread.setDaemon(true);
		m_thread.start();
		if (m_debug)
		{
			System.out.println("Foxtrot Worker initialized successfully");
		}
	}

	/**
	 * Cannot be instantiated, use static methods only.
	 */
	private Worker() {}

	/**
	 * Enqueues the given task to be executed in the worker thread. <br>
	 * This method can be called only from the Event Dispatch Thread, and blocks until the task has been executed,
	 * either by finishing normally or throwing an exception. <br>
	 * Even in case of AWT events that throw RuntimeExceptions or Errors, this method will not return until
	 * the Task is finished.
	 * @throws IllegalStateException if the method is not called from the Event Dispatch Thread
	 */
	public static synchronized Object post(Task task) throws Exception
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			throw new IllegalStateException("This method can be called only from the AWT Event Dispatch Thread");
		}

		addTask(task);

		// Must create a new object every time, since from pumpEvents() I can pump an event that ends up calling
		// again post, and thus coming here again.
		EventPump pump = new EventPump(task);
		// The following line blocks until the task has been executed, or it is interrupted
		pump.pumpEvents();

		return task.getResult();
	}

	private static void addTask(Task t)
	{
		// Synchronized since the variable m_current is accessed from two threads.
		// See getTask()
		synchronized (m_lock)
		{
			if (hasTasks())
			{
				// Append the given task at the end of the queue
				Link item = m_current;
				while (item.m_next != null)
				{
					item = item.m_next;
				}
				item.m_next = new Link(t);

				if (m_debug)
				{
					System.out.println("Worker queue not empty, enqueueing task:" + t);
				}
			}
			else
			{
				// Add the given task
				m_current = new Link(t);

				if (m_debug)
				{
					System.out.println("Worker queue empty, adding task:" + t);
				}

				m_lock.notifyAll();
			}
		}
	}

	private static Task getTask() throws InterruptedException
	{
		// Synchronized since the variable m_current is accessed from two threads.
		// See addTask()
		synchronized (m_lock)
		{
			while (!hasTasks())
			{
				if (m_debug)
				{
					System.out.println("Waiting for tasks...");
				}

				m_lock.wait();
			}

			// Taking the current task
			Task t = m_current.m_task;

			if (m_debug)
			{
				System.out.println("Returning posted task:" + t);
			}

			return t;
		}
	}

	private static void removeTask()
	{
		synchronized (m_lock)
		{
			m_current = m_current.m_next;
		}
	}

	private static boolean hasTasks()
	{
		synchronized (m_lock)
		{
			return m_current != null;
		}
	}

	private static void stop()
	{
		if (m_debug)
		{
			System.out.println("Ending Foxtrot Worker");
		}
		m_thread.interrupt();
	}

	/**
	 * The class that dequeues events from the EventQueue
	 */
	private static class EventPump
	{
		private static Method m_pumpMethod;

		static
		{
			try
			{
				Class dispatchThreadClass = ClassLoader.getSystemClassLoader().loadClass("java.awt.EventDispatchThread");
				Class conditionalClass = ClassLoader.getSystemClassLoader().loadClass("java.awt.Conditional");
				m_pumpMethod = dispatchThreadClass.getDeclaredMethod("pumpEvents", new Class[] {conditionalClass});
				m_pumpMethod.setAccessible(true);
			}
			catch (Exception x) {x.printStackTrace();}

			// See remarks for use of this property in java.awt.EventDispatchThread
			String property = "sun.awt.exception.handler";
			String handler = System.getProperty(property);
			if (handler == null) {System.setProperty(property, AWTThrowableHandler.class.getName());}
		}

		private Task m_task;

		private EventPump(Task t)
		{
			m_task = t;
		}

		private void pumpEvents()
		{
			try
			{
				if (m_debug)
				{
					System.out.println("Start dequeueing events from AWT Event Queue: " + this);
				}

				// Invoke EventDispatchThread.pumpEvents(new FoxtrotConditional(m_task));
				// This call blocks until the task is completed
				m_pumpMethod.invoke(Thread.currentThread(), new Object[] {new FoxtrotConditional(m_task)});
			}
			catch (InvocationTargetException x)
			{
				Throwable t = x.getTargetException();
				System.err.println("Foxtrot - Exception occurred during event dispatching:");
				t.printStackTrace();

				System.err.println("Foxtrot - WARNING: uncaught exception during event dispatching, Task is still running !");

				// Rethrow. This will exit from Worker.post with a runtime exception or an error, and
				// the original event pump will take care of it. Beware that the Task will continue to run
				// It should never happen: the contract of the Worker.post method is strong: don't return
				// until the Task has finished. We will use awt exception handler to enforce this contract.
				if (t instanceof RuntimeException) {throw (RuntimeException)t;}
				else {throw (Error)t;}
			}
			catch (Throwable x)
			{
				x.printStackTrace();
				System.err.println("Foxtrot - WARNING: uncaught exception in Foxtrot code, Task is still running !");
			}
			finally
			{
				if (m_debug)
				{
					System.out.println("Stop dequeueing events from AWT Event Queue: " + this);
				}
			}
		}
	}

	/**
	 * A class that handles RuntimeExceptions or Errors thrown during dispatching of AWT events. <p>
	 * The name of this class is used as a value of the property <code>sun.awt.exception.handler</code>, and
	 * the AWT event dispatch mechanism calls it when an unexpected exception or error is thrown during
	 * event dispatching. <br>
	 * If the user specified a different exception handler, this one will not be used.
	 */
	public static class AWTThrowableHandler
	{
		/**
		 * The callback method invoked by the AWT event dispatch mechanism when an unexpected
		 * exception or error is thrown during event dispatching. <br>
		 * It just logs the exception.
		 */
		public void handle(Throwable t)
		{
			System.err.println("Foxtrot - Exception occurred during event dispatching:");
			t.printStackTrace();
		}
	}

	private static class Runner implements Runnable
	{
		private static Runnable EMPTY_EVENT = new Runnable() {public final void run() {}};

		public void run()
		{
			if (m_debug)
			{
				System.out.println("Foxtrot Worker Thread started");
			}

			while (!m_thread.isInterrupted())
			{
				try
				{
					final Task t = getTask();

					if (m_debug)
					{
						System.out.println("Got posted task:" + t);
					}

					try
					{
						// Run the Task
						Object obj = AccessController.doPrivileged(new PrivilegedExceptionAction()
						{
							public Object run() throws Exception
							{
								return t.run();
							}
						}, t.getSecurityContext());

						t.setResult(obj);
					}
					catch (PrivilegedActionException x)
					{
						t.setThrowable(x.getException());
					}
					catch (Throwable x)
					{
						t.setThrowable(x);
					}

					// Mark the task as completed
					t.completed();

					// In any case, completed or interrupted, remove the task
					removeTask();

					// Needed in case that no events are posted on the AWT Event Queue:
					// posting this one we exit from pumpEvents(), that is waiting in
					// EventQueue.getNextEvent()
					SwingUtilities.invokeLater(EMPTY_EVENT);
				}
				catch (InterruptedException x)
				{
					if (m_debug)
					{
						System.out.println("Foxtrot Worker Thread interrupted, shutting down");
					}
					break;
				}
			}
		}
	}

	private static class Link
	{
		private Link m_next;
		private Task m_task;

		private Link(Task t)
		{
			m_task = t;
		}
	}
}
