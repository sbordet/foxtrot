<?php include 'header.php';?>

<tr><td align="center" colspan="3">
<a href="http://foxtrot.sourceforge.net"><img class="nav-btn-en" hspace="10" src="../images/cnvhome.gif"/></a>
<a href="toc.php"><img class="nav-btn-en" hspace="10" src="../images/cnvup.gif"/></a>
<img class="nav-btn-dis" hspace="10" src="../images/cnvprev.gif"/>
<img class="nav-btn-dis" hspace="10" src="../images/cnvnext.gif"/>
</td></tr>

<tr><td class="date" colspan="3">Last Updated: $Date$</td></tr>

<tr><td class="documentation">

<h2>Foxtrot: synchronous solution</h2>
<p>The <b>Foxtrot</b> framework is based on a different approach than asynchronous solutions. While a worker thread is still
used to execute time-consuming tasks, SwingUtilities.invokeLater() is not used.<br>
The main problem of the asynchronous solution is that it lets the listener returns immediately. This is done to allow the
Event Dispatch Thread to dequeue the next event and to process it.<br>
Foxtrot instead, blocks the Event Dispatch Thread <em>inside</em> the listener, but tells the Event Dispatch Thread to
do a job that it knows very well: dequeuing events from the Event Queue and processing them.</p>
<p>This approach is similar to the one used to display modal dialogs in AWT or Swing; unfortunately all classes that allow
dialogs to block the Event Dispatch Thread inside a listener while continuing to dequeue and to process events are private
to package java.awt. However, AWT and Swing architects left enough room to achieve exactly the same behavior, just with
a little more coding necessary in the Foxtrot implementation.</p>
<p>The main idea behind the synchronous solution is to block inside the time-consuming listener, after having
delegated the worker thread to execute the time-consuming code, but telling the Event Dispatch Thread to continue to
dequeue events from the Event Queue and to process them.</p>
<p>Take a look at the code below that uses the Foxtrot API.</p>
<p>Let's concentrate on the button's listener: the first statement, as in the freeze example, changes
the text of the button and thus posts a repaint event on the queue.<br>
The next statement uses the Foxtrot API to create a Task and to post it to the worker queue, using the Worker class.
The Worker.post() method is blocking and must be called from the Event Dispatch Thread. <br>
The Worker class starts a single worker thread (when initialized) to execute time-consuming tasks,
and has a single worker queue where time-consuming tasks are enqueued before being executed.<br>
When a Task is posted, the worker thread executes the code contained in Task.run() and contemporarly the Event Dispatch
Thread is told to dequeue events from the Event Queue. In the Event Queue it finds the repaint event posted by the
first setText(), and processes it.<br>
The Worker.post() method does not return until the time-consuming task is finished.
When the time-consuming task is finished, the Worker class tells the Event Dispatch Thread to stop dequeueing events
from the Event Queue, and allows the Worker.post() method to return. When the Worker.post() method returns, the listener calls
the second setText() and returns, allowing the Event Dispatch Thread to do its job in the normal way.<br>
This is why we called this solution synchronous: the event listener does not return, and the code the
listener is supposed to execute is run synchronously.</p>
<p>Let's compare this solution with the asynchronous ones, to see how it resolves their drawbacks:
<ul>
<li>Simple exception handling: exceptions can be caught and rethrown by the listener. No need of chained if-else
statements. The only drawback is that the Worker.post() method requires to always catch Exception.
<li>Note the symmetry: the two setText() are both inside the listener.
<li>No get() method: whether you expect a result or not, eventual exceptions will be rethrown.
<li>The code after the time-consuming task is independent of the time-consuming task itself. This allow refactoring of
Worker.post() calls, and it is possible to execute different code after Worker.post() depending on the place from where we
want to execute the time-consuming task.
<li>Code written after Worker.post() is always executed afterwards. This greatly improve code readability and semplicity.
No worries about code executed after Worker.post().
<li>No nesting of Worker.post() is necessary, just 2 consecutive Worker.post() calls
</ul>
</p>
<p>We have heavily used Foxtrot in a large Swing application, and the code greatly improved from the use of the
Foxtrot framework.</p>
<h3>Acknowledgements</h3>
<p>Marco Cravero had the first idea of exploring how dialogs work to find a better solution for using threads in Swing.<br>
Simone Bordet implemented the Foxtrot API following this idea.</p>
<br><br>
<pre>
public class FoxtrotExample extends JFrame
{
   public static void main(String[] args)
   {
      FoxtrotExample example = new FoxtrotExample();
      example.setVisible(true);
   }

   public FoxtrotExample()
   {
      super("Foxtrot Example");

      final JButton button = new JButton("Take a nap !");
      button.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            button.setText("Sleeping...");

            try
            {
               String text = (String)Worker.post(new Task()
               {
                  public Object run() throws Exception
                  {
                     Thread.sleep(10000);
                     return "Slept !";
                  }
               });
            }
            catch (Exception x) ...

            button.setText(text);

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
