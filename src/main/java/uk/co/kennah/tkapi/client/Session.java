package uk.co.kennah.tkapi.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import uk.co.kennah.tkapi.model.AppConfig;

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

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class Session {

	private final String bfun;
	private final String bfpw;
	private final String appid;
	private String sessionToken;
	private String status;
	private final String ctpw;

	public Session(AppConfig config) {
		this.ctpw = config.getCertPassword(); // Ensure certPassword is loaded
		this.appid = config.getAppId(); // Ensure appId is loaded
		this.bfun = config.getUsername(); // Ensure username is loaded
		this.bfpw = config.getPassword(); // Ensure password is loaded
	}

	public String getSessionToken() {
		return sessionToken;
	}

	public String getStatus() {
		return status;
	}

	public String getAppid() {
		return appid;
	}

	public void login() {
		// Reverted to DefaultHttpClient for compatibility with older JARs on the
		// classpath
		DefaultHttpClient httpClient = new DefaultHttpClient();
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			KeyStore keyStore = KeyStore.getInstance("pkcs12");
			// Load keystore from the classpath (src/main/resources)
			try (InputStream keyStoreStream = Session.class.getClassLoader()
					.getResourceAsStream("client-2048.p12")) {
				if (keyStoreStream == null) {
					throw new RuntimeException(
							"Could not find client-2048.p12 on the classpath. Make sure it's in src/main/resources.");
				}
				keyStore.load(keyStoreStream, ctpw.toCharArray());
			}

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
				this.sessionToken = jsonObject.has("sessionToken") ? jsonObject.get("sessionToken").getAsString() : "";
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

	public void logout() {
		// Reverted to DefaultHttpClient for compatibility
		DefaultHttpClient httpClient = new DefaultHttpClient();
		try {
			HttpPost httpPost = new HttpPost("https://identitysso.betfair.com/api/logout");
			httpPost.setHeader("Accept", "application/json");
			httpPost.setHeader("X-Authentication", this.sessionToken);
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
}