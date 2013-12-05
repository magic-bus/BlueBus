package edu.umich.pts.mbus.bluebus;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

import edu.umich.pts.mbus.bluebus.feed.BusItem;
import edu.umich.pts.mbus.bluebus.feed.LocationParser;
import edu.umich.pts.mbus.bluebus.feed.PathItem;
import edu.umich.pts.mbus.bluebus.feed.PathParser;
import edu.umich.pts.mbus.bluebus.feed.PublicParser;
import edu.umich.pts.mbus.bluebus.feed.RouteItem;
import edu.umich.pts.mbus.bluebus.feed.StopItem;
import edu.umich.pts.mbus.bluebus.feed.StopsParser;

public class BlueBus extends MapActivity implements OnClickListener {
	
	// Map items
	private MainView mapView;
	private MapItemOverlay mapItemOverlay;
	private MyLocationOverlay myLocationOverlay;
	private View balloonView;
	private Toast toast;
	
	// Selected item reference
	private BusItem selectedBus;
	private StopItem selectedStop;
	private int selectedRouteId = RouteItem.ROUTE_NULL;
	private int numLoading = 0;
	
	// Strings and enums used for intent extras
	public final static String EX_INTENT = "Intent";
	public final static String EX_STOPNAME = "Stop name";
	public final static int EX_INTENT_SHOWSTOP = 0;
	
	public final static String SHOWSTOP_ALL = "SHOW_ALL";
	private final static String MAP_LAT = "MAP_LAT";
	private final static String MAP_LON = "MAP_LON";
	private final static String MAP_ZOOM = "MAP_ZOOM";
	private final static String DISPLAYED_ROUTE = "DISPLAYED_ROUTE";
	
	private int renderRate;
	private boolean useLocation;
	
	private Timer animationTimer;
	private Timer feedTimer;
	
	private AnimationTask animationTask;
	
	/** Timer for location feed update */
	private class LocationUpdateTask extends TimerTask {
		public void run() {
			// run the location feed update on worker thread
			try {
				final List<BusItem> items;
				items = LocationParser.parse(BlueBus.this);
				mapItemOverlay.updateBuses(items);
			}
			catch (Exception e) {
				
			}
		}
	};

	/** Timer for animation frame advance */
	private class AnimationTask extends TimerTask {
		public void run() {
			runOnUiThread(new Runnable() {
				public void run(){
					if(mapView != null)
						mapView.invalidate();
				}
			});
		}
	};
	
	/** Regular cache of feed data */
	private class LoadFeedTask extends TimerTask {
		public void run() {
			setLoading(true);
			try {
				StopsParser.parse();
				PublicParser.parse();
				List<RouteItem> cachedRoutes = PublicParser.getCachedRoutes();
				for(RouteItem route : cachedRoutes)
					PathParser.getPath(BlueBus.this, route.id);
			} catch (Exception e) {
				showToast(getString(R.string.cmn_no_network));
			}
			setLoading(false);
		}
	};
	
	/** load the selected route path and stops onto the map */
	private class ShowRouteTask extends AsyncTask<Integer, Void, Boolean> {
		
		private int routeId;
		private PathItem pathItem;
		private List<RouteItem> routes;
		
		protected void onPreExecute() {
			clearMap();
			setLoading(true);
		}
		
		protected Boolean doInBackground(Integer... routeIds) {
			routeId = routeIds[0];
			try {
				// use getRoutes() to force caching if not already available
				routes = PublicParser.getRoutes(false);
				pathItem = PathParser.getPath(BlueBus.this, routeId);
				
			} catch (Exception e) {
				return false;
			}
			return true;
		}

		protected void onPostExecute(Boolean success) {
			if(success) {
				try {
					// Show RoutePath
					if(pathItem != null) {
						mapItemOverlay.setPathItem(pathItem);
					} else {
						showToast(getString(R.string.act_map_loc_not_available));
					}
		
					// Show route stops
					RouteItem routeItem = PublicParser.getRouteItem(routes, routeId);
					if(routeItem != null) {
						mapItemOverlay.updateStops(routeItem.stops);
						showClearMapButton();
					} else {
						showToast(getString(R.string.act_map_no_path));
					}
				} catch(Exception e) {
					// catch null point exception
				}
			}
			setLoading(false);
	    }
	}
	
	/** Loads stops and show on map.
	 * 
	 * Input: the name of the stop. If null, show all stops.
	 *
	 */
	private class ShowStopTask extends AsyncTask<String, Void, Boolean> {
		
		private final List<StopItem> targetStops = new ArrayList<StopItem>();
		private String stopName;
		
		protected void onPreExecute() {
			clearMap();
			setLoading(true);
		}
		
