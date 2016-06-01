/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sachin.spider;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;

/**
 *
 * @author JARVIS
 */
public final class SpiderConfig {
	public final List<Long> hashCodes;
	public final List<WebURL> links;
	private int socketTimeout = 20000;
	private int connectionTimeout = 30000;
	private int ConnectionRequestTimeout = 30000;
	private boolean followRedirects = false;
	private boolean authenticate = false;
	private String username = null;
	private String password = null;
	private String userAgentString = "Mozilla/5.0 (Windows NT 6.2; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.82 Safari/537.36";
	private String modifiedSiteName, siteName;
	private int totalSpiders = 15;
	public int tracker = 0;
	public CountDownLatch latch;
	public ExecutorService executor;
	private String host;
	private HttpClient httpclient = null;
	private PoolingHttpClientConnectionManager cm;

	/**
	 * Constructor of the SpiderConfig Class
	 *
	 * @param url
	 *            String parameter for the site which is going to be crawled.
	 */

	public SpiderConfig(String url) {
		hashCodes=new CopyOnWriteArrayList<>();
		url = URLCanonicalizer.getCanonicalURL(url);
		modifiedSiteName = url;
		createHttpClient();
		try {
			host = new URL(url).getHost().replaceAll("www.", "");
		} catch (MalformedURLException ex) {
			Logger.getLogger(SpiderConfig.class.getName()).log(Level.SEVERE, null, ex);
		}
		links = new CopyOnWriteArrayList<WebURL>();
		modifiedSiteName = handleRedirect(url);
		this.setSiteName(modifiedSiteName);
		links.add(new WebURL(url, httpclient));
		WebURL redirectLocation = new WebURL(modifiedSiteName, httpclient);
		if (!links.contains(redirectLocation)) {
			links.add(0, redirectLocation);
		}

	}

	private void createHttpClient() {
		try {
			HttpClientBuilder builder = HttpClientBuilder.create();
			builder.setUserAgent(this.getUserAgentString());
			SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
				@Override
				public boolean isTrusted(java.security.cert.X509Certificate[] xcs, String string)
						throws java.security.cert.CertificateException {
					return true;
				}
			}).build();
			builder.setSSLContext(sslContext);
			HostnameVerifier hostnameVerifier = SSLConnectionSocketFactory.getDefaultHostnameVerifier();

			SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
					.register("http", PlainConnectionSocketFactory.getSocketFactory())
					.register("https", sslSocketFactory).build();
			cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
			cm.setDefaultMaxPerRoute(this.getTotalSpiders() * 2);
			cm.setMaxTotal(this.getTotalSpiders() * 2);

			RequestConfig requestConfig = getRequestConfigWithRedirectDisabled();

