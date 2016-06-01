package sachin.spider;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.EnglishReasonPhraseCatalog;

/**
 *
 * @author Sachin
 */
public class WebURL {
	private final String url;
	private int statusCode;
	private String statusMessage;
	private boolean proccessed;
	private int resposneTime;
	private String redirectTo = "";
	private Header[] headers;
	private String baseHref;
	private Set<WebURL> parents;
	private int depth = 0;
	private long size;
	private String mimeType;
	private HttpResponse response;
	private ContentType contentType;
	private String host;

	public HttpResponse getResponse() {
		return response;
	}

	public String getHost() {
		return host;
	}

	public WebURL(String url, HttpClient httpclient, String host) {
		this.url = url;
		this.host = host;
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
			resposneTime = ((int) (endingTime - startingTime)) / 1000;
			baseHref = context.getTargetHost().toString();
			headers = response.getAllHeaders();
			HttpEntity entity = response.getEntity();
			File file = new File(System.getProperty("user.dir") + File.separator + "data" + File.separator + host);
			file.mkdirs();
			ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(file));
			entity.writeTo(os);
			os.close();
			if (statusCode <= 200) {
				size = entity.getContentLength();
				contentType = ContentType.get(entity);
				mimeType = contentType.getMimeType();
			}
		} catch (Exception ex) {
			System.out.println(getUrl());
			Logger.getLogger(WebSpider.class.getName()).log(Level.SEVERE, null, ex);
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

	public Header[] getHeaders() {
		return headers;
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

	public ContentType getContentType() {
		return contentType;
	}

}
