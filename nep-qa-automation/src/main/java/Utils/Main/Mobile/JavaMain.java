package Utils.Main.Mobile;

import org.testng.TestListenerAdapter;
import org.testng.TestNG;

public class JavaMain {

	public static void main(String[] args) {
		TestListenerAdapter tla = new TestListenerAdapter();
		TestNG testng = new TestNG();
		testng.setTestClasses(new Class[] { Tests.Mobile.InstallApplication.class });
		testng.addListener(tla);
		testng.run();

	}

}
