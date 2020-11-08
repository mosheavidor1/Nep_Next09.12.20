package Actions;

import Tests.GenericTest;
import Utils.Logs.JLog;
import Utils.NepDbConnector;

import java.sql.ResultSet;
import java.time.Duration;
import java.time.LocalDateTime;

public class DbActions {
    protected static final int checkInterval = 5 ; // 5 sec


    static public void verifyCallToCentcom(LNEActions.CentcomMethods method, String endpointName, String endpointOldName, String endPointIP ,String customerID, String timestamp, int timeout) {
        String message = "CentCom call: " + method.getMethodName() + " Endpoint name: " + endpointName + " Endpoint IP: "+ endPointIP + " Customer id: " + customerID + " after timestamp: " + timestamp  ;
        JLog.logger.info("Verifying " + message + " Query: " + NepDbConnector.VERIFY_CENTCOM_CALLS);
        try {
            NepDbConnector db = GenericTest.getDbConnector();

            LocalDateTime start = LocalDateTime.now();
            LocalDateTime current = start;
            Duration durationTimeout = Duration.ofSeconds(timeout);

            ResultSet rs = db.getCentComCallByType(method.getMethodName(), endpointName, endpointOldName, endPointIP  ,customerID, timestamp);
            message = message + " Parametrized Query: " + NepDbConnector.verifyCentComCalls;


            while (durationTimeout.compareTo(Duration.between(start, current)) > 0) {
                if(rs!=null && rs.isBeforeFirst()){
                    break;
                }
                JLog.logger.debug("Waiting for DB entry to appear at centcom_calls table. Query: " +message);
                Thread.sleep(checkInterval *1000);
                current = LocalDateTime.now();
                rs = db.getCentComCallByType(method.getMethodName(), endpointName, endpointOldName, endPointIP  ,customerID, timestamp);

            }

            if (rs == null) {
                org.testng.Assert.fail("Could not verify CentCom call as result received is NULL " + message);
            }
            //if there is no result
            if (!rs.next()) {
                org.testng.Assert.fail("Could not verify CentCom call as no result found at DB for " + message);
            }
            // if there is more than one result
            if (rs.next()) {
                org.testng.Assert.fail("Could not verify CentCom call as more than one rows received for " + message);

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
