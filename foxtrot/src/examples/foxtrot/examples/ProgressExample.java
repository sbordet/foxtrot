package foxtrot.examples;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.JPanel;

import foxtrot.Worker;
import foxtrot.Job;

/**
 *
 * @version $Revision$
 */
public class ProgressExample extends JFrame
{
	private JButton button;
	private JProgressBar bar;
	private boolean running;
	private boolean taskInterrupted;

	public static void main(String[] args)
	{
		ProgressExample example = new ProgressExample();
		example.setVisible(true);
	}

	public ProgressExample()
	{
		super("Foxtrot Example");

		button = new JButton("Run Task !");
		button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				onButtonClick();
			}
		});

		bar = new JProgressBar();
        bar.setStringPainted(true);

		setDefaultCloseOperation(EXIT_ON_CLOSE);

		Container c = getContentPane();
		c.setLayout(new GridBagLayout());

		JPanel p = new JPanel(new BorderLayout(20,20));
		p.add(bar, BorderLayout.NORTH);
		p.add(button, BorderLayout.SOUTH);

		c.add(p);

		setSize(300,200);

		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension size = getSize();
		int x = (screen.width - size.width) >> 1;
		int y = (screen.height - size.height) >> 1;
		setLocation(x, y);
	}

	private void onButtonClick()
	{
		if (!running)
		{
			running = true;

			// We will execute a long operation, change the text signaling
			// that the user can interrupt the operation
			button.setText("Cancel");

			// getData() will block until the heavy operation is finished
			ArrayList list = getData();

			// getData() finished or was interrupted ?
			// If was interrupted we get back a null list
			if (list == null)
			{
				// Task was interrupted, return quietly, we already cleaned up
				return;
			}
			else
			{
				// Task completed successfully, do whatever useful with the list
				// For example, populate a JComboBox
				javax.swing.DefaultComboBoxModel model = new javax.swing.DefaultComboBoxModel(list.toArray());
				// The reader will finish this part :)
			}

			// Restore anyway the button's text
			button.setText("Run Task !");

			// Restore anyway the interrupt status for another call
			setTaskInterrupted(false);

			// We're not running anymore
			running = false;
		}
		else
		{
			// Here if we want to interrupt the Task

			// Interrupt the task
			setTaskInterrupted(true);

			// Restore the button text to the previous value
			button.setText("Run Task !");
		}
	}

	private ArrayList getData()
	{
		return (ArrayList)Worker.post(new Job()
		{
			public Object run()
			{
				ArrayList list = new ArrayList();
				StringBuffer buffer = new StringBuffer();

				// A repetitive operation that updates a progress bar.
				int max = 100;
				for (int i = 0; i < max; ++i)
				{
					// Simulate a heavy operation to retrieve data
					try	{Thread.sleep(250);}
					catch (InterruptedException ignored) {}

					// Populate the data structure
					Object data = new Object();
					list.add(data);

					// Prepare the progress bar string
					buffer.setLength(0);
					buffer.append("Step ").append(i).append(" of ").append(max);

					if (isTaskInterrupted())
					{
						buffer.append(" - Interrupted !");
						update(i, max, buffer.toString());
						break;
					}
					else
					{
						// Update the progress bar
						update(i, max, buffer.toString());
					}
				}

				if (isTaskInterrupted())
				{
					// Task is interrupted, clean the half-populated data structure
					// and return from the Task
					list.clear();
					return null;
				}
				else
				{
					return list;
				}
			}
		});
	}

	private void update(final int index, final int max, final String string)
	{
		// This method is called by the Foxtrot Worker thread, but I want to
		// update the GUI, so I use SwingUtilities.invokeLater, as the Task
		// is not finished yet.

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				bar.setMaximum(max);
				bar.setValue(index);
				bar.setString(string);
			}
		});
	}

	private synchronized boolean isTaskInterrupted()
	{
		// Called from the Foxtrot Worker Thread.
		// Must be synchronized, since the variable taskInterrupted is accessed from 2 threads.
		// While it is easier just to change the variable value without synchronizing, it is possible
		// that the Foxtrot worker thread doesn't see the change (it may cache the value of the variable
		// in a registry).
		return taskInterrupted;
	}

	private synchronized void setTaskInterrupted(boolean value)
	{
		// Called from the AWT Event Dispatch Thread.
		// See comments above on why it must be synchronized.
		taskInterrupted = value;
	}
}
