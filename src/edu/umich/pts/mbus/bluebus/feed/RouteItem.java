package edu.umich.pts.mbus.bluebus.feed;

import java.util.ArrayList;
import java.util.List;


public class RouteItem implements Comparable<RouteItem>, Cloneable {
	public String	name;
	public int		id;
	public int		color;
	public int		topofloop;
	
	public final List<StopItem> stops = new ArrayList<StopItem>();
	
	// define customized null for route because routeId can be negative
	public static int ROUTE_NULL = -100;
	
	public RouteItem() {
		id = ROUTE_NULL;
	}
	
	public RouteItem(int _id) {
		id = _id;
	}
	
	public RouteItem(RouteItem item) {
		name = item.name;
		id = item.id;
		color = item.color;
		topofloop = item.topofloop;
		for(StopItem stop : item.stops)
			stops.add(stop.clone());
	}
	
	public RouteItem shallowCopy() {
		RouteItem item = new RouteItem();
		item.name = name;
		item.id = id;
		item.color = color;
		item.topofloop = topofloop;
		return item;
	}
	
	@Override
	public RouteItem clone() {
		return new RouteItem(this);
	}
	
	public boolean equals(RouteItem item) {
		if(id == item.id)
			return true;
		else
			return false;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	/** Generates a readable string describing TOA of online buses */
	public String getToaDescription(int stopIndex) {
		// add bus TOA
		
		StopItem stop;
		String strNext = new String("Next ");
		String strBus = "";
		String strDir = "";
		String strCol = new String(": ");
		String strTime = "";
		
		try {
			stop = stops.get(stopIndex);
		}
		catch(IndexOutOfBoundsException e) {
			return null;
		}

		if (stop.toaList.size() == 0) {
			// No bus
			if (stopIndex == 0)
				return new String("Unknown time of arrival");
			else
				return null;
		} else if (stop.toaList.size() == 1) {
			// One bus
			strBus = new String("bus");
			strTime = new String(stop.toaList.get(0).toString());
		} else {
			// Two buses
			strBus = new String("buses");
			strTime = new String(stop.toaList.get(0).toString() + " / " + stop.toaList.get(1).toString());
		}
		
		if(stops.size() > 1) {
			// add direction if multiple stops
			if(stop.id < topofloop)
				strDir = " (out)";
			else
				strDir = " (in)";
		}
		
		return new String(strNext+strBus+strDir+strCol+strTime);
	}

	public int compareTo(RouteItem item) {
		return this.id - item.id;
	}
	
}
