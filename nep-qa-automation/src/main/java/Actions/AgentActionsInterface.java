package Actions;

public interface AgentActionsInterface  {

    
    public void installEndpointWithoutAdditions(int timeout);
    
    public void uninstallEndpoint(int timeout);
    
    public void startEPService(int timeout);
    public void stopEPService(int timeout);
    
    public String getVerifySiemCommand();
    public String getVerifyLcaCommand();
    public String getVerifyLca2Command();
    
    public String getAgentLogPath();
    
    public String getDownloadFolder();
    
    public String getInstallationFile();
    
    public String getRemoteCaFile();
    
    public String getConfigPath();
    
    public String getHostsFile();
    
    public String getDbJsonPath();
    
    public String getClearFileCommand();
    
    public String getVersionJsonPath();
    
    public String getStartCommand();
    
    public void writeAndExecute(String text );
    
    public boolean endpointServiceRunning();
    
}

