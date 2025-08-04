
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
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.Serializable;

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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.Properties;

public class BetfairFace
{
	private String appid;
	private String bfun;
	private String bfpw;
	private String ctpw;
	private String session = "";
	private String status = "";
	private ApiNgOperations jsonOperations = ApiNgJsonRpcOperations.getInstance();
	private String applicationKey;
	private String sessionToken;
	private HashMap<Long, MyRunner> mine = new HashMap<Long, MyRunner>();

	public BetfairFace() {
		Properties props = new Properties();
		// Use try-with-resources for automatic closing of the reader
		try (FileInputStream in = new FileInputStream("config.properties")) {
			props.load(in);
			this.appid = props.getProperty("betfair.appid");
			this.bfun = props.getProperty("betfair.username");
			this.bfpw = props.getProperty("betfair.password");
			this.ctpw = props.getProperty("betfair.cert.password");

			if (this.appid == null || this.bfun == null || this.bfpw == null || this.ctpw == null ||
				this.appid.isEmpty() || this.bfun.isEmpty() || this.bfpw.isEmpty() || this.ctpw.isEmpty()) {
				throw new IllegalStateException("One or more required properties are missing from config.properties. Please check the file.");
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not load config.properties. Make sure the file exists in the project root and is readable.", e);
		}
	}

	public void betfairLogin()
	{
		// Reverted to DefaultHttpClient for compatibility with older JARs on the classpath
		DefaultHttpClient httpClient = new DefaultHttpClient();
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			KeyStore keyStore = KeyStore.getInstance("pkcs12");
			keyStore.load(new FileInputStream(new File("client-2048.p12")), ctpw.toCharArray());

			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(keyStore, ctpw.toCharArray());
			KeyManager[] keyManagers = kmf.getKeyManagers();
			ctx.init(keyManagers, null, new SecureRandom());

			// Use the deprecated SSLSocketFactory compatible with httpclient-4.2.x
			@SuppressWarnings("deprecation")
			SSLSocketFactory factory = new SSLSocketFactory(ctx, new StrictHostnameVerifier());

			ClientConnectionManager manager = httpClient.getConnectionManager();
			manager.getSchemeRegistry().register(new Scheme("https", 443, factory));

			HttpPost httpPost = new HttpPost("https://identitysso-cert.betfair.com/api/certlogin");
			httpPost.setHeader("X-Application", appid);

			List<NameValuePair> nvps = new ArrayList<>();
			nvps.add(new BasicNameValuePair("username", bfun));
			nvps.add(new BasicNameValuePair("password", bfpw));
			httpPost.setEntity(new UrlEncodedFormEntity(nvps));

			HttpResponse response = httpClient.execute(httpPost);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				String responseString = EntityUtils.toString(entity);
				// Use Gson for safe and reliable JSON parsing
				Gson gson = new Gson();
				JsonObject jsonObject = gson.fromJson(responseString, JsonObject.class);
				this.session = jsonObject.has("sessionToken") ? jsonObject.get("sessionToken").getAsString() : "";
				this.status = jsonObject.has("loginStatus") ? jsonObject.get("loginStatus").getAsString() : "FAIL";
			}
		} catch (Exception e) {
			System.out.println("Eception Caught : " + e.getMessage());
			e.printStackTrace();
		} finally {
			// Ensure the connection manager is always shut down
			httpClient.getConnectionManager().shutdown();
		}
	}

	public HashMap<Long, MyRunner> start(String date, String appKey, String ssoid)
	{
		this.applicationKey = appKey;
		this.sessionToken = ssoid;
		try
		{
			MarketFilter marketFilter;
			marketFilter = new MarketFilter();
			Set<String> eventTypeIds = new HashSet<String>();
			List<EventTypeResult> r = jsonOperations.listEventTypes(marketFilter, applicationKey, sessionToken);

			for (EventTypeResult eventTypeResult : r)
			{
				if (eventTypeResult.getEventType().getName().equals("Horse Racing"))
				{
					eventTypeIds.add(eventTypeResult.getEventType().getId().toString());
				}
			}
			Calendar cal = Calendar.getInstance();
			Date now = cal.getTime();
			cal.set(Calendar.HOUR_OF_DAY, 23);
			Date end = cal.getTime();
			if (!date.equals(""))
			{
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
			for (MarketCatalogue mc : marketCatalogueResult)
			{
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
				for (MarketBook mb : marketBookReturn)
				{
					printBookCatalogue(mb);
				}
			}
		}
		catch (APINGException apiExc)
		{
			System.out.println("\n\n\n\n\nAPINGException!!!!!!!!!!!\n\n\n\n\n HERE WHATS CAUGHT: " + apiExc.toString());
		}
		return mine;
	}
	
	private void printMarketCatalogue(MarketCatalogue mk)
	{
		List<RunnerCatalog> runners = mk.getRunners();
		if (runners != null)
		{
			for (RunnerCatalog rCat : runners)
			{
				//System.out.println(rCat.getRunnerName());
				String name = rCat.getRunnerName();
				if (Character.isDigit(rCat.getRunnerName().charAt(1)))
				{
					name = rCat.getRunnerName().substring(4, rCat.getRunnerName().length());
				}
				else if (Character.isDigit(rCat.getRunnerName().charAt(0)))
				{
					name = rCat.getRunnerName().substring(3, rCat.getRunnerName().length());
				}
				mine.put(rCat.getSelectionId(), new MyRunner(name));
			}
		}
	}

	private void printBookCatalogue(MarketBook mb)
	{
		List<Runner> runners = mb.getRunners();
		if (runners != null)
		{
			for (Runner rCat : runners)
			{
				if (rCat.getEx().getAvailableToBack().size() > 0)
				{
					mine.get(rCat.getSelectionId()).setOdds(rCat.getEx().getAvailableToBack().get(0).getPrice());
				}
			}
		}
	}

	public void betfairLogout()
	{
		// Reverted to DefaultHttpClient for compatibility
		DefaultHttpClient httpClient = new DefaultHttpClient();
		try {
			HttpPost httpPost = new HttpPost("https://identitysso.betfair.com/api/logout");
			httpPost.setHeader("Accept", "application/json");
			httpPost.setHeader("X-Authentication", this.session);
			httpPost.setHeader("X-Application", this.appid);
			HttpResponse response = httpClient.execute(httpPost);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				String responseString = EntityUtils.toString(entity);
				Gson gson = new Gson();
				JsonObject jsonObject = gson.fromJson(responseString, JsonObject.class);
				if (jsonObject.has("status")) {
					this.status = jsonObject.get("status").getAsString();
				}
			}
		} catch (Exception e) {
			System.out.println("Eception Caught : " + e.getMessage());
			e.printStackTrace();
		} finally {
			// Restore the finally block to ensure the connection is always shut down
			httpClient.getConnectionManager().shutdown();
		}
	}

	public String getSession()
	{
		return session;
	}

	public void setSession(String session)
	{
		this.session = session;
	}

	public String getStatus()
	{
		return status;
	}

	public void setStatus(String status)
	{
		this.status = status;
	}

	public String getAppid()
	{
		return appid;
	}
	
	/*public void postFileToServer(File textFile) {

		if (textFile.length() > 500) {
			try {
				String boundary = Long.toHexString(System.currentTimeMillis());
				URLConnection connection = new URL("http://pluckier-tkhorse.rhcloud.com/uploadFile").openConnection();
				connection.setDoOutput(true);
				connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
				OutputStream output = connection.getOutputStream();
				PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"), true);
				writer.append("--" + boundary).append("\r\n");
				writer.append(
						"Content-Disposition: form-data; name=\"textFile\"; filename=\"" + textFile.getName() + "\"")
						.append("\r\n");
				writer.append("Content-Type: text/plain; charset=" + "UTF-8").append("\r\n");
				writer.append("\r\n").flush();
				Files.copy(textFile.toPath(), output);
				output.flush();
				writer.append("\r\n").flush();
				writer.append("--" + boundary + "--").append("\r\n").flush();
				int responseCode = ((HttpURLConnection) connection).getResponseCode();
				System.out.println("server response " + responseCode);
				writer.close();
				output.close();
			} catch (Exception e) {
				System.out.println("EEEEEEEEk");
			}
		}
	}*/

	public File createTheFile(String nameOfFile, HashMap<Long, MyRunner> bd)
	{
		File file = new File(nameOfFile);
		try
		{
			if (!file.exists())
			{
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

			if (length > 500 && bd instanceof HashMap)
			{
				for (Long keys : bd.keySet())
				{
					Double odd = 0.0;
					if (mine.get(keys).getOdds() != null)
					{
						odd = mine.get(keys).getOdds();
					}
					bw.write(mine.get(keys).getName() + "#" + odd + "\n");
				}
				bw.close();
				// System.out.println("-----CREATED----");

			}
			else
			{
				System.out.println("The odds are foooked");
			}
			fw.close();
		}
		catch (FileNotFoundException fnfe)
		{
			fnfe.printStackTrace();
			System.out.println("Exception " + fnfe);
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
			System.out.println("Exception " + ioe);
		}
		return file;
	}

}

class MyRunner implements Serializable {
	private static final long serialVersionUID = 1L;
	public String name;
	public Double odds;

	public MyRunner(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Double getOdds() {
		return odds;
	}

	public void setOdds(Double odds) {
		this.odds = odds;
	}
}