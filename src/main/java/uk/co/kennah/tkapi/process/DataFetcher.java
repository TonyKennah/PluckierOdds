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
	private ApiNgOperations jsonOperations;
	private HashMap<Long, MyRunner> outputData;
	private Session session;

	public DataFetcher() {
		this.jsonOperations = ApiNgJsonRpcOperations.getInstance();
		this.outputData = new HashMap<Long, MyRunner>();
		this.session = new Session(new ConfigLoader().load());
	}

	public HashMap<Long, MyRunner> getData(String date) {
		return getAllData(date);
	}

	private HashMap<Long, MyRunner> getAllData(String date) {
		try {
			// This catalogue contains Markets, seledctionIds, names for each horse
			List<MarketCatalogue> catalogue = getMarketCatalogue(date);
			catalogue.forEach(entry -> addHorsesNamestoOutputData(entry));

			// This marketBook contains the odds for each seledctionId
			List<MarketBook> book = getMarketBook(catalogue);
			book.forEach(entry -> addHorsesOddstoOutputData(entry));

		} catch (APINGException apiExc) {
			System.out.println("\nAPINGException!!!!!!!!!!!\n WHATS CAUGHT: " + apiExc.toString());
		}
		return outputData;
	}

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
				List.of(MarketProjection.EVENT, MarketProjection.RUNNER_DESCRIPTION));
		return jsonOperations.listMarketCatalogue(marketFilter, marketProjection, MarketSort.FIRST_TO_START, "1000",
				session.getAppid(), session.getSessionToken());
	}

	private void addHorsesNamestoOutputData(MarketCatalogue mk) {
		mk.getRunners()
				.forEach(horse -> outputData.put(horse.getSelectionId(), new MyRunner(horse.getRunnerName())));
	}

	private void addHorsesOddstoOutputData(MarketBook mb) {
		mb.getRunners().stream()
				.filter(horse -> horse.getEx().getAvailableToBack().size() > 0)
				.forEach(horse -> outputData.get(horse.getSelectionId())
						.setOdds(horse.getEx().getAvailableToBack().get(0).getPrice()));
	}

	public Session getSession() {
		return session;
	}
}
