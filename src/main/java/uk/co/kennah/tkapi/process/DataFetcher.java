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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fetches and processes horse racing market data from the Betfair API.
 * This class is responsible for retrieving market catalogues and market books,
 * extracting relevant information about runners and their odds, and compiling
 * it into a structured format. It manages the API session and handles the
 * necessary API calls to get data for a specified date.
 */
public class DataFetcher {
	private ApiNgOperations jsonOperations;
	private HashMap<Long, MyRunner> outputData;
	private Session session;

	/**
	 * Constructor for DataFetcher.
	 * Initializes the Betfair API operations client, the output data map,
	 * and the session by loading application configuration.
	 */
	public DataFetcher() {
		this.jsonOperations = ApiNgJsonRpcOperations.getInstance();
		this.outputData = new HashMap<Long, MyRunner>();
		this.session = new Session(new ConfigLoader().load());
	}

	/**
	 * Retrieves all horse racing data for a specific date.
	 * This is the main public method to start the data fetching process.
	 *
	 * @param date The date for which to fetch data, in "yyyy-MM-dd" format.
	 * @return A HashMap where the key is the selection ID of the runner and the value is a MyRunner object containing its details.
	 */
	public HashMap<Long, MyRunner> getData(String date) {
		return getAllData(date);
	}

	/**
	 * Orchestrates the fetching of all market data for a given date.
	 * It first gets the market catalogue to identify runners and then fetches the market book for odds.
	 *
	 * @param date The date for which to fetch data, in "yyyy-MM-dd" format.
	 * @return A HashMap containing all the processed runner data.
	 */
	private HashMap<Long, MyRunner> getAllData(String date) {
		try {
			// This catalogue contains Markets, seledctionIds, names for each horse
			List<MarketCatalogue> catalogue = getMarketCatalogue(date);
			catalogue.forEach(entry -> addHorseNamesToOutputData(entry));

			// This marketBook contains the odds for each seledctionId
			List<MarketBook> book = getMarketBook(catalogue);
			book.forEach(entry -> addHorseOddsToOutputData(entry));

		} catch (APINGException apiExc) {
			System.out.println("\nAPINGException!!!!!!!!!!!\n WHATS CAUGHT: " + apiExc.toString());
		}
		return outputData;
	}

	/**
	 * Fetches the market books for a given list of market catalogues.
	 * The market book contains live pricing information.
	 * It chunks requests to the API to stay within request size limits (20 market IDs per request).
	 *
	 * @param catalogue A list of MarketCatalogue objects for which to fetch market books.
	 * @return A list of MarketBook objects containing the odds for runners.
	 * @throws APINGException if there is an error during the API call.
	 */
	private List<MarketBook> getMarketBook(List<MarketCatalogue> catalogue) throws APINGException {

		List<MarketBook> books = new ArrayList<MarketBook>();
		for (int i = 0; i < catalogue.size(); i += 20) {

			List<MarketCatalogue> chunk = catalogue.subList(i, Math.min(i + 20, catalogue.size()));
			List<String> marketIds = new ArrayList<>();
			for (MarketCatalogue mc : chunk) {
				marketIds.add(mc.getMarketId());
			}

			PriceProjection priceProjection = new PriceProjection();
			Set<PriceData> priceData = new HashSet<PriceData>(List.of(PriceData.EX_BEST_OFFERS));
			priceProjection.setPriceData(priceData);
			books.addAll(jsonOperations.listMarketBook(marketIds, priceProjection, null, null, null,
					session.getAppid(), session.getSessionToken()));
		}
		return books;
	}

	/**
	 * Fetches the market catalogue for horse racing events on a specific date.
	 * The catalogue contains information about markets, events, and runners.
	 * It filters for "Horse Racing" events in specific countries ("GB", "IE", "FR", "ZA", "AE") for "WIN" markets.
	 *
	 * @param date The date for which to fetch the market catalogue, in "yyyy-MM-dd" format.
	 * @return A list of MarketCatalogue objects.
	 * @throws APINGException if there is an error during the API call.
	 */
	private List<MarketCatalogue> getMarketCatalogue(String date) throws APINGException {
		List<EventTypeResult> eventTypes = jsonOperations.listEventTypes(new MarketFilter(), session.getAppid(),
				session.getSessionToken());
		Set<String> eventTypeIds = new HashSet<String>();
		eventTypes.stream()
				.filter(type -> type.getEventType().getName().equals("Horse Racing"))
				.forEach(type -> eventTypeIds.add(type.getEventType().getId().toString()));
		LocalDate parsedDate = LocalDate.parse(date);
		LocalDateTime startOfDay = parsedDate.atTime(LocalTime.of(0, 1));
		LocalDateTime endOfDay = parsedDate.atTime(LocalTime.of(23, 59));
		TimeRange time = new TimeRange();
		// Convert to java.util.Date for the Betfair SDK's TimeRange object
		time.setFrom(Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant()));
		time.setTo(Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant()));
		MarketFilter marketFilter = new MarketFilter();
		marketFilter.setEventTypeIds(eventTypeIds);
		marketFilter.setMarketStartTime(time);
		marketFilter.setMarketCountries(new HashSet<String>(List.of("GB", "IE", "FR", "ZA", "AE")));
		marketFilter.setMarketTypeCodes(new HashSet<String>(List.of("WIN")));
		marketFilter.setInPlayOnly(false);
		Set<MarketProjection> marketProjection = new HashSet<MarketProjection>(
				List.of(MarketProjection.EVENT, MarketProjection.RUNNER_DESCRIPTION, MarketProjection.MARKET_DESCRIPTION, MarketProjection.MARKET_START_TIME));
		return jsonOperations.listMarketCatalogue(marketFilter, marketProjection, MarketSort.FIRST_TO_START, "1000",
				session.getAppid(), session.getSessionToken());
	}

	/**
	 * Processes a MarketCatalogue to extract runner information and add it to the output data map.
	 * It creates a new MyRunner object for each runner in the market catalogue and stores it.
	 *
	 * @param mk The MarketCatalogue object to process.
	 */
	private void addHorseNamesToOutputData(MarketCatalogue mk) {
		Date retDate = mk.getDescription().getMarketTime();
		String niceDate = retDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
		mk.getRunners()
				.forEach(horse -> outputData.put(horse.getSelectionId(), new MyRunner(horse.getRunnerName(), null, niceDate + " " + mk.getEvent().getVenue() + " " + mk.toString())));
	}

	/**
	 * Processes a MarketBook to extract odds for each runner and updates the corresponding entry in the output data map.
	 * It filters for runners that have available "to back" prices.
	 *
	 * @param mb The MarketBook object to process.
	 */
	private void addHorseOddsToOutputData(MarketBook mb) {
		mb.getRunners().stream()
				.filter(horse -> horse.getEx().getAvailableToBack().size() > 0)
				.forEach(horse -> outputData.get(horse.getSelectionId())
						.setOdds(horse.getEx().getAvailableToBack().get(0).getPrice()));
	}

	/**
	 * Gets the current session object.
	 *
	 * @return The Session object containing session details like the session token and app ID.
	 */
	public Session getSession() {
		return session;
	}
}
