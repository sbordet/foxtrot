/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.pumps;

import java.awt.AWTEvent;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.LinkedList;

/**
 * Specialized class for Sun's JDK 1.4.0
 *
 * @version $Revision$
 */
public class SunJDK140ConditionalEventPump extends SunJDK14ConditionalEventPump
{
   private static Field listField;

   static
   {
      try
      {
         AccessController.doPrivileged(new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               listField = sequencedEventClass.getDeclaredField("list");
               listField.setAccessible(true);
               return null;
            }
         });
      }
      catch (Throwable x)
      {
         throw new Error(x.toString());
      }
   }

   protected Boolean canPumpSequencedEvent(AWTEvent event)
   {
      try
      {
         LinkedList list = (LinkedList)listField.get(event);
         synchronized (sequencedEventClass)
         {
            if (list.getFirst() == event) return Boolean.TRUE;
         }
      }
      catch (Exception x)
      {
      }
      return Boolean.FALSE;
   }
}
