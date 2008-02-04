/**
 * Copyright (c) 2002-2006, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.examples;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import foxtrot.AsyncTask;
import foxtrot.AsyncWorker;

/**
 * @version $Revision$
 */
public class AsyncExample extends JFrame
{
    public static void main(String[] args)
    {
        AsyncExample example = new AsyncExample();
        example.setVisible(true);
    }

    private JLabel tasksSending;
    private JLabel tasksSent;

    public AsyncExample()
    {
        super("Async Foxtrot Example");
        init();
    }

    private void init()
    {
        tasksSending = new JLabel("Sending: ");
        tasksSent = new JLabel("Sent:    ");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(2, 0));
        panel.add(tasksSending);
        panel.add(tasksSent);

        Container c = getContentPane();
        c.setLayout(new BorderLayout());
        c.add(panel, BorderLayout.NORTH);

        JButton button = new JButton("Send !");
        c.add(button, BorderLayout.SOUTH);
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                send();
            }
        });

        setSize(300, 200);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension size = getSize();
        int x = (screen.width - size.width) >> 1;
        int y = (screen.height - size.height) >> 1;
        setLocation(x, y);
    }

    public void send()
    {
        tasksSending.setText(tasksSending.getText() + ".");
        AsyncWorker.post(new AsyncTask()
        {
            public Object run() throws Exception
            {
                // Send the data to a system that is asynchronous
                // Assume it will take time to send
                Thread.sleep(2000);
                return null;
            }

            public void finish()
            {
                try
                {
                    // Be sure the send has been performed successfully
                    getResultOrThrow();
                    tasksSent.setText(tasksSent.getText() + ".");
                }
                catch (Exception x)
                {
                    // Show the problem to the user
                    x.printStackTrace();
                }
            }
        });
    }
}
