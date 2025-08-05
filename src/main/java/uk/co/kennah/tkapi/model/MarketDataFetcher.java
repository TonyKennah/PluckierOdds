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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
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

	public HashMap<Long, MyRunner> getData(String date, String appKey, String ssoid){
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

			LocalDate parsedDate = LocalDate.parse(date);
			LocalDateTime startOfDay = parsedDate.atTime(LocalTime.of(0, 1));
			LocalDateTime endOfDay = parsedDate.atTime(LocalTime.of(23, 59));

			TimeRange time = new TimeRange();
			// Convert to java.util.Date for the Betfair SDK's TimeRange object
			time.setFrom(Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant()));
			time.setTo(Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant()));
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
			System.out.println("\nAPINGException!!!!!!!!!!!\n WHATS CAUGHT: " + apiExc.toString());
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