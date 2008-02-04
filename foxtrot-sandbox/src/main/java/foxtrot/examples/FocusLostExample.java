package foxtrot.examples;

import java.awt.BorderLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JFrame;
import javax.swing.JTextField;

import foxtrot.Job;
import foxtrot.Worker;
import foxtrot.pumps.SunJDK141ConditionalEventPump;

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
        setLayout(new BorderLayout(10, 10));
        getContentPane().add(textField, BorderLayout.NORTH);
        getContentPane().add(new JTextField(20), BorderLayout.SOUTH);
        textField.addFocusListener(new FocusListener()
        {
            public void focusGained(FocusEvent e)
            {
            }

            public void focusLost(FocusEvent e)
            {
/*
                JDialog dialog = new JDialog(FocusLostExample.this, "Dialog", true);
                dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                dialog.setSize(400, 300);
                dialog.setVisible(true);
*/
                Worker.setEventPump(new SunJDK141ConditionalEventPump());
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
}
