<?php include 'header.php';?>

<h2>Tips & Tricks</h2>
<p>In this section we will discuss some tip and trick that applies when using threads in Swing Applications.</p>
<p>Topics are:
<ul>
<li>Working correctly with Swing Models
<li>Working correctly with Custom Event Emitters
<li>Working correctly with <tt>JComboBox</tt>
</ul>
</p>

<h3>Working correctly with Swing Models</h3>
<p>When threads are used in a Swing Application, the issue of concurrent access to shared data structures is always
present. No matter if the chosen solution is asynchronous or synchronous, care must be taken
to interact with Swing Models, since code working well against a plain Swing solution (i.e. without use of threads), may
not work as well when using threads.</p>
<p>Let's make an example: suppose you have a <tt>JTable</tt>, and you use as a model a subclass of <tt>AbstractTableModel</tt> that you feeded with
your data. Suppose also that the user can change the content of a cell by editing it, but the operation to validate the
new input takes time.<br />
Using plain Swing programming, this code looks similar to this:</p>
<div class="legend">Legend<br/>
<span class="main">Main Thread</span><br/>
<span class="event">Event Dispatch Thread</span><br/>
<span class="foxtrot">Foxtrot Worker Thread</span>
</div>
<pre><span class="code">
public class MyModel extends AbstractTableModel
{
   private Object[][] m_data;
   ...
   public void setValueAt(Object value, int row, int column)
   {</span><span class="event">
      if (isValid(value))
      {
         m_data[row][column] = value;
      }</span><span class="code">
   }
}
</span></pre>

<p>If <tt>isValid(Object value)</tt> is fast, no problem; otherwise the user has the GUI frozen and no feedback on what
is going on.<br />
Thus you may decide to use Foxtrot, and you convert the old code to this:</p>
<pre><span class="code">
public class MyModel extends AbstractTableModel
{
   private Object[][] m_data;
   ...
   public void getValueAt(int row, int col)
   {</span><span class="event">
      return m_data[row][col];</span><span class="code">
   }
   public void setValueAt(final Object value, final int row, final int column)
   {</span><span class="event">
      Worker.post(new Job()</span><span class="code">
      {
         public Object run()
         {</span><span class="foxtrot">
            if (isValid(value))
            {
               m_data[row][column] = value;
            }
            return null;</span><span class="code">
         }
      }</span><span class="event">);</span><span class="code">
   }
}
</span></pre>
<p>The above is just plain <b>wrong</b>.<br />
It is wrong because the data member <tt>m_data</tt> is accessed from two threads: from the Foxtrot Worker Thread
(since it is modified inside <tt>Job.run()</tt>) and from the AWT Event Dispatch Thread (since any repaint event
that occurs will call <tt>getValueAt(int row, int col)</tt>).</p>
<p>Avoid the temptation to modify <em>anything</em> from inside <tt>Job.run()</tt>. It should just take data
from outside, perform some heavy operation and <em>return the result of the operation</em>.<br />
The pattern to follow in the implementation of <tt>Job.run()</tt> is <b>Compute and Return</b>, see example below.</p>
<pre><span class="code">
public class MyModel extends AbstractTableModel
{
   private Object[][] m_data;
   ...
   public void getValueAt(int row, int col)
   {</span><span class="event">
      return m_data[row][col];</span><span class="code">
   }
   public void setValueAt(final Object value, int row, int column)
   {</span><span class="event">
      Boolean isValid = (Boolean)Worker.post(new Job()</span><span class="code">
      {
         public Object run()
         {</span><span class="foxtrot">
            // Compute and Return
            return isValid(value);</span><span class="code">
         }
      }</span><span class="event">);

      if (isValid.booleanValue())
      {
         m_data[row][column] = value;
      }</span><span class="code">
   }
}
</span></pre>
<p>Note how <em>only</em> the heavy operation is isolated inside <tt>Job.run()</tt>, while modifications to the
data member <tt>m_data</tt> now happen in the AWT Event Dispatch Thread, thus following the Swing Programming Rules
and avoiding concurrent read/write access to it.</p>

<h3>Working correctly with Custom Event Emitters</h3>
<p>Sometimes you code your application with the use of custom data structures that are able to notify listeners upon some
state change, following the well-known <b>Subject-Observer</b> pattern.<br />
When threads are used in such a Swing Application, you have to be careful about which thread will actually notify the
listeners.</p>
<p>Let's make an example: suppose you created a custom data structure that emits event when its state changes, and
suppose that state change is triggered by <tt>JButton</tt>s. In plain Swing programming, the code may be similar to this:</p>
<pre><span class="code">
public class Machine
{
   private ArrayList m_listeners;

   public void addListener(Listener l) {...}
   public void removeListener(Listener l) {...}

   public void start()
   {
      // Starts the machine
      ...
      MachineEvent event = new MachineEvent("Running");
      notifyListeners(event);
   }

   private void notifyListeners(MachineEvent e)
   {
      for (Iterator i = m_listeners.iterator(); i.hasNext();)
      {
         Listener listener = (Listener)i.next();
         listener.stateChanged(e);
      }
   }
}</span><span class="main">

