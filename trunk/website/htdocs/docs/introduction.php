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
<p>The jar containing the Foxtrot core classes is called foxtrot.jar, and should be included in the classpath. <br>
Foxtrot version 1.2 does not require to be in the boot classpath (previous versions did), and it is thus compatible
with the Java Web Start<sup><font="-2">TM</font></sup> technology.<br>
<p>Below you can find an example of how to set the foxtrot core classes in the classpath.</p>
<p>Let's suppose that your Swing application is contained in my-swing.jar, that the main class is called
my.swing.Application, and the Foxtrot core classes in foxtrot.jar; then you should start your application
with a command line similar to this one (under Windows):</p>
<pre>
> java -classpath foxtrot.jar;my-swing.jar my.swing.Application
</pre>
<p>or this one (under Linux):</p>
<pre>
> java -classpath foxtrot.jar:my-swing.jar my.swing.Application
</pre>
<p>The Foxtrot framework is released under the BSD license.</p>

</td></tr>

<?php include 'footer.php';?>
