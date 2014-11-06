package src.server.horseprovider.betfair;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import src.shared.odds.MyRunner;

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
import com.betfair.aping.enums.MatchProjection;
import com.betfair.aping.enums.OrderProjection;
import com.betfair.aping.enums.PriceData;
import com.betfair.aping.exceptions.APINGException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BetfairFace {
	private String appid = "xxxxxxx";
	private String bfun = "xxxxxxx";
	private String bfpw = "xxxxxxx";
	private String ctpw = "xxxxxxx";
	private String session = "";
	private String status = "";
	private ApiNgOperations jsonOperations = ApiNgJsonRpcOperations
			.getInstance();
	private String applicationKey;
	private String sessionToken;
	private HashMap<Long, MyRunner> mine = new HashMap<Long, MyRunner>();

	public void betfairLogin() {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			KeyStore keyStore = KeyStore.getInstance("pkcs12");
			keyStore.load(new FileInputStream(new File("client-2048.p12")),
					ctpw.toCharArray());
			KeyManagerFactory kmf = KeyManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(keyStore, ctpw.toCharArray());
			KeyManager[] keyManagers = kmf.getKeyManagers();
			ctx.init(keyManagers, null, new SecureRandom());
			SSLSocketFactory factory = new SSLSocketFactory(ctx,
					new StrictHostnameVerifier());
			ClientConnectionManager manager = httpClient.getConnectionManager();
			manager.getSchemeRegistry().register(
					new Scheme("https", 443, factory));
			HttpPost httpPost = new HttpPost(
					"https://identitysso.betfair.com/api/certlogin");
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("username", bfun));
			nvps.add(new BasicNameValuePair("password", bfpw));
			httpPost.setEntity(new UrlEncodedFormEntity(nvps));
			httpPost.setHeader("X-Application", appid);
			HttpResponse response = httpClient.execute(httpPost);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				String responseString = EntityUtils.toString(entity);
				responseString = responseString.replace("}", "")
						.replace("{", "").replace("\"", "");
				String[] params = responseString.split(",");
				for (String s : params) {
					String[] pv = s.split(":");
					for (int i = 0; i < pv.length; i++) {
						if (pv[i].equals("sessionToken")) {
							session = pv[i + 1];
						}
						if (pv[i].equals("loginStatus")) {
							status = pv[i + 1];
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Eception Caught : " + e.getMessage());
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
	}

	public HashMap<Long, MyRunner> start(String appKey, String ssoid) {
		this.applicationKey = appKey;
		this.sessionToken = ssoid;
		try {
			MarketFilter marketFilter;
			marketFilter = new MarketFilter();
			Set<String> eventTypeIds = new HashSet<String>();
			List<EventTypeResult> r = jsonOperations.listEventTypes(
					marketFilter, applicationKey, sessionToken);

			for (EventTypeResult eventTypeResult : r) {
				if (eventTypeResult.getEventType().getName()
						.equals("Horse Racing")) {
					eventTypeIds.add(eventTypeResult.getEventType().getId()
							.toString());
				}
			}
			Calendar cal = Calendar.getInstance();
			TimeRange time = new TimeRange();
			Date now = cal.getTime();
			time.setFrom(now);
			cal.set(Calendar.HOUR_OF_DAY, 23);
			Date end = cal.getTime();
			time.setTo(end);
			Set<String> countries = new HashSet<String>();
			countries.add("GB");
			countries.add("IE");
			countries.add("ZA");
			countries.add("FR");
			countries.add("AE");
			Set<String> typesCode = new HashSet<String>();
			typesCode.add("WIN");
			marketFilter = new MarketFilter();
			marketFilter.setEventTypeIds(eventTypeIds);
			marketFilter.setMarketStartTime(time);
			marketFilter.setMarketCountries(countries);
			marketFilter.setMarketTypeCodes(typesCode);
			Set<MarketProjection> marketProjection = new HashSet<MarketProjection>();
			marketProjection.add(MarketProjection.EVENT);
			marketProjection.add(MarketProjection.RUNNER_DESCRIPTION);
			String maxResults = "1000";
			List<MarketCatalogue> marketCatalogueResult = jsonOperations
					.listMarketCatalogue(marketFilter, marketProjection,
							MarketSort.FIRST_TO_START, maxResults,
							applicationKey, sessionToken);
			for (MarketCatalogue mc : marketCatalogueResult) {
				printMarketCatalogue(mc);
			}

			int loops = (marketCatalogueResult.size() / 20) + 1;
			ArrayList<List<MarketCatalogue>> chunked = new ArrayList<List<MarketCatalogue>>();
			for (int i = 0; i < loops; i++) {
				List<MarketCatalogue> tobeused = marketCatalogueResult.subList(
						Math.max(marketCatalogueResult.size() - 20, 0),
						marketCatalogueResult.size());
				List<MarketCatalogue> chunk = new ArrayList<MarketCatalogue>(
						tobeused);
				tobeused.clear();
				chunked.add(chunk);
			}

			for (int i = 0; i < loops; i++) {
				List<String> marketIds = new ArrayList<String>();
				for (MarketCatalogue mc : chunked.get(i)) {
					marketIds.add(mc.getMarketId());
				}
				PriceProjection priceProjection = new PriceProjection();
				Set<PriceData> priceData = new HashSet<PriceData>();
				priceData.add(PriceData.EX_BEST_OFFERS);
				priceProjection.setPriceData(priceData);
				OrderProjection orderProjection = null;
				MatchProjection matchProjection = null;
				String currencyCode = null;
				List<MarketBook> marketBookReturn = jsonOperations
						.listMarketBook(marketIds, priceProjection,
								orderProjection, matchProjection, currencyCode,
								applicationKey, sessionToken);
				for (MarketBook mb : marketBookReturn) {
					printBookCatalogue(mb);
				}
			}
		} catch (APINGException apiExc) {
			System.out
					.println("\n\n\n\n\nAPINGException!!!!!!!!!!!\n\n\n\n\n HERE WHATS CAUGHT: "
							+ apiExc.toString());
		}
		return mine;
	}

	private void printMarketCatalogue(MarketCatalogue mk) {
		List<RunnerCatalog> runners = mk.getRunners();
		if (runners != null) {
			for (RunnerCatalog rCat : runners) {
				mine.put(rCat.getSelectionId(),
						new MyRunner(rCat.getRunnerName()));
			}
		}
	}

	private void printBookCatalogue(MarketBook mb) {
		List<Runner> runners = mb.getRunners();
		if (runners != null) {
			for (Runner rCat : runners) {
				if (rCat.getEx().getAvailableToBack().size() > 0) {
					mine.get(rCat.getSelectionId())
							.setOdds(
									rCat.getEx().getAvailableToBack().get(0)
											.getPrice());
				}
			}
		}
	}

	public void betfairLogout() {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		try {
			HttpPost httpPost = new HttpPost(
					"https://identitysso.betfair.com/api/logout");
			httpPost.setHeader("Accept", "application/json");
			httpPost.setHeader("X-Authentication", session);
			httpPost.setHeader("X-Application", appid);
			HttpResponse response = httpClient.execute(httpPost);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				String responseString = EntityUtils.toString(entity);
				responseString = responseString.replace("}", "")
						.replace("{", "").replace("\"", "");
				String[] params = responseString.split(",");
				for (String s : params) {
					String[] pv = s.split(":");
					for (int i = 0; i < pv.length; i++) {
						if (pv[i].equals("status")) {
							status = pv[i + 1];
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Eception Caught : " + e.getMessage());
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
	}

	public String getSession() {
		return session;
	}

	public void setSession(String session) {
		this.session = session;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getAppid() {
		return appid;
	}

	public void createTheFile(String nameOfFile, HashMap<Long, MyRunner> bd) {
		try {
			File file = new File(nameOfFile);
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);

			// test
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(bd);
			int length = baos.toByteArray().length;
			System.out.println("Length of file is: " + length + " bytes");

			if (length > 100 && bd instanceof HashMap) {
				for (Long keys : bd.keySet()) {
					Double odd = 0.0;
					if (mine.get(keys).getOdds() != null) {
						odd = mine.get(keys).getOdds();
					}
					bw.write(mine.get(keys).getName() + "#" + odd + "\n");
				}
				bw.close();
				// System.out.println("-----CREATED----");

			} else {
				System.out.println("The odds file is very short!");
			}
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
			System.out.println("Exception " + fnfe);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.out.println("Exception " + ioe);
		}
	}

	public static void main(String[] args) {
		BetfairFace bf = new BetfairFace();
		bf.betfairLogin();
		HashMap<Long, MyRunner> mine = bf.start(bf.getAppid(), bf.getSession());
		for (Long keys : mine.keySet()) {
			System.out.println(mine.get(keys).getName() + " "
					+ mine.get(keys).getOdds());
		}
		bf.betfairLogout();
	}
}
