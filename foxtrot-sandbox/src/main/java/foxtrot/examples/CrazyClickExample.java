/**
 * Copyright (c) 2002-2008, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.examples;

import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import foxtrot.Job;
import foxtrot.Worker;
import foxtrot.utils.EventListenerProxy;

/**
 * @version $Revision$
 */
public class CrazyClickExample extends JFrame implements ActionListener
{
    public static void main(String[] args)
    {
        CrazyClickExample example = new CrazyClickExample();
        example.setVisible(true);
    }

    public CrazyClickExample()
    {
        super("Crazy Click Example");

        JButton button = new JButton("Show Dialog");
        button.addActionListener(this);

        Container content = getContentPane();
        content.setLayout(new GridBagLayout());
        content.add(button);

        setSize(800, 600);
        setLocation(100, 100);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    public void actionPerformed(ActionEvent e)
    {
        MyDialog dialog = new MyDialog(this);
        dialog.setVisible(true);
    }

    private static class MyDialog extends JDialog implements ActionListener
    {
        public MyDialog(Frame owner)
        {
            super(owner, "FIRST", true);
            JButton child = new JButton("Child");
/*
         child.addActionListener(this);
*/
            child.addActionListener((ActionListener)EventListenerProxy.create(ActionListener.class, this));

            JButton cancel = new JButton("Cancel");
/*
         cancel.addActionListener(new ActionListener()
         {
            public void actionPerformed(ActionEvent e)
            {
               MyDialog.this.dispose();
            }
         });
*/
            cancel.addActionListener((ActionListener)EventListenerProxy.create(ActionListener.class, new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    MyDialog.this.dispose();
                }
            }));

            Container content = getContentPane();
            content.setLayout(new FlowLayout());
            content.add(child);
            content.add(cancel);
            setLocationRelativeTo(owner);
            pack();
        }

        public void actionPerformed(ActionEvent e)
        {
            ((Component)e.getSource()).setEnabled(false);
            Worker.post(new Job()
            {
                public Object run()
                {
                    try
                    {
                        Thread.sleep(2000);
                    }
                    catch (InterruptedException x)
                    {
                    }
                    return null;
                }
            });

            JOptionPane.showMessageDialog(this, "Child Dialog", "SECOND", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
