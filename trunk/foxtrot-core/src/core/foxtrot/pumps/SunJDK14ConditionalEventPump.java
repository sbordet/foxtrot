/**
 * Copyright (c) 2002-2006, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.pumps;

import java.awt.AWTEvent;
import java.awt.EventQueue;

/**
 * Specialized ConditionalEventPump for Sun's JDK 1.4 and 5.0.
 * It fixes what I think is a misbehavior of {@link java.awt.EventQueue#peekEvent()},
 * that does not flush pending events to the EventQueue before peeking for them.
 *
 * @version $Revision$
 */
public class SunJDK14ConditionalEventPump extends ConditionalEventPump
{
    /**
     * Flushes pending events before waiting for the next event.
     * There is a mismatch between the behavior of {@link java.awt.EventQueue#getNextEvent()}
     * and {@link java.awt.EventQueue#peekEvent()}: the first always flushes pending events,
     * the second does not. This missing flushing is the reason why peekEvent() returns null
     * causing the proxy implementation of Conditional.evaluate() to never return
     */
    protected AWTEvent waitForEvent()
    {
        EventQueue queue = getEventQueue();
        AWTEvent nextEvent = peekEvent(queue);
        if (nextEvent != null) return nextEvent;

        while (true)
        {
            sun.awt.SunToolkit.flushPendingEvents();
            synchronized (queue)
            {
                nextEvent = peekEvent(queue);
                if (nextEvent != null) return nextEvent;
                if (debug) System.out.println("[SunJDK14ConditionalEventPump] Waiting for events...");
                try
                {
                    queue.wait();
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
    }
}
