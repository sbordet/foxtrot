<?php include 'header.php';?>

<tr><td align="center" colspan="3">
<a href="http://foxtrot.sourceforge.net"><img class="nav-btn-en" hspace="10" src="../images/cnvhome.gif"/></a>
<a href="toc.php"><img class="nav-btn-en" hspace="10" src="../images/cnvup.gif"/></a>
<img class="nav-btn-dis" hspace="10" src="../images/cnvprev.gif"/>
<img class="nav-btn-dis" hspace="10" src="../images/cnvnext.gif"/>
</td></tr>

<tr><td class="date" colspan="3">Last Updated: $Date$</td></tr>

<tr><td class="documentation">

<h2>Foxtrot API</h2>
<p>The Foxtrot API is very small and simple, and consists of 3 classes:</p>
<ul>
<li><code>foxtrot.Worker</code></li>
<li><code>foxtrot.Task</code></li>
<li><code>foxtrot.Job</code></li>
</ul>
<p>The <code>Worker</code> class is used to post <code>Task</code>s or <code>Job</code>s that will be executed in the Foxtrot
Worker Thread.</p>
<p>The <code>Task</code> class is subclassed by the user to perform heavy tasks that throw checked exceptions.</p>
<p>The <code>Job</code> class, conversely, is subclassed by the user to perform heavy tasks that do not throw checked exceptions,
but only RuntimeExceptions (or Errors).</p>
<p>The <code>Worker</code> class has the following 2 public methods:</p>
<ul>
<li><code>public static Object post(Task task) throws Exception</code></li>
<li><code>public static Object post(Job job)</code></li>
</ul>
<p>The <code>Task</code> class has a single abstract method that must be implemented by the user, with the time-consuming code
that may throw checked exceptions:</p>
<ul>
<li><code>public abstract Object run() throws Exception</code></li>
</ul>
<p>The <code>Job</code> class, conversely, has a single abstract method that must be implemented by the user, with the time-consuming code
that does not throw checked exceptions:</p>
<ul>
<li><code>public abstract Object run()</code></li>
</ul>
<p>The exceptions or errors thrown inside the <code>Task.run()</code> or <code>Job.run()</code> methods are re-thrown by the corrispondent
<code>Worker.post(...)</code> method <b>as is</b>, i.e. without being wrapped into, for example, an InvocationTargetException.</p>
<p>The usage is very simple; here's an example with the <code>Job</code> class:</p>
<pre><span class="code">
Worker.post(new Job()
{
   public Object run()
   {
      // Here write the time-consuming code
      // that does not throw checked exceptions
   }
});
</span></pre>
<p>and here's an example with the <code>Task</code> class:</p>
<pre><span class="code">
try
{
   Worker.post(new Task()
   {
      public Object run() throws Exception
      {
         // Here write the time-consuming code
         // that may throw checked exceptions
      }
   });
}
catch (Exception x)
{
   // Handle the exception thrown by the Task
}
</span></pre>
<p>It is possible to narrow the throws clause of the <code>Task</code> class, but unfortunately not the one of the <code>Worker</code> class. <br>
So, when using the <code>Worker.post(Task task)</code> method, you have to surround it in a <code>try...catch(Exception x)</code>
block (unless the method that contains <code>Worker.post(Task task)</code> throws <code>Exception</code> itself).</p>
<pre><span class="code">
try
{
   Worker.post(new Task()
   {
      public Object run() throws FileNotFoundException
      {
         // Here write the time-consuming code
         // that accesses the file system
      }
   });
}
catch (FileNotFoundException x)
{
   // Handle the exception or rethrow.
}
catch (RuntimeException x)
{
   // RuntimeExceptions are always possible.
   // Catch them here to prevent they are
   // ignored by the catch(Exception ignored)
   // block below.
   throw x;
}
catch (Exception ignored)
{
   // No other checked exceptions are thrown
   // by the Task (the compiler will enforce this),
   // so we can safely ignore it, but we're forced
   // to write this catch block: Worker.post(Task t)
   // requires it.
}
</span></pre>
<p>Refer to the bundled Javadoc documentation for further information, and to the bundled examples for
further details on how to use the Foxtrot classes with Swing. And do not forget the <a href="tips.php">Tip 'n' Tricks</a> section !<p>

</td></tr>

<?php include 'footer.php';?>
