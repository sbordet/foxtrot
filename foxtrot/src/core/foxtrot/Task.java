/**
 * Copyright (c) 2002, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot;

import java.security.AccessControlContext;
import java.security.AccessController;

/**
 * A time-consuming task to be executed in the Worker Thread that may throw checked exceptions. <p>
 * Users must implement the {@link #run} method with the time-consuming code, and not worry about
 * exceptions, for example:
 * <pre>
 * Task task = new Task()
 * {
 *     public Object run() throws InterruptedException
 *     {
 *        Thread.sleep(10000);
 *        return null;
 *     }
 * };
 * </pre>
 * Exceptions and Errors thrown by the <code>run()</code> method will be rethrown automatically by
 * {@link Worker#post(Task) Worker.post(Task task)}.
 *
 * @see Worker
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision$
 */
public abstract class Task
{
	private Object m_result;
	private Throwable m_throwable;
	private boolean m_completed;
	private AccessControlContext m_securityContext;

	/**
	 * Creates a new Task
	 */
	protected Task()
	{
		m_securityContext = AccessController.getContext();
	}

	/**
	 * The method to implement with time-consuming code.
	 * It must NOT be synchronized or synchronize on this instance.
	 */
	public abstract Object run() throws Exception;

	/**
	 * Returns the result of this Task operation, as set in {@ #setResult}.
	 * If an exception or an error is thrown by {@link #run}, it is rethrown here.
	 * Synchronized since the variable is accessed from 2 threads.
	 * Accessed by the AWT Event Dispatch Thread.
	 * Package protected, used by Worker
	 */
	final synchronized Object getResultOrThrow() throws Exception
	{
		Throwable t = getThrowable();
		if (t != null)
		{
			if (t instanceof Exception) throw (Exception)t;
			else throw (Error)t;
		}
		return getResult();
	}

	/**
	 * Returns the result of this Task operation
	 * Synchronized since the variable is accessed from 2 threads
	 * Accessed by the AWT Event Dispatch Thread.
	 * Package protected, used by Worker
	 * @see #setResult
	 * @see #getResultOrThrow
	 */
	final synchronized Object getResult()
	{
		return m_result;
	}

	/**
	 * Sets the result of this Task operation, as returned by the {@link #run} method.
	 * Synchronized since the variable is accessed from 2 threads
	 * Accessed by the Foxtrot Worker Thread.
	 * Package protected, used by Worker
	 * @see #getResultOrThrow
	 * @see #getResult
	 */
	final synchronized void setResult(Object o)
	{
		m_result = o;
	}

	/**
	 * Returns the throwable as set in {@link #setThrowable}.
	 * Synchronized since the variable is accessed from 2 threads
	 * Accessed by the AWT Event Dispatch Thread.
	 */
	final synchronized Throwable getThrowable()
	{
		return m_throwable;
	}

	/**
	 * Sets the throwable eventually thrown by the {@link #run} method.
	 * Synchronized since the variable is accessed from 2 threads
	 * Accessed by the Foxtrot Worker Thread.
	 * Package protected, used by Worker
	 * @see #getThrowable
	 */
	final synchronized void setThrowable(Throwable x)
	{
		m_throwable = x;
	}

	/**
	 * Returns if this Task is completed.
	 * Synchronized since the variable is accessed from 2 threads
	 * Accessed by the AWT Event Dispatch Thread.
	 */
	public final synchronized boolean isCompleted()
	{
		return m_completed;
	}

	/**
	 * Marks this Task as completed.
	 * Synchronized since the variable is accessed from 2 threads
	 * Accessed by the Foxtrot Worker Thread.
	 * Package protected, used by Worker
	 * @see #isCompleted
	 */
	final synchronized void completed()
	{
		m_completed = true;
	}

	/**
	 * Returns the protection domain stack at the moment of instantiation of this Task
	 * Accessed by the Foxtrot Worker Thread.
	 * Package protected, used by Worker
	 */
	final synchronized AccessControlContext getSecurityContext()
	{
		return m_securityContext;
	}
}
