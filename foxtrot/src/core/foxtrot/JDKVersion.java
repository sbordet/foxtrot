package foxtrot;

/**
 * Helper class that returns which is the current JDK version
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision$
 */
class JDKVersion
{
   private static Boolean jdk14;
   private static Boolean jdk13;
   private static Boolean jdk12;

   static boolean isJDK14()
   {
      if (jdk14 == null) jdk14 = canLoadClass("java.nio.ByteBuffer");
      return jdk14.booleanValue();
   }

   static boolean isJDK13()
   {
      if (jdk13 == null) jdk13 = canLoadClass("java.lang.reflect.Proxy");
      return jdk13.booleanValue();
   }

   static boolean isJDK12()
   {
      if (jdk12 == null) jdk12 = canLoadClass("java.util.Collection");
      return jdk12.booleanValue();
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
