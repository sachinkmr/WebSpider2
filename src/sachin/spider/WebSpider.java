/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sachin.spider;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 *
 * @author JARVIS
 */
public class WebSpider implements Runnable {

	private CountDownLatch latch;
	private int trackers = 0;
	private HttpClient httpclient = null;
	private SpiderConfig config;
	private static boolean flag2 = true;

	/**
	 *
	 * @param config
	 * @param latch
	 */
	public void setValues(SpiderConfig config, CountDownLatch latch) {
		this.config = config;
		this.latch = latch;
		httpclient = config.getHttpclient();
	}

	/**
	 * This function is called just before starting the crawling by this spider
	 * instance. It can be used for setting up the data structures or
	 * initializations needed by this spider instance.
	 */
	protected void onStart() {
	}

	/**
	 * This function is called just before the termination of the current spider
	 * instance. It can be used for other finalization tasks.
	 */
	protected void onBeforeExit() {
	}

	/**
	 * It can be overridden by sub-classes to perform custom logic for different
	 * status codes. For example, 404 pages can be logged, etc.
	 *
	 * @param webUrl
	 *            WebUrl containing the statusCode
	 * @param response
	 *            response of the url
	 * @param statusCode
	 *            Html Status Code number
	 * @param statusDescription
	 *            Html Status COde description
	 */
	protected void handleLink(WebURL webUrl, HttpResponse response, int statusCode, String statusDescription) {

	}

	/**
	 * This function is called before processing of the page's URL It can be
	 * overridden by subclasses for tweaking of the url before processing it.
	 * For example, http://abc.com/def?a=123 - http://abc.com/def
	 *
	 * @param curURL
	 *            current URL which can be tweaked before processing
	 * @return tweaked WebURL
	 */
	protected String handleUrlBeforeProcess(String curURL) {
		return curURL;
	}

	/**
	 * Classes that extends WebSpider should overwrite this function to tell the
	 * spider whether the given url should be crawled or not. The following
	 * default implementation indicates that all urls should be included in the
	 * crawling process.
	 *
	 * @param url
	 *            the url which we are interested to know whether it should be
	 *            included in the crawl or not.
	 *
	 * @return if the url should be included in the crawl it returns true,
	 *         otherwise false is returned.
	 */
	protected boolean shouldVisit(String url) {
		// By default allow all urls to be crawled.
		return true;
	}

	/**
	 * Classes that extends WebSpider should overwrite this function to process
	 * the response of the url if it has OK status message(Status code: 200)
	 *
	 * @param document
	 *            document of the page
	 * @param webUrl
	 *            url of the page
	 *
	 */
	protected void visitPage(Document document, WebURL webUrl) {

	}

