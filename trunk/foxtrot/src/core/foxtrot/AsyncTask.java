package foxtrot;

import javax.swing.SwingUtilities;

/**
 *
 * @version $Revision$
 */
public abstract class AsyncTask extends Task
{
   public abstract void finish();

   void postRun()
   {
      SwingUtilities.invokeLater(new Runnable()
      {
         public void run()
         {
            finish();
         }
      });
   }
}
