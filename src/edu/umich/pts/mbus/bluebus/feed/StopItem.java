package edu.umich.pts.mbus.bluebus.feed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StopItem implements Cloneable, Comparable<StopItem> {

	/** Time of arrival */
	public static class TOA implements Cloneable, Comparable<TOA> {
		public float time = -1;
		public int id = -1;
		
		public TOA() {
			
		}
		
		public TOA(TOA toa) {
			time = toa.time;
			id = toa.id;
		}
		
		@Override
		public TOA clone() {
			return new TOA(this);
		}
		
		public int getTimeInMin() {
			return (int)time/60;
		}
		
		@Override
		public String toString() {
			int minute = (int)time/60;
			if(minute > 0)
				return new String(minute+" min");
			else
				return new String("NOW");
		}

		public int compareTo(TOA another) {
			return (int)(this.time - another.time);
		}
	};
	
	public int				id;
	public String			name;	// unique name
	public String			name2;	// commonly referred name. Non-unique!
	public String			name3;	// Less used name
	public int				lat;
	public int				lon;

	public final List<TOA>	toaList = new ArrayList<TOA>();
	public final List<Integer>	routeColors = new ArrayList<Integer>();

	public StopItem() {

	}
	
	public StopItem(StopItem item) {
		id = item.id;
		name = item.name;
		name2 = item.name2;
		name3 = item.name3;
		lat = item.lat;
		lon = item.lon;
		
		for(TOA toa : item.toaList)
			toaList.add(toa.clone());
	}
	
	public StopItem clone() {
		return new StopItem(this);
	}
	
	// called before reuse
	public void clear() {
		id++;
		name = "";
		name2 = "";
		name3 = "";
		lat = 0;
		lon = 0;
		toaList.clear();
	}
	
	public void sortTOA() {
		Collections.sort(toaList);
	}
	
	@Override
	public String toString() {
		return name2;
	}

	public int compareTo(StopItem another) {
		return this.toString().compareTo(another.toString());
	}
}