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
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;

import foxtrot.Job;
import foxtrot.Worker;

/**
 * @version $Revision$
 */
public class ProgressMonitorExample extends JFrame
{
    public static void main(String[] args)
    {
        ProgressMonitorExample example = new ProgressMonitorExample();
        example.setVisible(true);
    }

    private int length = 10;
    private int count = 0;
    private JButton button;


    public ProgressMonitorExample() throws HeadlessException
    {
        super("ProgressMonitor Example");

        button = new JButton("Start Long Task");
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                start();
            }
        });

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        Container c = getContentPane();
        c.setLayout(new GridBagLayout());
        c.add(button);

        setSize(300, 200);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension size = getSize();
        int x = (screen.width - size.width) >> 1;
        int y = (screen.height - size.height) >> 1;
        setLocation(x, y);
    }

    private void start()
    {
        final ProgressMonitor monitor = new ProgressMonitor(this, "Message", "Note", 0, length);
        monitor.setProgress(0);
        monitor.setMillisToDecideToPopup(0);

//      foxtrotStartJob(monitor);
        threadStartJob(monitor);
    }

    private void foxtrotStartJob(final ProgressMonitor monitor)
    {
        Worker.post(new Job()
        {
            public Object run()
            {
                try
                {
                    for (int i = 0; i < length; ++i)
                    {
                        ++count;
                        Thread.sleep(1000);
                        SwingUtilities.invokeLater(new Runnable()
                        {
                            public void run()
                            {
                                monitor.setProgress(count);
                                monitor.setNote(monitor.getNote() + " - " + count);
                            }
                        });
                    }
                    count = 0;
                    monitor.setProgress(0);
                    monitor.setNote("Note");
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        public void run()
                        {
                            monitor.close();
                        }
                    });
                }
                catch (InterruptedException ignored)
                {
                }
                return null;
            }
        });
    }

    private void threadStartJob(final ProgressMonitor monitor)
    {
        new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    for (int i = 0; i < length; ++i)
                    {
                        ++count;
                        Thread.sleep(1000);
                        SwingUtilities.invokeLater(new Runnable()
                        {
                            public void run()
                            {
                                monitor.setProgress(count);
                                monitor.setNote(monitor.getNote() + " - " + count);
                            }
                        });
                    }
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        public void run()
                        {
                            monitor.close();
                        }
                    });
                }
                catch (InterruptedException ignored)
                {
                }
            }
        }).start();
    }
}
