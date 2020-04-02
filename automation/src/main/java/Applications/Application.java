package Applications;

import java.io.IOException;

public interface Application {
    void Launch(String applicationType, String proxyIP,boolean loadProxyExtension) throws IOException;
    void Launch(String applicationType) throws IOException;
    void LoadUrl(String URL);
    void MoveToNewWindow();
    void CloseCurrentWindow();
    void Close();

}
