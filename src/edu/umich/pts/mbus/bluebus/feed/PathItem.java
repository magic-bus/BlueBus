package edu.umich.pts.mbus.bluebus.feed;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Path;
import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

public class PathItem implements Comparable<PathItem>{
	
	public final int id;
	public int color;
	public int transparency;
	private final List<GeoPoint> nodes = new ArrayList<GeoPoint>();
	
	public int tmpLat;
	public int tmpLon;
	
	public void addNode() {
		nodes.add(new GeoPoint(tmpLat, tmpLon));
	}
	
	public PathItem(int _id) {
		id = _id;
	}
	
	public Path getPath(MapView mapView) {
		
		if(nodes.isEmpty())
			return null;
		
		Path path = new Path();
		Point pt = new Point();
		
		mapView.getProjection().toPixels(nodes.get(0), pt);
		path.moveTo(pt.x, pt.y);

		for(GeoPoint geoPt : nodes) {
			mapView.getProjection().toPixels(geoPt, pt);
			path.lineTo(pt.x, pt.y);
		}
		
		return path;
	}

	public int compareTo(PathItem another) {
		return this.id - another.id;
	}
}
