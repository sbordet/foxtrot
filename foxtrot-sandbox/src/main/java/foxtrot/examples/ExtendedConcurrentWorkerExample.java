/**
 * Copyright (c) 2002-2008, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.examples;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import foxtrot.ConcurrentWorker;
import foxtrot.Job;

/**
 * @version $Revision$
 */
public class ExtendedConcurrentWorkerExample extends JFrame
{
    private JButton button;
    private JTextField textField;
    private JTextField secondTextField;

    public static void main(String[] args)
    {
        ExtendedConcurrentWorkerExample example = new ExtendedConcurrentWorkerExample();
        example.setVisible(true);
    }

    public ExtendedConcurrentWorkerExample()
    {
        super("ConcurrentWorker Foxtrot Example");
        init();
    }

    private void init()
    {
        button = new JButton("Start long task");
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                startLongTask();
            }
        });

        textField = new JTextField(10);
        textField.addFocusListener(new FocusAdapter()
        {

            public void focusLost(FocusEvent aEvent)
            {
                startLongTask();
                aEvent.getComponent().repaint();
            }
        });

        secondTextField = new JTextField(10);

        Container content = getContentPane();
        content.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        content.add(new JLabel("firstField"), c);
        c.gridx = 1;
        content.add(textField, c);

        c.gridx = 0;
        c.gridy = 1;
        content.add(new JLabel("secondField"), c);
        c.gridx = 1;
        content.add(secondTextField, c);

        c.gridx = 1;
        c.gridy = 2;
        content.add(button, c);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(300, 200);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension size = getSize();
        int x = (screen.width - size.width) >> 1;
        int y = (screen.height - size.height) >> 1;
        setLocation(x, y);
    }

    private void startLongTask()
    {
        disableFrame();

        final CancelDialog dialog = new CancelDialog();

        System.out.println("Posting task...");
        ConcurrentWorker.post(new Job()
        {
            public Object run()
            {
                Thread workerThread = Thread.currentThread();
                edtShowDialog(dialog, workerThread);

                try
                {
                    // Start very long task, calling a server
                    Thread.sleep(5000);
                    System.out.println("Task ended");
                }
                catch (InterruptedException x)
                {
                    System.out.println("Task interrupted");
                }

                edtHideDialog(dialog);

                return null;
            }
        });
        System.out.println("Task finished");

        enableFrame();
    }

    private void enableFrame()
    {
        button.setEnabled(true);
    }

    private void disableFrame()
    {
        button.setEnabled(false);
    }

    private void edtShowDialog(final CancelDialog dialog, final Thread workerThread)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                System.out.println("Showing the dialog");
                 dialog.display(workerThread);
            }
        });
    }

    private void edtHideDialog(final CancelDialog dialog)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                System.out.println("Hiding the dialog");
                dialog.undisplay();
            }
        });
    }

    private void cancelLongTask(CancelDialog dialog, final Thread workerThread)
    {
        dialog.cancelling();

        ConcurrentWorker.post(new Job()
        {
            public Object run()
            {
                // Call the server to cancel the task
                sleep(1000);
                if (workerThread != null) workerThread.interrupt();
                return null;
            }
        });

        dialog.undisplay();

        enableFrame();
    }

    private void sleep(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    private class CancelDialog extends JDialog
    {
        private JButton button;
        private volatile Thread workerThread;

        public CancelDialog()
        {
            super(ExtendedConcurrentWorkerExample.this, "Cancel Task ?", true);
            init();
        }

        private void init()
        {
            button = new JButton("Cancel");
            button.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    cancelLongTask(CancelDialog.this, workerThread);
                }
            });
            Container content = getContentPane();
            content.setLayout(new GridBagLayout());
            content.add(button);

            setSize(200, 150);
            setLocationRelativeTo(ExtendedConcurrentWorkerExample.this);
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        }

        private void cancelling()
        {
            button.setEnabled(false);
            button.setText("Cancelling...");
        }

        public void display(Thread workerThread)
        {
            this.workerThread = workerThread;
            setVisible(true);
        }

        public void undisplay()
        {
            this.workerThread = null;
            if (isVisible()) setVisible(false);
        }
    }
}
