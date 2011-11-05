<?php include 'header.php';?>

<h2>Foxtrot's synchronous solution: ConcurrentWorker</h2>
<p>Foxtrot's <tt>ConcurrentWorker</tt> is a synchronous solution like <a href="worker.php"><tt>Worker</tt></a>.<br />
Where <tt>Worker</tt> enqueues the <tt>Task</tt>s or <tt>Job</tt>s to be run in a single worker queue,
so that they're executed one after the other, in <tt>ConcurrentWorker</tt> the <tt>Task</tt>s or
<tt>Job</tt>s are run as soon as they are posted and each in its own worker thread.</p>
<p>While at first <tt>ConcurrentWorker</tt> seems more powerful than <tt>Worker</tt>, it has a peculiar
behavior that is less intuitive than <tt>Worker</tt>, and may lead to surprises when used in the wrong
context.</p>
<p>The ideal context for the correct usage of <tt>ConcurrentWorker</tt> is when a task posted
<em>after</em> a first task needs to be executed concurrently with the first task <b>and</b> there must
be a synchronous behavior (that is, the first call to <tt>ConcurrentWorker.post()</tt> must complete
only after the second call to <tt>ConcurrentWorker.post()</tt>).</p>
<p>A typical example is when an application executes a time-consuming task that can be canceled,
but the action of cancelling is also a time-consuming operation.</p>
<p>If <tt>Worker</tt> is used in this case, the two tasks will be both enqueued in the single worker queue of
<tt>Worker</tt>, and the second task (that should cancel the first one) gets the chance of being executed
only after the first task completes, making it useless.</p>
<p><tt>ConcurrentWorker</tt> instead executes the tasks as soon as they are posted, so in the above case
the second task is executed concurrently with the first task in order to get the chance of cancelling
the first task while it is still being executed.<br/>
However, the application requires that the first call to <tt>ConcurrentWorker.post()</tt> returns only
after the second call to <tt>ConcurrentWorker.post()</tt> completes cancelling the first task, so that
any code after the first <tt>ConcurrentWorker.post()</tt> call can discern whether the task completed
successfully or was canceled.</p>
<p>Remembering that <tt>ConcurrentWorker</tt> is a synchronous solution is the key to avoid to use it in
the wrong contexts.<br/>
The following example shows that using <tt>ConcurrentWorker</tt> in a strictly synchronous context does
not lead to any benefit with respect to <tt>Worker</tt>, and it's probably slower, because calls to
<tt>ConcurrentWorker.post()</tt> block until the task is completed.
Therefore, posting two jobs consecutively in the code results in the jobs being executed one after the
other, because the first call to blocks until the first task is completed.</p>
<div class="legend">Legend<br/>
<span class="main">Main Thread</span><br/>
<span class="event">Event Dispatch Thread</span><br/>
<span class="foxtrot">Foxtrot Worker Thread</span>
</div>
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
            // Blocking call
            ConcurrentWorker.post(new Job()</span><span class="code">
            {
               public Object run()
               {</span><span class="foxtrot">
                  sleep(10000);
                  return null;</span><span class="code">
               }
            }</span><span class="event">);</span><span class="code">

            </span><span class="event">
            // Blocking call
            ConcurrentWorker.post(new Job()</span><span class="code">
            {
               public Object run()
               {</span><span class="foxtrot">
                  sleep(5000);
                  return null;</span><span class="code">
               }
            }</span><span class="event">);</span><span class="code">
            </span><span class="event">

            button.setText("Slept !");
            </span><span class="code">
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

   public void sleep(long ms)
   {
      try
      {
         Thread.sleep(ms);
      }
      catch (InterruptedException x)
      {
         Thread.currentThread().interrupt();
      }
   }
}</span>
</pre>

<p>Another wrong example is to use <tt>ConcurrentWorker</tt> in an asynchronous context. <br />
For example, suppose to have an application with a <tt>javax.swing.JTabbedPane</tt>, and suppose that
each tab takes a while to load the data it presents to the user. A user may click on the first tab, then
click on the second tab before the first finishes loading, then on the third before the second finishes
loading and so on. The loading of each tab is usually an asynchronous operation, since we want the tabs
to load concurrently, but we do not want that the first tab waits until the last tab is loaded.
Each tab loading is independent from the others. The correct solution in this cases is to use
<a href="async.php"><tt>AsyncWorker</tt></a>.</p>
<p>Using <tt>ConcurrentWorker</tt> in the code that loads the tabs results in the tabs being displayed
one after the other, from the last to the first, which is not what is normally expected.</p>

<?php include 'footer.php';?>
