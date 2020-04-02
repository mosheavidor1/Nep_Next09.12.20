package Utils.TestNG;

import Applications.SeleniumBrowser;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

public class InvokedMethodListener implements IInvokedMethodListener {
    public static String video ="";
    public static String screenShot ="";

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult result) {
        if (method.isTestMethod() && ITestResult.FAILURE == result.getStatus()) {
            Throwable throwable = result.getThrowable();
            String originalMessage = throwable.getMessage();
            String newMessage = originalMessage +"\n\nVideo: " + video+"\n";
            if(SeleniumBrowser.InstanceExist())
                newMessage = newMessage + "ScreenShot: " +screenShot + "\n";
            newMessage = newMessage + "\n";

            try {
                FieldUtils.writeField(throwable, "detailMessage", newMessage, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
