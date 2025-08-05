package uk.co.kennah.tkapi;

import uk.co.kennah.tkapi.client.Session;
import uk.co.kennah.tkapi.io.Writer;
import uk.co.kennah.tkapi.process.DataFetcher;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Betfair {

	public void getOdds(String date) {
		try {
			DataFetcher fetcher = new DataFetcher();
			Session session = fetcher.getSession();
			session.login();// Use the authenticator to log in
			if ("SUCCESS".equals(session.getStatus())) {
				new Writer().publish(date + "-ODDS.data", 
					fetcher.getData(date, session.getAppid(), session.getSessionToken()));
				session.logout();
			} else {
				System.err.println("Login failed with status: " + session.getStatus() + ". Aborting operation.");
			}
		} catch (Exception e) {
			System.err.println("An unexpected error occurred in the main process:");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		String dateToUse;
		if (args.length > 0)
			dateToUse = args[0];
		else
			dateToUse = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE); // Formats as YYYY-MM-DD
		new Betfair().getOdds(dateToUse);
	}

}
