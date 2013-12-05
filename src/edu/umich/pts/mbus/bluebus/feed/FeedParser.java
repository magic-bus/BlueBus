package edu.umich.pts.mbus.bluebus.feed;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public final class FeedParser {
	
    protected static InputStream getInputStream(String url) {
    	final URL feedUrl;
		try {
			feedUrl = new URL(url);
			return feedUrl.openConnection().getInputStream();
		}
		catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