			if (this.isAuthenticate()) {
				CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
				credentialsProvider.setCredentials(AuthScope.ANY,
						new UsernamePasswordCredentials(this.getUsername(), this.getPassword()));
				httpclient = HttpClients.custom().setDefaultRequestConfig(requestConfig)
						.setUserAgent(this.getUserAgentString()).setDefaultCredentialsProvider(credentialsProvider)
						.setConnectionManager(cm).build();

			} else {
				httpclient = HttpClients.custom().setConnectionManager(cm).setUserAgent(this.getUserAgentString())
						.setDefaultRequestConfig(requestConfig).build();
			}
		} catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException ex) {
			Logger.getLogger(SpiderConfig.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public String getModifiedSiteName() {
		return modifiedSiteName;
	}

	/**
	 * This function is called to get the socket timeout for the request.
	 *
	 * @return socket time out for the request in mili-seconds.
	 */
	public int getSocketTimeout() {
		return socketTimeout;
	}

	/**
	 * This function is called to set the socket timeout for the request.
	 *
	 * @param socketTimeout
	 *            time in mili seconds.
	 */
	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	/**
	 * This function is called to get the connection timeout for the request.
	 *
	 * @return time out for the request in mili-seconds.
	 */
	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	/**
	 * This function is called to set the connection timeout for the request.
	 *
	 * @param connectionTimeout
	 *            time in mili seconds.
	 */
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	/**
	 * This function is called to get the connection request timeout for the
	 * request.
	 *
	 * @return time out for the request in mili-seconds.
	 */
	public int getConnectionRequestTimeout() {
		return ConnectionRequestTimeout;
	}

	/**
	 * This function is called to set the connection request timeout for the
	 * request.
	 *
	 * @param ConnectionRequestTimeout
	 *            time in mili seconds.
	 */
	public void setConnectionRequestTimeout(int ConnectionRequestTimeout) {
		this.ConnectionRequestTimeout = ConnectionRequestTimeout;
	}

	/**
	 * This function is called to check if the URL redirections.
	 *
	 * @return boolean value for redirection
	 */
	public boolean isFollowRedirects() {
		return followRedirects;
	}

	/**
	 * This function is called to set the URL redirections. If set to true it
	 * will follow the redirection chain and will land on last url.
	 *
	 * @param followRedirects
	 *            boolean value true/false to set redirection
	 */
	public void setFollowRedirects(boolean followRedirects) {
		this.followRedirects = followRedirects;
	}

	/**
	 * This function is called to check if site uses authentication
	 *
	 * @return boolean value
	 */
	public boolean isAuthenticate() {
		return authenticate;
	}

	/**
	 * This function is called to set authentication on the site
	 *
	 * @param authenticate
	 *            boolean value (true/false).
	 */
	public void setAuthenticate(boolean authenticate) {
		this.authenticate = authenticate;
	}

	/**
	 * This function is called to get username for the site if authentication is
	 * applicable.
	 *
	 * @return username as String if site has authentication enabled else return
	 *         null.
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * This function is called to set the username for authentication on the
	 * site.
	 *
	 * @param username
	 *            String parameter for username to authenticate.
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * This function is called to get password for the site if authentication is
	 * applicable.
	 *
	 * @return password as String if site has authentication enabled else return
	 *         null.
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * This function is called to set the password for authentication on the
	 * site.
	 *
	 * @param password
	 *            String parameter for password to authenticate.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * This function is called to get user-agent string for the site.
	 *
	 * @return user-agent String
	 */
	public String getUserAgentString() {
		return userAgentString;
	}

	/**
	 * This function is called to set user-agent string for the site.
	 *
	 * @param userAgentString
	 *            user-agent string as String datatype.
	 */
	public void setUserAgentString(String userAgentString) {
		this.userAgentString = userAgentString;
	}

	/**
	 * This function is called to get total number of crawlers.
	 *
	 * @return total crawlers count as integer.
	 */
	public int getTotalSpiders() {
		return totalSpiders;
	}

	/**
	 * This function is called to set total number of crawlers.
	 *
	 * @param totalSpiders
	 *            total crawlers count as integer.
	 */
	public void setTotalSpiders(int totalSpiders) {
		this.totalSpiders = totalSpiders;
	}

	private void validateConfig() throws Exception {
		if (this.getSiteName() == null) {
			throw new Exception("Site name is not specified in SpiderConfig.");
		}
	}

	/**
	 * This function is called to get site name.
	 *
	 * @return site name as String.
	 */
	public String getSiteName() {
		return siteName;
	}

	/**
	 * This function is called to set site name.
	 *
	 * @param siteName
	 *            site name as String.
	 */
	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}

	/**
	 * This function is called to start the crawling.
	 *
	 * @param <T>
	 * @param _c
	 *            Class name object which is extending WebSpider Class.
	 * @param config
	 * @throws java.lang.Exception
	 */
	public <T extends WebSpider> void start(final Object object, final SpiderConfig config) throws Exception {
		validateConfig();
		latch = new CountDownLatch(getTotalSpiders());
		executor = Executors.newFixedThreadPool(getTotalSpiders());
		for (int i = 0; i < getTotalSpiders(); i++) {
			WebSpider spider = (WebSpider) object;
			spider.setValues(config, latch);
			executor.execute(spider);
		}
		try {
			latch.await();
		} catch (InterruptedException ex) {
			Logger.getLogger(SpiderConfig.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			executor.shutdown();
			executor.awaitTermination(5, TimeUnit.SECONDS);
		}
	}

	private String handleRedirect(String url) {
		try {
			HttpGet httpget = new HttpGet(url);
			RequestConfig requestConfig = RequestConfig.custom().setRedirectsEnabled(true)
					.setCircularRedirectsAllowed(true).setRelativeRedirectsAllowed(true)
					.setConnectionRequestTimeout(getConnectionRequestTimeout()).setSocketTimeout(getSocketTimeout())
					.setConnectTimeout(getConnectionTimeout()).build();
			httpget.setConfig(requestConfig);
			HttpClientBuilder builder = HttpClientBuilder.create();
			builder.setUserAgent(getUserAgentString());
			if (isAuthenticate()) {
				CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
				credentialsProvider.setCredentials(AuthScope.ANY,
						new UsernamePasswordCredentials(getUsername(), getPassword()));
				builder.setDefaultCredentialsProvider(credentialsProvider);
			}
			CloseableHttpClient httpclient = builder.build();
			HttpClientContext context = HttpClientContext.create();
			httpclient.execute(httpget, context);
			HttpHost target = context.getTargetHost();
			List<URI> redirectLocations = context.getRedirectLocations();
			URI location = URIUtils.resolve(httpget.getURI(), target, redirectLocations);
			url = location.toString();
		} catch (IOException | URISyntaxException ex) {
			Logger.getLogger(SpiderConfig.class.getName()).log(Level.SEVERE, null, ex);
			System.err.println(url);
		}
		return url;
	}

	/**
	 * This function is called verify if crawling has been finished
	 *
	 * @return true if crawling is completed
	 */
	public boolean isCompleted() {
		return executor.isTerminated();
	}

	/**
	 * This function is called to stop crawling
	 *
	 */
	public void stop() {
		executor.shutdownNow();
	}

	public String getHostName() {
		return host;
	}

	// private RequestConfig getRequestConfigWithRedirectEnabled() {
	// return RequestConfig.custom().setRedirectsEnabled(true)
	// .setConnectionRequestTimeout(getConnectionRequestTimeout()).setSocketTimeout(getSocketTimeout())
	// .setConnectTimeout(getConnectionTimeout()).build();
	// }
	private RequestConfig getRequestConfigWithRedirectDisabled() {
		return RequestConfig.custom().setRedirectsEnabled(false)
				.setConnectionRequestTimeout(getConnectionRequestTimeout()).setSocketTimeout(getSocketTimeout())
				.setConnectTimeout(getConnectionTimeout()).build();
	}

	public HttpClient getHttpclient() {
		return httpclient;
	}

}
