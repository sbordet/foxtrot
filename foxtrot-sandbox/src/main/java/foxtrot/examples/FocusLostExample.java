package foxtrot.examples;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextField;

import foxtrot.Job;
import foxtrot.Worker;

/**
 * @version $Revision$ $Date$
 */
public class FocusLostExample extends JFrame
{
    public static void main(String[] args)
    {
        FocusLostExample example = new FocusLostExample();
        example.init();
        example.setVisible(true);
    }

    private JTextField textField;

    private void init()
    {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        textField = new JTextField(10);
        getContentPane().setLayout(new BorderLayout(10, 10));
        getContentPane().add(textField, BorderLayout.NORTH);
        getContentPane().add(new JTextField(20), BorderLayout.SOUTH);
        textField.addFocusListener(new FocusListener()
        {
            public void focusGained(FocusEvent e)
            {
            }

            public void focusLost(FocusEvent e)
            {
//                EventQueue.invokeLater(new Runnable()
//                {
//                    public void run()
//                    {
//                        System.out.println("FOCUS LOST");
//                        JDialog dialog = new WaitDialog(FocusLostExample.this);
//                        dialog.setVisible(true);
//                    }
//                });

                Worker.post(new Job()
                {
                    public Object run()
                    {
                        try
                        {
                            Thread.sleep(1000);
                        }
                        catch (InterruptedException x)
                        {
                            Thread.currentThread().interrupt();
                        }
                        return null;
                    }
                });
            }
        });
        setSize(800, 600);
    }

    private class WaitDialog extends JDialog
    {
        private WaitDialog(Frame owner) throws HeadlessException
        {
            super(owner, "Dialog", true);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setSize(400, 300);
            setLocationRelativeTo(null);
        }

        public void setVisible(boolean visible)
        {
            if (visible) startWait();
            super.setVisible(visible);
        }

        private void startWait()
        {
            new Thread(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        System.out.println("DIALOG WAITING");
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException x)
                    {
                    }
                    EventQueue.invokeLater(new Runnable()
                    {
                        public void run()
                        {
                            System.out.println("DIALOG HIDING");
                            WaitDialog.this.setVisible(false);
                        }
                    });
                }
            }).start();
        }
    }
}
