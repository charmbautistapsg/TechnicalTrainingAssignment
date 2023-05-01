package com.psg.liq;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.opencsv.CSVWriter;
import com.psg.liq.util.ProcessUtilities;

public class ActiveLoanJobAction {

	private static final Logger LOGGER = LogManager.getLogger(ActiveLoanJobAction.class);
	public static ProcessUtilities utilObj = new ProcessUtilities();
	private static Connection connection = null;
	public static Properties properties;
	private static int numActiveLoansRecordsProcessed = 0;
	private static int numActiveLoansRecordsInserted = 0;
	private static int numActiveLoansRecordsUpdated = 0;
	private static int numActiveLoansRecords = 0;

	public static void main(String args[]) {

		try {
			LOGGER.info("Starting the Active Loan job");
			LOGGER.info("Reading all the active loans from  the database");
			List<String> updatedRecords = new ArrayList<>();
			if (connection == null) {
				connection = utilObj.getConnectionDB(properties);
			}
			PreparedStatement qryGetActiveLoans = connection
					.prepareStatement(properties.getProperty("queryGetActiveLoans"));
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

							String customOstId = resCustomTableValue.getString(1);
							double customAmtOstCurrent = resCustomTableValue.getDouble(2);

							if (customAmtOstCurrent != activeAmtOstCurrent) {
								
								// update record
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
									LOGGER.info("Update executed but no row updated for this Outstanding ID: "
											+ customOstId);
								}
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

			LOGGER.info("######### No of active loans :" + numActiveLoansRecords + " #########");
			LOGGER.info("######### No of active loans processed :" + numActiveLoansRecordsProcessed + " #########");
			LOGGER.info("######### No of active loans inserted :" + numActiveLoansRecordsInserted + " #########");
			LOGGER.info("######### No of active loans updated :" + numActiveLoansRecordsUpdated + " #########");

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			e.printStackTrace();
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (Exception e) {
					LOGGER.error(e);
					e.printStackTrace();
				}
			}
		}

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
