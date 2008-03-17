<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html>

<head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8" />
    <meta name="description" content="Foxtrot - Easy API for JFC/Swing" />
    <meta name="keywords" content="Foxtrot,Swing,Threads,Swing Threads,SwingWorker,Worker,SwingUtilities,SwingUtilities.invokeLater,invokeLater,AWT,Simone Bordet" />
    <link rel="stylesheet" type="text/css" href="/styles.css" media="screen" />
    <title>Foxtrot - Easy API for JFC/Swing</title>
</head>

<body>

<div class="header">

    <div class="left">
        <h2>Foxtrot</h2>
    </div>

    <div class="right">
        <a href="http://sourceforge.net">
            <img src="http://sourceforge.net/sflogo.php?group_id=49197&type=4" width="125" height="37" alt="SourceForge.net Logo"/>
        </a>
        <a href="http://www.java.net">
            <img src="/images/javanet.gif" alt="Java.net Logo"/>
        </a>
    </div>

</div>

<div class="container">

    <div class="navigation">
        <a href="/docs/index.php">User Guide</a>
        <a href="http://sourceforge.net/project/showfiles.php?group_id=49197">Download</a>
        <a href="http://sourceforge.net/projects/foxtrot/">Development</a>
        <a href="http://sourceforge.net/mail/?group_id=49197">Mailing Lists</a>
        <div class="clearer"></div>
    </div>

    <div class="main-right">

        <div class="content">
            <h2>Overview</h2>
            <p><b>Foxtrot</b> is an easy and powerful API to use threads with the Java<sup><font size="-2">TM</font></sup> Foundation Classes (JFC/Swing).</p>
            <p>The Foxtrot API are based on a new concept, the <b>Synchronous Model</b>, that allow you to easily integrate
            in your Swing code time-consuming operations without incurring in "GUI-freeze" problem, typical of Swing applications.</p>
            <p>While other solutions have been developed to solve this problem, being the
            <a href="http://java.sun.com/products/jfc/tsc/articles/threads/threads3.html">SwingWorker</a>
            (see also <a href="http://java.sun.com/products/jfc/tsc/articles/threads/update.html">here</a> for an update)
            the most known, they are all based on the Asynchronous Model which, for non-trivial Swing applications,
            carries several problems such as code asymmetry, bad code readability and difficult exception handling.</p>
            <p>The Foxtrot API cleanly solves the problems that solutions based on the Asynchronous Model have, and it's
            simpler to use.<br />
            Your Swing code will immediately benefit of:
            <ul>
                <li>code symmetry and readability
                <li>easy exception handling
                <li>improved mantainability
            </ul>
            </p>
        </div>

        <div class="news">
            <h2>News</h2>
            <?php include 'http://sourceforge.net/export/projnews.php?group_id=49197&limit=3&flat=0&show_summaries=0'; ?></p>
        </div>

        <div class="clearer"></div>

    </div>

    <?php include 'footer.php'; ?>

</div>

<?php include 'ga.php'; ?>

</body>
</html>
