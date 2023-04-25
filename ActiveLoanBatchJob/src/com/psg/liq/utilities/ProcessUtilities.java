package com.psg.liq.utilities;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

//import com.misys.liq.utils.crypto.CryptoAesLite;
//import com.misys.liq.utils.crypto.CryptoBase;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

/**
 * This class handles all the utilities need for the Main action
 * 
 * @author Ranga Harshini R
 * @version 1.0
 */
public class ProcessUtilities {

	private static final Logger LOGGER = LogManager.getLogger(ProcessUtilities.class);
	private static Connection connection = null;
	public static Properties properties;

	/**
	 * This method is responsible for getting the necessary database connection
	 * 
	 * @return connection Database Connection
	 */
	public Connection getConnectionDB(Properties properties) throws SQLException, ClassNotFoundException, IOException,
			FileNotFoundException, UnsupportedEncodingException, GeneralSecurityException {
		if (connection == null) {
			Boolean isEncryptedPasswordUsed = false;
			String strJSQLFilePath = properties.getProperty("JSQLFILEPATH");
			LOGGER.info("Loading Database property File:" + strJSQLFilePath);

			Properties jSqlAccessProperties = loadPropertiesFileFromXml(strJSQLFilePath);
			String strJSqlAccessJdbcClass = jSqlAccessProperties.getProperty("JSqlAccessJdbcClass");
			String strJSqlAccessJdbcUrl = jSqlAccessProperties.getProperty("JSqlAccessJdbcUrl");
			String strDBDefaultPassword = jSqlAccessProperties.getProperty("DBDefaultPassword");
			String strDBDefaultUserName = jSqlAccessProperties.getProperty("DBDefaultUserName");
			String encryptedValues = jSqlAccessProperties.getProperty("Encrypted.Values");
			if (encryptedValues.toUpperCase().contains("PASSWORD")) {
				isEncryptedPasswordUsed = Boolean.TRUE;
			}
			if (isEncryptedPasswordUsed.booleanValue()) {
				strDBDefaultPassword = "Password@20"; //decrypt(strDBDefaultPassword);
			}

			Class.forName(strJSqlAccessJdbcClass);

			connection = DriverManager.getConnection(strJSqlAccessJdbcUrl, strDBDefaultUserName, strDBDefaultPassword);
		}
		if (connection != null)
			LOGGER.info("Successfully created a Database Connection");

		return connection;
	}

	/**
	 * This method is responsible for getting and loading the log property file.
	 */
	public static void loadLogPropertiesFile() throws IOException, FileNotFoundException {
		LOGGER.info("Trying to Load Properties file");
		Properties log4jProperties = new Properties();

		LOGGER.info("LOANIQ::LOADING LOG4J Properties");

		log4jProperties.load(new FileInputStream("./config/Log4j.properties"));
		LOGGER.info("LOANIQ::LOADING LOG4J Properties 2");
		PropertyConfigurator.configure(log4jProperties);

	}

	/**
	 * This method is responsible for getting and loading the interface property
	 * file.
	 */
	public static Properties loadPropertiesFile() throws IOException, FileNotFoundException {
		Properties interfaceProperties = new Properties();
		InputStream inputStream = null;

		LOGGER.info("LOANIQ::Loading Interface Properties");
		inputStream = new FileInputStream("./config/nplEodBatch.properties");
		interfaceProperties.load(inputStream);

		return interfaceProperties;
	}

	/**
	 * This method is responsible for getting and loading the property file when it
	 * is in the XML format.
	 * 
	 * @param filePath Path of the XML file
	 * @return properties Property file
	 */
	private static Properties loadPropertiesFileFromXml(String filePath)
			throws IOException, FileNotFoundException, InvalidPropertiesFormatException {
		LOGGER.info("Trying to Load XML Properties file::" + filePath);
		Properties properties = new Properties();
		InputStream inputStream = null;

		inputStream = new FileInputStream(filePath);

		properties.loadFromXML(inputStream);

		return properties;
	}

	/**
	 * This method is responsible for generating unique RID for the custom tables.
	 *
	 * @return uniqueID Unique RID
	 */
	public String generateID() {
		/**** LOCAL VARIABLES ****/
		int intPref = 2;
		int intSuf = 6;
		String strPref = "";
		String strSuf = "";
		String strRid = "";

		strPref = getPrefString(intPref);
		strSuf = getBodyString(intSuf);

		strRid = strPref + strSuf;

		return strRid;
	}

	/**
	 * This method is responsible for generating prefix of the unique RID.
	 *
	 * @return prefUniqueID Prefix of Unique RID
	 */
	public String getPrefString(int n) {

		String strAlphaNumericString = "!#$()*+-.0123456789:;=?@ABCDEFGHIJKLMNOPQRSTUVWXYZ";

		StringBuilder sb = new StringBuilder(n); // create StringBuffer of size 'n'

		for (int i = 0; i < n; i++) {

			int intIndex = (int) (strAlphaNumericString.length() * Math.random());
			sb.append(strAlphaNumericString.charAt(intIndex));
		}

		return sb.toString();
	}

	/**
	 * This method is responsible for generating suffix of the unique RID.
	 *
	 * @return suffUniqueID Suffix of Unique RID
	 */
	public String getBodyString(int n) {

		String strAlphaNumericString = "!#$()*+-./0123456789:;=?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]_{}";

		StringBuilder sb = new StringBuilder(n);

		for (int i = 0; i < n; i++) {

			int intIndex = (int) (strAlphaNumericString.length() * Math.random());
			sb.append(strAlphaNumericString.charAt(intIndex));
		}

		return sb.toString();
	}

	/**
	* This method is responsible for decrypting the encryted password
	*/
//	public static String decrypt(String strPassword) throws UnsupportedEncodingException, GeneralSecurityException {
//		CryptoBase crypto = null;
//		String decryptedPassword = "";
//			crypto = new CryptoAesLite();		
//			decryptedPassword = new String(crypto.decrypt(crypto.stringToBytes(strPassword)), "utf8");
//		return decryptedPassword;
//		
//	}
}
