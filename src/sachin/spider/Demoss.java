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
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
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
		return (url != null && !url.contains("?") && !url.contains("#"));
	}

	@Override
	public void viewLink(WebURL webUrl, HttpResponse response, int statusCode, String statusDescription) {
		System.out.println(statusCode + " : " + webUrl.getUrl());
		System.out.println("Depth: " + webUrl.getDepth());
		System.out.println("Mime: " + webUrl.getMimeType());
		System.out.println(
				"--------------------------------------------------------------------------------------------------------");

	}

	void go() {
		try {
			config = new SpiderConfig(site.trim());
		} catch (Exception ex) {
			Logger.getLogger(Demoss.class.getName()).log(Level.SEVERE, null, ex);
		}
		if (null == config)
			return;
		config.setConnectionRequestTimeout(120000);
		config.setConnectionTimeout(120000);
		config.setSocketTimeout(120000);
		config.setTotalSpiders(30);
		config.setPoliteness(200);
		// config.setUserAgentString(
		// "Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X)
		// AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143
		// Safari/601.1");
		// config.setAuthenticate(true);
		// config.setUsername("wldevuser");
		// config.setPassword("Pass@word11");
		try {
			config.start(this);
			System.out.println(config.isCompleted());
		} catch (Exception ex) {
			Logger.getLogger(Demoss.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public static void main(String... str) {
		Demoss d = new Demoss("http://dove.co.uk");
		d.go();
	}
}
