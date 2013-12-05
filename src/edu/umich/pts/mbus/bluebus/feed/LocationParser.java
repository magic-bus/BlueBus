package edu.umich.pts.mbus.bluebus.feed;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.util.Xml;

import edu.umich.pts.mbus.bluebus.R;


public final class LocationParser {

	public static List<BusItem> parse(Context context) {
		final BusItem curBus = new BusItem();
		final List<BusItem> buses = new ArrayList<BusItem>();

		RootElement root = new RootElement("livefeed");
		Element item = root.getChild("item");

		// End element
		item.setEndElementListener(new EndElementListener() {
			public void end() {
				buses.add(new BusItem(curBus));
			}
		});
		item.getChild("id").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						curBus.id = Integer.parseInt(body.trim());
					}
				});
		item.getChild("latitude").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						Float f = new Float(body.trim());
						curBus.lat = (int) (f * 1e6);
					}
				});
		item.getChild("longitude").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						Float f = new Float(body.trim());
						curBus.lon = (int) (f * 1e6);
					}
				});
		item.getChild("heading").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						curBus.heading = Integer.parseInt(body.trim());
					}
				});
		item.getChild("route").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						curBus.routeName = body.trim();
					}
				});
		item.getChild("routeid").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						curBus.routeId = Integer.parseInt(body.trim());
					}
				});
		item.getChild("busroutecolor").setEndTextElementListener(
				new EndTextElementListener() {
					public void end(String body) {
						curBus.color = Color.parseColor('#' + body.trim());
					}
				});

		try {
			Xml.parse(FeedParser.getInputStream(context.getString(R.string.param_feed_location)), Xml.Encoding.UTF_8,
					root.getContentHandler());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return buses;
	}

}