// Somewhere else in your application...</span><span class="code">

final Machine machine = new Machine();

final JLabel statusLabel = new JLabel();

machine.addListener(new Listener()
{
   public void stateChanged(MachineEvent e)
   {</span><span class="event">
      statusLabel.setText(e.getStatus());</span><span class="code">
   }
});

JButton button = new JButton("Start Machine");
button.addActionListener(new ActionListener()
{
   public void actionPerformed(ActionEvent e)
   {</span><span class="event">
      machine.start();</span><span class="code">
   }
});
</span></pre>
<p>The <tt>Machine</tt> class is a JavaBean, and does not deal with Swing code.<br />
While you implement <tt>Machine.start()</tt> you discover that the process of starting a Machine is a long one, and
decide to not freeze the GUI after pressing the button.<br />
With the Foxtrot API, a small change in the listener will do the job:</p>
<pre><span class="code">
button.addActionListener(new ActionListener()
{
   public void actionPerformed(ActionEvent e)
   {</span><span class="event">
      Worker.post(new Job()</span><span class="code">
      {
         pulic Object run()
         {</span><span class="foxtrot">
            machine.start();
            return null;</span><span class="code">
         }
      }</span><span class="event">);</span><span class="code">
   }
});
</span></pre>
<p>Unfortunately, the above is plain <b>wrong</b>.<br />
It is wrong because now <tt>Machine.start()</tt> is called in the Foxtrot Worker Thread, and so is
<tt>Machine.notifyListeners()</tt> and finally also any registered listener have the
<tt>Listener.stateChanged()</tt> called in the Foxtrot Worker Thread.<br />
In the example above, the <tt>statusLabel</tt>'s text is thus changed in the Foxtrot Worker Thread, violating the
Swing Programming Rules.</p>
<p>Below you can find one solution to this problem (my favorite), that fixes the <tt>Machine.notifyListeners()</tt>
implementation using <tt>SwingUtilities.invokeAndWait()</tt>:</p>
<pre><span class="code">
public class Machine
{
   ...
   private void notifyListeners(final MachineEvent e)
   {
      if (SwingUtilities.isEventDispatchThread())
      {</span><span class="event">
         notify(e);</span><span class="code">
      }
      else
      {
         SwingUtilities.invokeAndWait(new Runnable()
         {
            public void run()
            {</span><span class="event">
               notify(e);</span><span class="code">
            }
         });
      }
   }

   private void notify(MachineEvent e)
   {</span><span class="event">
      for (Iterator i = m_listeners.iterator(); i.hasNext();)
      {
         Listener listener = (Listener)i.next();
         listener.stateChanged(e);
      }</span><span class="code">
   }
}
</span></pre>
<p>The use of <tt>SwingUtilities.invokeAndWait()</tt> preserves the semantic of the <tt>Machine.notifyListeners()</tt>
method, that returns when all the listeners have been notified. Using <tt>SwingUtilities.invokeLater()</tt> causes
this method to return immediately, normally before listeners have been notified, breaking the semantic.</p>

<h3>Working correctly with <tt>JComboBox</tt></h3>
<p><tt>JComboBox</tt> shows a non-usual behavior with respect to item selection when compared, for example,
with <tt>JMenu</tt>: both show a <tt>JPopup</tt> with a list of items to be selected by the user, but after
selecting an item in <tt>JMenu</tt> the <tt>JPopup</tt> disappears immediately, while in <tt>JComboBox</tt>
it remains shown until all listeners are processed.</p>
<p>Swing Applications that contain <tt>JComboBox</tt>es that have to perform heavy operations when an item is
selected will suffer of the "<tt>JPopup</tt> shown problem" when using plain Swing programming (in this case
the GUI is also frozen) and when using synchronous APIs (this problem does not appear when using asynchronous APIs).</p>
<p>However this problem is easily solved by asking <tt>JComboBox</tt> to explicitely close the <tt>JPopup</tt>,
as the example below shows:
<pre><span class="code">
final JComboBox combo = new JComboBox(...);
combo.addActionListener(new ActionListener()
{
   public void actionPerformed(ActionEvent e)
   {
      try
      {</span><span class="event">
          // Explicitely close the popup
         combo.setPopupVisible(false);

         // Heavy operation
         Worker.post(new Task()</span><span class="code">
         {
            public Object run() throws InterruptedException
            {</span><span class="foxtrot">
               Thread.sleep(5000);
               return null;</span><span class="code">
            }
         }</span><span class="event">);</span><span class="code">
      }
      catch (InterruptedException x)
      {
         x.printStackTrace();
      }
      catch (RuntimeException x)
      {
         throw x;
      }
      catch (Exception ignored) {}
   }
});
</span></pre></p>
<p>This is the only small anomaly I've found so far using Swing with the Foxtrot API, and I tend to think it's more
a Swing anomaly than Foxtrot's.</p>

<?php include 'footer.php';?>
