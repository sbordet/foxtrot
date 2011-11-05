/**
 * Copyright (c) 2002-2008, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.pumps;

import java.awt.AWTEvent;
import java.awt.ActiveEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.EmptyStackException;

import foxtrot.EventPump;
import foxtrot.Task;

/**
 * This class is an attempt to use the push()/pop() mechanism of EventQueue
 * to obtain the Foxtrot effect.
 * However, it does not work :(
 * When calling pumpEvents() in a nested way (an event pumped by a first queue
 * enters a method that calls post() and thus pumpEvents() again), the mechanism
 * messes since pop() forwards the call to the latest queue. So if the first task
 * finished before the second, pop() on the first pushed queue is forwarded to
 * the second pushed queue, and when the second task finishes, calling pop()
 * on the second queue causes an exception.
 *
 * @version $Revision$
 */
public class StackEventPump implements EventPump
{
    private static final boolean debug = true;

    public void pumpEvents(Task task)
    {
        EventQueue oldQueue = getEventQueue();
        FoxtrotEventQueue newQueue = new FoxtrotEventQueue();

        if (debug) System.out.println("[FoxtrotEventQueue] About to push FoxtrotEventQueue " + newQueue);

        // Events are transferred to the new oldQueue, and a new event dispatch thread is started
        // The old event dispatch thread will stop dispatching events, and here we make it wait
        // for the task to be finished
        oldQueue.push(newQueue);

        if (debug) System.out.println("[FoxtrotEventQueue] Pushed FoxtrotEventQueue " + newQueue);
        try
        {
            waitForTask(task);
        }
        finally
        {
            if (debug) System.out.println("[FoxtrotEventQueue] About to pop FoxtrotEventQueue " + newQueue);
            newQueue.pop();
            if (debug) System.out.println("[FoxtrotEventQueue] Popped FoxtrotEventQueue " + newQueue);
        }
    }

    // From QueueEventPump
    private EventQueue getEventQueue()
    {
        return (EventQueue)AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                return Toolkit.getDefaultToolkit().getSystemEventQueue();
            }
        });
    }

    // From ConditionalEventPump
    private void waitForTask(Task task)
    {
        try
        {
            synchronized (task)
            {
                while (!task.isCompleted())
                {
                    task.wait();
                }
            }
        }
        catch (InterruptedException x)
        {
            // Someone interrupted the Event Dispatch Thread, re-interrupt
            Thread.currentThread().interrupt();
        }
    }

    private static class FoxtrotEventQueue extends EventQueue
    {
        // Overridden to make it visible
        protected void pop() throws EmptyStackException
        {
            super.pop();
        }

        public AWTEvent peekEvent()
        {
            // This call is only needed to flush all pending events
            // (see SunToolkit.flushPendingEvents())
//         EmptyAWTEvent flushEvent = new EmptyAWTEvent(this);
//         postEvent(flushEvent);
            AWTEvent event = super.peekEvent();
//         if (event == flushEvent) return null;

            if (debug) System.out.println("[FoxtrotEventQueue] Peeked event: " + event);
            return event;
        }

        public AWTEvent getNextEvent() throws InterruptedException
        {
            AWTEvent event = super.getNextEvent();
            if (debug) System.out.println("[FoxtrotEventQueue] Got event: " + event);
            return event;
        }
    }

    private static class EmptyAWTEvent extends AWTEvent implements ActiveEvent
    {
        public EmptyAWTEvent(Object source)
        {
            super(source, AWTEvent.RESERVED_ID_MAX + 1);
        }

        public final void dispatch()
        {
        }
    }
}
