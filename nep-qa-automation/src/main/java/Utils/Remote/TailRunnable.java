package Utils.Remote;

import Actions.LNEActions;
import Utils.Logs.JLog;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;

public class TailRunnable implements Runnable {
    LNEActions.NepService nepService;
    private Session jschSession;
    ChannelExec channelExecute;
    String hostName;


    public TailRunnable(LNEActions.NepService nepService, String hostname, Session jschSession) {
        this.nepService = nepService;
        this.jschSession=jschSession;
        this.hostName=hostname;
    }

    @Override
    public void run() {
        boolean res = false;
        String file = nepService.getLogFileName();
        String strToFind = nepService.getStartedString();
        String command = "tail -f "+file;

        try {
            ChannelExec channelExecute = (ChannelExec)jschSession.openChannel("exec");

            InputStream in = channelExecute.getInputStream();
            InputStream err = channelExecute.getErrStream();
            channelExecute.setCommand(command);
            channelExecute.connect();
            JLog.logger.debug("Executing tail -f on file " + file);

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line="";

            Instant started = Instant.now();

            while ((line = reader.readLine()) != null) {
//                JLog.logger.info(line);
                if(line.contains(strToFind)){
                    JLog.logger.info("service {} started",file);
                    break;
                }
            }

        }
        catch(Exception e) {
            org.testng.Assert.fail("Could not execute command: " + command +  ". At machine: " + hostName + "\n" + e.toString());
        }
        finally {
            if (channelExecute != null) {
                channelExecute.disconnect();
            }
        }
    }
}
