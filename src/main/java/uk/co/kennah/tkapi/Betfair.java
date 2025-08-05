package uk.co.kennah.tkapi;

import java.util.HashMap;

import uk.co.kennah.tkapi.client.BetfairAuthenticator;
import uk.co.kennah.tkapi.io.OddsWriter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Betfair {

	public void odds(String date) {
		try {
			MarketDataFetcher fetcher = new MarketDataFetcher();
			BetfairAuthenticator auth = fetcher.getAuthenticator();
			auth.login();// Use the authenticator to log in

			// Check for successful login before proceeding
			if ("SUCCESS".equals(auth.getStatus())) {
				// Fetch market data only ONCE to avoid redundant API calls
				HashMap<Long, MyRunner> marketData = fetcher.start(date, auth.getAppid(), auth.getSession());

				// Use the new OddsWriter to handle file creation
				new OddsWriter().write(date + "ODDS.data", marketData);
				auth.logout();
			} else {
				System.err.println("Login failed with status: " + auth.getStatus() + ". Aborting operation.");
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
