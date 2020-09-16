package Utils.Remote;

import Utils.Logs.JLog;
import com.jcraft.jsch.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

public class SSHManager  {
    private String userName, password, hostName;
    private int port;
    private ChannelSftp sftpChannel;
    private Session jschSession;
    ChannelExec channelExecute;


    public SSHManager(String userName, String password, String hostName,int port){
        this.userName = userName;
        this.password = password;
        this.hostName = hostName;
        this.port = port;
        sftpChannel =null;
        jschSession = null;
        channelExecute = null;
        Connect();

    }

    //limit creation of objects without connection parameters
    private SSHManager(){

    }

    public void Connect(){
        try {
            JSch jsch = new JSch();
            jschSession = jsch.getSession(userName, hostName, port);
            jschSession.setPassword(password);
            jschSession.setConfig("StrictHostKeyChecking", "no");
            //System.out.println("Establishing Connection...");
            jschSession.connect();
            //System.out.println("Connection established.");
            //System.out.println("Crating SFTP Channel.");

            sftpChannel = (ChannelSftp) jschSession.openChannel("sftp");
            sftpChannel.connect();
            //System.out.println("SFTP Channel created.");
            JLog.logger.info("Connected Successfully to host {}", hostName);



        }
        catch(Exception e){
                org.testng.Assert.fail("Could not create SSH connection to machine: " + hostName + "  user name: " + userName + " password: " + password + " port number: " + port +  "\n" + e.toString());
            }
    }


    public String Execute(String command) {
        try {
            channelExecute = (ChannelExec)jschSession.openChannel("exec");
            InputStream in = channelExecute.getInputStream();
            InputStream err = channelExecute.getErrStream();
            channelExecute.setCommand(command);
            channelExecute.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String response ="";
            String errResponse ="";
            String line="";

            while ((line = reader.readLine()) != null) {
                response += "\n" + line;
            }

            //System.out.println(response);

            BufferedReader errReader = new BufferedReader(new InputStreamReader(err));
            while ((line = errReader.readLine()) != null) {
                errResponse += "\n" + line;
            }
            if(!errResponse.isEmpty()) {
            	JLog.logger.error("Error response: " + errResponse);
            }

            JLog.logger.info("Execute " + command + " done!");
            return response;
        } catch (Exception e) {
            org.testng.Assert.fail("Could not execute command: " + command +  ". At machine: " + hostName + "\n" + e.toString());
            return null;
        }
        finally {
            channelExecute.disconnect();
        }

    }

    public boolean IsFileExists(String StringfilePath) {
        try {
            sftpChannel.lstat(StringfilePath);
        } catch (SftpException e) {
            return false;
        }
        return true;
    }

    public List<String> ListOfFiles (String path) {
        try {

               return  ListOfFilesWithoutExceptionProtection(path);

        } catch (Exception e) {
            org.testng.Assert.fail("Could not get list of content (ls) of folder:  " + path +  " at machine: " + hostName + "\n" + e.toString());
            return null;
        }
    }

    public List<String> ListOfFilesWithoutExceptionProtection (String path) throws SftpException {
            List<String> list = new ArrayList<String>();
            Vector<ChannelSftp.LsEntry> vector = sftpChannel.ls(path);
            for (ChannelSftp.LsEntry entry : vector) {
                list.add(entry.getFilename());
            }
            return list;
    }

    public void DeleteFile (String path) {
        try {
            sftpChannel.rm(path);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not delete the file:  " + path +  " at machine: " + hostName + "\n" + e.toString());
        }
    }

    public void CreateDirectory (String path) {
        try {
            sftpChannel.mkdir(path);
        }
        catch (Exception e) {
            org.testng.Assert.fail("Could not create directory:  " + path +  " at machine: " + hostName + "\n" + e.toString());
        }
    }


    public void CopyToLocal(String source, String destination) {
        try {
            sftpChannel.get(source, destination);
        } catch (Exception e) {
            org.testng.Assert.fail("Could not copy file from :  " + source +  " at machine: " + hostName + " to local destination: " + destination + "\n" + e.toString());
        }

    }

    public void CopyToRemote(String source, String destination) {
        try {
            sftpChannel.put(source, destination);
        } catch (Exception e) {
            org.testng.Assert.fail("Could not copy file from :  " + source +  " to machine: " + hostName + " local destination: " + destination + "\n" + e.toString());
        }

    }

    public void Close (){
        try {

            if (sftpChannel != null) {
                sftpChannel.quit();
            }
            if(jschSession != null ) {
                jschSession.disconnect();
            }
        }
        catch (Exception e){
            JLog.logger.error("Could not close SSH manager session" +  "\n" + e.toString());

        }
    }


    public String GetTextFromFile(String filePath){
        try {
            InputStream inputStream = sftpChannel.get(filePath);
            String text = "";
            try (Scanner scanner = new Scanner(new InputStreamReader(inputStream))) {
                while (scanner.hasNextLine()) {
                    text += scanner.nextLine();
                }
            }
            System.out.println(text);
            return text;
        }

        catch(Exception e){
            org.testng.Assert.fail("Error getting text from file: " + filePath +  "\n" + e.toString());
            return null;
        }
    }

    public void WriteTextToFile(String text, String filePath){
        try {
            File file = File.createTempFile("AutomationTemp", null);
            file.deleteOnExit();
            PrintWriter out = new PrintWriter(file.getAbsolutePath());
            out.print(text);
            out.close();
            sftpChannel.put(file.getAbsolutePath(),filePath);
        }

        catch(Exception e){
            org.testng.Assert.fail("Could not write text to file: " + filePath  + "\n" + e.toString() +"\n" + "text to write: " + text);
        }
    }



}

