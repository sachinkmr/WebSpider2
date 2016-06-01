/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sachin.spider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 *
 * @author JARVIS
 */
public class WebSpider implements Runnable {

	private CountDownLatch latch;
	private HttpClient httpclient = null;
	private SpiderConfig config;
	private static boolean flag2 = true;
	private PoolingHttpClientConnectionManager cm;

	/**
	 *
	 * @param config
	 * @param latch
	 */
	public void setValues(SpiderConfig config, CountDownLatch latch) {
		this.config = config;
		this.latch = latch;
		createHttpClient();
	}

	private void createHttpClient() {
		try {
			HttpClientBuilder builder = HttpClientBuilder.create();
			builder.setUserAgent(config.getUserAgentString());
			SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
				@Override
				public boolean isTrusted(java.security.cert.X509Certificate[] xcs, String string)
						throws java.security.cert.CertificateException {
					return true;
				}
			}).build();
			builder.setSSLContext(sslContext);
			@SuppressWarnings("deprecation")
			HostnameVerifier hostnameVerifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

			SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
					.register("http", PlainConnectionSocketFactory.getSocketFactory())
					.register("https", sslSocketFactory).build();
			cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
			cm.setDefaultMaxPerRoute(config.getTotalSpiders() * 2);
			cm.setMaxTotal(config.getTotalSpiders() * 2);

			RequestConfig requestConfig = getRequestConfigWithRedirectDisabled();

			if (config.isAuthenticate()) {
				CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
				credentialsProvider.setCredentials(AuthScope.ANY,
						new UsernamePasswordCredentials(config.getUsername(), config.getPassword()));
				httpclient = HttpClients.custom().setDefaultRequestConfig(requestConfig)
						.setUserAgent(config.getUserAgentString()).setDefaultCredentialsProvider(credentialsProvider)
						.setConnectionManager(cm).build();

			} else {
				httpclient = HttpClients.custom().setConnectionManager(cm).setUserAgent(config.getUserAgentString())
						.setDefaultRequestConfig(requestConfig).build();
			}
		} catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException ex) {
			Logger.getLogger(SpiderConfig.class.getName()).log(Level.SEVERE, null, ex);
		}
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
		return !url.contains("#");
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
					curUrl = config.links.remove(0);
					if (!curUrl.isProccessed()) {
						processURL(curUrl);
					}
					flag2 = false;
				}
			}

			while (flag) {
				curUrl = config.links.remove(0);
				if (null != curUrl && !curUrl.isProccessed()) {
					processURL(curUrl);
				}
				if (config.links.isEmpty()) {
					flag = false;
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
		if (curUrl.getStatusCode() >= 300 && curUrl.getStatusCode() < 400) {
			handleRedirectedLink(curUrl);
		} else if (curUrl.getStatusCode() == 200 && curUrl.getMimeType().toLowerCase().contains("/htm")) {
			try {
				processPage(curUrl);
			} catch (Exception ex) {
				Logger.getLogger(WebSpider.class.getName()).log(Level.SEVERE, null, ex + curUrl.getUrl());
			}
		}
		curUrl.setProccessed(true);
		handleLink(curUrl, curUrl.getResponse(), curUrl.getStatusCode(),
				EnglishReasonPhraseCatalog.INSTANCE.getReason(curUrl.getStatusCode(), Locale.ENGLISH));
		// EntityUtils.consumeQuietly(curUrl.getResponse().getEntity());
		// HttpClientUtils.closeQuietly(curUrl.getResponse());
	}

	private void processPage(WebURL curUrl) {
		long startingTime = System.currentTimeMillis();
		Document doc = Jsoup.parse(getContentAsString(curUrl), curUrl.getBaseHref());
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
				WebURL weburl = new WebURL(linkUrl, httpclient, config.getHostName());
				weburl.addParent(curUrl);
				if (shouldVisit(linkUrl)) {
					if (!config.hashCodes.contains(weburl.hashCode()) && !linkUrl.contains("mailto:")) {
						config.links.add(weburl);
						config.hashCodes.add(weburl.hashCode());
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
			WebURL weburl = new WebURL(redirectUrl, httpclient, config.getHostName());
			weburl.addParent(curUrl);
			if (!config.hashCodes.contains(weburl.hashCode())) {
				config.links.add(weburl);
				config.hashCodes.add(weburl.hashCode());
			}
			try {
				if (redirectLocations != null) {
					WebURL par = curUrl;
					for (URI s : redirectLocations) {
						String urls = URLCanonicalizer.getCanonicalURL(s.toString());
						WebURL url1 = new WebURL(urls, httpclient, config.getHostName());
						if (!config.hashCodes.contains(url1.hashCode())) {
							url1.addParent(par);
							config.links.add(url1);
							config.hashCodes.add(url1.hashCode());
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
			Logger.getLogger(WebSpider.class.getName()).log(Level.SEVERE, null, ex);
		} catch (Exception ex) {
			System.out.println(curUrl.getUrl());
			Logger.getLogger(WebSpider.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			httpget.releaseConnection();
		}

	}

	@SuppressWarnings("deprecation")
	private RequestConfig getRequestConfigWithRedirectDisabled() {
		return RequestConfig.custom().setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY).setRedirectsEnabled(false)
				.setConnectionRequestTimeout(config.getConnectionRequestTimeout())
				.setSocketTimeout(config.getSocketTimeout()).setConnectTimeout(config.getConnectionTimeout()).build();
	}

	private void verifyUnProcessedUrls() {
		for (WebURL url : config.links) {
			if (!url.isProccessed()) {
				processURL(url);
			}
		}
	}

	private String getContentAsString(WebURL url) {
		String str = null;
		try {
			ObjectInputStream st = new ObjectInputStream(new FileInputStream(
					new File(System.getProperty("user.dir") + File.separator + "data" + File.separator + url.getHost(),
							url.getUrl().hashCode() + ".webUrl")));
			HttpEntity entity = (HttpEntity) st.readObject();
			st.close();
			str = IOUtils.toString(entity.getContent(), "UTF-8");
			EntityUtils.consumeQuietly(entity);
		} catch (Exception ex) {
			Logger.getLogger(WebSpider.class.getName()).log(Level.SEVERE, null, ex);

		}
		return str;
	}

	protected void viewPage(Page page) {

	}
}
