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
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;

import foxtrot.Task;
import foxtrot.Worker;

/**
 * @version $Revision$
 */
public class ThreadInterruptExample extends JFrame implements Runnable
{
    private boolean running;
    private JButton button;
    private ServerSocket socket;
    private Thread foxtrot;

    public static void main(String[] args) throws Exception
    {
        ThreadInterruptExample example = new ThreadInterruptExample();
        example.setVisible(true);
    }

    public ThreadInterruptExample() throws Exception
    {
        super("Foxtrot Example");
        button = new JButton("Click me !");
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                onButtonClicked();
            }
        });

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Container c = getContentPane();
        c.setLayout(new GridBagLayout());
        c.add(button);

        setSize(300, 200);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension size = getSize();
        int x = (screen.width - size.width) >> 1;
        int y = (screen.height - size.height) >> 1;
        setLocation(x, y);

        // Setup a ServerSocket
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    private void onButtonClicked()
    {
        if (!running)
        {
            running = true;
            button.setText("Interrupt");
            button.repaint();
            execute();
            button.setText("Run me !");
            button.repaint();
            running = false;
        }
        else
        {
            if (foxtrot != null) foxtrot.interrupt();
            button.setText("Run me !");
            button.repaint();
        }
    }

    private void execute()
    {
        try
        {
            Worker.post(new Task()
            {
                public Object run() throws Exception
                {
                    foxtrot = Thread.currentThread();
                    Socket socket = new Socket("localhost", 8872);
                    OutputStream os = socket.getOutputStream();
                    os.write("Hello !".getBytes());
                    os.write('\n');
                    // Wait for answer
                    InputStream is = socket.getInputStream();
                    int read = -1;
                    while ((read = is.read()) >= 0)
                    {
                        String message = new String(new byte[]{(byte)read});
                        System.out.print(message.toUpperCase());
                        if (read == '\n')
                        {
                            // End of message
                            break;
                        }
                    }
                    foxtrot = null;
                    return null;
                }
            });
        }
        catch (Exception x)
        {
            x.printStackTrace();
        }
    }

    public void run()
    {
        try
        {
            socket = new ServerSocket(8872);
            while (true)
            {
                Socket client = socket.accept();
                InputStream is = client.getInputStream();
                int read = -1;
                while ((read = is.read()) >= 0)
                {
                    if (read == '\n')
                    {
                        // End of message, wait...
                        synchronized (this)
                        {
                            wait();
                        }
                    }
                    else
                    {
                        String message = new String(new byte[]{(byte)read});
                        System.out.print(message.toLowerCase());
                    }
                }
            }
        }
        catch (IOException x)
        {
            x.printStackTrace();
        }
        catch (InterruptedException x)
        {
            x.printStackTrace();
        }
    }
}
