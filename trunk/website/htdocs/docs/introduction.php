<?php include 'header.php';?>

<tr><td align="center" colspan="3">
<a href="http://foxtrot.sourceforge.net"><img class="nav-btn-en" hspace="10" src="../images/cnvhome.gif"/></a>
<a href="toc.php"><img class="nav-btn-en" hspace="10" src="../images/cnvup.gif"/></a>
<img class="nav-btn-dis" hspace="10" src="../images/cnvprev.gif"/>
<a href="license.php"><img class="nav-btn-en" hspace="10" src="../images/cnvnext.gif"/></a>
</td></tr>

<tr><td class="date" colspan="3">Last Updated: $Date$</td></tr>

<tr><td class="documentation">

<h2>Introduction</h2>

<p>Foxtrot is a small but powerful framework for using threads with the Java<sup><font="-2">TM</font></sup>
Foundation Classes (JFC/Swing).</p>
<p>It is compatible with the J2SE<sup><font="-2">TM</font></sup>, and has been tested with version 1.3.x and 1.4.x.</p>
<p>The jar containing the Foxtrot core classes (namely foxtrot.jar), from Foxtrot version 1.1, must be included in
the boot classpath.<br>
This is due to the fact that in the Java Development Kit (JDK<sup><font="-2">TM</font></sup>), version 1.4.x, the
mechanism of event dispatching has changed; consequently, for Foxtrot to be compatible with JDK 1.4.x and 1.3.x, the
Foxtrot implementation is required to be in the boot classpath.</p>
<p>Below you can find an example of how to set the foxtrot core classes in the boot classpath.</p>
<p>Let's suppose that your Swing application is contained in my-swing.jar, that the main class is called my.swing.Application,
and the Foxtrot core classes in foxtrot.jar; then you should start your application with a command line similar to this one:</p>
<pre>
> java -Xbootclasspath/a:foxtrot.jar -classpath my-swing.jar my.swing.Application
</pre>
<p>The Foxtrot framework is released under the BSD license.</p>

</td></tr>

<?php include 'footer.php';?>
