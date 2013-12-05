package edu.umich.pts.mbus.bluebus;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import edu.umich.pts.mbus.bluebus.feed.PublicParser;
import edu.umich.pts.mbus.bluebus.feed.RouteItem;
import edu.umich.pts.mbus.bluebus.feed.StopItem;

public class StopActivity extends Activity implements OnClickListener {
	
	private String stopName;
	private RoutesAdapter routesAdapter;
	private Timer feedTimer;
	
	public final static String EX_STOP_NAME = new String("STOP_NAME");
	
	/** Timer for location feed update */
	private class LoadPublicFeedTask extends TimerTask {
		
		public void run() {
			// run the location feed update on worker thread
			final List<RouteItem> routes;
			
			runOnUiThread(new Runnable() {
				public void run() {
					findViewById(R.id.stopDetailLoading).setVisibility(View.VISIBLE);
				}
			});
			
			try {
				PublicParser.parse();
				routes = PublicParser.getRoutes(true);
				
				runOnUiThread(new Runnable() {
					public void run() {
						update(routes);
					}
				});
				
			} catch(Exception e) {
				runOnUiThread(new Runnable() {
					public void run() {
						Toast.makeText(StopActivity.this, getString(R.string.cmn_no_network), Toast.LENGTH_SHORT).show();
					}
				});
			}
			
			runOnUiThread(new Runnable() {
				public void run() {
					findViewById(R.id.stopDetailLoading).setVisibility(View.GONE);
				}
			});
		}
	};
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stop_detail);

		// set stop title
		stopName = this.getIntent().getStringExtra(EX_STOP_NAME);
		TextView txTitle = (TextView)findViewById(R.id.stopName);
		txTitle.setText(stopName);
		
		// set data adapter
		routesAdapter = new RoutesAdapter(this, R.layout.route_list_item, R.id.raTitle, new ArrayList<RouteItem>());
		ListView routesView = (ListView)findViewById(R.id.stopRoutes);
		routesView.setAdapter(routesAdapter);
		
		// set onclick listener
		ImageButton btn = (ImageButton)findViewById(R.id.bnShowOnMap);
		btn.setOnClickListener(this);
		
		// initialize feed
		PublicParser.initialize(getApplicationContext());
		update(PublicParser.getCachedRoutes());
    }
    
	@Override
	public void onResume() {
		super.onResume();
		
		feedTimer = new Timer();
		feedTimer.schedule(new LoadPublicFeedTask(), 0, getResources().getInteger(R.integer.param_timer_stopactivity_feed));
	}
	
	@Override
	public void onPause() {
		super.onPause();
		feedTimer.cancel();
		feedTimer = null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// remove onclick listener
		ImageButton btn = (ImageButton)findViewById(R.id.bnShowOnMap);
		btn.setOnClickListener(null);
		
		// release resources
		routesAdapter = null;
	}

	private void update(List<RouteItem> newRoutes) {

		if(routesAdapter == null)
			return;
		
		// get all stops with name stopName.
		if (newRoutes != null) {
			routesAdapter.clear();
			for (RouteItem route : newRoutes) {
				RouteItem newRoute = route.shallowCopy();
				for (StopItem stop : route.stops) {
					if (stop.name2.equals(stopName)) {
						newRoute.stops.add(stop.clone());
					}
				}
				// Add route if there is attached stop
				if (newRoute.stops.size() > 0)
					routesAdapter.add(newRoute.clone());
			}
		}
		
		TextView txNoItem = (TextView)findViewById(R.id.sd_noitem);
		TextView txAvail = (TextView)findViewById(R.id.tx_available_route);
		if(routesAdapter.getCount() > 0) {
			txNoItem.setVisibility(View.GONE);
			txAvail.setVisibility(View.VISIBLE);
		
		} else {
			txNoItem.setVisibility(View.VISIBLE);
			txAvail.setVisibility(View.GONE);
		}
	}

	public void onClick(View v) {
		
		switch(v.getId()) {
		case R.id.bnShowOnMap:
			Intent intent = new Intent(this, BlueBus.class);
			intent.putExtra(BlueBus.EX_INTENT, BlueBus.EX_INTENT_SHOWSTOP);
			intent.putExtra(BlueBus.EX_STOPNAME, stopName);
			startActivity(intent);
		}
		
	}
}
