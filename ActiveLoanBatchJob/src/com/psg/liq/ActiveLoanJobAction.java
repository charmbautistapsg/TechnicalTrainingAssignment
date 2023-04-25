package com.psg.liq;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.opencsv.CSVWriter;
import com.psg.liq.utilities.CommonUtils;
import com.psg.liq.utilities.ProcessUtilities;

public class ActiveLoanJobAction {

	private static final Logger LOGGER = LogManager.getLogger(ActiveLoanJobAction.class);
	public static ProcessUtilities utilObj = new ProcessUtilities();
	private static Connection connection = null;
	private static String strTimeZone;
	public static Properties properties;
	public static boolean isUnlocked;
	private static int numActiveLoansRecordsProcessed = 0;
	private static int numActiveLoansRecordsInserted = 0;
	private static int numActiveLoansRecordsUpdated = 0;
	private static int numActiveLoansRecords = 0;
	private static int numInActiveLoansRecordsProcessed = 0;
	private static int numInActiveLoansRecords = 0;

	public static void main(String args[]) {

		try {
			// Standard Procedure for a job of active loans
			List<String> updatedRecords = new ArrayList<>();
			BasicConfigurator.configure();
			LOGGER.info("args.length:::" + args.length);
			ProcessUtilities.loadLogPropertiesFile();
			LOGGER.info("Starting the Active Loan job");
			strTimeZone = args[0];
			if (strTimeZone == null || "".equalsIgnoreCase(strTimeZone)) {
				throw new Exception("Timezone is null, so batch terminated");
			}
			LOGGER.info("TimeZone of the current run is : " + strTimeZone);
			properties = ProcessUtilities.loadPropertiesFile();
			connection = utilObj.getConnectionDB(properties);
			releaseTimeZoneLock(connection, properties, strTimeZone);
			LOGGER.info("Reading all the active loans from  the database");
			ArrayList<String> arrBusinessDates = getBusinessDates(strTimeZone, connection);
			String strNextBusinessDate = arrBusinessDates.get(1);
			LOGGER.info("Next Business date : " + strNextBusinessDate);

			LOGGER.info("Reading all the active loans from  the database");
			if (connection == null) {
				connection = utilObj.getConnectionDB(properties);
			}
			PreparedStatement qryGetActiveLoans = connection
					.prepareStatement(properties.getProperty("queryGetActiveLoans"));
			qryGetActiveLoans.setString(1, strTimeZone);
			ResultSet resGetActiveLoans = qryGetActiveLoans.executeQuery();
			while (resGetActiveLoans.next()) {
				numActiveLoansRecords++;
				try {
					// Getting the data to meet the logic goal
					LOGGER.info("Processing the loans one by one");
					String activeOstId = resGetActiveLoans.getString(1);
					double activeAmtOstCurrent = resGetActiveLoans.getDouble(2);

					LOGGER.info("Getting all the loan records in Custom table");
					if (connection == null) {
						connection = utilObj.getConnectionDB(properties);
					}
					PreparedStatement qryGetCustomTable = connection
							.prepareStatement(properties.getProperty("queryGetLoanCustomTable"));
					qryGetCustomTable.setString(1, activeOstId);
					ResultSet resCustomTableValue = qryGetCustomTable.executeQuery();

					// logic started
					if (resCustomTableValue != null && resCustomTableValue.getRow() > 0) {
						while (resGetActiveLoans.next()) {

							// update record
							String customOstId = resCustomTableValue.getString(1);
							double customAmtOstCurrent = resCustomTableValue.getDouble(2);

							LOGGER.info("Updating the record with Outstanding ID :" + customOstId
									+ " with current amount :" + customAmtOstCurrent);

							PreparedStatement updateCustom = connection
									.prepareStatement(properties.getProperty("queryUpdateLoanCustomTable"));
							updateCustom.setDouble(1, customAmtOstCurrent);
							updateCustom.setString(2, customOstId);
							int row = updateCustom.executeUpdate();

							if (row > 0) {
								numActiveLoansRecordsUpdated++;
								LOGGER.info("Successfully updated the Current Amount of this Outstanding ID: "
										+ customOstId);

								updatedRecords.add(customOstId + " " + customAmtOstCurrent);
							} else {
								LOGGER.info(
										"Update executed but no row updated for this Outstanding ID: " + customOstId);
							}
						}
					} else {
						// insert record

						LOGGER.info("Inserting the record with Outstanding ID :" + activeOstId
								+ " with current amount :" + activeAmtOstCurrent);

						PreparedStatement insertCutom = connection
								.prepareStatement(properties.getProperty("queryUpdateLoanCustomTable"));
						insertCutom.setString(1, "123123"); // this is temporary. Should be sequence generated
						insertCutom.setString(2, activeOstId);
						insertCutom.setDouble(3, activeAmtOstCurrent);
						int row = insertCutom.executeUpdate();

						if (row > 0) {
							numActiveLoansRecordsInserted++;
							LOGGER.info("Successfully inserted a record with Outstanding ID: " + activeOstId
									+ " and Current Amount: " + activeAmtOstCurrent);
						} else {
							LOGGER.info("Insert executed but no record is inserted for this Outstanding ID: "
									+ activeOstId);
						}
					}

					numActiveLoansRecordsProcessed++;
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
					e.printStackTrace();
				}
			}

			// Create the CSV file based on the updated Loan
			addDataToCSV(updatedRecords);

			LOGGER.info("######### No of active loans processed :" + numActiveLoansRecordsProcessed + " #########");
			LOGGER.info("######### No of active loans inserted :" + numActiveLoansRecordsInserted + " #########");
			LOGGER.info("######### No of active loans updated :" + numActiveLoansRecordsUpdated + " #########");
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			e.printStackTrace();
		} finally {
			try {
				insertExclusiveLock(connection, properties, strTimeZone);
			} catch (Exception e) {
				LOGGER.error(e);
				e.printStackTrace();
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (Exception e) {
					LOGGER.error(e);
					e.printStackTrace();
				}
			}
			if (isUnlocked) {
				LOGGER.error("Timezone Lock not released");
				LOGGER.error(numActiveLoansRecordsProcessed + " active loans was processed successfully out of "
						+ numActiveLoansRecords + " active loans");
				LOGGER.error(numInActiveLoansRecordsProcessed + " inactive loans was processed successfully out of "
						+ numInActiveLoansRecords + " inactive loans");
				System.exit(CommonUtils.getExitErrorCode());
			} else if (numActiveLoansRecords == 0 && numInActiveLoansRecords == 0) {
				LOGGER.error("No records were read");
				System.exit(CommonUtils.getExitErrorCode());
			} else if (numActiveLoansRecordsProcessed == 0 && numInActiveLoansRecordsProcessed == 0) {
				LOGGER.error("No records were processed");
				System.exit(CommonUtils.getExitErrorCode());
			} else if (numActiveLoansRecords == 0 || numInActiveLoansRecords == 0) {
				LOGGER.error("Either Active/Inactive loan records not read.");
				System.exit(CommonUtils.getExitWarnCode());
			} else if (numActiveLoansRecordsProcessed < numActiveLoansRecords
					|| numInActiveLoansRecordsProcessed < numInActiveLoansRecords) {
				LOGGER.error(numActiveLoansRecordsProcessed + " active loans was processed successfully out of "
						+ numActiveLoansRecords + " active loans");
				LOGGER.error(numInActiveLoansRecordsProcessed + " inactive loans was processed successfully out of "
						+ numInActiveLoansRecords + " inactive loans");
				System.exit(CommonUtils.getExitWarnCode());
			} else {
				System.exit(CommonUtils.getExitSuccessCode());
			}

		}

	}

