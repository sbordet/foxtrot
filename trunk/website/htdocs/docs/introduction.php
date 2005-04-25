<?php include 'header.php';?>

<tr><td class="documentation">

<h2>Introduction</h2>

<p>Foxtrot is a small but powerful framework for using threads with the Java<sup><font="-2">TM</font></sup>
Foundation Classes (JFC/Swing).</p>
<p>It is compatible with the J2SE<sup><font="-2">TM</font></sup>, and has been tested with version 1.3.x, 1.4.x and 5.0.x.</p>
<p>The jar containing the Foxtrot core classes is called foxtrot.jar, and should be included in the classpath. <br />
Foxtrot version 1.2 does not require to be in the boot classpath anymore (previous versions did), and it is thus compatible
with the Java Web Start<sup><font="-2">TM</font></sup> technology. <br />
<p>Below you can find an example of how to set the foxtrot core classes in the classpath.</p>
<p>Let's suppose that your Swing application is contained in <code>my-swing.jar</code>, that the main class is called
<code>my.swing.Application</code>, and the Foxtrot core classes in <code>foxtrot.jar</code>; then you should start your application
with a command line similar to this one (under Windows):</p>
<pre>
> java -classpath foxtrot.jar;my-swing.jar my.swing.Application
</pre>
<p>or this one (under Linux):</p>
<pre>
> java -classpath foxtrot.jar:my-swing.jar my.swing.Application
</pre>
<p>The Foxtrot framework is released under the <a href="license.php">BSD license</a>.</p>
<p>It is possible to use Foxtrot to develop and sell commercial Swing applications, provided that the requirements of the BSD
license are met.<br />
These requirements aren't restrictive at all, and roughly say that when Swing applications developed using Foxtrot are
redistributed (along with Foxtrot's binaries or source code), the Foxtrot license must be redistributed as well, for example
in the documentation.<br />
A nice suggestion may be to use a Help/About dialog similar to Internet Explorer's, with a sentence like this: <br />
<b>"This application has been developed using Foxtrot (http://foxtrot.sourceforge.net). <br />
Foxtrot is released under the BSD license, see the included documentation for further information".</b></p>
</td></tr>

<?php include 'footer.php';?>
