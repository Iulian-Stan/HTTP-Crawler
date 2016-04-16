package urlParser;

import java.net.URI;
import java.net.URISyntaxException;

public class URL {
	public static final String DEFAULT_SCHEME = "HTTP";
	public static final String SECURE_SCHEME = "HTTPS";
	
	private URI uri;

	public URL(URI uri) {
		this.uri = uri;
	}

	public static URL ParseURL(String link) {
		URI uri = null;
		try {			
			uri = new URI(link);
			if (uri.getScheme() == null) {
				link = "http://" + link;
				uri = new URI(link);
			}
			if (!uri.getScheme().equalsIgnoreCase(DEFAULT_SCHEME) && !uri.getScheme().equalsIgnoreCase(SECURE_SCHEME)) {
				System.err.println("Scheme was different from http[s].");
				return null;
			}
			if (uri.getPath().isEmpty()) {
				link = link + "/";
				uri = new URI(link);
			}
		} catch (URISyntaxException e) {
			System.err.println("Syntax error in " + link);
			return null;
		}
		return new URL(uri);
	}
	
	public String getScheme() {
		return uri.getScheme();
	}

	public String getHost() {
		return uri.getHost();
	}

	public String getResurce() {
		if (uri.getQuery() == null)
			return uri.getPath();
		return uri.getPath() + "?" + uri.getQuery();
	}

	public int getPort() {
		if (uri.getPort() > 0)
			return uri.getPort();
		if (uri.getScheme().equalsIgnoreCase(DEFAULT_SCHEME))
			return 80;
		if (uri.getScheme().equalsIgnoreCase(SECURE_SCHEME))
			return 443;
		return -1;
	}
}
