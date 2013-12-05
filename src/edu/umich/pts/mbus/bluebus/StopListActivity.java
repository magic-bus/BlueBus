package edu.umich.pts.mbus.bluebus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.Toast;

import edu.umich.pts.mbus.bluebus.feed.PublicParser;
import edu.umich.pts.mbus.bluebus.feed.StopItem;
import edu.umich.pts.mbus.bluebus.feed.StopsParser;

public class StopListActivity extends ListActivity implements OnItemClickListener  {

	private final List<StopItem> stops = new ArrayList<StopItem>();
	private StopsAdapter stopsAdapter;
	private LoadStopsTask loadStopsTask;
	
	public final static String RESULT_STOP_NAME = "STOP_NAME";
			
	private class StopsAdapter extends ArrayAdapter<StopItem> {
		
		private Filter textFilter;
		
		public StopsAdapter() {
			super(StopListActivity.this, R.layout.simple_list_item_1, new ArrayList<StopItem>());
		}
		
		public void showAll() {
			clear();
			for(StopItem stop : stops)
				add(stop);

			notifyDataSetChanged();
		}
		
		@Override
		public Filter getFilter() {
			if(textFilter == null) {
				textFilter = new Filter() {

					@Override
					protected FilterResults performFiltering(CharSequence constraint) {
						List<StopItem> filteredStops = new ArrayList<StopItem>();
						constraint = constraint.toString().toLowerCase();
						
						for(StopItem stop : stops) {
							if(stop.toString().toLowerCase().contains(constraint))
								filteredStops.add(stop);
						}
						
						FilterResults filterResults = new FilterResults();
						filterResults.count = filteredStops.size();
						filterResults.values = filteredStops;
						return filterResults;
					}

					@SuppressWarnings("unchecked")
					@Override
					protected void publishResults(CharSequence constraint, FilterResults results) {
						List<StopItem> filteredList = (List<StopItem>)results.values;
						
						clear();
						for(StopItem stop : filteredList)
							add(stop);
						
						notifyDataSetChanged();
					}
				};
			}
			
			return textFilter;
		}
	};
	
	private class LoadStopsTask extends AsyncTask<Boolean, Void, Boolean> {
		
		private Map<String, StopItem> stopMap = new HashMap<String, StopItem>();
		private ProgressDialog progDlg;
		
		protected void onPreExecute() {
			progDlg = ProgressDialog.show(StopListActivity.this, "", "Loading stops", true, false);
		}
		
		protected Boolean doInBackground(Boolean... forced) {
			try {
				List<StopItem> stopFeed = StopsParser.getStops(forced[0]);
				
				// Organize the stops
				// -- Put unique stops into map
				for(StopItem stop : stopFeed) {
					if(! stopMap.containsKey(stop.toString()))
						stopMap.put(stop.toString(), stop.clone());
				}
				
				// -- Sort stops
				stops.addAll(stopMap.values());
				Collections.sort(stops);
				
/*	for future release: add route color			
				// Attach route colors
				for(RouteItem route : routeList) {
					for(StopItem rStop : route.stops) {
						if(stopsMap.containsKey(rStop.toString())) {
							// Add route color to stop
							StopItem stop = stopsMap.get(rStop.toString());
							stop.routeColors.add(route.color);
							// TODO: continue here
						}
					}
				}
				
*/			
			} catch(Exception e) {
				return false;
			}
			
			return true;
		}

		protected void onPostExecute(Boolean success) {
			if(success)
				stopsAdapter.showAll();
			else
				Toast.makeText(StopListActivity.this, getString(R.string.cmn_no_network), Toast.LENGTH_SHORT).show();
			
			if(stopsAdapter.getCount() > 0)
				findViewById(R.id.sl_noitem).setVisibility(View.GONE);
			else
				findViewById(R.id.sl_noitem).setVisibility(View.VISIBLE);
			
			progDlg.dismiss();
	    }
	}
	
	/** Update filter based on input text */
	TextWatcher onTextChanged = new TextWatcher() {

		public void afterTextChanged(Editable s) {
			if(s.length() > 0) {
				stopsAdapter.getFilter().filter(s);
			} else {
				stopsAdapter.showAll();
			}
		}

		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
		public void onTextChanged(CharSequence s, int start, int before, int count) {}
	};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stop_list);
		PublicParser.initialize(getApplicationContext());
		StopsParser.initialize(getApplicationContext());
		
		EditText tx = (EditText)findViewById(R.id.filterText);
		tx.addTextChangedListener(onTextChanged);

		// suppress soft keyboard at startup
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
				
		stopsAdapter = new StopsAdapter();
		setListAdapter(stopsAdapter);
		
		loadStopsTask = new LoadStopsTask();
		loadStopsTask.execute(false);
		
		ListView lv = getListView();
		lv.setTextFilterEnabled(true);
		lv.setOnItemClickListener(this);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if(loadStopsTask != null) {
			loadStopsTask.cancel(true);
			loadStopsTask = null;
		}
		
		stops.clear();
		stopsAdapter = null;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_stoplist, menu);
		return true;
	}

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {		
		Intent intent = new Intent(this, StopActivity.class);
		intent.putExtra(StopActivity.EX_STOP_NAME, parent.getAdapter().getItem(position).toString());
		startActivity(intent);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.mb_routelist_refresh:
				loadStopsTask = new LoadStopsTask();
				loadStopsTask.execute(true);
				return true;
	
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}
