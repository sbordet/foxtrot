<?php include 'header.php'; ?>

<h2>Known Foxtrot Limitations</h2>
<p>The mechanism used by <tt>Worker</tt> and <tt>ConcurrentWorker</tt> to block and
re-route the Event Dispatch Thread is very similar to the mechanism used by JDialog,
but not exactly the same because the API is private to package <tt>java.awt</tt>.</p>
<p>With JDK 1.4.x, 5.0.x and 6.0.x, it is possible that in some case (more specifically
in some case that involves focus events or window focus events), Foxtrot is not able
to start the event pumping mechanism that ensures that the user interface does not 
"freeze".<br/>
In these cases, Foxtrot will execute the task in the worker thread, while AWT events
will add up in the event queue. When the task completes and the listener returns,
the events will be dequeued normally by the Event Dispatch Thread.<br/>
The result is that the user interface will appear to be "frozen", exactly as if the
time-consuming task is executed in the Event Dispatch Thread.</p>
<p>For maximum user interface responsiveness, it is better to use <tt>AsyncWorker</tt>
in the implementation of methods of <tt>java.awt.event.FocusListener</tt> and
<tt>java.awt.event.WindowFocusListener</tt>.

<?php include 'footer.php'; ?>
