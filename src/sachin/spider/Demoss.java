/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sachin.spider;

/**
 *
 * @author sku202
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;

public class Demoss extends WebSpider {
	final String site;
	String host;
	SpiderConfig config;

	public Demoss(String site) {
		this.site = site;
		try {
			this.host = new URL(site).getHost().replaceAll("www.", "");
		} catch (MalformedURLException ex) {
			Logger.getLogger(Demoss.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public boolean shouldVisit(String url) {
		// return false;
		return (url != null && !url.contains("?"));
	}

	@Override
	public void handleLink(WebURL webUrl, HttpResponse response, int statusCode, String statusDescription) {

		System.out.println(statusCode + " : " + webUrl.getUrl());
		// System.out.println("Parent : " + webUrl.getParents());
		// System.out.println("--------------------------------------------------------------------------------------------------------");

	}

	void go() {
		config = new SpiderConfig(site.trim());
		config.setConnectionRequestTimeout(120000);
		config.setConnectionTimeout(120000);
		config.setSocketTimeout(120000);
		config.setTotalSpiders(30);
		config.setUserAgentString(
				"Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1");
//		config.setAuthenticate(true);
//		config.setUsername("wldevuser");
//		config.setPassword("Pass@word11");
		try {
			config.start(this, config);
			System.out.println(config.isCompleted());
		} catch (Exception ex) {
			Logger.getLogger(Demoss.class.getName()).log(Level.SEVERE, null, ex);
		}
		// System.out.println(config.getModifiedSiteName());
	}

	public static void main(String... str) {

		new Demoss("http://www.liptontea.com/").go();
	}
}
