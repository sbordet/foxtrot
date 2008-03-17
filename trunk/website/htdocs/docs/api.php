<?php include 'header.php';?>

<tr><td class="documentation">

<h2>Foxtrot API</h2>
<p>The Foxtrot API is very small and simple, and consists of six main classes to be used in Swing applications:</p>
<ul>
<li><tt>class foxtrot.Worker</tt></li>
<li><tt>class foxtrot.ConcurrentWorker</tt></li>
<li><tt>class foxtrot.Task</tt></li>
<li><tt>class foxtrot.Job</tt></li>
<li><tt>class foxtrot.AsyncWorker</tt></li>
<li><tt>class foxtrot.AsyncTask</tt></li>
</ul>
<p>From Foxtrot 2.x, the API has been extended to allow customization of the part that handles event pumping
and of the part that handles execution of <tt>Task</tt>s and <tt>Job</tt>s in a worker thread, via
the following classes:</p>
<ul>
<li><tt>interface foxtrot.EventPump</tt></li>
<li><tt>interface foxtrot.WorkerThread</tt></li>
<li><tt>class foxtrot.AbstractWorkerThread</tt></li>
</ul>
<p>Normally users do not need to deal with the above three classes to use Foxtrot in their Swing applications, since
Foxtrot will configure itself with the most suitable implementations; however, if
a specific customization of the event pumping mechanism or of the worker thread mechanism is needed, the APIs
provided by these classes allow fine grained control on Foxtrot's behavior.</p>

<h2>Foxtrot API Details</h2>
<p>The <tt>Worker</tt> class is used to post <tt>Task</tt>s or <tt>Job</tt>s that will be executed
sequentially in one Foxtrot Worker Thread. <br />
<p>The <tt>ConcurrentWorker</tt> class is used to post <tt>Task</tt>s or <tt>Job</tt>s that
will be executed each one in its own Foxtrot Worker Thread (thus <tt>Task</tt>s or <tt>Job</tt>s are
executed concurrently). <br />
<p>The <tt>AsyncWorker</tt> class is used to post <tt>AsyncTask</tt>s that will be executed each one
in its own Foxtrot Worker Thread (thus <tt>AsyncTask</tt>s are executed concurrently).</p>
<p>The <tt>Task</tt> class is subclassed by the user to perform heavy tasks that throw checked exceptions.</p>
<p>The <tt>Job</tt> class, conversely, is subclassed by the user to perform heavy tasks that do not throw
checked exceptions, but only RuntimeExceptions (or Errors).</p>
<p>The <tt>AsyncTask</tt> class is subclassed by the user to perform asynchronous heavy tasks and to post
an event to the Event Dispatch Thread when the <tt>AsyncTask</tt> is completed.</p>
<p>The <tt>Worker</tt> and <tt>ConcurrentWorker</tt> classes have the following two public methods that
can be used to post <tt>Task</tt>s or <tt>Job</tt>s:</p>
<ul>
<li><tt>public static Object post(Task task) throws Exception</tt></li>
<li><tt>public static Object post(Job job)</tt></li>
</ul>
<p>The <tt>Task</tt> class has a single abstract method that must be implemented by the user, with the
time-consuming code that may throw checked exceptions:</p>
<ul>
<li><tt>public abstract Object run() throws Exception</tt></li>
</ul>
<p>The <tt>Job</tt> class, conversely, has a single abstract method that must be implemented by the user,
with the time-consuming code that does not throw checked exceptions:</p>
<ul>
<li><tt>public abstract Object run()</tt></li>
</ul>
<p>The exceptions or errors thrown inside the <tt>Task.run()</tt> or <tt>Job.run()</tt> methods are
re-thrown by the corrispondent <tt>Worker.post(...)</tt> method <b>as is</b>, i.e. without being wrapped into,
for example, an InvocationTargetException.</p>
<p>The <tt>AsyncWorker</tt> class has only one public method that can be used to post <tt>AsyncTask</tt>s:</p>
<ul>
<li><tt>public static Object post(AsyncTask task)</tt></li>
</ul>
<p>The <tt>AsyncTask</tt> class has two abstract method that must be implemented by the user:</p>
<ul>
<li><tt>public abstract Object run() throws Exception</tt></li>
<li><tt>public abstract void finish()</tt></li>
</ul>
The <tt>run()</tt> method must be implemented with the time-consuming code exactly like the <tt>Task</tt>
class, while the <tt>finish()</tt> method must be implemented by calling the
<tt>AsyncTask.getResultOrThrow()</tt> method to get the result returned by the <tt>run()</tt> method (or to
rethrow any exception thrown during its execution), and eventually other code to be executed in the
Event Dispatch Thread.</p>

