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
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * @version $Revision$
 */
public class WaitDialogExample extends JFrame implements Runnable
{
    public static void main(String[] args)
    {
        WaitDialogExample example = new WaitDialogExample();
        example.setVisible(true);
    }

    private Timer timer;

    public WaitDialogExample()
    {
        super("Wait Dialog Example");

        JButton button = new JButton("Long Task");
        Container cp = getContentPane();
        cp.add(button);

        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                startLongTask();
            }
        });

        timer = new Timer(500, null);
        timer.setInitialDelay(500);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setSize(600, 400);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension size = getSize();
        int x = (screen.width - size.width) >> 1;
        int y = (screen.height - size.height) >> 1;
        setLocation(x, y);
    }

    private void startLongTask()
    {
        WaitDialog dialog = new WaitDialog(this);
        dialog.setSize(300, 200);
        dialog.setLocationRelativeTo(this);

        Thread thread = new Thread(this);
        thread.start();

        timer.addActionListener(dialog);
        timer.start();

        dialog.setVisible(true);

        timer.removeActionListener(dialog);
        timer.stop();
    }

    public void run()
    {
        try
        {
            Thread.sleep(15000);
        }
        catch (InterruptedException ignored)
        {
        }

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                setVisible(false);
            }
        });
    }

    private static class WaitDialog extends JDialog implements ActionListener
    {
        private int count;
        private JLabel label;

        public WaitDialog(Frame owner)
        {
            super(owner, "Wait", true);
            label = new JLabel();

            Container cp = getContentPane();
            cp.setLayout(new GridBagLayout());
            cp.add(label);

            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            setSize(300, 200);
            setLocationRelativeTo(owner);
        }

        public void actionPerformed(ActionEvent e)
        {
            ++count;
            label.setText("I've been called " + count + " times");
        }
    }
}