	@Override
	public void run() {
		onStart();
		try {
			boolean flag = true;
			WebURL curUrl = null;
			synchronized (this) {
				if (flag2) {
					curUrl = config.links.get(0);
					if (!curUrl.isProccessed()) {
						processURL(curUrl);
					}
					updateTracker();
					flag2 = false;
				}
			}
			while (flag) {
				int track = getTracker();
				if (track < config.links.size()) {
					curUrl = config.links.get(track);
					if (!curUrl.isProccessed()) {
						processURL(curUrl);
					}
					updateTracker();
				} else {
					Thread.sleep(2000);
					if (track < config.links.size()) {

					} else {
						flag = false;
					}
				}
			}

			if (latch.getCount() == 1) {
				onBeforeExit();
			}
		} catch (Exception ex) {
			Logger.getLogger(WebSpider.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			this.latch.countDown();
		}
		verifyUnProcessedUrls();
	}

	private void processURL(WebURL curUrl) {
		handleLink(curUrl, curUrl.getResponse(), curUrl.getStatusCode(),
				EnglishReasonPhraseCatalog.INSTANCE.getReason(curUrl.getStatusCode(), Locale.ENGLISH));
		if (curUrl.getStatusCode() >= 300 && curUrl.getStatusCode() < 400) {
			handleRedirectedLink(curUrl);
		} else if (curUrl.getStatusCode() == 200 && curUrl.getMimeType().toLowerCase().contains("/html")) {
			try {
				processPage(curUrl);
			} catch (Exception ex) {
				Logger.getLogger(WebSpider.class.getName()).log(Level.SEVERE, null, ex);
			}
		} else {
			EntityUtils.consumeQuietly(curUrl.getResponse().getEntity());
			HttpClientUtils.closeQuietly(curUrl.getResponse());
		}
		curUrl.setProccessed(true);
	}

	private void processPage(WebURL curUrl) {
		HttpResponse response = curUrl.getResponse();
		long startingTime = System.currentTimeMillis();
		Document doc = Jsoup.parse(getContentAsString(response), curUrl.getBaseHref());
		long endingTime = System.currentTimeMillis();
		Page page = new Page(curUrl, doc);
		page.setHeaders(curUrl.getHeaders());
		page.setResposneTime(curUrl.getResposneTime() + ((int) (endingTime - startingTime)) / 1000);
		page.setStatusCode(curUrl.getStatusCode());
		page.setStatusMessage(curUrl.getStatusMessage());
		page.setProccessed(true);
		viewPage(page);
		List<String> list = page.getOutgoingLinks();
		for (String linkUrl : list) {
			if (null != linkUrl) {
				linkUrl = URLCanonicalizer.getCanonicalURL(linkUrl);
				linkUrl = handleUrlBeforeProcess(linkUrl);
				WebURL weburl = new WebURL(linkUrl, httpclient);
				weburl.addParent(curUrl);
				if (shouldVisit(linkUrl)) {
					if (!config.links.contains(weburl) && !linkUrl.contains("#") && !linkUrl.contains("mailto:")) {
						config.links.add(weburl);
					}
				}
			}
		}
		visitPage(doc, curUrl);
	}

	private void handleRedirectedLink(WebURL curUrl) {
		String url = curUrl.getUrl();
		HttpGet httpget = new HttpGet(url);
		RequestConfig requestConfig = getRequestConfigWithRedirectDisabled();
		httpget.setConfig(requestConfig);

		try {
			HttpClientContext context = HttpClientContext.create();
			HttpResponse response = httpclient.execute(httpget, context);
			HttpHost target = context.getTargetHost();
			List<URI> redirectLocations = context.getRedirectLocations();
			URI location = URIUtils.resolve(httpget.getURI(), target, redirectLocations);
			String redirectUrl = location.toString();
			curUrl.setRedirectTo(redirectUrl);

			redirectUrl = URLCanonicalizer.getCanonicalURL(redirectUrl);
			WebURL weburl = new WebURL(redirectUrl, httpclient);
			weburl.addParent(curUrl);
			if (!config.links.contains(weburl)) {
				config.links.add(weburl);
			}
			try {
				if (redirectLocations != null) {
					WebURL par = curUrl;
					for (URI s : redirectLocations) {
						String urls = URLCanonicalizer.getCanonicalURL(s.toString());
						WebURL url1 = new WebURL(urls, httpclient);
						if (!config.links.contains(url1)) {
							url1.addParent(par);
							config.links.add(url1);
						}
						par = url1;
					}
				}
			} catch (Exception ex) {
				Logger.getLogger(WebSpider.class.getName()).log(Level.SEVERE, null, ex);
			}
			EntityUtils.consumeQuietly(response.getEntity());
			HttpClientUtils.closeQuietly(response);
		} catch (IOException | URISyntaxException ex) {
			System.out.println(curUrl.getUrl());
			curUrl.setErrorMsg(ex.toString());
			Logger.getLogger(WebSpider.class.getName()).log(Level.SEVERE, null, ex);
		} catch (Exception ex) {
			System.out.println(curUrl.getUrl());
			curUrl.setErrorMsg(ex.toString());
			Logger.getLogger(WebSpider.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			httpget.releaseConnection();
		}

	}

	private RequestConfig getRequestConfigWithRedirectDisabled() {
		return RequestConfig.custom().setRedirectsEnabled(false)
				.setConnectionRequestTimeout(config.getConnectionRequestTimeout())
				.setSocketTimeout(config.getSocketTimeout()).setConnectTimeout(config.getConnectionTimeout()).build();
	}

	private synchronized int getTracker() {
		return trackers;
	}

	private synchronized void updateTracker() {
		trackers++;
	}

	private void verifyUnProcessedUrls() {
		for (WebURL url : config.links) {
			if (!url.isProccessed()) {
				processURL(url);
			}
		}
	}

	private String getContentAsString(HttpResponse response) {
		String str = null;
		try {
			HttpEntity entity = response.getEntity();
			str = IOUtils.toString(entity.getContent(), "UTF-8");
		} catch (IOException ex) {
			Logger.getLogger(WebSpider.class.getName()).log(Level.SEVERE, null, ex);
		}
		return str;
	}

	protected void viewPage(Page page) {

	}
}
