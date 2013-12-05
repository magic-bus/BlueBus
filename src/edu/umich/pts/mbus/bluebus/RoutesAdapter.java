package edu.umich.pts.mbus.bluebus;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import edu.umich.pts.mbus.bluebus.feed.RouteItem;

public class RoutesAdapter extends ArrayAdapter<RouteItem> {
	
	public RoutesAdapter(Context context, int resource,
			int textViewResourceId, List<RouteItem> objects) {
		super(context, resource, textViewResourceId, objects);
	}
	@Override
	public long getItemId (int position) {
		return getItem(position).id;
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = super.getView(position, convertView, parent);
		if(v != null) {
			RouteItem route = getItem(position);
			ImageView im = (ImageView)v.findViewById(R.id.raColor);
			im.setBackgroundColor(route.color);

			TextView txTitle = (TextView)v.findViewById(R.id.raTitle);
			TextView tx1 = (TextView)v.findViewById(R.id.raText1);
			TextView tx2 = (TextView)v.findViewById(R.id.raText2);

			// add bus TOA
			String strToa1 = route.getToaDescription(0);
			if(strToa1 != null) {
				
				// reset title size to medium
				txTitle.setTextAppearance(getContext(), android.R.style.TextAppearance_Medium);

				// set text for toa1
				
				tx1.setText(strToa1);
				tx1.setVisibility(View.VISIBLE);
				
				// check toa2
				String strToa2 = route.getToaDescription(1);
				if(strToa2 != null) {
					
					tx2.setText(strToa2);
					tx2.setVisibility(View.VISIBLE);
				} else {
					tx2.setVisibility(View.GONE);
				}
				
			} else {
				tx1.setVisibility(View.GONE);
				tx2.setVisibility(View.GONE);
			}
		}
		
		return v;
	}
	
};