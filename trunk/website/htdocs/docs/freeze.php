<?php include 'header.php';?>

<tr><td align="center" colspan="3">
<a href="http://foxtrot.sourceforge.net"><img class="nav-btn-en" hspace="10" src="../images/cnvhome.gif"/></a>
<a href="toc.php"><img class="nav-btn-en" hspace="10" src="../images/cnvup.gif"/></a>
<img class="nav-btn-dis" hspace="10" src="../images/cnvprev.gif"/>
<img class="nav-btn-dis" hspace="10" src="../images/cnvnext.gif"/>
</td></tr>

<tr><td class="date" colspan="3">Last Updated: $Date$</td></tr>

<tr><td class="documentation">

<h2>The GUI freeze problem</h2>

<p>
When you write Swing applications, you show a GUI to the user; the user clicks on some components (buttons, menus, etc.)
to perform the desired action.<br>
The code that executes the action is written in event listeners, and event listeners are always executed in the
<b>Event Dispatch Thread</b>.<br>
The Event Dispatch Thread is responsible of taking one event and processing it; the processing involves calling
the event listeners, that are then executed. If an event listener requires a long time to be executed, then the
Event Dispatch Thread cannot process the next event, that will wait in the Event Queue.<br>
If the pending event is a repaint event, the GUI cannot be repainted, so it appears to be <b>frozen</b>.<br>
So if you resize your window, if you overlap it with another window, if you click on other components, all these
events are queued but not processed until the time-consuming listener has finished. <br>
The user feels the application has hung.<br>
When the time-consuming listener finishes, all pending events are processed, and if they quick to execute (like
repaint events) it appears they're are executed like a storm.</p>
<p>Take a look at the following code, or look at the Foxtrot examples for code samples.</p>
<p>Let's concentrate on the button's listener: the first statement changes the text of the button. This
cause JButton to post a repaint event to the Event Queue, that is not be executed until this listener finishes.<br>
But the listener is waiting for 10 seconds, so what happens is that the button remains pressed, and its text doesn't
change. When the 10 seconds are elapsed, the text of the button is changed again, thus posting another repaint
event to the Event Queue, but still, this listener is not finished so no events are dequeued and processed.<br>
When the listener finishes, on the way back Swing takes care of repainting the button as not pressed, and the pending
events in the Event Queue get finally a chance to be executed.<br>
Being repaint events, they're executed quickly, so first the button text will be change to "Sleeping...", and immediately
after to "Slept !", too quick for the eye to see.<br>
During the 10 seconds, the GUI was <b>frozen</b>.</p>
<pre>
public class SimpleExample extends JFrame
{
   public static void main(String[] args)
   {
      SimpleExample example = new SimpleExample();
      example.setVisible(true);
   }

   public SimpleExample()
   {
      super("Foxtrot Example");

      final JButton button = new JButton("Take a nap !");
      button.addActionListener(new ActionListener()
      {
         public void actionPerformed(ActionEvent e)
         {
            button.setText("Sleeping...");
            try {Thread.sleep(10000);}
            catch (Exception ignored) {}
            button.setText("Slept !");
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

</p>

</td></tr>

<?php include 'footer.php';?>
