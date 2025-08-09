package uk.co.kennah.tkapi;

import uk.co.kennah.tkapi.client.Session;
import uk.co.kennah.tkapi.io.Writer;
import uk.co.kennah.tkapi.process.DataFetcher;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * The main entry point for the PluckierOdds application.
 * This class orchestrates the process of logging into Betfair, fetching odds data
 * for a specific date, and writing the results to a file.
 */
public class Betfair {

	/**
	 * Fetches odds for a given date and writes them to a data file.
	 * It handles the entire workflow: session creation, login, data fetching,
	 * writing to a file, and logout.
	 * @param date The date for which to fetch the odds, in "YYYY-MM-DD" format.
	 */
	public void getOdds(String date) {
		try {
			DataFetcher fetcher = new DataFetcher();
			Session session = fetcher.getSession();
			session.login();// Use the authenticator to log in
			if ("SUCCESS".equals(session.getStatus())) {
				new Writer().publish(date + "-ODDS.data", 
					fetcher.getData(date));
				session.logout();
			} else {
				System.err.println("Login failed with status: " + session.getStatus() + ". Aborting operation.");
			}
		} catch (Exception e) {
			System.err.println("An unexpected error occurred in the main process:");
			e.printStackTrace();
		}
	}

	/**
	 * The main method to run the application from the command line.
	 * It accepts an optional date string as a command-line argument. If no date is
	 * provided, it defaults to the current date.
	 * @param args Command-line arguments. The first argument can be a date in
	 * "YYYY-MM-DD" format.
	 */
	public static void main(String[] args) {
		String dateToUse;
		if (args.length > 0)
			dateToUse = args[0];
		else
			dateToUse = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE); // Formats as YYYY-MM-DD
		new Betfair().getOdds(dateToUse);
	}

}
