/**
 * Copyright (c) 2002, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.test;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import foxtrot.Task;
import foxtrot.Worker;

/**
 * Tests for the Foxtrot framework
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision$
 */
public class FoxtrotTest
{
	public static void main(String[] args) throws Exception
	{
		final JFrame frame = new JFrame("Foxtrot Test");
		Container pane = frame.getContentPane();
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		frame.setSize(800, 600);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension size = frame.getSize();
		int x = (screen.width - size.width) >> 1;
		int y = (screen.height - size.height) >> 1;
		frame.setLocation(x, y);
		frame.setVisible(true);

		final FoxtrotTest test = new FoxtrotTest();

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				System.out.println("Start Testing");
				int count = 0;
				int success = 0;
//				Object[] arg = new Object[0];
				Object[] arg = new Object[] {frame};
				Method[] methods = test.getClass().getMethods();
				for (int i = 0; i < methods.length; ++i)
				{
					Method method = methods[i];
					String name = method.getName();
					if (name.startsWith("test") &&
						method.getReturnType() == Void.TYPE &&
//						method.getParameterTypes().length == 0)
						method.getParameterTypes().length == 1 &&
						method.getParameterTypes()[0] == JFrame.class)
					{
						try
						{
							++count;
							System.out.println("Executing Test '" + name + "'");
							method.invoke(test, arg);
							++success;
							System.out.println("Test '" + name + "' SUCCESSFUL");
						}
						catch (IllegalAccessException x) {x.printStackTrace();}
						catch (InvocationTargetException x)
						{
							Throwable t = x.getTargetException();
							System.out.println("Test '" + name + "' FAILED !");
							t.printStackTrace();
						}
					}
				}

				String result = "Testing Completed: " + count + " Tests - " + success + " successful - " + (count - success) + " failed";
				JLabel label = new JLabel(result);
				frame.getContentPane().add(label);
				frame.getContentPane().validate();
				System.out.println(result);
			}
		});
	}

	private JButton createButton(JFrame frame, String text)
	{
		JButton button = new JButton(text);
		JPanel pane = (JPanel)frame.getContentPane();
		pane.add(button);
		pane.revalidate();
		return button;
	}

	public void testThreads(JFrame frame) throws Exception
	{
		JButton button = createButton(frame, "Threads");

		button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				post(new Task()
				{
					public Object run() throws Exception
					{
						// Check that I'm NOT in the AWT Event Dispatch Thread
						if (SwingUtilities.isEventDispatchThread()) {throw new RuntimeException();}

						// Check that I'm really in the Foxtrot Worker Thread
						if (Thread.currentThread().getName().indexOf("Foxtrot") < 0) {throw new RuntimeException();}

						return null;
					}
				});
			}
		});

		button.doClick();
	}

	public void testBlocking(JFrame frame) throws Exception
	{
		JButton button = createButton(frame, "Blocking");

		button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				final long sleep = 1000;

				long start = System.currentTimeMillis();
				post(new Task()
				{
					public Object run() throws Exception
					{
						Thread.sleep(sleep);
						return null;
					}
				});
				long end = System.currentTimeMillis();

				if (end - start < sleep) {throw new RuntimeException();}
			}
		});

		button.doClick();
	}

	public void testDequeueing(JFrame frame) throws Exception
	{
		final String original = "Dequeueing";
		final JButton button = createButton(frame, original);

		button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				final String text = "Dequeueing - Text Change";

				// This event is dequeued only after Worker.post
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						button.setText(text);
					}
				});

				// Check that the text is still the original one
				if (!button.getText().equals(original)) {throw new RuntimeException();}

				post(new Task()
				{
					public Object run() throws Exception
					{
						Thread.sleep(1000);
						return null;
					}
				});

				// Check that the event posted with invokeLater has been dequeued
				if (!button.getText().equals(text)) {throw new RuntimeException();}
			}
		});

		button.doClick();
	}

	public void testException(JFrame frame) throws Exception
	{
		JButton button = createButton(frame, "Exception");

		button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					Worker.post(new Task()
					{
						public Object run() throws Exception
						{
							throw new NumberFormatException();
						}
					});
					throw new RuntimeException();
				}
				catch (NumberFormatException x) {}
				catch (Throwable x) {throw new RuntimeException();}
			}
		});

		button.doClick();
	}

	public void testError(JFrame frame) throws Exception
	{
		JButton button = createButton(frame, "Error");

		button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					Worker.post(new Task()
					{
						public Object run() throws Exception
						{
							throw new NoClassDefFoundError();
						}
					});
					throw new RuntimeException();
				}
				catch (NoClassDefFoundError x) {}
				catch (Throwable x) {throw new RuntimeException();}
			}
		});

		button.doClick();
	}

	public void testAWTException(JFrame frame) throws Exception
	{
		JButton button = createButton(frame, "AWT Exception");

		button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						throw new NullPointerException();
					}
				});

				final long sleep = 1000;
				long start = System.currentTimeMillis();
				post(new Task()
				{
					public Object run() throws Exception
					{
						Thread.sleep(sleep);
						return null;
					}
				});
				long end = System.currentTimeMillis();

				// Must check that really elapsed all the time
				if (end - start < sleep) {throw new RuntimeException();}
			}
		});

		button.doClick();
	}

	public void testAWTError(JFrame frame) throws Exception
	{
		JButton button = createButton(frame, "AWT Error");

		button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
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
				post(new Task()
				{
					public Object run() throws Exception
					{
						Thread.sleep(sleep);
						return null;
					}
				});
				long end = System.currentTimeMillis();

				// Must check that really elapsed all the time
				if (end - start < sleep) {throw new RuntimeException();}
			}
		});

		button.doClick();
	}

	public void testRecursion(JFrame frame) throws Exception
	{
		final JButton button = createButton(frame, "Recursion");

		button.addActionListener(new ActionListener()
		{
			private int m_max = 5;
			private int m_count = 1;
			public void actionPerformed(ActionEvent e)
			{
				if (m_count <= m_max)
				{
					long start = System.currentTimeMillis();
					post(new Task()
					{
						public Object run() throws Exception
						{
							SwingUtilities.invokeLater(new Runnable()
							{
								public void run()
								{
									++m_count;
									button.doClick();
								}
							});

							Thread.sleep(1000 * m_count);

							return null;
						}
					});
					long end = System.currentTimeMillis();
					System.out.println("Time: " + (end - start));
				}
			}
		});

		button.doClick();
	}

	public void testPostInInvokeLater(JFrame frame) throws Exception
	{
		final JButton button = createButton(frame, "Worker.post in invokeLater");

		button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				post(new Task()
				{
					public Object run() throws Exception
					{
						for (int i = 0; i < 5; ++i)
						{
							SwingUtilities.invokeLater(new Runnable()
							{
								public void run()
								{
									post(new Task()
									{
										public Object run() throws Exception
										{
											Thread.sleep(1000);
											return null;
										}
									});
								}
							});
						}

						Thread.sleep(2000);
						return null;
					}
				});
			}
		});

		button.doClick();
	}

	public void testLoad(JFrame frame) throws Exception
	{
		// Run this test with a very small heap: with 2 mega of heap there are no out of memory errors
		// (less than 2 mega is defaulted to 2 mega, it seems)
		// java -Xms2m -Xmx2m -verbosegc

		JButton button = createButton(frame, "Load");

		button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				post(new Task()
				{
					public Object run() throws Exception
					{
						return null;
					}
				});
			}
		});

		int count = 1000;
		for (int i = 0; i < count; ++i)
		{
			button.doClick();
		}
	}

	public void testPerformance(JFrame frame) throws Exception
	{
		JButton button = createButton(frame, "Performance");
		int count = 100;

		ActionListener listener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					Worker.post(new Task()
					{
						public Object run() throws Exception
						{
							Thread.sleep(100);
							return null;
						}
					});
				}
				catch (Exception x) {throw new RuntimeException();}
			}
		};
		button.addActionListener(listener);

		long start = System.currentTimeMillis();
		for (int i = 0; i < count; ++i)
		{
			button.doClick();
		}
		long end = System.currentTimeMillis();
		System.out.println("Worker.post performance: " + count + " calls in " + (end - start) + " ms");

		button.removeActionListener(listener);

		listener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				try	{Thread.sleep(100);}
				catch (InterruptedException x) {}
			}
		};
		button.addActionListener(listener);

		start = System.currentTimeMillis();
		for (int i = 0; i < count; ++i)
		{
			button.doClick();
		}
		end = System.currentTimeMillis();
		System.out.println("Plain Listener performance: " + count + " calls in " + (end - start) + " ms");

	}

	public void testSecurity(JFrame frame) throws Exception
	{
		// This test must be run under a security manager, and with the policy file present in the src/etc directory

        JButton button = createButton(frame, "Security");

		button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				// No permission to read the file from here
				try
				{
					Worker.post(new Task()
					{
						public Object run() throws Exception
						{
							return System.getProperty("user.dir");
						}
					});
					throw new RuntimeException();
				}
				catch (SecurityException x) {}
				catch (Throwable x) {x.printStackTrace(); throw new RuntimeException();}
			}
		});

		button.doClick();
	}

	private Object post(Task task)
	{
		try
		{
			return Worker.post(task);
		}
		catch (RuntimeException x) {throw x;}
		catch (Throwable x) {x.printStackTrace(); throw new RuntimeException();}
	}
}
