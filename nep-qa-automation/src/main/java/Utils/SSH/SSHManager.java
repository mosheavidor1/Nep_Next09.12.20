package Utils.SSH;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

public class SSHManager {
    private String userName, password, hostName;
    int port;
    private ChannelSftp sftpChannel;

    public SSHManager(String userName, String password, String hostName,int port){
        this.userName = userName;
        this.password = password;
        this.hostName = hostName;
        this.port = port;
        Connect();

    }

    public void Connect(){
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(userName, hostName, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            System.out.println("Establishing Connection...");
            session.connect();
            System.out.println("Connection established.");
            System.out.println("Crating SFTP Channel.");

            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            System.out.println("SFTP Channel created.");
        }
        catch(Exception e){
                org.testng.Assert.fail("Could not create SSH connection to machine: " + hostName + "  user name: " + userName + " password: " + password + " port number: " + port +  "\n" + e.toString());
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


    public void CopyToLocal(String source, String destination) {
        try {
            sftpChannel.get(source, destination);
        } catch (Exception e) {
            org.testng.Assert.fail("Could not copy file from :  " + source +  " at machine: " + hostName + " to local destination: " + destination + "\n" + e.toString());
        }

    }


    public void printTextFile(String filePath){
        try {
            InputStream inputStream = sftpChannel.get(filePath);

            try (Scanner scanner = new Scanner(new InputStreamReader(inputStream))) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    System.out.println(line);
                }
            }
        }
        catch(Exception e){
            org.testng.Assert.fail("Error printing file: " + filePath +  "\n" + e.toString());
        }
    }


}

