/**
 * Copyright (c) 2002, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot;

/**
 * A time-consuming task to be executed in the Worker Thread. <p>
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
 * Exception thrown by the <code>run()</code> method will be rethrown automatically by
 * {@link Worker#post Worker.post(Task task)}.
 *
 * @see Worker
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision$
 */
public abstract class Task
{
	private Object m_result;
	private Exception m_exception;
	private boolean m_completed;

	/**
	 * The method to implement with time-consuming code.
	 */
	public abstract Object run() throws Exception;

	/**
	 * Synchronized since the variable is accessed from 2 threads
	 * Package protected, used by Worker
	 */
	synchronized Object getResult() throws Exception
	{
		Exception x = getException();
		if (x != null) throw x;
		return m_result;
	}

	/**
	 * Synchronized since the variable is accessed from 2 threads
	 * Package protected, used by Worker
	 */
	synchronized void setResult(Object o)
	{
		m_result = o;
	}

	/**
	 * Synchronized since the variable is accessed from 2 threads
	 * Package protected, used by Worker
	 */
	synchronized void setException(Exception x)
	{
		m_exception = x;
	}

	/**
	 * Synchronized since the variable is accessed from 2 threads
	 */
	private synchronized Exception getException()
	{
		return m_exception;
	}

	/**
	 * Synchronized since the variable is accessed from 2 threads
	 * Package protected, used by Worker
	 */
	synchronized void setCompleted(boolean value)
	{
		m_completed = value;
	}

	/**
	 * Synchronized since the variable is accessed from 2 threads
	 * Package protected, used by Worker
	 */
	synchronized boolean isCompleted()
	{
		return m_completed;
	}
}
