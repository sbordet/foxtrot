/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.pumps;

import java.awt.AWTEvent;

/**
 * Filters AWT events pumped by {@link foxtrot.EventPump EventPump}s before they're dispatched.
 * @see EventFilterable
 * @version $Revision$
 */
public interface EventFilter
{
   /**
    * Callback called by {@link foxtrot.EventPump EventPump}s to filter the given AWT event.
    * @param event The event to filter
    * @return True if the event should be dispatched, false otherwise
    */
   public boolean accept(AWTEvent event);
}
