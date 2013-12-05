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


public final class PathParser {
	
	private final static List<PathItem> cachedPaths = new ArrayList<PathItem>();

	private static PathItem parse(Context context, int routeId) {
		final PathItem pathItem = new PathItem(routeId);

		RootElement root = new RootElement("histdata");
		Element info = root.getChild("route_info");
		Element item = root.getChild("item");

		// get route info
		info.getChild("color").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						pathItem.color = Color.parseColor('#' + body.trim());
					}
				});
		info.getChild("transparency").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						pathItem.transparency = Integer.parseInt(body.trim(), 16);
					}
				});
		
		// End element
		item.setEndElementListener(new EndElementListener() {
			public void end() {
				pathItem.addNode();
			}
		});
		item.getChild("latitude").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						Float f = new Float(body.trim());
						pathItem.tmpLat = (int) (f * 1e6);
					}
				});
		item.getChild("longitude").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						Float f = new Float(body.trim());
						pathItem.tmpLon = (int) (f * 1e6);
					}
				});
		
		try {
			Xml.parse(FeedParser.getInputStream("http://mbus.pts.umich.edu/shared/map_trace_route_"+routeId+".xml"),
					Xml.Encoding.UTF_8, root.getContentHandler());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return pathItem;
	}
	
	public static PathItem getPath(Context context, int routeId) {
		// TODO
		int index = Collections.binarySearch(cachedPaths, new PathItem(routeId));
		if(index >= 0) {
			return cachedPaths.get(index);
		} else {
			try {
				PathItem path = parse(context, routeId);
				cachedPaths.add(path);
				Collections.sort(cachedPaths);
				return path;
			} catch(Exception e) {
				return null;
			}
		}
	}
}