		protected Boolean doInBackground(String... stopNames) {
			try {
				stopName = stopNames[0];
				List<StopItem> stopList = StopsParser.getStops(false);

				if(stopName.equals(SHOWSTOP_ALL)) {
					for(StopItem stop : stopList)
						targetStops.add(stop.clone());
				} else {
					for(StopItem stop : stopList)
						if(stop.toString().equals(stopNames[0]))
							targetStops.add(stop.clone());
				}
				
			} catch (Exception e) {
				return false;
			}
			return true;
		}

		protected void onPostExecute(Boolean success) {
			if(success) {
				// Show stops
				mapItemOverlay.updateStops(targetStops);
				
				if(!stopName.equals(SHOWSTOP_ALL) && targetStops.size() > 0) {
					// center map at stop
					StopItem stop = targetStops.get(0);
					GeoPoint pt = new GeoPoint(stop.lat, stop.lon);
					mapView.getController().animateTo(pt);
					showBalloon(pt, stop.toString());
				}
				
				showClearMapButton();
			}
			setLoading(false);
	    }
	}
	
	/** Listener to stop tap event */
	private MapItemOverlay.StopTapListener stopTapListener = new MapItemOverlay.StopTapListener() {

		public void onTap(StopItem stop) {
			hideBalloon();
			hideBusInfo();
			if(stop != null) {
				showBalloon(new GeoPoint(stop.lat, stop.lon), stop.toString());
				selectedStop = stop;
			}
			else
				selectedStop = null;
		}
	};
	
	/** Listener to bus tap event */
	private MapItemOverlay.BusTapListener busTapListener = new MapItemOverlay.BusTapListener() {

		public void onTap(BusItem item) {
			// show bus info view
			hideBalloon();
			showBusInfo(item);
		}
	};

	/** Universal button click handler */
	public void onClick(View v) {
		
		switch(v.getId()) {
		case R.id.bnMyLocation:
			animateToMyLocation.run();
			break;
		case R.id.biShowRoute:
			if(selectedBus != null) {
				if(((ToggleButton)v).isChecked()) {
					// show bus route
					new ShowRouteTask().execute(selectedBus.routeId);
				} else {
					// hide bus route
					clearMap();
				}
			}
			break;
		case R.id.balloonButton:
			if(selectedStop != null) {
				Intent intent = new Intent(BlueBus.this, StopActivity.class);
				intent.putExtra(StopActivity.EX_STOP_NAME, selectedStop.toString());
				startActivity(intent);
			}
			break;
		case R.id.bnShowStop:
			new ShowStopTask().execute(SHOWSTOP_ALL);
			break;
		case R.id.bnClearMap:
			clearMap();
			// cancel toggle button
			((ToggleButton)findViewById(R.id.biShowRoute)).setChecked(false);
			break;
		case R.id.bnZoomIn:
			mapView.getController().zoomIn();
			break;
		case R.id.bnZoomOut:
			mapView.getController().zoomOut();
		}
	}
	
	/**
	 * Animate the map to center on the current user location
	 * Can be called from any thread. This function will internally force execution
	 * on the UI thread.
	 * */
	private Runnable animateToMyLocation = new Runnable() {

		public void run() {
			runOnUiThread(new Runnable() {
				public void run() {
					if( myLocationOverlay.isMyLocationEnabled() ) {
						GeoPoint myLoc = myLocationOverlay.getMyLocation();
						if(myLoc != null) {
							mapView.getController().animateTo(myLoc);
							return;
						}
					}
					
					// no location
					showToast(getString(R.string.act_map_loc_not_available));
				}
			});
		}
	};
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		//initialize public feed parser
		PublicParser.initialize(getApplicationContext());
		StopsParser.initialize(getApplicationContext());
		
