/**
 * Copyright (c) 2002, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.io.InterruptedIOException;

/**
 * Partial implementation of the WorkerThread interface.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision$
 */
public abstract class AbstractWorkerThread implements WorkerThread
{
   /**
    * Creates a new instance of this AbstractWorkerThread, called by subclasses.
    */
   protected AbstractWorkerThread()
   {
   }

   public void runTask(final Task task)
   {
      if (Worker.debug) System.out.println("[AbstractWorkerThread] Executing task " + task);

      try
      {
         Object obj = AccessController.doPrivileged(new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return task.run();
            }
         }, task.getSecurityContext());

         task.setResult(obj);
      }
      catch (PrivilegedActionException x)
      {
         Exception xx = x.getException();
         task.setThrowable(xx);
         if (xx instanceof InterruptedException || xx instanceof InterruptedIOException) Thread.currentThread().interrupt();
      }
      catch (Throwable x)
      {
         task.setThrowable(x);
      }
      finally
      {
         // Mark the task as completed
         task.setCompleted(true);

         if (Worker.debug) System.out.println("[AbstractWorkerThread] Completing run for task " + task);
         task.postRun();
      }
   }
}
