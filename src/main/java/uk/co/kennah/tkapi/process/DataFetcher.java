package uk.co.kennah.tkapi.process;

import uk.co.kennah.tkapi.client.Session;
import uk.co.kennah.tkapi.config.ConfigLoader;
import uk.co.kennah.tkapi.model.AppConfig;
import uk.co.kennah.tkapi.model.MyRunner;

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

public class DataFetcher {
	private ApiNgOperations jsonOperations = ApiNgJsonRpcOperations.getInstance();
	private HashMap<Long, MyRunner> outputData = new HashMap<Long, MyRunner>();
	private AppConfig config;
	private Session session;

	public DataFetcher() {
		this.config = new ConfigLoader().load(); // Load configuration from properties file
		this.session = new Session(config);
		
	}

	public Session getSession() {
		return session;
	}

	public HashMap<Long, MyRunner> getData(String date) {
		return getAllData(date);
	}

	private HashMap<Long, MyRunner> getAllData(String date) {
		try {

			//This catalogue contains Markets, seledctionIds, names for each horse
			List<MarketCatalogue> catalogue = getMarketCatalogue(date);
			for (MarketCatalogue mc : catalogue) {
				System.out.println("\nMARKET CATALOGUE: "+mc.toString()+"\n");
				addHorsesNamestoOutputData(mc);
			}

			//This marketBook contains the odds for each seledctionId
			List<MarketBook> book = getMarketBook(catalogue);
			for (MarketBook mb : book) {
				System.out.println("MARKET BOOK: "+mb.getRunners().size() + " runners");
				addHorsesOddstoOutputData(mb);
			}

		} catch (APINGException apiExc) {
			System.out.println("\nAPINGException!!!!!!!!!!!\n WHATS CAUGHT: " + apiExc.toString());
		}
		return outputData;
	}

	private List<MarketBook> getMarketBook(List<MarketCatalogue> catalogue) throws APINGException {		
		// Extract market IDs from the current chunk
		List<String> marketIds = new ArrayList<>();
		for (MarketCatalogue mc : catalogue) {
			marketIds.add(mc.getMarketId());
		}

		PriceProjection priceProjection = new PriceProjection();
		Set<PriceData> priceData = new HashSet<PriceData>();
		priceData.add(PriceData.EX_BEST_OFFERS);
		priceProjection.setPriceData(priceData);
		List<MarketBook> marketBookReturn = jsonOperations.listMarketBook(marketIds, priceProjection, null, null, null,
				session.getAppid(), session.getSessionToken());
		return marketBookReturn;

	}

	private List<MarketCatalogue> getMarketCatalogue(String date) throws APINGException {
		MarketFilter marketFilter;
		marketFilter = new MarketFilter();
		Set<String> eventTypeIds = new HashSet<String>();
		List<EventTypeResult> r = jsonOperations.listEventTypes(marketFilter, session.getAppid(), session.getSessionToken());

		for (EventTypeResult eventTypeResult : r) {
			if (eventTypeResult.getEventType().getName().equals("Horse Racing")) {
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
		// countries.add("US");
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
		List<MarketCatalogue> marketCatalogueResult = jsonOperations.listMarketCatalogue(marketFilter, marketProjection,
				MarketSort.FIRST_TO_START, maxResults, session.getAppid(), session.getSessionToken());
		return marketCatalogueResult;

	}

	private void addHorsesNamestoOutputData(MarketCatalogue mk) {
		List<RunnerCatalog> runners = mk.getRunners();
		if (runners != null) {
			for (RunnerCatalog rCat : runners) {
				System.out.println(rCat.getRunnerName());
				String name = rCat.getRunnerName();
				if (name == null || name.isEmpty()) {
					name = "UKNOWN RUNNER NAME";
				}
				outputData.put(rCat.getSelectionId(), new MyRunner(name));
			}
		}
	}

	private void addHorsesOddstoOutputData(MarketBook mb) {
		List<Runner> runners = mb.getRunners();
		if (runners != null) {
			for (Runner rCat : runners) {
				if (rCat.getEx().getAvailableToBack().size() > 0) {
					outputData.get(rCat.getSelectionId()).setOdds(rCat.getEx().getAvailableToBack().get(0).getPrice());
				}
			}
		}
	}
}
