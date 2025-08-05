package uk.co.kennah.tkapi;

import java.util.HashMap;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Betfair {

	public void odds(String date) {
		try {
			BetfairFace bf = new BetfairFace();
			bf.betfairLogin();

			// Check for successful login before proceeding
			if ("SUCCESS".equals(bf.getStatus())) {
				// Fetch market data only ONCE to avoid redundant API calls
				HashMap<Long, MyRunner> marketData = bf.start(date, bf.getAppid(), bf.getSession());

				// Create the two required files from the same data
				String fileCalledLatest = "C:\\prj\\TK-API-NG\\" + date + "-ODDSlatest.data";
				bf.createTheFile(fileCalledLatest, marketData);

				String fileCalled = "C:\\prj\\TK-API-NG\\" + date + "ODDS.data";
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
		String dateToUse;

		if (args.length > 0) {
			// A date is provided as a command-line argument
			// You might want to add validation here to ensure it's in YYYY-MM-DD format
			dateToUse = args[0];
		} else {
			// Default to today's date if no argument is provided
			dateToUse = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE); // Formats as YYYY-MM-DD
		}
		new Betfair().odds(dateToUse);
	}

}
