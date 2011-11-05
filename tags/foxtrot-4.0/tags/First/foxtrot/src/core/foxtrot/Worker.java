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
	private static EventQueue m_awtQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();

	private static final boolean m_debug = false;

	static
	{
		m_thread = new Thread(new Runner(), "Foxtrot Worker Thread");
		m_thread.start();
		if (m_debug)
		{
			System.out.println("Foxtrot Worker initialized successfully");
		}
	}

	private static void interrupt()
	{
		if (m_debug)
		{
			System.out.println("Ending Foxtrot Worker");
		}
		m_thread.interrupt();
	}

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
			throw new IllegalStateException("This method can be called only from AWT Thread");
		}

		addTask(task);

		AWTEventDequeuer dequeuer = new AWTEventDequeuer(task);
		// The following line blocks until the task has been executed
		dequeuer.dequeue();

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
				// Add the given task at the end of the queue
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

			// Taking the current task and shifting to the next in the queue
			Link item = m_current;
			m_current = m_current.m_next;
			Task t = item.m_task;
			if (m_debug)
			{
				System.out.println("Returning posted task:" + t);
			}
			return t;
		}
	}

	private static boolean hasTasks()
	{
		synchronized (m_lock)
		{
			return m_current != null;
		}
	}

	private static class AWTEventDequeuer
	{
		private Task m_task;

		private AWTEventDequeuer(Task t)
		{
			m_task = t;
		}

		private void dequeue()
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
					AWTEvent event = m_awtQueue.getNextEvent();

					if (m_debug)
					{
						System.out.println("Event dequeued from AWT:" + event);
					}

					Object src = event.getSource();

					try
					{
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
							ActiveEvent e = (ActiveEvent)event;
							e.dispatch();
						}
						else
						{
							System.err.println("Unable to dispatch event: " + event);
						}
					}
					catch (Throwable x)
					{
						// JDK 1.1 just prints the stack trace, while jdk 1.3 (not sure for jdk 1.2)
						// may plug in a handler for this exception, see EventDispatchThread; for now just print the stack
						System.err.println("Exception occurred during event dispatching:");
						x.printStackTrace();
					}
				}

				if (m_debug)
				{
					System.out.println("Stop dequeueing events from AWT Event Queue");
				}
			}
			catch (InterruptedException ignored)
			{
				/* Normally the AWT Event Queue is not interrupted */
			}
		}
	}

	private static class Runner implements Runnable
	{
		private static Runnable EMPTY_EVENT = new Runnable() {public void run() {}};

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
					Task t = getTask();
					if (m_debug)
					{
						System.out.println("Got posted task:" + t);
					}
					try
					{
						Object obj = t.run();
						t.setResult(obj);
					}
					catch (Exception x)
					{
						t.setException(x);
					}

					// Notify that the task completed
					t.setCompleted(true);

					// Needed in case that no events are posted on the AWT Event Queue:
					// posting this one we exit from dequeue, that is waiting in
					// EventQueue.getNextEvent
					SwingUtilities.invokeLater(EMPTY_EVENT);
				}
				catch (InterruptedException x)
				{
					if (m_debug)
					{
						System.out.println("Foxtrot Worker interrupted");
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
