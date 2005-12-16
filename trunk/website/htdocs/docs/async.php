<?php include 'header.php';?>

<tr><td class="documentation">

<h2>Asynchronous Solutions</h2>

<p>Solutions have been proposed for the <a href="freeze.php">GUI freeze problem</a>; asynchronous solutions
rely on the combined usage of a worker thread and the <code>SwingUtilities.invokeLater()</code> method.
We will see in few lines why they're called asynchronous.</p>
<p>The main idea behind an asynchronous solution is to return quickly from the time-consuming listener, after having
delegated the time-consuming task to a worker thread. The worker thread, usually, does two things:
<ul>
<li>Executes the time-consuming task
<li>Posts an event to the Event Queue using <code>SwingUtilities.invokeLater()</code>
</ul>
</p>
<p>Take a look at the code below which uses Foxtrot's <b>AsyncWorker</b>, which is an asynchronous solution.</p>
<p>Let's concentrate on the button's listener (the <code>actionPerformed()</code> method): the first statement,
as in the freeze example, changes the text of the button and thus posts a repaint event to the Event Queue.<br />
The next statement posts an <b>AsyncTask</b> to Foxtrot's AsyncWorker. This operation is quick, non blocking, and returns
immediately.<br />
Posting an AsyncTask to to Foxtrot's AsyncWorker causes the Foxtrot worker thread to start executing the code contained
in the <code>run()</code> method of AsyncTask;
the Event Dispatch Thread is free to continue its execution, and will execute the <code>somethingElse()</code>
method.<br />
Therefore, there is a potentially concurrent execution of the code in the AsyncTask and of the code in
<code>somethingElse()</code>.
When the <code>run()</code> method of AsyncTask ends, its <code>finish()</code> method is called (using
<code>SwingUtilities.invokeLater()</code>) and executed in the Event Dispatch Thread.
</p>
<p>This is why these solutions are called asynchronous: the code in the event listener and the code of the AsyncTask
are executed concurrently and noone waits for the other to complete.
</p>
<p>This solution, while resolving the freeze problem, has several drawbacks:
<ul>
<li>Note the non-optimal exception handling.
The exception handling is done inside the AsyncTask, not inside the listener, where it would be more intuitive.
<li>Note the asymmetry: the first <code>setText()</code> is made outside the AsyncTask, the second inside of it.
<li>Note the <code>getResultOrThrow()</code> method: if <code>run()</code> returns null (because the operation had
a void return value - like <code>Thread.sleep()</code>), it is easy to forget to call <code>getResultOrThrow()</code>
to see if any exception was thrown by the time-consuming code of the AsyncTask.
<li>What happens if the time-consuming code stays the same, but we want to execute 2 different <code>finish()</code> methods
depending on the place from where we want to execute the time-consuming task ?
<li>If some code is written after <code>AsyncWorker.post()</code>, like the <code>somethingElse()</code> method,
it will be always executed <em>before</em> the <code>finish()</code> method.
Reading the source code we see, from top to bottom:
<ul>
<li><code>setText("Sleeping...")</code>
<li><code>Thread.sleep()</code>
<li><code>setText("Slept !");</code>
<li><code>somethingElse()</code>
</ul>
but the real order of execution is:
<ul>
<li><code>setText("Sleeping...")</code>
<li><code>somethingElse()</code> [before, concurrently or after <code>Thread.sleep()</code>]
<li><code>Thread.sleep()</code>
<li><code>setText("Slept !");</code>
</ul>
making debugging and code readability very difficult.<br />
This is why a golden rule of the AsyncWorker is to never put code after <code>AsyncWorker.post()</code>.
<li>If the code inside <code>finish()</code> requires a new time-consuming operation, a new <em>nested</em>
AsyncWorker should be used, making the code complex and obscure, especially with respect to the sequence order
of the operations executed.
</ul>
</p>
<p>Fortunately, <a href="toc.php">Foxtrot's synchronous solutions</a> solve these issues.</p>
<table width="100%" cellspacing="0" cellpadding="0">
<tr><td width="60%">
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
               {</span><span class="worker">
                  Thread.sleep(10000);
                  return "Slept !";</span><span class="code">
               }

               public void finish()
               {
                  try
                  {</span><span class="event">
                     String text = (String)getResultOrThrow();
                     button.setText(text);</span><span class="code">
                  }
                  catch (Exception x)
                  {</span><span class="event">
                     // Do exception handling: here is rethrown
                     // what is eventually thrown inside run()</span><span class="code">
                  }
               }
            }</span><span class="event">);

            somethingElse();</span><span class="code">
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
<tr><td class="legend-entry"><span class="worker">AsyncWorker Thread</span></td></tr>
</table>
</td></tr>
</table>

</td></tr>

<?php include 'footer.php';?>
