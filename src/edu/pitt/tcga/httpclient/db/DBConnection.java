package edu.pitt.tcga.httpclient.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import edu.pitt.tcga.httpclient.log.ErrorLog;
import edu.pitt.tcga.httpclient.util.MySettings;


public class DBConnection {
private String driver = null;
private String url = null;
private String user = null;
private String pass = null;


private Connection conn = null;

public DBConnection(){

	this.driver = MySettings.getStrProperty("db.driver");
	this.url = MySettings.getStrProperty("db.url");
	this.user =  MySettings.getStrProperty("db.user");
	this.pass = MySettings.getStrProperty("db.pwd");
	
}

public Connection getConnection() throws Exception {
	try {
		if (conn == null || conn.isClosed()) {
			try {
				Class.forName(driver).newInstance();
				conn = DriverManager.getConnection(url, user, pass);
			} catch (org.postgresql.util.PSQLException ex) {
				String mess = ex.getMessage();
				if(mess.startsWith("FATAL: database") && mess.endsWith("does not exist"))
					System.err.println("NO DBL "+mess);
				throw ex;
			}
		}
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		//e.printStackTrace();
		ErrorLog.log("DBConnection getConnection: "+e.getMessage());
		throw e;
	}
	return conn;
}

public void disconnect(){
	if(conn == null)
		return;
	try {
		if (conn != null || !conn.isClosed())
			conn.close();
	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		ErrorLog.log("DBConnection disconnect: "+e.getMessage());
	} catch (NullPointerException ex){
		ex.printStackTrace();
		ErrorLog.log("DBConnection disconnect: "+ex.getMessage());
	}
}

}