		// setup map view
		mapView = (MainView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(false);
		mapView.setZoomControlButtons(findViewById(R.id.bnZoomIn), findViewById(R.id.bnZoomOut));
		
		// set button click listener
		findViewById(R.id.biShowRoute).setOnClickListener(this);
		findViewById(R.id.bnMyLocation).setOnClickListener(this);
		findViewById(R.id.bnShowStop).setOnClickListener(this);
		findViewById(R.id.bnClearMap).setOnClickListener(this);

		// create map item overlay
		mapItemOverlay = new MapItemOverlay(this);
		mapItemOverlay.setBusTapListener(busTapListener);
		mapItemOverlay.setStopTapListener(stopTapListener);
		mapView.getOverlays().add(mapItemOverlay);
		
		// create my location overlay
		myLocationOverlay = new MyLocationOverlay(this, mapView);
		mapView.getOverlays().add(myLocationOverlay);
		
		// -- get default value
		int mapLatE6 = getResources().getInteger(R.integer.param_map_latE6_default);
		int mapLonE6 = getResources().getInteger(R.integer.param_map_lonE6_default);
		int mapZoom  = getResources().getInteger(R.integer.param_map_zoom_default);
		
		// -- load data from saved state
		if(savedInstanceState != null) {
			mapLatE6 = savedInstanceState.getInt(MAP_LAT, mapLatE6);
			mapLonE6 = savedInstanceState.getInt(MAP_LON, mapLonE6);
			mapZoom  = savedInstanceState.getInt(MAP_ZOOM, mapZoom);
			
			selectedRouteId = savedInstanceState.getInt(DISPLAYED_ROUTE, RouteItem.ROUTE_NULL);
		}
		
		// -- set default map position
		MapController ctrl = mapView.getController();
		ctrl.setCenter(new GeoPoint(mapLatE6, mapLonE6));
		ctrl.setZoom(mapZoom);
		
		switch( getIntent().getIntExtra(EX_INTENT, -1) ) {
		case -1:
			// Regular startup

			// -- Animate to my location on first fix
			// -- Add this condition to suppress fix on orientation change
			if(savedInstanceState == null)
				myLocationOverlay.runOnFirstFix(animateToMyLocation);	
			break;
			
		case EX_INTENT_SHOWSTOP:
			// create this activity to show a certain stop
			ctrl.setZoom(getResources().getInteger(R.integer.param_map_zoom_showstop));
			
			String stopName = getIntent().getStringExtra(EX_STOPNAME);
			// Assign AsyncTask
			new ShowStopTask().execute(stopName);
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		boolean render3D = prefs.getBoolean(getString(R.string.settings_render3d_key), getResources().getBoolean(R.bool.settings_render3d_default));
		mapItemOverlay.setRender3D(render3D);
		
		String strRenderRate = prefs.getString(getString(R.string.settings_renderrate_key),
				getString(R.string.settings_renderrate_default));
		renderRate = Integer.parseInt(strRenderRate);
		
		useLocation = prefs.getBoolean(getString(R.string.settings_uselocation_key), getResources().getBoolean(R.bool.settings_uselocation_default));
		
		feedTimer = new Timer();
		animationTimer = new Timer();
		
		feedTimer.schedule(new LocationUpdateTask(), 0, getResources().getInteger(R.integer.param_timer_mapactivity_locationupdate));
		feedTimer.schedule(new LoadFeedTask(), 0, getResources().getInteger(R.integer.param_timer_mapactivity_loadfeed));
		
		if(selectedRouteId != RouteItem.ROUTE_NULL)
			new ShowRouteTask().execute(selectedRouteId);
	}
	
	@Override
	public void onResume() {
		super.onResume();

		animationTask = new AnimationTask();
		animationTimer.schedule(animationTask, 0, renderRate);

		if(useLocation)
			myLocationOverlay.enableMyLocation();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		animationTask.cancel();
		animationTask = null;
		
		myLocationOverlay.disableMyLocation();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		
		animationTimer.cancel();
		feedTimer.cancel();
		animationTimer = null;
		feedTimer = null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		// set button click listener
		findViewById(R.id.biShowRoute).setOnClickListener(null);
		findViewById(R.id.bnMyLocation).setOnClickListener(null);
		findViewById(R.id.bnShowStop).setOnClickListener(null);
		findViewById(R.id.bnClearMap).setOnClickListener(null);
		
		Button bnBalloon = (Button)findViewById(R.id.balloonButton);
		if(bnBalloon != null)
			bnBalloon.setOnClickListener(null);
		
		mapView.finish();
		mapView = null;
		
		mapItemOverlay.finish();
		mapItemOverlay = null;
		
		myLocationOverlay = null;
		balloonView = null;
		toast = null;
		
		selectedBus = null;
		selectedStop = null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_map, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.mbClearMap:
				clearMap();
				return true;
			case R.id.mbRouteList:
				// Start route selection activity
				Intent routeIntent = new Intent(this, RouteListActivity.class);
				startActivityForResult(routeIntent, RouteListActivity.REQUEST_PICK_ROUTE);
				return true;
			case R.id.mbStopList:
				startActivity(new Intent(this, StopListActivity.class));
				return true;
			case R.id.mbStatus:
				return true;
			case R.id.mbSettings:
				startActivity(new Intent(this, SettingsActivity.class));
				return true;
	
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		GeoPoint mapCenter = mapView.getMapCenter();
		outState.putInt(MAP_LAT, mapCenter.getLatitudeE6());
		outState.putInt(MAP_LON, mapCenter.getLongitudeE6());
		outState.putInt(MAP_ZOOM, mapView.getZoomLevel());
		
		int routeId = mapItemOverlay.getRouteId();
		if(routeId != RouteItem.ROUTE_NULL)
			outState.putInt(DISPLAYED_ROUTE, routeId);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {	
		if (resultCode == Activity.RESULT_OK) {
			
			switch(requestCode) {
			
			case RouteListActivity.REQUEST_PICK_ROUTE:
				// Show route
				int routeId = data.getIntExtra(RouteListActivity.RESULT_ROUTE_ID, RouteItem.ROUTE_NULL);
				if(routeId != RouteItem.ROUTE_NULL)
					new ShowRouteTask().execute(routeId);			
			}
		}
	}

	/** utility function to show messages */
	protected void showAlertDialog(String title, String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setPositiveButton("OK", null);

		AlertDialog alert = builder.create();
		alert.show();
	}

	/** utility function to show messages */
	protected void showAlertDialog(Exception e) {
		showAlertDialog("Error", e.toString());
	}
	
	/** Show an information balloon on the map */
	protected void showBalloon(GeoPoint pos, String text) {
		if(balloonView != null)
			mapView.removeView(balloonView);
		
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		balloonView = inflater.inflate(R.layout.balloon, mapView, false);
		
		mapView.addView(balloonView, new MapView.LayoutParams(
				MapView.LayoutParams.WRAP_CONTENT,
				MapView.LayoutParams.WRAP_CONTENT,
				pos,
				MapView.LayoutParams.BOTTOM_CENTER));
		
		// Set text and click listener
		Button bnBalloon = (Button)findViewById(R.id.balloonButton);
		bnBalloon.setText(text);
		bnBalloon.setOnClickListener(this);
		
		// Apply animation
		Animation showBalloonAnimation = AnimationUtils.loadAnimation(this, R.anim.balloon_show);
		balloonView.startAnimation(showBalloonAnimation);
	}
	
	protected void hideBalloon() {
		if(balloonView != null) {
			mapView.removeView(balloonView);
			balloonView = null;
		}
	}
	
	protected void showToast(final String text) {
		runOnUiThread(new Runnable() {
			public void run() {
				if(toast != null)
					toast.cancel();
				toast = Toast.makeText(BlueBus.this, text, Toast.LENGTH_SHORT);
				toast.show();				
			}
		});
	}

	protected void showBusInfo(BusItem item) {
		selectedBus = item;
		((TextView)findViewById(R.id.busTitle)).setText(selectedBus.routeName);
		findViewById(R.id.busColor).setBackgroundColor(selectedBus.color);
		
		if(selectedBus.routeId == mapItemOverlay.getRouteId())
			((ToggleButton)findViewById(R.id.biShowRoute)).setChecked(true);
		else
			((ToggleButton)findViewById(R.id.biShowRoute)).setChecked(false);
		
		View v = findViewById(R.id.busInfoView);
		if(v.getVisibility() == View.GONE) {
			v.setVisibility(View.VISIBLE);
			// Apply animation
			Animation showBalloonAnimation = AnimationUtils.loadAnimation(this, R.anim.infobar_show);
			v.startAnimation(showBalloonAnimation);
		}
	}
	
	protected void hideBusInfo() {
		View v = findViewById(R.id.busInfoView);
		if(v.getVisibility() != View.GONE) {
			v.setVisibility(View.GONE);
			// Apply animation
			Animation showBalloonAnimation = AnimationUtils.loadAnimation(this, R.anim.infobar_hide);
			v.startAnimation(showBalloonAnimation);
		}
	}
	
	protected void setLoading(boolean isLoading) {
		
		if(isLoading)
			numLoading++;
		else
			numLoading--;
		
		runOnUiThread( new Runnable() {
			public void run() {
				ProgressBar pr = (ProgressBar)findViewById(R.id.mapLoading);
				if(numLoading > 0)
					pr.setVisibility(View.VISIBLE);
				else
					pr.setVisibility(View.INVISIBLE);		
			}
		});
	}
	
	protected void clearMap() {
		mapItemOverlay.clear();
		hideBalloon();
		
		View bnClear = findViewById(R.id.bnClearMap);
		if(bnClear.getVisibility() == View.VISIBLE) {
			bnClear.setVisibility(View.GONE);
			Animation fadeoutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
			bnClear.startAnimation(fadeoutAnimation);
		}
	}
	
	protected void showClearMapButton() {
		
		View bnClear = findViewById(R.id.bnClearMap);
		if(bnClear.getVisibility() != View.VISIBLE) {
			bnClear.setVisibility(View.VISIBLE);
			Animation fadeinAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
			bnClear.startAnimation(fadeinAnimation);
		}
	}
}