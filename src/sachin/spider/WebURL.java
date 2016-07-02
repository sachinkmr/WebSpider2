package sachin.spider;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author Sachin
 */
public class WebURL implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private final String url;
	private int statusCode;
	private String statusMessage;
	private transient boolean proccessed;
	private int resposneTime;
	private String redirectTo;
	private String baseHref;
	private Set<WebURL> parents;
	private int depth = 0;
	private long size;
	private String mimeType;
	transient HttpResponse response;
	private String host;
	private String dom;
	private boolean internal;

	public boolean isInternal() {
		return internal;
	}

	public String getDom() {
		return dom;
	}

	public String getHost() {
		return host;
	}

	public WebURL(String url, HttpClient httpclient, String host) {
		this.url = url;
		this.host = host;
		try {
			this.host = new URL(url).getHost().replaceAll("www.", "");
			internal = this.host.contains(host);
		} catch (MalformedURLException ex) {
			Logger.getLogger(SpiderConfig.class.getName()).log(Level.SEVERE, null, ex);
			return;
		}
		this.proccessed = false;
		this.parents = new HashSet<>();
		HttpGet httpget = new HttpGet(url);
		HttpClientContext context = HttpClientContext.create();
		long startingTime = System.currentTimeMillis();
		try {
			response = httpclient.execute(httpget, context);
			long endingTime = System.currentTimeMillis();
			StatusLine statusLine = response.getStatusLine();
			statusCode = statusLine.getStatusCode();
			statusMessage = EnglishReasonPhraseCatalog.INSTANCE.getReason(statusCode, Locale.ENGLISH);
			resposneTime = ((int) (endingTime - startingTime));
			baseHref = context.getTargetHost().toString();
			HttpEntity entity = response.getEntity();
			dom = EntityUtils.toString(entity, "UTF-8");
			if (statusCode == 200) {
				size = entity.getContentLength();
				mimeType = ContentType.get(entity).getMimeType();
			}
			EntityUtils.consumeQuietly(entity);

		} catch (Exception ex) {
			Logger.getLogger(WebURL.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			httpget.releaseConnection();
		}
	}

	public int getDepth() {
		return depth;
	}

	public long getSize() {
		return size;
	}

	public String getUrl() {
		return url;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public boolean isProccessed() {
		return proccessed;
	}

	public void setProccessed(boolean proccessed) {
		this.proccessed = proccessed;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 53 * hash + Objects.hashCode(this.url);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final WebURL other = (WebURL) obj;
		if (!Objects.equals(this.url, other.url)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return url;
	}

	public int getResposneTime() {
		return resposneTime;
	}

	public String getRedirectTo() {
		return redirectTo;
	}

	public String getBaseHref() {
		return baseHref;
	}

	public Set<WebURL> getParents() {
		return parents;
	}

	public void addParent(WebURL parent) {
		if (this.depth == 0) {
			this.depth = parent.getDepth() + 1;
		} else if (this.depth - 1 > parent.depth) {
			this.depth = parent.getDepth() + 1;
		}
		parents.add(parent);
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setRedirectTo(String redirectTo) {
		this.redirectTo = redirectTo;
	}

}
