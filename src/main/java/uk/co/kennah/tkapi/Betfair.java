package uk.co.kennah.tkapi;

import java.util.HashMap;

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
		new Betfair().odds("2025-08-05");
	}

}
