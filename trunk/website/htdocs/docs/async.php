<?php include 'header.php';?>

<tr><td align="center" colspan="3">
<a href="http://foxtrot.sourceforge.net"><img class="nav-btn-en" hspace="10" src="../images/cnvhome.gif"/></a>
<a href="toc.php"><img class="nav-btn-en" hspace="10" src="../images/cnvup.gif"/></a>
<img class="nav-btn-dis" hspace="10" src="../images/cnvprev.gif"/>
<img class="nav-btn-dis" hspace="10" src="../images/cnvnext.gif"/>
</td></tr>

<tr><td class="date" colspan="3">Last Updated: $Date$</td></tr>

<tr><td class="documentation">

<h2>Asynchronous solutions</h2>

<p>Solutions for the <a href="freeze.php">GUI freeze problem</a> have been proposed; asynchronous solutions
rely on the combined usage of a worker thread and of SwingUtilities.invokeLater(). We will see in few lines why
they're called asynchronous.</p>
<p>The main idea behind asynchronous solution is to return quickly from the time-consuming listener, after having
delegated the worker thread to do 2 things:
<ul><ul>
<li>Execute the time-consuming task
<li>Post an event to the Event Queue using SwingUtilities.invokeLater
</ul></ul>
</p>
<p>Take a look at the code below that uses the SwingWorker, based on the asynchronous solution.</p>
<p>Let's concentrate on the button's listener: the first statement, as in the freeze example, changes
the text of the button and thus posts a repaint event on the queue.<br>
The next statement create a SwingWorker object and starts it. This operation is quick, and non blocking.
When a SwingWorker is started, a worker thread is also started to execute the code contained in construct();
when the construct() method ends, the finished() method is called in the Event Dispatch Thread using
SwingUtilities.invokeLater().<br>
So we create the SwingWorker, we start it, the listener finishes and returns; the Event Dispatch Thread
can thus dequeue the next event and process it (very likely this event is the one posted by the first statement,
that changes the button's text to "Sleeping...") .<br>
When the worker thread finishes, the finished() method is posted as event in the Event Queue, and again the
Event Dispatch Thread can dequeue it and process it, finally calling finished().<br>
This is why these solutions are called asynchronous: they let the event listener return immediately, and the code the
listener is supposed to execute is run asynchronously, after the listener returns.
</p>
<p>This solution, while resolving the freeze problem, has several drawbacks:
<ul>
<li>Note the ugly exception handling. Even in this simple example, 3 chained if-else statements are required. Furthermore,
the exception handling is done inside the SwingWorker, not inside the listener: there is no simple way to rethrow exceptions
(it is possible with some more coding).
<li>Note the asymmetry: the first setText() is made outside the SwingWorker, the second inside it
<li>Note the confusing get() method: if construct() returns null (because the operation had void return
value - like Thread.sleep), it is easy to forget to call get() to see if any exception was thrown by the
time-consuming code.
<li>What happens if the time-consuming code is the same, but we want to execute 2 different finished() methods
depending on the place from where we want to execute the time-consuming task ?
<li>If some code is written after SwingWorker.start(), it will be always executed <em>before</em> finished().
Thus looking at the code, we see:
<ul>
<li>setText("Sleeping...")
<li>Thread.sleep()
<li>setText("Slept !");
<li>somethingElse()
</ul>
but the real order of execution is:
<ul>
<li>setText("Sleeping...")
<li>somethingElse() [before, concurrently or after Thread.sleep()]
<li>Thread.sleep()
<li>setText("Slept !");
</ul>
making debugging and code readability very difficult.<br>
This is why a golden rule of the SwingWorker is to never put code after SwingWorker.start().
<li>If the code inside finished() requires a new time-consuming operation, a new <em>nested</em> SwingWorker should be used,
making the code complex and obscure, especially regarding the sequence order of the operations executed.
</ul>
</p>
<p>Fortunately, <a href="foxtrot.php">synchronous solution</a> solve these issues.</p>
<br><br>
<pre>
public class AsyncExample extends JFrame
{
   public static void main(String[] args)
   {
      AsyncExample example = new AsyncExample();
      example.setVisible(true);
   }

   public AsyncExample()
   {
      super("SwingWorker Example");

      final JButton button = new JButton("Take a nap !");
      button.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            button.setText("Sleeping...");

            new SwingWorker()
            {
               protected Object construct() throws Exception
               {
                  Thread.sleep(10000);
                  return "Slept !";
               }
               protected void finished()
               {
                  try
                  {
                     String text = (String)get();
                     button.setText(text);
                  }
                  catch (InterruptedException ignored) {}
                  catch (InvocationTargetException x)
                  {
                     // Do exception handling
                     Throwable t = x.getException();
                     if (t instanceof InterruptedException) ...
                     else if (t instanceof RuntimeException) ...
                     else if (t instanceof Error) ...
                  }
               }
            }.start();

            somethingElse();
         }
      });

      setDefaultCloseOperation(EXIT_ON_CLOSE);

      Container c = getContentPane();
      c.setLayout(new GridBagLayout());
      c.add(button);

      setSize(300,200);

      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      Dimension size = getSize();
      int x = (screen.width - size.width) >> 1;
      int y = (screen.height - size.height) >> 1;
      setLocation(x, y);
   }
}
</pre>

</td></tr>

<?php include 'footer.php';?>
