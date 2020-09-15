package Actions;

public interface AgentActionsInterface  {

    
    public void installEndpoint(int timeout);
    
    public void uninstallEndpoint(int timeout);
    
    public void startEPService(int timeout);
    public void stopEPService(int timeout);
    
    public String getVerifySiemCommand();
    public String getVerifyLcaCommand();
    public String getVerifyLca2Command();
    public String getVerifyLFMLca2Command();

    public String getAgentLogPath();
    
    public String getDownloadFolder();
    
    public String getInstallationFile();
    
    public String getRemoteCaFile();

    
    public String getHostsFile();
    
    public String getDbJsonPath();
    
    public String getClearFileCommand();
    
    public String getVersionJsonPath();
    
    public void writeAndExecute(String text );
    
    public boolean endpointServiceRunning();
    public String getScriptName(String scriptName);
    public String getConfigPath(boolean afterUpdate);
}

