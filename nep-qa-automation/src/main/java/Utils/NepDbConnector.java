package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import Actions.LNEActions;
import Utils.Data.GlobalTools;
import Utils.Logs.JLog;
import Utils.PropertiesFile.PropertiesFile;

/**
 * MySQL connector to connect Nep DB on Lenny
 * This is suitable only for local tests, against Lenny
 * 
 * @author RSalmon
 *
 */
public class NepDbConnector {
	
	//private static Statement stmt;
	
	public static final String SELECT_UUID = "select uuid_text from endpoint_data where name = ? and customer_id = ?";
	public static final String GET_EP_ERROR = "select error_msg from endpoint_errors where id = (select id from endpoint_data where name=?)";
	public static final String RESET_WINDOWS_UPDATE_VER = "update global_versions set version = '1.0.0' where component = 'win_binary_update'";
	public static final String RESET_CENTOS_UPDATE_VER = "update global_versions set version = '1.0.0' where component = 'centos_binary_update'";
	public static final String RESET_BIN_VER_EP_REQ = "update endpoint_data set bin_ver_ep_request=bin_version";
	public static final String VERIFY_CENTCOM_CALLS = "select id from centcom_calls where type = ? and plain_data like ? and plain_data like ? and plain_data like ? and updated_on > ? ";
	public static final String GET_DB_TIMESTAMP = "SELECT CURRENT_TIMESTAMP()";

	public static PreparedStatement selectUuidStmt;
	public static PreparedStatement getEpErrorStmt;
	public static PreparedStatement resetCentosUpdateVerStmt;
	public static PreparedStatement resetWinUpdateVerStmt;
	public static PreparedStatement resetEpBinVerEpReqStmt;
	public static PreparedStatement verifyCentComCalls;
	public static PreparedStatement getDbTimestamp;

	public NepDbConnector(String url, String username, String password) {
		try {
			Class.forName("com.mysql.jdbc.Driver");  
			if (url.contains("{lenny-ip}")) {
				url = url.replace("{lenny-ip}", GlobalTools.getClusterToTest());
			}
			Connection conn= DriverManager.getConnection(  
					url, username, password);  
			
			//stmt = conn.createStatement();  
			
			selectUuidStmt = conn.prepareStatement(SELECT_UUID);
			getEpErrorStmt = conn.prepareStatement(GET_EP_ERROR);
			resetCentosUpdateVerStmt = conn.prepareStatement(RESET_CENTOS_UPDATE_VER);
			resetWinUpdateVerStmt = conn.prepareStatement(RESET_WINDOWS_UPDATE_VER);
			resetEpBinVerEpReqStmt = conn.prepareStatement(RESET_BIN_VER_EP_REQ);
			verifyCentComCalls = conn.prepareStatement(VERIFY_CENTCOM_CALLS);
			getDbTimestamp = conn.prepareStatement(GET_DB_TIMESTAMP);
		}
		catch(Exception ex) {
			JLog.logger.error("Failed to connect to Nep DB", ex);
			throw new RuntimeException("Failed to connect to Nep DB", ex);
		}
	}
	/*
	public ResultSet runQueryAndGetResult(String query){
		try {
			return stmt.executeQuery(query); 
		}
		catch(SQLException ex) {
			JLog.logger.error("Failed to run the query", ex);
			org.testng.Assert.fail("Failed to run SQL query", ex);
		}
		return null;
		 
	}
	*/
	public String getUuidByName(String name, String customerId) {
		try {
			selectUuidStmt.setString(1, name);
			selectUuidStmt.setLong(2, new Long(customerId));
			ResultSet rs = selectUuidStmt.executeQuery();
			if (!rs.next()) {
				return null;
			}
			return rs.getString(1);
		}
		catch(SQLException ex) {
			JLog.logger.error("Failed to run getUuidByName query", ex);
			org.testng.Assert.fail("Failed to run getUuidByName query", ex);
		}
		return null;
		
	}

	public void cleanGlobalVersionsAfterBinaryUpdate() {
		try {
			int res = resetCentosUpdateVerStmt.executeUpdate();
			if (res == 1) {
				JLog.logger.info("Set linux version in global_versions executed successfully.");
			}

			res = resetWinUpdateVerStmt.executeUpdate();
			if (res == 1) {
				JLog.logger.info("Set windows version in global_versions executed successfully.");
			}

		} catch (SQLException ex) {
			JLog.logger.error("Failed to clean global versions table", ex);
			org.testng.Assert.fail("Failed to clean global versions table", ex);
		}
	}

	public String getEpErrorMsg(String epName) {
		try {
			getEpErrorStmt.setString(1, epName);
			ResultSet rs = getEpErrorStmt.executeQuery();
			if (!rs.next()) {
				return null;
			}
			return rs.getString(1);
		}
		catch(SQLException ex) {
			JLog.logger.error("Failed to run getEpErrorStmt query", ex);
			org.testng.Assert.fail("Failed to run getEpErrorStmt query", ex);
		}
		return null;
	}

	public void cleanEndpointBinVerEpRequest() {
		try {
			int res = resetEpBinVerEpReqStmt.executeUpdate();
			JLog.logger.debug("Clean bin_ver_ep_request, returned result: " + res);
		} catch (SQLException ex) {
			JLog.logger.error("Failed to clean endpoints bin_ver_ep_request value", ex);
			org.testng.Assert.fail("Failed to clean endpoints bin_ver_ep_request value", ex);
		}
	}

	public ResultSet getCentComCallByType(String centComCall, String epName, String oldEpName,String endpointIP, String customerID, String timestamp) {
		try {
			verifyCentComCalls.setString(1, centComCall);

			customerID = "%customerId=" + customerID + "%";
			verifyCentComCalls.setString(2, customerID);

			if(centComCall.equalsIgnoreCase(LNEActions.CentcomMethods.RENAME_ENDPOINT.toString())){
				epName = "%newName=" + epName + "]%";
			}
			else {
				epName = "%name=" + epName + ",%";

			}

			verifyCentComCalls.setString(3, epName);

			if(centComCall.equalsIgnoreCase(LNEActions.CentcomMethods.RENAME_ENDPOINT.toString())){
				oldEpName = "%oldName=" + oldEpName + ",%";
				verifyCentComCalls.setString(4, oldEpName);

			}
			else {
				endpointIP = "%Ip=" + endpointIP + ",%";
				verifyCentComCalls.setString(4, endpointIP);
			}

			verifyCentComCalls.setString(5, timestamp);

			ResultSet rs = verifyCentComCalls.executeQuery();
			return rs;
		} catch (Exception ex) {
			String message = "Could not verify CentCom call: " + centComCall + " Endpoint name: " + epName + " Customer id: " + customerID + " after timestamp: " + timestamp + "\n" + ex.toString();
			org.testng.Assert.fail(message);
			return null;
		}
	}


	public String GetDbCurrentTimestamp() {
		try {
			ResultSet rs = getDbTimestamp.executeQuery();
			if (!rs.next()) {
				return null;
			}
			return rs.getString(1);

		} catch (Exception ex) {
			String message = "Could not get DB current time stamp. Query: " + getDbTimestamp + "\n" + ex.toString();
			org.testng.Assert.fail(message);
			return null;

		}
	}


}
