<?php include 'header.php';?>

<h2>Foxtrot's synchronous solution: Worker</h2>
<p>The <b>Foxtrot</b> framework is based on a different approach than asynchronous solutions. While a worker thread is still
used to execute time-consuming tasks, <tt>SwingUtilities.invokeLater()</tt> is not used.<br />
The main problem of the asynchronous solution is that it lets the listener continue during the execution of the task;
in the most common cases, the listener returns immediately. This is done to allow the
Event Dispatch Thread to dequeue the next event and process it, so that the GUI does not appear to be frozen.<br />
In contrast, Foxtrot lets the Event Dispatch Thread enter but not return from the listener method; instead, it re-routes the
Event Dispatch Thread to continue dequeuing events from the Event Queue and processing them. Once the worker thread
has finished, the Event Dispatch Thread is re-routed again, continuing the execution of the listener method (in the most
common cases, just returning from the listener method).</p>
<p>This approach is similar to the one used to display modal dialogs in AWT or Swing; unfortunately all classes that allow
dialogs to re-route the Event Dispatch Thread inside a listener to continue dequeueing and processing events are private
to package <tt>java.awt</tt>. However, AWT and Swing architects left enough room to achieve exactly the same behavior,
just with a little more coding necessary in the Foxtrot implementation.</p>
<p>The main idea behind the synchronous solution is to prevent the Event
Dispatch Thread from returning from the time-consuming listener, while
having the worker thread executing the time consuming code, and the
Event Dispatch Thread continuing dequeuing and processing events from
the Event Queue. As soon as the worker thread is done with the task execution, the Event
Dispatch Thread will resume its execution of the listener method, and eventually return. That's why
these solution are synchronous: the code in the event listener and the code of the task are executed sequentially,
as the appear in the code.</p>
<p>Take a look at the code below that uses the Foxtrot API.</p>
<p>Let's concentrate on the button's listener (the <tt>actionPerformed()</tt> method): the first statement, as in
the freeze example, changes the text of the button and thus posts a repaint event to the queue.<br />
The next statement uses the Foxtrot API to create a <tt>Task</tt> and post it to the worker queue, using the
<tt>foxtrot.Worker</tt> class.
The <tt>Worker.post()</tt> method is blocking and must be called from the Event Dispatch Thread. <br />
When initialized, the <tt>Worker</tt> class starts a single worker thread to execute time-consuming tasks,
and has a single worker queue where time-consuming tasks are queued before being executed.<br />
When a <tt>Task</tt> is posted, the worker thread executes the code contained in <tt>Task.run()</tt>
and the Event Dispatch Thread is told to contemporarly dequeue events from the Event Queue.
On the Event Queue it finds the repaint event posted by the first <tt>setText()</tt> invocation, and processes it.<br />
The <tt>Worker.post()</tt> method does not return until the time-consuming task is finished or throws an exception.
When the time-consuming task is finished, the <tt>Worker</tt> class tells the Event Dispatch Thread
to stop dequeueing events from the Event Queue, and to return from the <tt>Worker.post()</tt> method.
When the <tt>Worker.post()</tt> method returns, the second <tt>setText()</tt> is called
and the listener returns, allowing the Event Dispatch Thread to do its job in the normal way.<br />
This is why we call this solution synchronous: the event listener does not return while the code in the
time-consuming task is run by the worker thread.</p>
<p>Let's compare this solution with the asynchronous ones, to see how it resolves their drawbacks:
<ul>
<li>Simple exception handling: exceptions can be caught and rethrown within the listener. No need for chained if-else
statements. The only drawback is that the listener is required to always catch <tt>Exception</tt> from the
<tt>Worker.post()</tt> method when posting <tt>Task</tt>s (this is not necessary when using <tt>Job</tt>s).</li>
<li>Note the symmetry: the two <tt>setText()</tt> calls are both inside the listener.</li>
<li>No callback methods, whether the Task completed successfully or threw an exception.</li>
<li>The code after the time-consuming task is independent of the time-consuming task itself. This allows refactoring of
<tt>Worker.post()</tt> calls, and it is possible to execute different code after <tt>Worker.post()</tt> depending on the place from where we
want to execute the time-consuming task.</li>
<li>Code written after <tt>Worker.post()</tt> is always executed after the code in <tt>Task.run()</tt>.
This greatly improve code readability and semplicity. No worries about code executed after <tt>Worker.post()</tt>.</li>
<li>No nesting of <tt>Worker.post()</tt> is necessary, just 2 consecutive <tt>Worker.post()</tt> calls.</li>
</ul>
</p>
<table width="100%" cellspacing="0" cellpadding="0">
<tr><td width="60%">
<pre><span class="code">
public class FoxtrotExample extends JFrame
{
   public static void main(String[] args)
   {</span><span class="main">
      FoxtrotExample example = new FoxtrotExample();
      example.setVisible(true);</span><span class="code">
   }

   public FoxtrotExample()
   {</span><span class="main">
      super("Foxtrot Example");

      final JButton button = new JButton("Take a nap !");
      button.addActionListener(new ActionListener()</span><span class="code">
      {
         public void actionPerformed(ActionEvent e)
         {</span><span class="event">
            button.setText("Sleeping...");</span><span class="code">

            String text = null;
            try
            {</span><span class="event">
               text = (String)Worker.post(new Task()</span><span class="code">
               {
                  public Object run() throws Exception
                  {</span><span class="foxtrot">
                     Thread.sleep(10000);
                     return "Slept !";</span><span class="code">
                  }
               }</span><span class="event">);</span><span class="code">
            }
            catch (Exception x) ...</span><span class="event">

            button.setText(text);

            somethingElse();</span><span class="code">
         }
      });</span><span class="main">

      setDefaultCloseOperation(DISPOSE_ON_CLOSE);

      Container c = getContentPane();
      c.setLayout(new GridBagLayout());
      c.add(button);

      setSize(300,200);

      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      Dimension size = getSize();
      int x = (screen.width - size.width) >> 1;
      int y = (screen.height - size.height) >> 1;
      setLocation(x, y);</span><span class="code">
   }
}</span>
</pre>
</td>
<td valign="top" align="left">
<table class="legend" width="50%" cellspacing="0" cellpadding="0">
<tr><td class="legend">Legend</td></tr>
<tr><td class="legend-entry"><span class="main">Main Thread</span></td></tr>
<tr><td class="legend-entry"><span class="event">Event Dispatch Thread</span></td></tr>
<tr><td class="legend-entry"><span class="foxtrot">Foxtrot Worker Thread</span></td></tr>
</table>
</td></tr>
</table>

<?php include 'footer.php';?>
