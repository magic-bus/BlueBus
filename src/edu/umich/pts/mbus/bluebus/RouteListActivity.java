package edu.umich.pts.mbus.bluebus;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import edu.umich.pts.mbus.bluebus.feed.PublicParser;
import edu.umich.pts.mbus.bluebus.feed.RouteItem;

public class RouteListActivity extends ListActivity implements OnItemClickListener {	
	
	private RoutesAdapter routesAdapter;
	private boolean showProgressDialog = false;
	
	public final static int REQUEST_PICK_ROUTE = 1;
	public final static String RESULT_ROUTE_ID = "ROUTE_ID";

	private Timer feedTimer;
	/** Timer for location feed update */
	private class LoadPublicFeedTask extends TimerTask {
		
		private ProgressDialog progDlg;
		
		public void run() {

			// Display progress dialog
			if(showProgressDialog) {
				runOnUiThread(new Runnable() {
					public void run() {
						progDlg = ProgressDialog.show(RouteListActivity.this, "", getString(R.string.act_routelist_loading_message), true, false);
					}
				});
				
				// only show once
				showProgressDialog = false;
			}
			
			// run the location feed update on worker thread
			final List<RouteItem> routes;
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
						Toast.makeText(RouteListActivity.this, getString(R.string.cmn_no_network), Toast.LENGTH_SHORT).show();
					}
				});
			}
			
			runOnUiThread(new Runnable() {
				public void run() {
					if(progDlg != null) {
						progDlg.dismiss();
					}
				}
			});
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.route_list);
		PublicParser.initialize(getApplicationContext());
		
		routesAdapter = new RoutesAdapter(this, R.layout.route_list_item, R.id.raTitle, new ArrayList<RouteItem>());
		setListAdapter(routesAdapter);
		
		if(PublicParser.hasCache()) {
			update(PublicParser.getCachedRoutes());
			showProgressDialog = false;
		} else {
			showProgressDialog = true;
		}
		
		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		lv.setOnItemClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_routelist, menu);
		return true;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		feedTimer = new Timer();
		feedTimer.schedule(new LoadPublicFeedTask(), 0, getResources().getInteger(R.integer.param_timer_routelistactivity_feed));
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

		// remove listener
		ListView lv = getListView();
		lv.setOnItemClickListener(null);

		routesAdapter = null;
	}
	
	private void update(List<RouteItem> routes) {
		
		if(routesAdapter == null)
			return;
		
		if(routes != null) {
			routesAdapter.clear();
			for(RouteItem route : routes)
				routesAdapter.add(route.shallowCopy());
		}
		View txNoItem = findViewById(R.id.rl_noitem);
		if(routesAdapter.getCount() > 0)
			txNoItem.setVisibility(View.GONE);
		else
			txNoItem.setVisibility(View.VISIBLE);
	}

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// return selected route
		Intent data = new Intent();
		data.putExtra(RESULT_ROUTE_ID,(int)id);
		setResult(Activity.RESULT_OK, data);
		finish();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.mb_routelist_refresh:
				showProgressDialog = true;
				new Timer().schedule(new LoadPublicFeedTask(), 0);

				return true;
	
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}