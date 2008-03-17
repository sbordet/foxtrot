<?php include 'header.php';?>

<tr><td class="documentation">

<h2>Foxtrot's synchronous solution: ConcurrentWorker</h2>
<p>The class <tt>foxtrot.ConcurrentWorker</tt> is a synchronous solutions like
<a href="worker.php"><tt>foxtrot.Worker</tt></a>.<br />
Where <tt>Worker</tt> enqueues the <tt>Task</tt>s or <tt>Job</tt>s to be run in a single worker queue,
so that they're executed one after the other, in <tt>ConcurrentWorker</tt> the <tt>Task</tt>s or
<tt>Job</tt>s are run each one in its own worker thread.</p>
<p>While at first <tt>ConcurrentWorker</tt> seems more powerful than <tt>Worker</tt>, it has a peculiar
behavior that is less intuitive than <tt>Worker</tt>, and may lead to surprises when used in the wrong
context.</p>
<p>For example, posting two jobs consecutively in the code results in the jobs being executed one after the other,
since <tt>ConcurrentWorker.post()</tt> is synchronous. The following example shows that using
<tt>ConcurrentWorker</tt> in a strictly synchronous context does not lead to any benefit with respect to
<tt>Worker</tt>, and it's probably slower.</p>
<table width="100%" cellspacing="0" cellpadding="0">
<tr><td width="60%">
<pre><span class="code">
public class ConcurrentWorkerWrongExample1 extends JFrame
{
   public static void main(String[] args)
   {</span><span class="main">
      ConcurrentWorkerWrongExample1 example = new ConcurrentWorkerWrongExample1();
      example.setVisible(true);</span><span class="code">
   }

   public ConcurrentWorkerWrongExample1()
   {</span><span class="main">
      super("ConcurrentWorker Wrong Example 1");

      final JButton button = new JButton("Sleep !");
      button.addActionListener(new ActionListener()</span><span class="code">
      {
         public void actionPerformed(ActionEvent e)
         {</span><span class="event">
            button.setText("Sleeping...");</span><span class="code">

            </span><span class="event">
            ConcurrentWorker.post(new Job()</span><span class="code">
            {
               public Object run()
               {</span><span class="foxtrot">
                  Thread.sleep(10000);
                  return null;</span><span class="code">
               }
            }</span><span class="event">);</span><span class="code">

            </span><span class="event">
            ConcurrentWorker.post(new Job()</span><span class="code">
            {
               public Object run()
               {</span><span class="foxtrot">
                  Thread.sleep(5000);
                  return null;</span><span class="code">
               }
            }</span><span class="event">);</span><span class="code">
            </span><span class="event">

            button.setText("Slept !");
            </span><span class="code">
         }
      });</span><span class="main">

      setDefaultCloseOperation(EXIT_ON_CLOSE);

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
<p>Another wrong usage is to use <tt>ConcurrentWorker</tt> in an asynchronous context. <br />
For example, loading tabs of a <tt>javax.swing.JTabbedPane</tt> when the user clicks on them is normally an
asynchronous operation, since it's driven by the user (the application does not know when the user clicks on a
tab). Using <tt>ConcurrentWorker</tt> in the code that loads the tabs results in the tabs being displayed one
after the other. The following example shows that using <tt>ConcurrentWorker</tt> in an asynchronous context
leads to unexpected behavior.</p>
<table width="100%" cellspacing="0" cellpadding="0">
<tr><td width="60%">
<pre><span class="code">
public class ConcurrentWorkerWrongExample2 extends JFrame
{
   public ConcurrentWorkerWrongExample2() { ... }

   public void loadTab(final int tabIndex)
   {</span><span class="event">
      ConcurrentWorker.post(new Job()</span><span class="code">
      {
         public Object run()
         {</span><span class="foxtrot">
            // Here goes the code that loads the tabs
            Thread.sleep(5000 * tabIndex);
            return null;</span><span class="code">
         }
      }</span><span class="event">);</span><span class="code">

      </span><span class="event">
      System.out.println("Done tab " + tabIndex);
   }
}</span>
</pre>
</td></tr>
</table>
<p>Why <tt>ConcurrentWorker</tt> shows this behavior in this case ?</p>
<p>The user clicks on the first tab, and an AWT event is generated, "tab1", that calls <tt>loadTab(1)</tt>.
Inside <tt>loadTab(1)<tt>, in the example, a <tt>Job</tt> "job1" is posted to load the tab in worker thread
"thread1", and in the meanwhile <tt>ConcurrentWorker</tt> dequeues other AWT events. <br />
If the user clicks on another tab, it generates another AWT event, "tab2", that is dequeued by
<tt>ConcurrentWorker</tt>, that calls <tt>loadTab(2)</tt>, where "job2" is posted and the tab loaded in worker
thread "thread2". <br />
Now, since <tt>ConcurrentWorker.post()</tt> is synchronous, it cannot return until "job2" is finished. But event
"tab1" (which is the one that spawned "job2") also cannot finish until "job2" is finished, because
<tt>ConcurrentWorker.post()</tt> is synchronous. Since "tab1" has been dequeued by the first
<tt>ConcurrentWorker.post()</tt> call, that call cannot return until "tab1" is finished (and hance until "job2" is
finished), no matter if "job1" is already finished.











<p>To understand the peculiar behavior of <tt>ConcurrentWorker</tt>, one must remember that there is only one
AWT event queue. When a <tt>Task</tt> or <tt>Job</tt> is posted using the Foxtrot APIs, events are dequeued
from the AWT event queue, one after the other


</td></tr>

<?php include 'footer.php';?>
