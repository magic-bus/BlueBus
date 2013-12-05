package edu.umich.pts.mbus.bluebus.feed;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.util.Xml;

import edu.umich.pts.mbus.bluebus.R;

public final class StopsParser {
	private static final List<StopItem> cachedStops = new ArrayList<StopItem>();
	private static Context context;
	private static boolean initialized = false;
	private static boolean _hasCache = false;
	
	public static void initialize(Context c) {
		context = c;
		initialized = true;
	}
	
	public static void parse() throws Exception {
		
		if(!initialized)
			throw new Exception("StopParser not initialized");
		
		final StopItem stop = new StopItem();
		final List<StopItem> stops = new ArrayList<StopItem>();

		RootElement root = new RootElement("stopfeed");
		Element item = root.getChild("stop");

		// End element
		item.setEndElementListener(new EndElementListener() {
			public void end() {
				stops.add(new StopItem(stop));
			}
		});
		item.getChild("name").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						stop.name = body.trim();
					}
				});
		item.getChild("name2").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						stop.name2 = body.trim();
					}
				});
		item.getChild("name3").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						stop.name3 = body.trim();
					}
				});
		item.getChild("lat").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						Float f = new Float(body.trim());
						stop.lat = (int) (f * 1e6);
					}
				});
		item.getChild("lon").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						Float f = new Float(body.trim());
						stop.lon = (int) (f * 1e6);
					}
				});

		try {
			Xml.parse(FeedParser.getInputStream(context.getString(R.string.param_feed_stops)), Xml.Encoding.UTF_8,
					root.getContentHandler());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		updateCache(stops);
	}
	
	private static synchronized void updateCache(List<StopItem> stops) {
		cachedStops.clear();
		cachedStops.addAll(stops);
		_hasCache = true;
	}
	
	public static synchronized List<StopItem> getCachedStops() {
		if(_hasCache)
			return new ArrayList<StopItem>(cachedStops);
		else
			return null;
	}
	
	/** 
	 * Returns the cached stops data.
	 * If no cache is available, the function will attempt to update the cache.
	 * Returns null if all attempts fail.
	 * */
	public static synchronized List<StopItem> getStops(boolean forceUpdate) {
		
		if( forceUpdate || !_hasCache ) {
			try {
				parse();		
			} catch(Exception e) {
				return null;
			}
		}
		
		return getCachedStops();
	}
}
