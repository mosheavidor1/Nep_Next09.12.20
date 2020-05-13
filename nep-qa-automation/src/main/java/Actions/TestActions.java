package Actions;

import Applications.Application;
import Applications.SeleniumBrowser;

import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class TestActions {
	
	private Application application;
	
	public TestActions() {
		application = SeleniumBrowser.GetInstance();
	}

    public void SetApplicationUrl(String Url)
    {
        application.LoadUrl(Url);
    }

    public void CloseApplication()
    {
        application.Close();
    }

    public void LaunchApplication(String ApplicationType, String proxyIP) throws IOException {
        application.Launch(ApplicationType, proxyIP, false);
    }

    public void LaunchApplicationWithProxyExtension(String ApplicationType, String proxyIP) throws IOException {
        application.Launch(ApplicationType, proxyIP, true);
    }

    public void LaunchApplication(String ApplicationType) throws IOException {
        application.Launch(ApplicationType);
    }

    public void CloseCurrentWindow()
    {
        application.CloseCurrentWindow();
    }

    public static String execCmd(String cmd, boolean runAsAdmin) throws java.io.IOException {
	    final String elevatePath = "C:\\Selenium\\Utils\\Elevate.exe";

        if (runAsAdmin) {
            File file = new File(elevatePath);
            if( ! file.exists())
                org.testng.Assert.fail("Could not run commands as administrator. Please copy missing file: " + elevatePath);
        }

        if (runAsAdmin)
	        cmd = elevatePath + " " + cmd;

        java.util.Scanner s = new java.util.Scanner(Runtime.getRuntime().exec(cmd).getInputStream()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }


    public static void WriteToWindowsEventLog () throws InterruptedException, IOException {

            Process process = Runtime.getRuntime().exec("EventCreate /t INFORMATION /id 123 /l APPLICATION /so Java /d \"Rosetta Code Example\"");
            process.waitFor(10, TimeUnit.SECONDS);
            int exitValue = process.exitValue();
            System.out.printf("Process exited with value %d\n", exitValue);
            if (exitValue != 0) {
                InputStream errorStream = process.getErrorStream();
                String result = new BufferedReader(new InputStreamReader(errorStream))
                        .lines()
                        .collect(Collectors.joining("\n"));
                System.err.println(result);
            }

    }



}
