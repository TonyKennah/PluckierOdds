package uk.co.kennah.tkapi.model;

import uk.co.kennah.tkapi.client.Authenticator;
import uk.co.kennah.tkapi.config.AppConfig;
import uk.co.kennah.tkapi.config.ConfigLoader;
import com.betfair.aping.api.ApiNgJsonRpcOperations;
import com.betfair.aping.api.ApiNgOperations;
import com.betfair.aping.entities.EventTypeResult;
import com.betfair.aping.entities.MarketBook;
import com.betfair.aping.entities.MarketCatalogue;
import com.betfair.aping.entities.MarketFilter;
import com.betfair.aping.entities.PriceProjection;
import com.betfair.aping.entities.Runner;
import com.betfair.aping.entities.RunnerCatalog;
import com.betfair.aping.entities.TimeRange;
import com.betfair.aping.enums.MarketProjection;
import com.betfair.aping.enums.MarketSort;
import com.betfair.aping.enums.PriceData;
import com.betfair.aping.exceptions.APINGException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MarketDataFetcher {
	private ApiNgOperations jsonOperations = ApiNgJsonRpcOperations.getInstance();
	private String applicationKey;
	private String sessionToken;
	private HashMap<Long, MyRunner> mine = new HashMap<Long, MyRunner>();
	private AppConfig config;
	private Authenticator authenticator;

	public MarketDataFetcher() {
		this.config = new ConfigLoader().load(); // Load configuration from properties file
		this.authenticator = new Authenticator(config);
	}

	public Authenticator getAuthenticator() {
		return authenticator;
	}

	public HashMap<Long, MyRunner> start(String date, String appKey, String ssoid){
		this.applicationKey = appKey;
		this.sessionToken = ssoid;
		try
		{
			MarketFilter marketFilter;
			marketFilter = new MarketFilter();
			Set<String> eventTypeIds = new HashSet<String>();
			List<EventTypeResult> r = jsonOperations.listEventTypes(marketFilter, applicationKey, sessionToken);

			for (EventTypeResult eventTypeResult : r){
				if (eventTypeResult.getEventType().getName().equals("Horse Racing")){
					eventTypeIds.add(eventTypeResult.getEventType().getId().toString());
				}
			}
			Calendar cal = Calendar.getInstance();
			Date now = cal.getTime();
			cal.set(Calendar.HOUR_OF_DAY, 23);
			Date end = cal.getTime();
			if (!date.equals("")){
				String[] params = date.split("-");
				cal.set(Calendar.YEAR, Integer.parseInt(params[0]));
				cal.set(Calendar.MONTH, Integer.parseInt(params[1]) - 1);
				cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(params[2]));
				cal.set(Calendar.HOUR_OF_DAY, 01);
				now = cal.getTime();
				cal.set(Calendar.HOUR_OF_DAY, 23);
				end = cal.getTime();
			}

			TimeRange time = new TimeRange();
			time.setFrom(now);
			time.setTo(end);
			Set<String> countries = new HashSet<String>();
			countries.add("GB");
			countries.add("IE");
			countries.add("ZA");
			countries.add("FR");
			countries.add("AE");
			countries.add("US");
			// countries.add("HKG");
			Set<String> typesCode = new HashSet<String>();
			typesCode.add("WIN");
			marketFilter = new MarketFilter();
			marketFilter.setEventTypeIds(eventTypeIds);
			marketFilter.setMarketStartTime(time);
			marketFilter.setMarketCountries(countries);
			marketFilter.setMarketTypeCodes(typesCode);
			marketFilter.setInPlayOnly(false);
			Set<MarketProjection> marketProjection = new HashSet<MarketProjection>();
			marketProjection.add(MarketProjection.EVENT);
			marketProjection.add(MarketProjection.RUNNER_DESCRIPTION);
			String maxResults = "1000";
			List<MarketCatalogue> marketCatalogueResult = jsonOperations.listMarketCatalogue(marketFilter, marketProjection, MarketSort.FIRST_TO_START, maxResults, applicationKey, sessionToken);
			for (MarketCatalogue mc : marketCatalogueResult){
				printMarketCatalogue(mc);
			}

			// *** CRITICAL BUG FIX: Correctly chunk the market catalogue list ***
			// The old implementation repeatedly processed the same last 20 markets and discarded the rest.
			// This new loop correctly iterates through all markets in chunks of 20.
			final int CHUNK_SIZE = 20;
			for (int i = 0; i < marketCatalogueResult.size(); i += CHUNK_SIZE) {
				// Get the sublist for the current chunk
				List<MarketCatalogue> chunk = marketCatalogueResult.subList(i, Math.min(i + CHUNK_SIZE, marketCatalogueResult.size()));

				// Extract market IDs from the current chunk
				List<String> marketIds = new ArrayList<>();
				for (MarketCatalogue mc : chunk) {
					marketIds.add(mc.getMarketId());
				}

				if (marketIds.isEmpty()) {
					continue;
				}

				PriceProjection priceProjection = new PriceProjection();
				Set<PriceData> priceData = new HashSet<PriceData>();
				priceData.add(PriceData.EX_BEST_OFFERS);
				priceProjection.setPriceData(priceData);
				List<MarketBook> marketBookReturn = jsonOperations.listMarketBook(marketIds, priceProjection, null, null, null, applicationKey, sessionToken);
				for (MarketBook mb : marketBookReturn){
					printBookCatalogue(mb);
				}
			}
		}
		catch (APINGException apiExc){
			System.out.println("\n\n\n\n\nAPINGException!!!!!!!!!!!\n\n\n\n\n HERE WHATS CAUGHT: " + apiExc.toString());
		}
		return mine;
	}
	
	private void printMarketCatalogue(MarketCatalogue mk){
		List<RunnerCatalog> runners = mk.getRunners();
		if (runners != null){
			for (RunnerCatalog rCat : runners){
				//System.out.println(rCat.getRunnerName());
				String name = rCat.getRunnerName();
				if (Character.isDigit(rCat.getRunnerName().charAt(1))){
					name = rCat.getRunnerName().substring(4, rCat.getRunnerName().length());
				}
				else if (Character.isDigit(rCat.getRunnerName().charAt(0))){
					name = rCat.getRunnerName().substring(3, rCat.getRunnerName().length());
				}
				mine.put(rCat.getSelectionId(), new MyRunner(name));
			}
		}
	}

	private void printBookCatalogue(MarketBook mb){
		List<Runner> runners = mb.getRunners();
		if (runners != null){
			for (Runner rCat : runners){
				if (rCat.getEx().getAvailableToBack().size() > 0){
					mine.get(rCat.getSelectionId()).setOdds(rCat.getEx().getAvailableToBack().get(0).getPrice());
				}
			}
		}
	}
}