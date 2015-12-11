package edu.pitt.tcga.httpclient.util;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


public class Mailer {
	
	public static final int PGRR_SENDER = 0;
	public static final int MANAGER_SENDER = 1;

	private static String pgrr_from_address = null;
	private static String manager_from_address = null;
	
	private static Session pgrrSession = null;
	private static Session managerSession = null;

	
	
	
	
	public static void postMail(int sender, String subject, String message, String[] toAddresses) throws MessagingException {
		if(sender == MANAGER_SENDER && !MySettings.getBooleanProperty("manager.donotify"))
			return;
		Session session = null;
		String from_address = null;
		switch (sender){
			case 0:
				if(pgrrSession == null)
					setEmailSessions(sender);
				session = pgrrSession;
				from_address = pgrr_from_address;
				break;
			case 1:
				if(managerSession == null)
					setEmailSessions(sender);
				session = pgrrSession;
				from_address = manager_from_address;
				break;
		}
      	
    	message =  message + "</body></html>";
 
    	//session.setDebug(true);
    	
    	MimeMessage msg = new MimeMessage(session);
    	msg.setContent(message, "text/html");
    	msg.setSubject(subject);
    	msg.setFrom(new InternetAddress(from_address));
    	for(String toAddress:toAddresses){
    		msg.addRecipient(Message.RecipientType.TO, new InternetAddress(toAddress));
    		Transport.send(msg);
    	}	
	}
	
	private static void setEmailSessions(int sender){
		
		
		String smtp_port = MySettings.getStrProperty("smtp.port");
		boolean useTLS = MySettings.getBooleanProperty("useTLS");
		boolean useSSL = MySettings.getBooleanProperty("useSSL");
		
		switch (sender){
			case 0:
				//================================ pgrr  ==================
				Properties pgrrProperties = new Properties();
				pgrrProperties.put("mail.smtp.user", MySettings.getStrProperty("pgrr.smtp.username"));
		    	
				pgrrProperties.put("mail.smtp.host", MySettings.getStrProperty("pgrr.smtp.server"));
				pgrrProperties.put("mail.smtp.port", MySettings.getStrProperty("smtp.port"));
				pgrrProperties.put("mail.smtp.starttls.enable",useTLS ? "true" : "false");
				pgrrProperties.put("mail.smtp.auth", "true");
		    	if(useSSL)
		    	{
		    		pgrrProperties.put("mail.smtp.socketFactory.port", smtp_port);
		    		pgrrProperties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		    		pgrrProperties.put("mail.smtp.socketFactory.fallback", "false");
		    	}
		    	
		    	pgrr_from_address = MySettings.getStrProperty("pgrr.from.address");
		    	
		    	//SecurityManager security = System.getSecurityManager();
		    	Authenticator auth = new SMTPAuthenticator(MySettings.getStrProperty("pgrr.smtp.username"), MySettings.getStrProperty("pgrr.smtp.password"));
		    	pgrrSession = Session.getInstance(pgrrProperties, auth);
		    	break;
		    	
			case 1:
		    	//================================ manager  ==================
		    	Properties managerProperties = new Properties();
		    	managerProperties.put("mail.smtp.user", MySettings.getStrProperty("manager.smtp.username"));
		    	
		    	managerProperties.put("mail.smtp.host", MySettings.getStrProperty("manager.smtp.server"));
		    	managerProperties.put("mail.smtp.port", MySettings.getStrProperty("smtp.port"));
		    	managerProperties.put("mail.smtp.starttls.enable",useTLS ? "true" : "false");
		    	managerProperties.put("mail.smtp.auth", "true");
		    	if(useSSL)
		    	{
		    		managerProperties.put("mail.smtp.socketFactory.port", smtp_port);
		    		managerProperties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		    		managerProperties.put("mail.smtp.socketFactory.fallback", "false");
		    	}
		    	
		    	manager_from_address = MySettings.getStrProperty("manager.from.address");
				
		    	Authenticator auth2 = new SMTPAuthenticator(MySettings.getStrProperty("manager.smtp.username"), MySettings.getStrProperty("manager.smtp.password"));
		    	managerSession = Session.getInstance(managerProperties, auth2);
		    	break;
		}
	}
	
    private static class SMTPAuthenticator extends javax.mail.Authenticator
    {
    	String username;
    	String password;
    	SMTPAuthenticator(String username, String password)
    	{
    		this.username = username;
    		this.password = password;
    	}
	    public PasswordAuthentication getPasswordAuthentication()
	    {
	    	return new PasswordAuthentication(username, password);
	    }
    }
    
    public static void main(String [] args) throws MessagingException {

			postMail(Mailer.PGRR_SENDER,"Test Email from pgrr",  /*subject*/
				 "This is just a test.  If this were a real message you might be recieving some useful information.", /*message*/
				 new String[] {"aaa@google.com"} /* recipient*/ 
				 );
			System.out.println("Message sent");
	}
}