	/**
	 * Check if LS2LOCK exist and delete if exist
	 * 
	 */
	protected static void releaseTimeZoneLock(Connection connection, Properties properties, String strTimeZone)
			throws SQLException, Exception {
		if (!isUnlocked) {

			LOGGER.info("CHECK LOCK ");
			// check if lock record exist
			if (properties.getProperty("timezoneuser") == null || properties.getProperty("lockDeleteQuery") == null) {
				LOGGER.error("UNABLE TO READ #timezoneuser or #lockDeleteQuery BATCH JOB FAILED!");
				throw new Exception("UNABLE TO READ #batchUser or #lockDeleteQuery BATCH JOB FAILED!");
			}
			if (connection == null) {
				connection = utilObj.getConnectionDB(properties);
			}
			PreparedStatement qrylockDeleteQuery = connection
					.prepareStatement(properties.getProperty("lockDeleteQuery"));
			qrylockDeleteQuery.setString(1, strTimeZone);
			qrylockDeleteQuery.setString(2, properties.getProperty("timezoneuser"));

			qrylockDeleteQuery.execute();
			connection.commit();

			isUnlocked = true;
			LOGGER.info(" LOCK DELETED ");
		}
	}

	protected static void insertExclusiveLock(Connection connection, Properties properties, String strTimeZone)
			throws SQLException, Exception {

		String strRegionDesc = "";
		if (isUnlocked) {
			LOGGER.info("INSERTING time zone LOCK ");
			// check if lock record exist
			String insertLockQuery = properties.getProperty("lockInsertQuery");

			if (properties.getProperty("checkLockQuery") == null || properties.getProperty("timezoneuser") == null
					|| insertLockQuery == null || strTimeZone == null
					|| properties.getProperty("getRegionDesc") == null) {
				LOGGER.error(
						"UNABLE TO READ #checkLockQuery or #timezoneuser or #lockInsertQuery or strTimeZone or getRegionDescQuery BATCH JOB FAILED!");
				throw new Exception(
						"UNABLE TO READ #checkLockQuery or #timezoneuser or #lockInsertQuery or strTimeZone or getRegionDescQuery BATCH JOB FAILED!");
			}
			if (connection == null) {
				connection = utilObj.getConnectionDB(properties);
			}
			PreparedStatement qryGetTimeZoneDesc = connection.prepareStatement(properties.getProperty("getRegionDesc"));
			qryGetTimeZoneDesc.setString(1, strTimeZone);
			ResultSet resGetTimeZoneDesc = qryGetTimeZoneDesc.executeQuery();
			if (resGetTimeZoneDesc.next()) {
				strRegionDesc = resGetTimeZoneDesc.getString(1);
			} else {
				LOGGER.error("UNABLE TO READ region description BATCH JOB FAILED!");
				throw new Exception("UNABLE TO READ region description BATCH JOB FAILED!");
			}
			PreparedStatement qryCheckLockQuery = connection.prepareStatement(properties.getProperty("checkLockQuery"));
			qryCheckLockQuery.setString(1, strTimeZone);
			qryCheckLockQuery.setString(2, properties.getProperty("timezoneuser"));
			ResultSet resCheckLockQuery = qryCheckLockQuery.executeQuery();
			if (!resCheckLockQuery.next()) {
				PreparedStatement qryLockInsertQuery = connection.prepareStatement(insertLockQuery);
				qryLockInsertQuery.setString(1, "TRG");
				qryLockInsertQuery.setString(2, strTimeZone);
				qryLockInsertQuery.setString(3, "U");
				qryLockInsertQuery.setTimestamp(4, new java.sql.Timestamp(System.currentTimeMillis()));
				qryLockInsertQuery.setTimestamp(5, new java.sql.Timestamp(System.currentTimeMillis()));
				qryLockInsertQuery.setString(6, properties.getProperty("timezoneuser"));
				qryLockInsertQuery.setString(7, properties.getProperty("timezoneuser"));
				qryLockInsertQuery.setString(8, null);
				qryLockInsertQuery.setString(9, null);
				qryLockInsertQuery.setString(10, strRegionDesc);
				qryLockInsertQuery.setString(11, " ");

				qryLockInsertQuery.execute();
				connection.commit();
				isUnlocked = false;
				LOGGER.info("INSERTING LOCK SUCCESSFUL");
			}
		}

	}

	public static ArrayList<String> getBusinessDates(String strTimeZone, Connection connection) throws Exception {
		ArrayList<String> arrBusinessDates = new ArrayList<>();
		if (connection == null) {
			connection = utilObj.getConnectionDB(properties);
		}
		PreparedStatement qryGetBusinessDate = connection
				.prepareStatement(properties.getProperty("queryGetBusinessDates"));
		qryGetBusinessDate.setString(1, strTimeZone);
		ResultSet resBusinessDates = qryGetBusinessDate.executeQuery();
		while (resBusinessDates.next()) {
			arrBusinessDates.add(resBusinessDates.getString(1));
		}
		return arrBusinessDates;

	}

	public static void addDataToCSV(List<String> values) {
		File file = new File(properties.getProperty("csvFilePath"));
		try {
			LOGGER.info("Storing the updated records in CSV file: " + values);

			FileWriter outputfile = new FileWriter(file);
			CSVWriter writer = new CSVWriter(outputfile, ';', CSVWriter.NO_QUOTE_CHARACTER,
					CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);

			List<String[]> data = new ArrayList<String[]>();
			for (String val : values) {
				String[] rowdata = val.split(" ");
				data.add(rowdata);
			}
			writer.writeAll(data);

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
