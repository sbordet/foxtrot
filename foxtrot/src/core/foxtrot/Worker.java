/**
 * Copyright (c) 2002, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.MenuComponent;
import java.awt.Toolkit;
import java.awt.ActiveEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;

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
		m_queue = (EventQueue)AccessController.doPrivileged(new PrivilegedAction()
		{
			public Object run()
			{
				return Toolkit.getDefaultToolkit().getSystemEventQueue();
			}
		});

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
	 * either by finishing normally or throwing an exception.
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
		private static EventThrowableHandler m_handler;

		static
		{
			// See remarks in java.awt.EventDispatchThread about this property
            String handler = (String)AccessController.doPrivileged(new PrivilegedAction()
			{
				public Object run()
				{
					return System.getProperty("sun.awt.exception.handler");
				}
			});

			if (handler != null && handler.length() > 0)
			{
				try
				{
					Object exceptionHandler = Thread.currentThread().getContextClassLoader().loadClass(handler).newInstance();
					Method method = exceptionHandler.getClass().getMethod("handle", new Class[] {Throwable.class});
					m_handler = new EventThrowableHandler(exceptionHandler, method);
				}
				catch (Throwable ignored) {}
			}
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
					System.out.println("Start dequeueing events from AWT Event Queue:" + this);
				}

				while (!m_task.isCompleted())
				{
					// get next AWT event
					AWTEvent event = m_queue.getNextEvent();

					if (m_debug)
					{
						System.out.println("Event dequeued from AWT Event Queue: " + this + " - " + event);
					}

					try
					{
						dispatch(event);
					}
					catch (Throwable x)
					{
						handleThrowable(x);
					}
				}

				if (m_debug)
				{
					System.out.println("Stop dequeueing events from AWT Event Queue: " + this);
				}
			}
			catch (InterruptedException ignored)
			{
				// Normally the AWT Event Queue is not interrupted.
				// Set again the interrupted flag in any case
				Thread.currentThread().interrupt();
			}
		}

		private void dispatch(AWTEvent event)
		{
			Object src = event.getSource();

			// Dispatch the event
			// In JDK 1.1 events posted using SwingUtilities.invokeLater are subclasses of AWTEvent
			// with source a dummy subclass of Component
			// In JDK 1.2 and superior events posted using SwingUtilities.invokeLater are subclasses
			// of AWTEvent that implement ActiveEvent with source the Toolkit implementation
			if (src instanceof Component)
			{
				Component c = (Component)src;
				c.dispatchEvent(event);
			}
			else if (src instanceof MenuComponent)
			{
				MenuComponent mc = (MenuComponent)src;
				mc.dispatchEvent(event);
			}
			else if (event instanceof ActiveEvent)
			{
				// ActiveEvent is JDK 1.1 is in package java.awt.peer, while in JDK 1.2 and superior
				// is in package java.awt. Just change the import statement to compile against JDK 1.1
				ActiveEvent e = (ActiveEvent)event;
				e.dispatch();
			}
			else
			{
				System.err.println("Unable to dispatch event: " + event);
			}
		}

		private void handleThrowable(Throwable x)
		{
			if (m_handler == null)
			{
				System.err.println("Exception occurred during event dispatching:");
				x.printStackTrace();
			}
			else
			{
				m_handler.handle(x);
			}
		}
	}

	private static class EventThrowableHandler
	{
		private Object m_handler;
		private Method m_method;

		private EventThrowableHandler(Object handler, Method method)
		{
			m_handler = handler;
			m_method = method;
		}

		private void handle(Throwable t)
		{
			try
			{
				m_method.invoke(m_handler, new Object[] {t});
			}
			catch (Throwable x)
			{
				if (m_debug) {x.printStackTrace();}

				System.err.println("Exception occurred during event exception handling:");
				t.printStackTrace();
			}
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

					synchronized (t)
					{
						// Mark the task as completed
						t.completed();
					}

					// In any case, completed or interrupted, remove the task
					removeTask();

					// Needed in case that no events are posted on the AWT Event Queue:
					// posting this one we exit from pumpEvents(), that is waiting in
					// EventQueue.getNextEvent
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
