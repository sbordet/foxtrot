/**
 * Copyright (c) 2002, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package java.awt;

import foxtrot.Task;

/**
 * Implementation of the package-private <code>java.awt.Conditional</code> interface to interact with the
 * <code>java.awt.EventDispatchThread</code> event pump.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision$
 */
public class FoxtrotConditional implements Conditional
{
	private Task m_task;

	public FoxtrotConditional(Task task)
	{
		m_task = task;
	}

	public boolean evaluate()
	{
		return !m_task.isCompleted();
	}
}
