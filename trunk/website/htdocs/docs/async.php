<?php include 'header.php';?>

<h2>Asynchronous Solutions</h2>

<p>Solutions have been proposed for the <a href="freeze.php">GUI freeze problem</a>; asynchronous solutions
rely on the combined usage of a worker thread and the <tt>SwingUtilities.invokeLater()</tt> method.
We will see in few lines why they're called asynchronous.</p>
<p>The main idea behind an asynchronous solution is to return quickly from the time-consuming listener, after having
delegated the time-consuming task to a worker thread. The worker thread, usually, does two things:
<ul>
<li>Executes the time-consuming task
<li>Posts an event to the Event Queue using <tt>SwingUtilities.invokeLater()</tt>
</ul>
</p>
<p>Take a look at the code below which uses Foxtrot's <b>AsyncWorker</b>, which is an asynchronous solution.</p>
<p>Let's concentrate on the button's listener (the <tt>actionPerformed()</tt> method): the first statement,
as in the freeze example, changes the text of the button and thus posts a repaint event to the Event Queue.<br />
The next statement posts an <b>AsyncTask</b> to Foxtrot's AsyncWorker. This operation is quick, non blocking, and returns
immediately.<br />
Posting an AsyncTask to Foxtrot's AsyncWorker causes the Foxtrot worker thread to start executing the code contained
in the <tt>run()</tt> method of AsyncTask;
the Event Dispatch Thread is free to continue its execution, and will execute the <tt>somethingElse()</tt>
method.<br />
Therefore, there is a potentially concurrent execution of the code in the AsyncTask and of the code in
<tt>somethingElse()</tt>.
When the <tt>run()</tt> method of AsyncTask ends, one of two callbacks is called and executed in the
Event Dispatch Thread:
<ul>
<li>method <tt>success(Object result)</tt> in case the AsyncTask execution completed successfully, or</li>
<li>method <tt>failure(Throwable x)</tt> in case the AsyncTask execution throws an Exception or an Error</li>
</ul>
</p>
<p>This is why these solutions are called asynchronous: the code in the event listener and the code of the AsyncTask
are executed concurrently and noone waits for the other to complete.</p>
<p>This solution, while resolving the freeze problem, has several drawbacks:
<ul>
<li>Note the non-optimal exception handling.
The exception handling is done inside the AsyncTask, not inside the listener, where it would be more intuitive.</li>
<li>Note the asymmetry: the first <tt>setText()</tt> is made outside the AsyncTask, the second inside of it.</li>
<li>What happens if the time-consuming code stays the same, but we want to execute 2 different <tt>success(Object result)</tt>
methods depending on the place from where we want to execute the time-consuming task ?</li>
<li>If some code is written after <tt>AsyncWorker.post()</tt>, like the <tt>somethingElse()</tt> method,
it will be always executed <em>before</em> the <tt>success(Object result)</tt> or <tt>failure(Throwable x)</tt> callbacks.
Reading the source code we see, from top to bottom:
<ul>
<li><tt>setText("Sleeping...")</tt></li>
<li><tt>Thread.sleep()</tt></li>
<li><tt>setText("Slept !");</tt></li>
<li><tt>somethingElse()</tt></li>
</ul>
but the real order of execution is:
<ul>
<li><tt>setText("Sleeping...")</tt></li>
<li><tt>somethingElse()</tt> [before, concurrently or after <tt>Thread.sleep()</tt>]</li>
<li><tt>Thread.sleep()</tt></li>
<li><tt>setText("Slept !");</tt></li>
</ul>
making debugging and code readability very difficult.<br />
This is why a golden rule of AsyncWorker is to never put code after <tt>AsyncWorker.post()</tt>.</li>
<li>If the code inside <tt>success(Object result)</tt> requires a new time-consuming operation, a new <em>nested</em>
AsyncWorker should be used, making the code complex and obscure, especially with respect to the sequence order
of the operations executed.</li>
</ul>
</p>
<p>Fortunately, <a href="worker.php">Foxtrot's synchronous solutions</a> solve these issues.</p>
<div class="legend">Legend<br/>
<span class="main">Main Thread</span><br/>
<span class="event">Event Dispatch Thread</span><br/>
<span class="foxtrot">Foxtrot Worker Thread</span>
</div>
<pre><span class="code">
public class AsyncExample extends JFrame
{
   public static void main(String[] args)
   {</span><span class="main">
      AsyncExample example = new AsyncExample();
      example.setVisible(true);</span><span class="code">
   }

   public AsyncExample()
   {</span><span class="main">
      super("AsyncWorker Example");

      final JButton button = new JButton("Take a nap !");
      button.addActionListener(new ActionListener()</span><span class="code">
      {
         public void actionPerformed(ActionEvent e)
         {</span><span class="event">
            button.setText("Sleeping...");

            AsyncWorker.post(new AsyncTask()</span><span class="code">
            {
               public Object run() throws Exception
               {</span><span class="foxtrot">
                  Thread.sleep(10000);
                  return "Slept !";</span><span class="code">
               }

               public void success(Object result)
               {</span><span class="event">
                  String text = (String)result;
                  button.setText(text);</span><span class="code">
               }

               public void failure(Throwable x)
               {</span><span class="event">
                  // Do exception handling: argument x is the Throwable
                  // that is eventually thrown inside run()</span><span class="code">
               }
            }</span><span class="event">);

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

<?php include 'footer.php';?>
