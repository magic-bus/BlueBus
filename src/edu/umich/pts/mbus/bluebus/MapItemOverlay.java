package edu.umich.pts.mbus.bluebus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import edu.umich.pts.mbus.bluebus.feed.BusItem;
import edu.umich.pts.mbus.bluebus.feed.PathItem;
import edu.umich.pts.mbus.bluebus.feed.RouteItem;
import edu.umich.pts.mbus.bluebus.feed.StopItem;

public class MapItemOverlay extends Overlay {

	public interface BusTapListener {
		void onTap(BusItem item);
	};
	
	public interface StopTapListener {
		void onTap(StopItem item);
	};
	
	private interface MapDrawable {
		boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when);
		boolean onTap(GeoPoint p, MapView mapView);
		int getLatitude();
	}
	
	class MapDrawableDescendComparator implements Comparator<MapDrawable> {

		public int compare(MapDrawable object1, MapDrawable object2) {
			return object2.getLatitude() - object1.getLatitude();
		}
	};
	
	class MapDrawableAscendComparator implements Comparator<MapDrawable> {

		public int compare(MapDrawable object1, MapDrawable object2) {
			return object1.getLatitude() - object2.getLatitude();
		}
	};

	private final List<BusDrawable> busList = new ArrayList<BusDrawable>();
	private final List<StopDrawable> stopList = new ArrayList<StopDrawable>();
	private PathItem pathItem;
	private Paint pathPaint = new Paint();
	
	// Rendering resources
	private final Bitmap busBitmap;
	private final Bitmap stopBitmap;
	private final Bitmap busFocusBitmap;
	private final Bitmap busFocusBitmap2;
	
	// Rendering parameters
	private float shadowAngle; // 0 = north, CW
	private float animationLatency;
	private boolean render3D;

	private BusTapListener busListener;
	private StopTapListener stopListener;
	
	public MapItemOverlay(Context context) {
		super();
		
		stopBitmap = ((BitmapDrawable)context.getResources().getDrawable(R.drawable.ic_map_stop)).getBitmap();
		busBitmap = ((BitmapDrawable)context.getResources().getDrawable(R.drawable.ic_map_bus)).getBitmap().extractAlpha();
		busFocusBitmap = ((BitmapDrawable)context.getResources().getDrawable(R.drawable.ic_map_bus_focus)).getBitmap();
		busFocusBitmap2 = ((BitmapDrawable)context.getResources().getDrawable(R.drawable.ic_map_bus_focus3d)).getBitmap();
		
		animationLatency = context.getResources().getInteger(R.integer.param_map_animationlatency);
		shadowAngle = context.getResources().getInteger(R.integer.param_shadow_angle);
		render3D = context.getResources().getBoolean(R.bool.settings_render3d_default);
	}
	
	/** Update the position of buses based on supplied info. */
	// TODO: may avoid sorting every time if inserting items properly
	public void updateBuses(final List<BusItem> newBusList) {

		if(newBusList == null)
			return;
		
		Collections.sort(busList);
		
		// set update flags to false
		for(BusDrawable bus : busList)
			bus.invalidate();
		
		// iterate new buses to update
		for(BusItem busItem : newBusList) {
			// check if item exists
			int index = Collections.binarySearch(busList, new BusDrawable(busItem.id));
			if(index >= 0)
				busList.get(index).update(busItem);
			else
				busList.add(new BusDrawable(busItem));
		}

		// find and remove deprecated buses
		int iBus=0;
		while(iBus < busList.size())
		{
			if(busList.get(iBus).updated())
				iBus++;
			else
				busList.remove(iBus);
		}
	}
	
	public void updateStops(final List<StopItem> newStopList) {
		stopList.clear();
		for(StopItem stop : newStopList)
			stopList.add(new StopDrawable(stop));		
	}
	
	public void setPathItem(PathItem item) {
		pathItem = item;
		
		pathPaint.setColor(pathItem.color);
		pathPaint.setAlpha(pathItem.transparency);
		pathPaint.setStyle(Style.STROKE);
		pathPaint.setStrokeWidth(10);
	}
	
	public void clear() {
		// clear all associated drawing items
		pathItem = null;
		stopList.clear();
	}
	
	/** draws all items on the screen */
	public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {

		// Draw route path
		if(!shadow && (pathItem != null)) {
			Path path = pathItem.getPath(mapView);
			if(path != null) {
				canvas.drawPath(path, pathPaint);
			}
		}
		
		// new crazy sorted drawing
		List<MapDrawable> drawables = new ArrayList<MapDrawable>();
		drawables.addAll(busList);
		drawables.addAll(stopList);
		Collections.sort(drawables, new MapDrawableDescendComparator());
		
		for(MapDrawable drawable : drawables)
			drawable.draw(canvas, mapView, shadow, when);

		return false;
	}
	
	public boolean onTap(GeoPoint p, MapView mapView) {
		
		// clear all bus focus
		for(BusDrawable bus : busList)
			bus.isFocused = false;
		
		// new crazy sorted drawing
		List<MapDrawable> drawables = new ArrayList<MapDrawable>();
		drawables.addAll(busList);
		drawables.addAll(stopList);
		Collections.sort(drawables, new MapDrawableAscendComparator());
		
		for(MapDrawable drawable : drawables) {
			if(drawable.onTap(p, mapView) == true) {
				// Recover class
				if( drawable.getClass() == BusDrawable.class ) {
					if(busListener != null) {
						((BusDrawable)drawable).isFocused = true;
						busListener.onTap(((BusDrawable)drawable).getBusItem());
						return true;
					}
				} else if( drawable.getClass() == StopDrawable.class ) {
					if(stopListener != null) {
						stopListener.onTap(((StopDrawable)drawable).getStopItem());
						return true;
					}
				}
			}
		}

		// Force clear map
		if(stopListener != null) {
			stopListener.onTap(null);
			return true;
		}
		return false;
	}
	
	public void setBusTapListener(BusTapListener listener) {
		busListener = listener;
	}
	
	public void setStopTapListener(StopTapListener listener) {
		stopListener = listener;
	}
	
	public int getRouteId() {
		if(pathItem != null)
			return pathItem.id;
		else
			return RouteItem.ROUTE_NULL;
	}
	
	public void setRender3D(boolean value) {
		render3D = value;
	}
	
	public void setAnimationLatency(int value) {
		animationLatency = value;
	}
	
	/** Call this function to release all resources attached */
	public void finish() {
		
		busList.clear();
		stopList.clear();
		pathItem = null;
		pathPaint = null;
		
		busListener = null;
		stopListener = null;
	}
	
	/** BusDrawable keeps all bus data and is responsible for drawing itself */
	public class BusDrawable implements MapDrawable, Comparable<BusDrawable> {
		
		private class Position {
			private int		lat;		// degE6
			private int		lon;		// degE6
			private int		heading;	// degree
			
			public void assign(Position pos) {
				lat = pos.lat;
				lon = pos.lon;
				heading = pos.heading;			
			}		
			public void assign(BusItem item) {
				lat = item.lat;
				lon = item.lon;
				heading = item.heading;			
			}		
		};
		
		private final BusItem base;
		
		private final Position	targetPos = new Position();
		private final Position	lastPos = new Position();
		private final Position	renderPos = new Position();
		
		private long		referenceTime;
		private long		lastRenderTime;
		
		public boolean		isFocused = false;
		private boolean		isUpdated = true;	// used by ItemizedOverlay to detect deprecated items.
		
		// to create dummy variable for comparison
		public BusDrawable(int _id) {
			base = new BusItem();
			base.id = _id;
		}

		public BusDrawable(BusItem item) {
			super();
			base = item.clone();

			targetPos.assign(item);
			lastPos.assign(targetPos);
			renderPos.assign(targetPos);
		}
		
		public int compareTo(BusDrawable bus) {
			return this.base.id - bus.getId();
		}
		
		public int getId() {
			return base.id;
		}
		
		public int getRouteId() {
			return base.routeId;
		}
		
		public boolean updated() {
			return isUpdated;
		}
		
		public void invalidate() {
			isUpdated = false;
		}
		
		public int getLatitude() {
			return renderPos.lat;
		}
		
		private Position interpolate(Position fromPos, Position toPos, float offset) {
			Position outPos = new Position();
			outPos.lat = fromPos.lat + (int)((toPos.lat - fromPos.lat)*offset);
			outPos.lon = fromPos.lon + (int)((toPos.lon - fromPos.lon)*offset);		

			// heading gets special treatment
			int dAng = toPos.heading - fromPos.heading;
			if(dAng > 180) {
				// force ccw turning
				outPos.heading = fromPos.heading - (int)((360-dAng)*offset);
				
			} else if(dAng < -180) {
				// force cw turning
				outPos.heading = fromPos.heading + (int)((360+dAng)*offset);
			} else {
				// normal turning
				outPos.heading = fromPos.heading + (int)(dAng*offset);
			}
			
			// adjust heading to [-180, 179)
			while(outPos.heading < -180)
				outPos.heading += 360;
			while(outPos.heading >= 180)
				outPos.heading -= 360;
			
			// angle saturation for 3D rendering
			if(render3D) {
				int angleCutoff = 25;
				if(Math.abs(outPos.heading) < angleCutoff)
					outPos.heading = (int) (angleCutoff*Math.signum(outPos.heading));
				else if(Math.abs(outPos.heading) > 180 - angleCutoff)
					outPos.heading = (int) ((180-angleCutoff)*Math.signum(outPos.heading));	
			}
			
			// adjust heading to [0, 360)
			if(outPos.heading < 0)
				outPos.heading += 360;

			return outPos;
		}
		
		public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
			lastRenderTime = when;
			
			// Get time offset and calculate render point
			float offset = ((float)(when - referenceTime))/animationLatency;
			
			if(offset > 1)
				renderPos.assign(targetPos);
			else if(offset > 0)
				renderPos.assign(interpolate(lastPos, targetPos, offset));
			
			Point transPt = new Point();
			mapView.getProjection().toPixels(new GeoPoint(renderPos.lat, renderPos.lon), transPt);
			
			if(shadow) {
				// render shadow
				// -- create matrix
				Matrix mtx = new Matrix();
				mtx.reset();
				
				if(render3D) {
					// 3D specific code here
					// TODO: hacked shadow angle
					float diffAngle = renderPos.heading - shadowAngle*2;
					float shadowSkewY = (float) (Math.cos(Math.toRadians(diffAngle))*-1);
					float shadowScaleX = (float) Math.sin(Math.toRadians(diffAngle));

					mtx.preTranslate(-busBitmap.getWidth()/2, -busBitmap.getHeight());
					mtx.postSkew(0, shadowSkewY);
					mtx.postScale(shadowScaleX, 1);
					mtx.postRotate(shadowAngle*2);
					
				} else {
					// 2D specific code here
					mtx.preTranslate(-busBitmap.getWidth()/2, -busBitmap.getHeight()/2);
					if(renderPos.heading > 180)
						mtx.postScale(1, -1);
					mtx.postRotate(renderPos.heading-90);
					float shadowOffset = ((float)busBitmap.getHeight())/10;
					mtx.postTranslate(shadowOffset, shadowOffset);
				}
				
				// translate to render position			
				mtx.postTranslate(transPt.x, transPt.y);

				Paint paint = new Paint();
				paint.setARGB(128, 0, 0, 0);
				BlurMaskFilter blurFilter = new BlurMaskFilter((float)2.5, BlurMaskFilter.Blur.NORMAL);
				paint.setMaskFilter(blurFilter);
				
				canvas.drawBitmap(busBitmap, mtx, paint);				
				
			} else {
				
				// render bus item 
				if(isFocused) {
					// draw focus circle
					Bitmap bmp;
					if(render3D)
						bmp = busFocusBitmap2;
					else
						bmp = busFocusBitmap;
					Matrix mtxFocus = new Matrix();
					mtxFocus.reset();
					mtxFocus.postTranslate(-bmp.getWidth()/2 + transPt.x, -bmp.getHeight()/2 + transPt.y);
					canvas.drawBitmap(bmp, mtxFocus, new Paint());
				}
				
				// draw bus icon
				// -- create matrix
				Matrix mtx = new Matrix();
				mtx.reset();
				
				if(render3D) {
					// 3D specific code here
					// -- calculate skew and scale
					float busScaleX = (float)Math.sin(Math.toRadians(renderPos.heading));
					float busSkewY = (float)(Math.cos(Math.toRadians(renderPos.heading))*-1);
		
					// -- apply transformation
					mtx.preTranslate(-busBitmap.getWidth()/2, -busBitmap.getHeight());
					mtx.postSkew(0, busSkewY);
					mtx.postScale(busScaleX, 1);
				} else {
					// 2D specific code here
					mtx.preTranslate(-busBitmap.getWidth()/2, -busBitmap.getHeight()/2);
					if(renderPos.heading > 180)
						mtx.postScale(1, -1);
					mtx.postRotate(renderPos.heading-90);
				}

				// -- translate to render position
				mtx.postTranslate(transPt.x, transPt.y);
				
				// -- fill in bus color
				Paint paint = new Paint();
				paint.setColor(base.color);

				// -- render
				canvas.drawBitmap(busBitmap, mtx, paint);
			}
			
			return false;
		}
		
		public boolean onTap(GeoPoint p, MapView mapView) {
			isFocused = false;
			Point ptBus = new Point();
			Point ptTap = new Point();
			mapView.getProjection().toPixels(new GeoPoint(renderPos.lat, renderPos.lon), ptBus);
			mapView.getProjection().toPixels(p, ptTap);
			// first order proximity approximation
			int dist = Math.abs(ptBus.x - ptTap.x) + Math.abs(ptBus.y - ptTap.y);
			// TODO: raw value
			if(dist < 50)
				return true;
			else
				return false;
		}
		
		public boolean equals(BusDrawable item) {
			return (this.base.id == item.getId());
		}

		public boolean equals(BusItem item) {
			return (this.base.id == item.id);
		}
		
		public void update(BusItem item) {
			lastPos.assign(renderPos);
			targetPos.assign(item);
			referenceTime = lastRenderTime;
			
			isUpdated = true;
		}
		
		public BusItem getBusItem() {
			return base.clone();
		}
	}

	public class StopDrawable implements MapDrawable {
		
		private StopItem base;
		
		public StopDrawable(StopItem item) {
			base = item.clone();
		}

		public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
			
			Point transPt = new Point();
			mapView.getProjection().toPixels(new GeoPoint(base.lat, base.lon), transPt);
			Matrix mtx = new Matrix();
			mtx.reset();
				
			if(shadow) {
				BlurMaskFilter blurFilter = new BlurMaskFilter((float)1, BlurMaskFilter.Blur.NORMAL);
				Paint paint = new Paint();
				paint.setARGB(128, 0, 0, 0);
				paint.setMaskFilter(blurFilter);
				
				float skewX = (float) Math.sin(Math.toRadians(shadowAngle*-1));
				float ScaleY = (float) Math.cos(Math.toRadians(shadowAngle));
				
				mtx.preTranslate(-stopBitmap.getWidth()/2, -stopBitmap.getHeight());
				mtx.postSkew(skewX, 0);
				mtx.postScale(1, ScaleY);
				// TODO: this is a hack, check shadow alignment
				mtx.postScale(1, (float)0.5);
				mtx.postTranslate(transPt.x, transPt.y);
				
				canvas.drawBitmap(stopBitmap.extractAlpha(), mtx, paint);
				
			} else {
				mtx.postTranslate(-stopBitmap.getWidth()/2, -stopBitmap.getHeight());
				mtx.postTranslate(transPt.x, transPt.y);
				canvas.drawBitmap(stopBitmap, mtx, new Paint());
			}
			return false;
		}

		public boolean onTap(GeoPoint p, MapView mapView) {
			Point ptStop = new Point();
			Point ptTap = new Point();
			mapView.getProjection().toPixels(new GeoPoint(base.lat, base.lon), ptStop);
			mapView.getProjection().toPixels(p, ptTap);
			// first order proximity approximation
			if(new Rect(ptStop.x-stopBitmap.getWidth()/2,
					ptStop.y-stopBitmap.getHeight(),
					ptStop.x+stopBitmap.getWidth()/2,
					ptStop.y).contains(ptTap.x, ptTap.y)) {
				// hit
				return true;
			}
			else
				return false;
		}
		
		public int getLatitude() {
			return base.lat;
		}
		
		public StopItem getStopItem() {
			return base.clone();
		}
	}


}
