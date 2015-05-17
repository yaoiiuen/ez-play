package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.http.HttpParameters;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import play.mvc.Controller;
import play.mvc.Http.Cookie;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Util {

	// application
	public static final String CONSUMER_KEY = "your consumer key";
	public static final String CONSUMER_SECRET = "your consumer secret";

	// sign in
	public static final String REQUEST_URL_REQUEST_TOKEN = "https://api.twitter.com/oauth/request_token";
	public static final String REQUEST_URL_AUTHENTICATE = "https://api.twitter.com/oauth/authenticate";
	public static final String REQUEST_URL_ACCESS_TOKEN = "https://api.twitter.com/oauth/access_token";

	/**
	 * [POST] get data from twitter ★ Twitter does not allow identically
	 * duplicate tweets.
	 * 
	 * @param endPointUrl
	 * @param data
	 * @see http://fanli7.net/a/caozuoxitong/OS/20121020/239532.html
	 */
	public static String fetchDataFromTwitter_Post(String endPointUrl,
			Map<String, String> dataMap, String consumerKey,
			String consumerSecret, String accessToken, String accessTokenSecret) {
		String fromTwitter = "";
		// Send the request and read the output
		StringBuilder result = new StringBuilder();
		try {
			// Create an HttpURLConnection and add some headers
			URL url = new URL(endPointUrl);
			HttpURLConnection urlConnection = (HttpURLConnection) url
					.openConnection();
			urlConnection.setRequestMethod("POST");
			// Post cannot use caches
			urlConnection.setUseCaches(false);
			urlConnection.setDoOutput(true);

			// Build the list of parameters
			HttpParameters params = new HttpParameters();
			if (dataMap != null)
				for (Entry<String, String> entry : dataMap.entrySet()) {
					params.put(entry.getKey(), entry.getValue());
				}
			// Sign the request
			OAuthConsumer dealabsConsumer = new DefaultOAuthConsumer(
					consumerKey, consumerSecret);
			dealabsConsumer.setTokenWithSecret(accessToken, accessTokenSecret);
			HttpParameters doubleEncodedParams = new HttpParameters();
			Iterator<String> iter = params.keySet().iterator();
			while (iter.hasNext()) {
				String key = iter.next();
				doubleEncodedParams.put(key,
						OAuth.percentEncode(params.getFirst(key)));
			}
			doubleEncodedParams.put("realm", endPointUrl);
			dealabsConsumer.setAdditionalParameters(doubleEncodedParams);
			dealabsConsumer.sign(urlConnection);
			// prepare the data
			StringBuilder sb = new StringBuilder();
			int count = 0;
			for (Entry<String, SortedSet<String>> entry : params.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue().first();
				if (count > 0) {
					sb.append("&");
				}
				sb.append(key + "=" + URLEncoder.encode(value, "UTF-8"));
				// sb.append(key + "=" + value);
				count++;
			}

			// Send to the connection
			String formEncoded = sb.toString();
			// System.out.println("formEncoded = " + formEncoded);
			OutputStreamWriter outputStreamWriter = null;
			try {
				outputStreamWriter = new OutputStreamWriter(
						urlConnection.getOutputStream(), "UTF-8");
				outputStreamWriter.write(formEncoded);
			} finally {
				if (outputStreamWriter != null) {
					outputStreamWriter.close();
				}
			}

			String line = "";
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(
						urlConnection.getInputStream(), "UTF-8"));
				while (null != (line = br.readLine())) {
					if (!line.contains("errors")) {
						result.append(line);
					} else {
						ObjectMapper mapper = new ObjectMapper();
						JsonNode rootNode = mapper.readTree(line);
						String errMsg = rootNode.findValue("message")
								.textValue();
						result.append("Errors：" + errMsg);
					}
				}
			} finally {
				urlConnection.disconnect();
			}
			// System.out.println("fromTwitter > " + fromTwitter);
		} catch (IOException e) {
			result.append("Errors：Something went wrong...");
			e.printStackTrace();
		} catch (Exception e) {
			result.append("Errors：Oops!.. Something went wrong.");
			e.printStackTrace();
		}
		fromTwitter = result.toString();
		return fromTwitter;
	}

	/**
	 * [GET] get data from twitter
	 * 
	 * @param endPointUrl
	 * @param dataMap
	 * @return
	 */
	public static String fetchDataFromTwitter_Get(String endPointUrl,
			Map<String, String> dataMap, String consumerKey,
			String consumerSecret, String accessToken, String accessTokenSecret) {
		String fromTwitter = "";
		StringBuilder sb = new StringBuilder();
		try {
			OAuthConsumer oAuthConsumer = new CommonsHttpOAuthConsumer(
					consumerKey, consumerSecret);
			oAuthConsumer.setTokenWithSecret(accessToken, accessTokenSecret);
			StringBuilder source = new StringBuilder();
			int count = 0;
			for (Entry<String, String> entry : dataMap.entrySet()) {
				if (count > 0) {
					source.append("&");
				}
				source.append(entry.getKey() + "=" + entry.getValue());
				count++;
			}
			String url = endPointUrl + "?" + source.toString();
			HttpGet httpGet = new HttpGet(url);
			oAuthConsumer.sign(httpGet);

			HttpClient httpClient = new DefaultHttpClient();
			HttpResponse httpResponse = httpClient.execute(httpGet);

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					httpResponse.getEntity().getContent()));
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (!line.contains("errors")) {
					sb.append(line);
				} else {
					ObjectMapper mapper = new ObjectMapper();
					JsonNode rootNode = mapper.readTree(line);
					String errMsg = rootNode.findValue("message").textValue();
					sb.append("Errors：" + errMsg);
				}
			}
			// System.out.println("fromTwitter get= " + fromTwitter);
		} catch (IOException e) {
			sb.append("Errors：Something went wrong...");
			e.printStackTrace();
		} catch (Exception e) {
			sb.append("Errors：Oops!.. Something went wrong.");
			e.printStackTrace();
		}
		fromTwitter = sb.toString();
		return fromTwitter;
	}

	/**
	 * setCookieSession
	 * 
	 * @param String
	 *            key
	 * @param String
	 *            value
	 */
	public static void setCookieSession(String key, String value) {
		Controller.response().setCookie(key, value);
	}

	/**
	 * delCookieSession
	 * 
	 * @param key
	 */
	public static void delCookieSession(String key) {
		Controller.response().discardCookie(key);
		// Controller.response().setCookie(key, "");
	}

	/**
	 * getCookieSession
	 * 
	 * @param key
	 * @return value
	 */
	public static String getCookieSession(String key) {
		// Cookies cookies = play.mvc.Controller.request().cookies();
		Cookie cookie = Controller.request().cookie(key);
		String result = "";
		try {
			if (cookie == null) {
				return result;
			}
			result = URLDecoder.decode(cookie.value(), "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
}
