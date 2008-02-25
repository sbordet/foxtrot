/**
 * Copyright (c) 2002-2006, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.examples;

import java.awt.AWTEvent;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import foxtrot.Job;
import foxtrot.Worker;
import foxtrot.pumps.EventFilter;
import foxtrot.pumps.EventFilterable;
import foxtrot.pumps.SunJDK14ConditionalEventPump;

/**
 * @version $Revision$
 */
public class OwnerModalDialog extends JDialog
{
    public final Object taskLock = new Object();

    public OwnerModalDialog(Frame owner, String title) throws HeadlessException
    {
        super(owner, title, false);
    }

    public OwnerModalDialog(Dialog owner, String title) throws HeadlessException
    {
        super(owner, title, false);
    }

    public void show()
    {
//       startFilteringEvents();
        super.show();

        waitUntilHidden();

        JDialog dialog = new JDialog(this, "Conditional", true);
        dialog.setBounds(100, 100, 100, 100);
        dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        dialog.setVisible(true);

//       stopFilteringEvents();
    }

    private void waitUntilHidden()
    {
        Worker.post(new Job()
        {
            public Object run()
            {
                synchronized (taskLock)
                {
                    try
                    {
                        System.out.println("Start Waiting...");
                        taskLock.wait();
                        System.out.println("Stopped Waiting");
                    }
                    catch (InterruptedException e)
                    {
                    }
                }
                return null;
            }
        });
    }

    public void hide()
    {
        super.hide();

        synchronized (taskLock)
        {
            taskLock.notifyAll();
        }
    }

    private void stopFilteringEvents()
    {
        System.out.println("OwnerModalDialog.stopFilteringEvents");
        ((EventFilterable)Worker.getEventPump()).setEventFilter(null);
    }

    private void startFilteringEvents()
    {
        System.out.println("OwnerModalDialog.startFilteringEvents");
        ((EventFilterable)Worker.getEventPump()).setEventFilter(new EventFilter()
        {
            public boolean accept(AWTEvent event)
            {
                System.out.println("[EVENT] " + event);
                Object eventSource = event.getSource();
                if (eventSource == getOwner())
                {
                    boolean eventTypeBlocked = eventTypeBlocked(event);
                    if (eventTypeBlocked || windowEventRequiresAction(event))
                    {
                        toFront();
                    }
                    return !eventTypeBlocked;
                }
                return true;
            }
        });
    }

    private boolean eventTypeBlocked(AWTEvent event)
    {
        return event instanceof KeyEvent
                || event instanceof MouseEvent;
    }

    private boolean windowEventRequiresAction(AWTEvent event)
    {
        if (!(event instanceof WindowEvent)) return false;
        return event.getID() == WindowEvent.WINDOW_ACTIVATED
                || event.getID() == WindowEvent.WINDOW_GAINED_FOCUS;
    }

    public static void main(String[] args) throws IllegalAccessException,
            UnsupportedLookAndFeelException, InstantiationException, ClassNotFoundException
    {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

        Worker.setEventPump(new SunJDK14ConditionalEventPump()
        {
            protected Boolean canPumpSequencedEvent(AWTEvent event)
            {
                return Boolean.TRUE;
            }
        });

        final JFrame frame = new JFrame("Main Frame");
        frame.getContentPane().add(new JButton(new AbstractAction("Dialog")
        {
            public void actionPerformed(ActionEvent e)
            {
                System.out.println("About to show dialog");
                JDialog dialog = new OwnerModalDialog(frame, "Inner Dialog");
                dialog.getContentPane().add(new JButton("Test"));
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.pack();
                dialog.show();
                System.out.println("Closed Dialog");
            }
        }));
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        frame.pack();
        frame.show();
    }
}
