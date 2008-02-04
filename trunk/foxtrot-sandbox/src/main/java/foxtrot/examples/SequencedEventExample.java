/**
 * Copyright (c) 2002-2006, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.examples;

import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JFrame;
import javax.swing.JTextField;

import foxtrot.Job;
import foxtrot.Worker;

/**
 * @version $Revision$
 */
public class SequencedEventExample
{
    public static void main(String[] args)
    {
        final JFrame frame1 = new JFrame("FRAME 1");
        frame1.setSize(300, 200);
        frame1.setLocation(200, 150);
        frame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Container cp = frame1.getContentPane();
        cp.setLayout(new GridLayout(1, 0, 10, 0));
        JTextField field1 = new JTextField();
        JTextField field2 = new JTextField();
        cp.add(field1);
        cp.add(field2);

        field1.addFocusListener(new FocusAdapter()
        {
            public void focusLost(FocusEvent e)
            {
                System.out.println("[Foxtrot] start");
                Worker.post(new Job()
                {
                    public Object run()
                    {
                        sleep(2000);
                        return null;
                    }
                });
                System.out.println("[Foxtrot] end");
            }
        });

        frame1.setVisible(true);
    }

    private static void sleep(long time)
    {
        try
        {
            Thread.sleep(time);
        }
        catch (InterruptedException x)
        {
        }
    }
}
