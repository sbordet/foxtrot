package foxtrot;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.swing.SwingUtilities;

/**
 * This implementation of EventPump calls the package protected method
 * <code>java.awt.EventDispatchThread.pumpEvents(Conditional)</code> and can be used with JDK 1.3+ only.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision$
 */
public class ConditionalEventPump implements EventPump
{
   private static Class conditionalClass;
   private static Method pumpMethod;

   static
   {
      try
      {
         AccessController.doPrivileged(new PrivilegedExceptionAction()
         {
            public Object run() throws ClassNotFoundException, NoSuchMethodException
            {
               ClassLoader loader = ClassLoader.getSystemClassLoader();
               conditionalClass = loader.loadClass("java.awt.Conditional");
               Class dispatchThreadClass = loader.loadClass("java.awt.EventDispatchThread");
               pumpMethod = dispatchThreadClass.getDeclaredMethod("pumpEvents", new Class[]{conditionalClass});
               pumpMethod.setAccessible(true);

               // See remarks for use of this property in java.awt.EventDispatchThread
               String property = "sun.awt.exception.handler";
               String handler = System.getProperty(property);
               if (handler == null)
               {
                  handler = ThrowableHandler.class.getName();
                  if (Worker.debug) System.out.println("Installing AWT Throwable Handler " + handler);
                  System.setProperty(property, handler);
                  if (Worker.debug) System.out.println("Using newly installed AWT Throwable Handler " + handler);
               }
               else
               {
                  if (Worker.debug) System.out.println("Using already installed AWT Throwable Handler " + handler);
               }
               return null;
            }
         });
      }
      catch (PrivilegedActionException x)
      {
         if (Worker.debug) x.printStackTrace();
         throw new Error(x.toString());
      }
      catch (RuntimeException x)
      {
         if (Worker.debug) x.printStackTrace();
         throw x;
      }
      catch (Exception x)
      {
         if (Worker.debug) x.printStackTrace();
         throw new Error(x.toString());
      }
   }

   /**
    * Implements the <code>java.awt.Conditional</code> interface, that is package private,
    * with a JDK 1.3+ dynamic proxy.
    */
   private static class Conditional implements InvocationHandler
   {
      private Task task;

      /**
       * Creates a new invocation handler for the given task.
       */
      private Conditional(Task task)
      {
         this.task = task;
      }

      /**
       * Method <code>java.awt.Conditional.evaluate()</code> is implemented here to return
       * true if the {@link Task} associated with this Conditional is NOT completed, and
       * to return false if the associated task is completed.
       */
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
      {
         String name = method.getName();
         if ("evaluate".equals(name))
         {
            return task.isCompleted() ? Boolean.FALSE : Boolean.TRUE;
         }
         throw new Error("Unknown java.awt.Conditional method: " + name);
      }
   }

   /**
    * Handler for RuntimeExceptions or Errors thrown during dispatching of AWT events. <br>
    * The name of this class is used as a value of the property <code>sun.awt.exception.handler</code>,
    * and the AWT event dispatch mechanism calls it when an unexpected runtime exception or error
    * is thrown during event dispatching. If the user specifies a different exception handler,
    * this one will not be used, and the user's one is used instead.
    */
   public static class ThrowableHandler
   {
      /**
       * The callback method invoked by the AWT event dispatch mechanism when an unexpected
       * exception or error is thrown during event dispatching. <br>
       * It just logs the exception.
       */
      public void handle(Throwable t)
      {
         System.err.println("Foxtrot - Exception occurred during event dispatching:");
         t.printStackTrace();
      }
   }

   public ConditionalEventPump()
   {
   }

   public void pumpEvents(Task task)
   {
      if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("This method can be called only from the AWT Event Dispatch Thread");

      if (Worker.debug) System.out.println("EventPump " + this + " pumping events for task: " + task);

      // A null task is passed for initialization of this class.
      if (task == null) return;

      try
      {
         if (Worker.debug) System.out.println("EventPump " + this + " starts dequeueing events from standard AWT Event Queue");
         // Invoke java.awt.EventDispatchThread.pumpEvents(new Conditional(task));
         // which will block until the task is completed
         Object conditional = Proxy.newProxyInstance(conditionalClass.getClassLoader(), new Class[]{conditionalClass}, new Conditional(task));
         pumpMethod.invoke(Thread.currentThread(), new Object[]{conditional});
      }
      catch (InvocationTargetException x)
      {
         // No exceptions should escape from java.awt.EventDispatchThread.pumpEvents(Conditional)
         // since we installed a throwable handler. But one provided by the user may fail.
         Throwable t = x.getTargetException();
         System.err.println("Foxtrot - Exception occurred during event dispatching:");
         t.printStackTrace();

         System.err.println("Foxtrot - WARNING: uncaught exception during event dispatching, Task " + task + " may still be running !");

         // Rethrow. This will exit from Worker.post with a runtime exception or an error, and
         // the original event pump will take care of it. Beware that the Task will continue to run
         // It should never happen: the contract of the Worker.post method is strong: don't return
         // until the Task has finished. We use awt exception handler to enforce this contract, but
         // an awt exception handler provided by the user may fail to respect this contract.
         if (t instanceof RuntimeException) {throw (RuntimeException)t;}
         else {throw (Error)t;}
      }
      catch (Throwable x)
      {
         // Here we have an implementation bug
         System.err.println("Foxtrot - WARNING: uncaught exception in Foxtrot code, Task " + task + " is still running !");
         x.printStackTrace();
      }
      finally
      {
         if (Worker.debug) System.out.println("Event pump " + this + " stops dequeueing events from standard AWT Event Queue");
      }
   }
}
