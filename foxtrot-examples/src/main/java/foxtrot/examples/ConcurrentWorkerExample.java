/**
 * Copyright (c) 2002-2006, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.examples;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import foxtrot.ConcurrentWorker;
import foxtrot.Job;

/**
 * @version $Revision$
 */
public class ConcurrentWorkerExample extends JFrame
{
    private JButton button;

    public static void main(String[] args)
    {
        ConcurrentWorkerExample example = new ConcurrentWorkerExample();
        example.setVisible(true);
    }

    public ConcurrentWorkerExample()
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

        Container content = getContentPane();
        content.setLayout(new GridBagLayout());
        content.add(button);

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
            super(ConcurrentWorkerExample.this, "Cancel Task ?", true);
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
            setLocationRelativeTo(ConcurrentWorkerExample.this);
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