package controllers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;
import util.Util;

public class IndexController extends Controller {

	// own account's behalf
	public static String ACCESS_TOKEN = "your access token";
	public static String ACCESS_TOKEN_SECRET = "your access token secret";
	public static String USER_NAME = "user name";

	public Result index() {
		// FIXME login not working, set isLogin TRUE to try other function
		Boolean isLogin = Boolean.TRUE;
		if (StringUtils.isNotBlank(ACCESS_TOKEN) && StringUtils.isNotBlank(ACCESS_TOKEN)) {
			isLogin = Boolean.TRUE;
		}
		// temp
		// Util.setCookieSession("userName", USER_NAME);
		// Util.setCookieSession("AT", ACCESS_TOKEN);
		// Util.setCookieSession("ATS", ACCESS_TOKEN_SECRET);
		return Results.ok(views.html.index.render(isLogin));
	}

	public Result requesttoken() {
		System.out.println("－－－－－in requesttoken start－－－－－");
		Map<String, String> requestTokenMap = new LinkedHashMap<String, String>();
		// FIXME @see route and need to match twitter.com app setting or it'll be wrong
		requestTokenMap.put("oauth_callback", "http://127.0.0.1:9000/callback");

		String step1 = Util.fetchDataFromTwitter_Post(Util.REQUEST_URL_REQUEST_TOKEN, requestTokenMap,
				Util.CONSUMER_KEY, Util.CONSUMER_SECRET, null, null);
		System.out.println("step1 = " + step1);
		String oauth_token_split = step1.split("&")[0];
		String oauth_token = oauth_token_split.replace("oauth_token=", "");
		Map<String, String> authMap = new LinkedHashMap<String, String>();
		authMap.put("oauth_token", oauth_token);
		String step2 = Util.fetchDataFromTwitter_Get(Util.REQUEST_URL_AUTHENTICATE, authMap, Util.CONSUMER_KEY,
				Util.CONSUMER_SECRET, null, null);
		//System.out.println("step2 = " + step2);
		return ok(step2);
	}

	/**
	 * @see https://dev.twitter.com/web/sign-in/implementing
	 * @see http://hayageek.com/login-with-twitter/ (***validate if its logged in or not)
	 * @return
	 */
	public Result accessaccount() {
		System.out.println("－－－－－in ACCESS ACCOUNT start－－－－－");
		DynamicForm form = Form.form().bindFromRequest();
		if (form != null) {
			String oauth_token = form.data().get("oauth_token");
			System.out.println("oauth_token = " + oauth_token);
			String oauth_verifier = form.data().get("oauth_verifier");
			if (StringUtils.isNotBlank(oauth_token) && StringUtils.isNotBlank(oauth_verifier)) {
				Map<String, String> verifierMap = new LinkedHashMap<String, String>();
				verifierMap.put("oauth_token", oauth_token);
				verifierMap.put("oauth_verifier", oauth_verifier);
				String accessStr = Util.fetchDataFromTwitter_Post(Util.REQUEST_URL_ACCESS_TOKEN, verifierMap,
						Util.CONSUMER_KEY, Util.CONSUMER_SECRET, ACCESS_TOKEN, ACCESS_TOKEN_SECRET);
				if (!accessStr.contains("Errors")) {
					String[] splitResult = accessStr.split("&");
					String access_token = splitResult[0];
					ACCESS_TOKEN = access_token.replace("oauth_token=", "");
					String access_secret = splitResult[1];
					ACCESS_TOKEN_SECRET = access_secret.replace("oauth_token_secret=", "");
					USER_NAME = splitResult[3].replace("screen_name=", "");
					Util.setCookieSession("AT", ACCESS_TOKEN);
					Util.setCookieSession("ATS", ACCESS_TOKEN_SECRET);
				} else {
					return ok(accessStr);
				}
			} else {
				return ok("access denied");
			}
		}
		return ok("access account");
	}

}
