<?php include 'header.php';?>

<tr><td class="documentation">

<h2>Foxtrot API</h2>
<p>The Foxtrot API is very small and simple, and consists of six main classes to be used in Swing applications:</p>
<ul>
<li><code>class foxtrot.Worker</code></li>
<li><code>class foxtrot.ConcurrentWorker</code></li>
<li><code>class foxtrot.Task</code></li>
<li><code>class foxtrot.Job</code></li>
<li><code>class foxtrot.AsyncWorker</code></li>
<li><code>class foxtrot.AsyncTask</code></li>
</ul>
<p>From Foxtrot 2.x, the API has been extended to allow customization of the part that handles event pumping
and of the part that handles execution of <code>Task</code>s and <code>Job</code>s in a worker thread, via
the following classes:</p>
<ul>
<li><code>interface foxtrot.EventPump</code></li>
<li><code>interface foxtrot.WorkerThread</code></li>
<li><code>class foxtrot.AbstractWorkerThread</code></li>
</ul>
<p>Normally users do not need to deal with these classes to use Foxtrot in their Swing applications, since
Foxtrot will configure itself with the most suitable implementations; however, if
a specific customization of the event pumping mechanism or of the worker thread mechanism is needed, the APIs
provided by these classes allow fine grained control on Foxtrot's behavior.</p>

<h2>Foxtrot API Details</h2>
<p>The <code>Worker</code> class is used to post <code>Task</code>s or <code>Job</code>s that will be executed
sequentially in one Foxtrot Worker Thread. <br />
<p>The <code>ConcurrentWorker</code> class is used to post <code>Task</code>s or <code>Job</code>s that
will be executed each one in its own Foxtrot Worker Thread (thus <code>Task</code>s or <code>Job</code>s are
executed concurrently). <br />
<p>The <code>AsyncWorker</code> class is used to post <code>AsyncTask</code>s that will be executed each one
in its own Foxtrot Worker Thread (thus <code>AsyncTask</code>s are executed concurrently).</p>
<p>The <code>Task</code> class is subclassed by the user to perform heavy tasks that throw checked exceptions.<br />
The <code>Job</code> class, conversely, is subclassed by the user to perform heavy tasks that do not throw
checked exceptions, but only RuntimeExceptions (or Errors).<br />
The <code>AsyncTask</code> class is subclassed by the user to perform asynchronous heavy tasks and to post
an event to the Event Dispatch Thread when the <code>AsyncTask</code> is completed.</p>
<p>The <code>Worker</code> and <code>ConcurrentWorker</code> classes have the following two public methods that
can be used to post <code>Task</code>s or <code>Job</code>s:</p>
<ul>
<li><code>public static Object post(Task task) throws Exception</code></li>
<li><code>public static Object post(Job job)</code></li>
</ul>
<p>The <code>Task</code> class has a single abstract method that must be implemented by the user, with the
time-consuming code that may throw checked exceptions:</p>
<ul>
<li><code>public abstract Object run() throws Exception</code></li>
</ul>
<p>The <code>Job</code> class, conversely, has a single abstract method that must be implemented by the user,
with the time-consuming code that does not throw checked exceptions:</p>
<ul>
<li><code>public abstract Object run()</code></li>
</ul>
<p>The exceptions or errors thrown inside the <code>Task.run()</code> or <code>Job.run()</code> methods are
re-thrown by the corrispondent <code>Worker.post(...)</code> method <b>as is</b>, i.e. without being wrapped into,
for example, an InvocationTargetException.</p>
<p>The <code>AsyncWorker</code> class has only one public method that can be used to post <code>AsyncTask</code>s:</p>
<ul>
<li><code>public static Object post(AsyncTask task)</code></li>
</ul>
<p>The <code>AsyncTask</code> class has two abstract method that must be implemented by the user:</p>
<ul>
<li><code>public abstract Object run() throws Exception</code></li>
<li><code>public abstract void finish()</code></li>
</ul>
The <code>run()</code> method must be implemented with the time-consuming code exactly like the <code>Task</code>
class, while the <code>finish()</code> method must be implemented by calling the
<code>AsyncTask.getResultOrThrow()</code> method to get the result returned by the <code>run()</code> method or to
rethrow any exception thrown during its execution, and eventually other code executed in the Event Dispatch Thread.</p>

<p>The usage is very simple; here's an example of <code>Worker</code> with the <code>Job</code> class:</p>
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
<p>and here's an example of <code>Worker</code> with the <code>Task</code> class:</p>
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
<p>It is possible to narrow the throws clause of the <code>Task</code> class, but unfortunately not the one of
the <code>Worker</code> or <code>ConcurrentWorker</code> classes. <br />
So, when using the <code>post(Task task)</code> method, you have to surround it in a
<code>try...catch(Exception x)</code> block (unless the method that contains <code>post(Task task)</code>
throws <code>Exception</code> itself).</p>
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

<p>Here's an example of <code>AsyncWorker</code> with the <code>AsyncTask</code>:</p>
<pre><span class="code">
AsyncWorker.post(new AsyncTask()
{
   public Object run() throws Exception
   {
      // Here write the time-consuming code
      // that may throw checked exceptions
   }

   public void finish()
   {
      try
      {
         Object result = getResultOrThrow();
         // Here handle the result
      }
      catch (Exception x)
      {
         // Here handle the exception possibly thrown by run(),
         // for example displaying a dialog to the user
      }
   }
});
</span></pre>
<p>All worker classes, from Foxtrot 2.x, have the following public methods to deal with the
<code>WorkerThread</code> component:</p>
<ul>
<li><code>public static WorkerThread getWorkerThread()</code></li>
<li><code>public static void setWorkerThread(WorkerThread worker)</code></li>
</ul>
<p>The <code>Worker</code> class, from Foxtrot 2.x, and the <code>ConcurrentWorker</code> class, have also the
following public methods to deal with the <code>EventPump</code> component:</p>
<ul>
<li><code>public static EventPump getEventPump()</code></li>
<li><code>public static void setEventPump(EventPump pump)</code></li>
</ul>
<p>Foxtrot configures itself automatically with the most suitable implementation of
<code>EventPump</code> and <code>WorkerThread</code>.
Some implementations of <code>EventPump</code> or <code>WorkerThread</code> allow an even further customization
of the component.</p>
<p>For example, implementations of <code>EventPump</code> that also implement the
<code>foxtrot.pumps.EventFilterable</code> interface may allow the user to filter events that are being dispatched
by the <code>java.awt.EventQueue</code>. See also the bundled Javadocs for further details. <br />
However, it is recommended not to exploit these features unless knowing <strong>exactly</strong> what one is doing:
Foxtrot's defaults may change from version to version to suit better implementations, and these defaults may depend
on the Java Runtime Environment version Foxtrot is running on, so that features working in JDK 1.3.x may not work
in JDK 1.4.x or viceversa. <br />
Playing with AWT events too badly is normally looking for troubles, so consider you warned :)</p>
<p>The same holds for <code>WorkerThread</code> implementations, that should extend the abstract class
<code>AbstractWorkerThread</code>: Foxtrot uses a <strong>synchronous</strong> model, so replacing (for example)
the default <code>WorkerThread</code> implementation may lead to unexpected behavior. <br />

<p>Refer to the bundled Javadoc documentation for further information, and to the bundled examples for
further details on how to use the Foxtrot classes with Swing.<br />
And do not forget the <a href="tips.php">Tips 'n' Tricks</a> section !<p>

</td></tr>

<?php include 'footer.php';?>