<p>Here's an example of <tt>Worker</tt> with the <tt>Job</tt> class:
<pre><span class="code">
Worker.post(new Job()
{
   public Object run()
   {
      // Here write the time-consuming code
      // that does not throw checked exceptions
   }
});
</span></pre></p>
<p>and here's an example of <tt>Worker</tt> with the <tt>Task</tt> class:
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
</span></pre></p>
<p>It is possible to narrow the throws clause of the <tt>Task</tt> class, but unfortunately not the one of
the <tt>Worker</tt> or <tt>ConcurrentWorker</tt> classes. <br />
So, when using the <tt>post(Task task)</tt> method, you have to surround it in a
<tt>try...catch(Exception x)</tt> block (unless the method that contains <tt>post(Task task)</tt>
throws <tt>Exception</tt> itself).
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
</span></pre></p>
<p>Here's an example of <tt>AsyncWorker</tt> with the <tt>AsyncTask</tt>:
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
</span></pre></p>
<p>All worker classes (from Foxtrot 2.x) have the following public methods to deal with the
<tt>WorkerThread</tt> component:</p>
<ul>
<li><tt>public static WorkerThread getWorkerThread()</tt></li>
<li><tt>public static void setWorkerThread(WorkerThread worker)</tt></li>
</ul>
<p>The <tt>Worker</tt> class (from Foxtrot 2.x) and the <tt>ConcurrentWorker</tt> class have also the
following public methods to deal with the <tt>EventPump</tt> component:</p>
<ul>
<li><tt>public static EventPump getEventPump()</tt></li>
<li><tt>public static void setEventPump(EventPump pump)</tt></li>
</ul>
<p>Foxtrot configures itself automatically with the most suitable implementation of
<tt>EventPump</tt> and <tt>WorkerThread</tt>.
Some implementations of <tt>EventPump</tt> or <tt>WorkerThread</tt> allow an even further customization
of the component.</p>
<p>For example, implementations of <tt>EventPump</tt> that also implement the
<tt>foxtrot.pumps.EventFilterable</tt> interface may allow the user to filter events that are being dispatched
by the <tt>java.awt.EventQueue</tt>. See also the bundled Javadocs for further details. <br />
However, it is recommended not to exploit these features unless knowing <strong>exactly</strong> what one is doing:
Foxtrot's defaults may change from version to version to suit better implementations, and these defaults may depend
on the Java Runtime Environment version Foxtrot is running on, so that features working in JDK 1.3.x may not work
in JDK 1.4.x or viceversa. <br />
Playing with AWT events too badly is normally looking for troubles, so consider you warned :)</p>
<p>The same holds for <tt>WorkerThread</tt> implementations, that should extend the abstract class
<tt>AbstractWorkerThread</tt>: replacing the default <tt>WorkerThread</tt> implementation may lead
to unexpected behavior.</p>
<p>Refer to the bundled Javadoc documentation for further information, and to the bundled examples for
further details on how to use the Foxtrot classes with Swing.<br />
And do not forget the <a href="tips.php">Tips 'n' Tricks</a> section !<p>

</td></tr>

<?php include 'footer.php';?>
