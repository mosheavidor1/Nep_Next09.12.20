package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

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
	
	private static Statement stmt;
	private static final String url = "jdbc:mysql://%s:3306/nep_data?useSSL=false";
	private static final String username = "root";
	private static final String password = "trustwave";
	
	public static final String SELECT_UUID = "select uuid_text from endpoint_data where name = ?";
	
	public static PreparedStatement selectUuidStmt;
	
	public NepDbConnector() {
		try {
			Class.forName("com.mysql.jdbc.Driver");  
			Connection conn= DriverManager.getConnection(  
					String.format(url, PropertiesFile.readProperty("ClusterToTest")), username, password);  
			
			stmt = conn.createStatement();  
			
			selectUuidStmt = conn.prepareStatement(SELECT_UUID);
		}
		catch(Exception ex) {
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
	public String getUuidByName(String name) {
		try {
			selectUuidStmt.setString(1, name);
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
	

}
