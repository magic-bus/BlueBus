package edu.umich.pts.mbus.bluebus.feed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.util.Xml;

import edu.umich.pts.mbus.bluebus.R;

public final class PublicParser {

	private static final List<RouteItem> cachedRoutes = new ArrayList<RouteItem>();
	private static Context context;
	private static boolean initialized = false;
	private static boolean _hasCache = false;
	
	public static void initialize(Context c) {
		context = c;
		initialized = true;
	}
	
	public static boolean hasCache() {
		return _hasCache;
	}
	
	public static void parse() throws Exception {
		
		if(!initialized)
			throw new Exception("PublicParser not initialized");
		
		final RouteItem route = new RouteItem();
		final List<RouteItem> routes = new ArrayList<RouteItem>();

		final StopItem stop = new StopItem();
		final StopItem.TOA toa = new StopItem.TOA();
		stop.id = 0;

		RootElement root = new RootElement("livefeed");
		Element routeElement = root.getChild("route");
		Element stopElement = routeElement.getChild("stop");

		// End element
		routeElement.setEndElementListener(new EndElementListener() {
			public void end() {
				routes.add(route.clone());
				route.stops.clear();
				stop.id = 0;
			}
		});
		routeElement.getChild("name").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						route.name = body.trim();
					}
				});
		routeElement.getChild("id").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						route.id = Integer.parseInt(body.trim());
					}
				});
		routeElement.getChild("topofloop").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						route.topofloop = Integer.parseInt(body.trim());
					}
				});
		routeElement.getChild("busroutecolor").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						route.color = Color.parseColor('#' + body.trim());
					}
				});

		// --- Stops information --- //
		
		// End element
		stopElement.setEndElementListener(new EndElementListener() {
			public void end() {
				stop.sortTOA();
				route.stops.add(stop.clone());
				stop.clear();
			}
		});
		stopElement.getChild("name").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						stop.name = body.trim();
					}
				});
		stopElement.getChild("name2").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						stop.name2 = body.trim();
					}
				});
		stopElement.getChild("name3").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						stop.name3 = body.trim();
					}
				});
		stopElement.getChild("latitude").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						Float f = new Float(body.trim());
						stop.lat = (int) (f * 1e6);
					}
				});
		stopElement.getChild("longitude").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						Float f = new Float(body.trim());
						stop.lon = (int) (f * 1e6);
					}
				});
		// TOA ----------------------
		for(int i=1;i<=10;i++) {
			
			stopElement.getChild("id"+i).setEndTextElementListener(
					new EndTextElementListener() {
						public void end(String body) {
							toa.id = Integer.parseInt(body.trim());
							stop.toaList.add(toa.clone());
						}
					});	
			stopElement.getChild("toa"+i).setEndTextElementListener(
					new EndTextElementListener() {
						public void end(String body) {
							toa.time = new Float(body.trim());
						}
					});
		}


		// ------------------------------------ //
		try {
			Xml.parse(FeedParser.getInputStream(context.getString(R.string.param_feed_public)), Xml.Encoding.UTF_8,
					root.getContentHandler());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		updateCache(routes);
	}
	
	private static synchronized void updateCache(List<RouteItem> routes) {
		cachedRoutes.clear();
		cachedRoutes.addAll(routes);
		Collections.sort(cachedRoutes);
		_hasCache = true;
	}
	
	/** Returns the cached routes data. If no cache is available, returns null. */
	public static synchronized List<RouteItem> getCachedRoutes() {
		if(_hasCache)
			return new ArrayList<RouteItem>(cachedRoutes);
		else
			return null;
	}
	
	/** 
	 * Returns the cached routes data.
	 * If no cache is available, the function will attempt to update the cache.
	 * Returns null if all attempts fail.
	 * */
	public static synchronized List<RouteItem> getRoutes(boolean forceUpdate) {
		
		
		if( forceUpdate || !_hasCache ) {
			try {
				parse();		
			} catch(Exception e) {
				return null;
			}
		}
		
		return getCachedRoutes();
	}
	
	/** 
	 * Returns the RouteItem with specified routeId.
	 * 
	 * This method assumes the list is sorted
	 * */
	public static synchronized RouteItem getRouteItem(List<RouteItem> routes, int routeId) {

		if(routes == null)
			return null;
		
		int index = Collections.binarySearch(routes, new RouteItem(routeId));
		if(index >= 0)
			return routes.get(index);
		else
			return null;
	}
}
