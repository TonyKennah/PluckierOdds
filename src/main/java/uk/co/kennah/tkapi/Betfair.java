package uk.co.kennah.tkapi;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class Betfair {

	public void getOdds(String date, String outputDir) {
		try {
			BetfairFace bf = new BetfairFace();
			bf.betfairLogin();

			// Check for successful login before proceeding
			if ("SUCCESS".equals(bf.getStatus())) {
				// Fetch market data only ONCE to avoid redundant API calls
				HashMap<Long, MyRunner> marketData = bf.start(date, bf.getAppid(), bf.getSession());

				// Create the two required files from the same data
				String fileCalledLatest = Paths.get(outputDir, date + "-ODDSlatest.data").toString();
				bf.createTheFile(fileCalledLatest, marketData);

				String fileCalled = Paths.get(outputDir, date + "ODDS.data").toString();
				bf.createTheFile(fileCalled, marketData);

				bf.betfairLogout();
			} else {
				System.err.println("Login failed with status: " + bf.getStatus() + ". Aborting operation.");
			}
		} catch (Exception e) {
			// Improve error handling to provide meaningful debug information
			System.err.println("An unexpected error occurred in the main process:");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// Default to today's date if no argument is provided
		String date;
		if (args.length > 0) {
			date = args[0];
		} else {
			date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
			System.out.println("No date provided, using today's date: " + date);
		}

		// Default to a 'data' subdirectory in the user's home directory
		String outputDir;
		if (args.length > 1) {
			outputDir = args[1];
		} else {
			outputDir = Paths.get(System.getProperty("user.home"), "tk-api-ng-data").toString();
		}

		new File(outputDir).mkdirs(); // Ensure the output directory exists
		System.out.println("Writing output files to: " + outputDir);

		new Betfair().getOdds(date, outputDir);
	}

}
