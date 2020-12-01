package Actions;

import DataModel.UpdateEpDetails;
import Tests.GenericTest;
import Utils.Logs.JLog;
import Utils.NepDbConnector;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.time.Duration;
import java.time.LocalDateTime;

public class DbActions {
    protected static final int checkInterval = 5 ; // 5 sec
    public static ObjectMapper objectMapper = new ObjectMapper();


    static public void verifyCallToCentcom(LNEActions.CentcomMethods method, UpdateEpDetails json, String timestamp, int timeout) {
            String message = null;
        try {
            message = "CentCom call: " + method.getMethodName() + objectMapper.writeValueAsString(json);
            JLog.logger.info("Verifying " + message );

            NepDbConnector db = GenericTest.getDbConnector();

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);

            JLog.logger.debug("Waiting for DB entry to appear at centcom_calls table parameters : " +message);

            ResultSet rs=null;
            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                rs = db.getCentComCallByType(method.getMethodName(), json,timestamp);
                if(rs!=null && rs.isBeforeFirst()){
                    break;
                }
                Thread.sleep(checkInterval *1000);
                current = LocalDateTime.now();

            }

            if (rs == null) {
                org.testng.Assert.fail("Could not verify CentCom call as result received from DB is NULL " + message);
            }
            //if there is no result
            if (!rs.next()) {
                org.testng.Assert.fail("Could not verify CentCom call. No result found at DB for " + message);
            }
            // if there is more than one result
            if (rs.next()) {
                org.testng.Assert.fail("Could not verify CentCom call. More than 1 rows received for " + message);
            }

            JLog.logger.info("CentCom Call verified successfully. " + message);
        }
        catch ( Exception e){
            org.testng.Assert.fail("Could not verify CentCom call " + message + "\n" + e.toString());

        }
    }


    static public String getCurrentDbTimeStamp() {
        String timestamp =null;
        try {
            NepDbConnector db = GenericTest.getDbConnector();
            timestamp = db.GetDbCurrentTimestamp();

            if (timestamp == null) {
                org.testng.Assert.fail("Could not get DB current timestamp as result received is NULL. Query: " +NepDbConnector.getDbTimestamp );
            }
            JLog.logger.debug("DB current timestamp received. " + timestamp);
            return timestamp;
        }
        catch ( Exception e){
            org.testng.Assert.fail("Could get DB timestamp. Query:" + NepDbConnector.getDbTimestamp + " Result: "+ timestamp + "\n" + e.toString());
            return null;
        }
    }


}
