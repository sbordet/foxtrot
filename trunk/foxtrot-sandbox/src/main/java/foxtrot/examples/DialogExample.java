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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextField;

/**
 * This example is very similar to the SequencedEventExample, but uses JDialog instead of Worker
 *
 * @version $Revision$
 */
public class DialogExample extends JFrame
{
    public static void main(String[] args)
    {
        DialogExample example = new DialogExample();
        example.setVisible(true);
    }

    public DialogExample()// throws HeadlessException
    {
        super("Dialog Example");

        setSize(300, 300);
//      setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Container cp = getContentPane();
        cp.setLayout(new BorderLayout());
        JTextField field1 = new JTextField();
        JTextField field2 = new JTextField();
        cp.add(field1, BorderLayout.NORTH);
        cp.add(field2, BorderLayout.SOUTH);

        field1.addFocusListener(new FocusAdapter()
        {
            public void focusLost(FocusEvent e)
            {
                JDialog dialog = new JDialog(DialogExample.this, "Dialog", true);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setSize(200, 200);
                dialog.setLocationRelativeTo(DialogExample.this);
                dialog.setVisible(true);
            }
        });
    }
}
