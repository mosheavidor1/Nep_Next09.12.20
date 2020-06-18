package Applications;

import java.io.IOException;

public interface Application {
    void Launch(String applicationType, String proxyIP,boolean loadProxyExtension);
    void Launch(String applicationType);
    void LoadUrl(String URL);
    void MoveToNewWindow();
    void CloseCurrentWindow();
    void Close();

}
