package foxtrot.test;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 *
 * @version $Revision$
 */
public class TestRunner
{
	public static void main(String[] args) throws Exception
	{
		// This usage of classloaders tries to simulate Java WebStart functionality
		// The context classloader must be set before the EventDispatchThread is initialized,
		// otherwise it will be difficult to change it later due to security constraints.
		File core = new File("dist/lib/foxtrot.jar");
		File tests = new File("dist/lib/foxtrot-test.jar");
//		URLClassLoader coreLoader = new URLClassLoader(new URL[] {core.toURL()}, TestRunner.class.getClassLoader());
//		URLClassLoader testsLoader = new URLClassLoader(new URL[] {tests.toURL()}, coreLoader);
		URLClassLoader testsLoader = new URLClassLoader(new URL[] {core.toURL(), tests.toURL()}, TestRunner.class.getClassLoader());
		Thread.currentThread().setContextClassLoader(testsLoader);

		// Create the frame
		final JFrame frame = new JFrame("Foxtrot Test");
		Container pane = frame.getContentPane();
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		frame.setSize(800, 600);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension size = frame.getSize();
		int x = (screen.width - size.width) >> 1;
		int y = (screen.height - size.height) >> 1;
		frame.setLocation(x, y);
		frame.setVisible(true);

		Class cls = testsLoader.loadClass("foxtrot.test.FoxtrotTest");
		final Object target = cls.newInstance();

		// Run the tests
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				System.out.println("Start Testing");

				int count = 0;
				int success = 0;
//				Object[] arg = new Object[0];
				Object[] arg = new Object[] {frame};

				Method[] methods = target.getClass().getMethods();
				for (int i = 0; i < methods.length; ++i)
				{
					Method method = methods[i];
					String name = method.getName();
					if (name.startsWith("test") &&
						method.getReturnType() == Void.TYPE &&
//						method.getParameterTypes().length == 0)
						method.getParameterTypes().length == 1 &&
						method.getParameterTypes()[0] == JFrame.class)
					{
						try
						{
							++count;
							System.out.println("Executing Test '" + name + "'");
							method.invoke(target, arg);
							++success;
							System.out.println("Test '" + name + "' SUCCESSFUL");
						}
						catch (IllegalAccessException x) {x.printStackTrace();}
						catch (InvocationTargetException x)
						{
							Throwable t = x.getTargetException();
							System.out.println("Test '" + name + "' FAILED !");
							t.printStackTrace();
						}
					}
				}

				String result = "Testing Completed: " + count + " Tests - " + success + " successful - " + (count - success) + " failed";
				JLabel label = new JLabel(result);
				frame.getContentPane().add(label);
				frame.getContentPane().validate();
				System.out.println(result);
			}
		});
	}
}
