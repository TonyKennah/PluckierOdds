package uk.co.kennah.tkapi;

import uk.co.kennah.tkapi.client.Authenticator;
import uk.co.kennah.tkapi.io.OddsWriter;
import uk.co.kennah.tkapi.model.MarketDataFetcher;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Betfair {

	public void getOdds(String date) {
		try {
			MarketDataFetcher fetcher = new MarketDataFetcher();
			Authenticator auth = fetcher.getAuthenticator();
			auth.login();// Use the authenticator to log in
			if ("SUCCESS".equals(auth.getStatus())) {
				new OddsWriter().write(date + "-ODDS.data", 
					fetcher.getData(date, auth.getAppid(), auth.getSession()));
				auth.logout();
			} else {
				System.err.println("Login failed with status: " + auth.getStatus() + ". Aborting operation.");
			}
		} catch (Exception e) {
			System.err.println("An unexpected error occurred in the main process:");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		String dateToUse;

		if (args.length > 0) {
			dateToUse = args[0];
		} else {
			dateToUse = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE); // Formats as YYYY-MM-DD
		}
		new Betfair().getOdds(dateToUse);
	}

}
