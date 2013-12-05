package edu.umich.pts.mbus.bluebus.feed;


public class BusItem implements Cloneable {
	public int			id;
	public int			lat;		// degE6
	public int			lon;		// degE6
	public int			heading;	// degree, [0~360), 0: north, CW
	public String		routeName;
	public int			routeId;
	public int			color;
	
	public BusItem() {
		
	}
	
	public BusItem(BusItem bus) {
		id = bus.id;
		lat = bus.lat;
		lon = bus.lon;
		heading = bus.heading;
		routeName = bus.routeName;
		routeId = bus.routeId;
		color = bus.color;
	}
	
	public BusItem clone() {
		return new BusItem(this);
	}
}