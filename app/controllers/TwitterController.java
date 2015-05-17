package controllers;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;
import util.Util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TwitterController extends Controller {

	// function
	public static final String REQUEST_URL_TIMELINE = "https://api.twitter.com/1.1/statuses/user_timeline.json";
	public static final String REQUEST_URL_UPDATE_STATUSES = "https://api.twitter.com/1.1/statuses/update.json";
	public static final String REQUEST_URL_CREATE_FRIENDSHIP = "https://api.twitter.com/1.1/friendships/create.json";
	public static final String REQUEST_URL_CREATE_FAVORITES = "https://api.twitter.com/1.1/favorites/create.json";
	public static final String REQUEST_URL_CREATE_MESSAGES = "https://api.twitter.com/1.1/direct_messages/new.json";
	public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	public String ACCESS_TOKEN = Util.getCookieSession("AT");
	public String ACCESS_TOKEN_SECRET = Util.getCookieSession("ATS");

	/* ↓[Start]↓-------- form submit from〔main.scala.html〕--------↓[Start]↓* */
	/**
	 * get someone's timeline
	 * 
	 * @return
	 */
	public Result timeline() {
		/** receive form data */
		DynamicForm form = Form.form().bindFromRequest();
		String screenName = form.data().get("screenName");
		Map<String, String> dataMap = new LinkedHashMap<String, String>();
		dataMap.put("screen_name", screenName);
		String timelineStr = Util.fetchDataFromTwitter_Get(REQUEST_URL_TIMELINE, dataMap, Util.CONSUMER_KEY,
				Util.CONSUMER_SECRET, ACCESS_TOKEN, ACCESS_TOKEN_SECRET);
		// System.out.println("timeline >>> " + timelineStr);
		// List<Map<String,Object>>
		List<Map<String, Object>> result = null;
		try {
			result = timelineSource(timelineStr, screenName);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Results.ok(views.html.timeline.render(result));
	}

	private List<Map<String, Object>> timelineSource(String dataStr, String screenName) throws ParseException,
			JsonProcessingException, IOException {
		String userName = Util.getCookieSession("userName");
		List<Map<String, Object>> returnList = new ArrayList<Map<String, Object>>();
		Map<String, Object> sourceMap = null;
		if (!dataStr.contains("Errors")) {
			/** @see http://wiki.fasterxml.com/JacksonTreeModel */
			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = mapper.readTree(dataStr);
			for (int i = 0; i < rootNode.size(); i++) {
				sourceMap = new LinkedHashMap<String, Object>();
				JsonNode jsonNode = rootNode.get(i);
				Date twitterDate = getTwitterDate(jsonNode.path("created_at").textValue());
				String created_at = sdf.format(twitterDate);
				// for the buttons
				String statusId = jsonNode.path("id_str").textValue();
				Integer retweet_count = jsonNode.path("retweet_count").asInt();
				Integer favorite_count = jsonNode.path("favorite_count").asInt();

				String text = jsonNode.path("text").textValue();
				String profile_image_url_https = jsonNode.findValue("profile_image_url_https").textValue();
				// -----
				/** add tag if text contains url */
				// separate input by spaces ( URLs don't have spaces )
				String[] parts = text.split("\\s+");
				// Attempt to convert each item into an URL.
				for (String item : parts) {
					try {
						URL url = new URL(item);
						String tempurl = url + "";
						String anchor = "<a href=\"" + url + "\">" + url + "</a>";
						// If possible then replace with anchor...
						if (StringUtils.containsIgnoreCase(text, tempurl.trim())) {
							text = text.replace(tempurl.trim(), anchor);
						}
						// System.out.print(anchor);
					} catch (MalformedURLException e) {
						// If there was an URL that was not it!...
					}
				}
				// ---
				sourceMap.put("screenName", screenName);
				sourceMap.put("createdAt", created_at);
				sourceMap.put("profileImgUrl", profile_image_url_https);
				sourceMap.put("text", text);

				/** add image if exist */
				List<String> mediaUrlList = new ArrayList<String>();
				for (JsonNode node : jsonNode.path("extended_entities").path("media")) {
					// System.out.println("Entry: "+node.toString());
					JsonNode smallSize = node.path("sizes").path("small");

					sourceMap.put("smallWidth", smallSize.path("w"));
					sourceMap.put("smallHeight", smallSize.path("h"));
					// System.out.println("圖：" +
					// node.path("media_url").textValue());
					// mediaUrlList.add(node.path("media_url").textValue());
					sourceMap.put("mediaUrl", node.path("media_url").textValue());
					// System.out.println("圖片張數#"+mediaUrlList.size());
				}

				// sourceMap.put("mediaUrl", mediaUrlList);
				// reply, rt, favorite
				sourceMap.put("statusId", statusId);
				sourceMap.put("retweetCount", retweet_count);
				sourceMap.put("favoriteCount", favorite_count);
				Boolean isAdmin = Boolean.FALSE;
				// System.out.println("session取得userName = "+userName);
				if (userName.equals(screenName)) {
					// 登入者為自己
					isAdmin = Boolean.TRUE;
				}
				sourceMap.put("isAdmin", isAdmin);
				// -----
				returnList.add(sourceMap);
			}
			// ./not blank
		} else {
			// Sorry, that page does not exist.
			sourceMap = new LinkedHashMap<String, Object>();
			sourceMap.put("errorMsg", dataStr);
			returnList.add(sourceMap);
		}
		return returnList;
	}

	/**
	 * add following
	 * 
	 * @return
	 */
	public Result createfriendship() {
		DynamicForm form = Form.form().bindFromRequest();
		String friendName = form.data().get("friendName");
		Map<String, String> dataMap = new LinkedHashMap<String, String>();
		dataMap.put("screen_name", friendName);
		String friendshipStr = Util.fetchDataFromTwitter_Post(REQUEST_URL_CREATE_FRIENDSHIP, dataMap,
				Util.CONSUMER_KEY, Util.CONSUMER_SECRET, ACCESS_TOKEN, ACCESS_TOKEN_SECRET);
		Boolean isSuccess = isReturnSuccess(friendshipStr);
		return Results.ok(views.html.msg.render(isSuccess));
	}

	/**
	 * post status (personal)
	 * 
	 * @return
	 */
	public Result updatestatus() {
		DynamicForm form = Form.form().bindFromRequest();
		String statusMsg = form.data().get("statusMsg");
		Map<String, String> dataMap = new LinkedHashMap<String, String>();
		dataMap.put("status", statusMsg);
		String statusMsgStr = Util.fetchDataFromTwitter_Post(REQUEST_URL_UPDATE_STATUSES, dataMap, Util.CONSUMER_KEY,
				Util.CONSUMER_SECRET, ACCESS_TOKEN, ACCESS_TOKEN_SECRET);
		// System.out.println("statusMsgStr = " + statusMsgStr);
		Boolean isSuccess = isReturnSuccess(statusMsgStr);
		return Results.ok(views.html.msg.render(isSuccess));
	}

	/* ↑[End]↑-------- form submit from〔main.scala.html〕--------↑[End]↑* */

	/* ↓[Start]↓-------- Ajax Call from〔hello.js〕 --------↓[Start]↓* */
	/**
	 * java invoke 動態呼叫對應method
	 * 
	 * @param methodName
	 * @see routes
	 */
	public Result ajaxCalledMethod(String methodName) {
		String result = "";
		try {
			DynamicForm form = Form.form().bindFromRequest();
			// ★ 要傳的參數需為play的形態 DynamicForm.class NOT Object.class
			Method method = this.getClass().getMethod(methodName, DynamicForm.class);
			// System.out.println("method found：" + method.toString());
			result = (String) method.invoke(this, form);
			if (!result.contains("Errors")) {
				result = "Action Successful.";
			}
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ok(result);
	}

	/**
	 * reply sb's status
	 * 
	 * @return
	 */
	public String replyto(DynamicForm form) {
		String statusId = form.data().get("statusId");
		String content = form.data().get("content");
		Map<String, String> dataMap = new LinkedHashMap<String, String>();
		dataMap.put("status", content);
		dataMap.put("in_reply_to_status_id", statusId);
		String returnData = Util.fetchDataFromTwitter_Post(REQUEST_URL_UPDATE_STATUSES, dataMap, Util.CONSUMER_KEY,
				Util.CONSUMER_SECRET, ACCESS_TOKEN, ACCESS_TOKEN_SECRET);
		return returnData;
	}

	/**
	 * send messages to sb
	 * 
	 * @return
	 */
	public String newMsg(DynamicForm form) {
		String screenName = form.data().get("screenName");
		String directMsg = form.data().get("directMsg");
		Map<String, String> dataMap = new LinkedHashMap<String, String>();
		dataMap.put("screen_name", screenName);
		dataMap.put("text", directMsg);
		String returnData = Util.fetchDataFromTwitter_Post(REQUEST_URL_CREATE_MESSAGES, dataMap, Util.CONSUMER_KEY,
				Util.CONSUMER_SECRET, ACCESS_TOKEN, ACCESS_TOKEN_SECRET);
		return returnData;
	}

	/**
	 * delete status
	 * 
	 * @return
	 */
	public String destroy(DynamicForm form) {
		String statusId = form.data().get("statusId");
		String request_url_destroy = "https://api.twitter.com/1.1/statuses/destroy/" + statusId + ".json";
		String returnData = Util.fetchDataFromTwitter_Post(request_url_destroy, null, Util.CONSUMER_KEY,
				Util.CONSUMER_SECRET, ACCESS_TOKEN, ACCESS_TOKEN_SECRET);
		return returnData;
	}

	/**
	 * add favorites
	 * 
	 * @return
	 */
	public String createFV(DynamicForm form) {
		String statusId = form.data().get("statusId");
		Map<String, String> dataMap = new LinkedHashMap<String, String>();
		dataMap.put("id", statusId);
		String returnData = Util.fetchDataFromTwitter_Post(REQUEST_URL_CREATE_FAVORITES, dataMap, Util.CONSUMER_KEY,
				Util.CONSUMER_SECRET, ACCESS_TOKEN, ACCESS_TOKEN_SECRET);
		return returnData;
	}

	/**
	 * retweet someone's status
	 * 
	 * @return
	 */
	public String rt(DynamicForm form) {
		String statusId = form.data().get("statusId");
		String request_url_retweet = "https://api.twitter.com/1.1/statuses/retweet/" + statusId + ".json";
		String returnData = Util.fetchDataFromTwitter_Post(request_url_retweet, null, Util.CONSUMER_KEY,
				Util.CONSUMER_SECRET, ACCESS_TOKEN, ACCESS_TOKEN_SECRET);
		return returnData;
	}

	/* ↑[End]↑-------- Ajax Call from hello.js --------↑[End]↑* */

	/**
	 * show Success/Failed (banner)
	 * 
	 * @param dataStr
	 * @return
	 */
	private Boolean isReturnSuccess(String dataStr) {
		Boolean isSuccess = Boolean.FALSE;
		if (!dataStr.contains("Errors")) {
			isSuccess = Boolean.TRUE;
		}
		return isSuccess;
	}

	/**
	 * parse twitter date for ENGLISH locale
	 * 
	 * @param date
	 * @return
	 * @throws ParseException
	 */
	public static Date getTwitterDate(String date) throws ParseException {
		String TWITTER_DATE_FORMAT = "EEE MMM dd HH:mm:ss Z yyyy";
		SimpleDateFormat sf = new SimpleDateFormat(TWITTER_DATE_FORMAT, Locale.ENGLISH);
		sf.setLenient(Boolean.TRUE);
		return sf.parse(date);
	}

}
