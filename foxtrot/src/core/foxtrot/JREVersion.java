/**
 * Copyright (c) 2002, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot;

/**
 * Helper class that returns which is the current JRE version
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision$
 */
class JREVersion
{
   private static Boolean jre14;
   private static Boolean jre13;
   private static Boolean jre12;

   static boolean isJRE14()
   {
      if (jre14 == null) jre14 = canLoadClass("java.nio.ByteBuffer");
      return jre14.booleanValue();
   }

   static boolean isJRE13()
   {
      if (jre13 == null) jre13 = canLoadClass("java.lang.reflect.Proxy");
      return jre13.booleanValue();
   }

   static boolean isJRE12()
   {
      if (jre12 == null) jre12 = canLoadClass("java.util.Collection");
      return jre12.booleanValue();
   }

   private static Boolean canLoadClass(String className)
   {
      // Avoid some smart guy puts the classes in the classpath or in lib/ext.
      // We ask directly to the boot classloader
      try
      {
         Class.forName(className, false, null);
         return Boolean.TRUE;
      }
      catch (ClassNotFoundException ignored)
      {
      }
      return Boolean.FALSE;
   }
}
