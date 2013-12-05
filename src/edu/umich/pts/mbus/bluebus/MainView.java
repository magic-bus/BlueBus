package edu.umich.pts.mbus.bluebus;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.google.android.maps.MapView;

public class MainView extends MapView {
	
	private View zoomInButton;
	private View zoomOutButton;
	
	private int hideZoomControlDelay;
	
	private Runnable showZoomControl = new Runnable() {
		public void run() {
			removeCallbacks(hideZoomControl);
			
			if(zoomInButton != null && zoomOutButton != null) {
								
				if(zoomInButton.getVisibility() != View.VISIBLE) {
					zoomInButton.setVisibility(View.VISIBLE);
					zoomOutButton.setVisibility(View.VISIBLE);
				
					Animation fadeInAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.zoomctrl_fade_in);
					zoomInButton.startAnimation(fadeInAnimation);
					zoomOutButton.startAnimation(fadeInAnimation);
				}
			}
		}
	};
	
	private Runnable hideZoomControl = new Runnable() {
		public void run() {
			if(zoomInButton != null && zoomOutButton != null) {
								
				if(zoomInButton.getVisibility() == View.VISIBLE) {
					zoomInButton.setVisibility(View.GONE);
					zoomOutButton.setVisibility(View.GONE);
				
					Animation fadeOutAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.zoomctrl_fade_out);
					zoomInButton.startAnimation(fadeOutAnimation);
					zoomOutButton.startAnimation(fadeOutAnimation);
				}
			}
		}
	};
	
	private final GestureDetector gestureDetector;
	private final GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
		
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			getController().zoomInFixing((int)e.getX(), (int)e.getY());
			return false;
		}
	};
	
	public MainView(Context context, AttributeSet attrs) {
		super(context, attrs);
		gestureDetector = new GestureDetector(context, gestureListener);
		hideZoomControlDelay = getContext().getResources().getInteger(R.integer.param_map_hidezoomcontrol_delay);
	}

	public MainView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		gestureDetector = new GestureDetector(context, gestureListener);
		hideZoomControlDelay = getContext().getResources().getInteger(R.integer.param_map_hidezoomcontrol_delay);
	}
	
	public MainView(Context context, String apiKey) {
		super(context, apiKey);
		gestureDetector = new GestureDetector(context, gestureListener);
		hideZoomControlDelay = getContext().getResources().getInteger(R.integer.param_map_hidezoomcontrol_delay);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		if(e.getAction() == MotionEvent.ACTION_DOWN) {
			removeCallbacks(hideZoomControl);
			post(showZoomControl);
		} else if(e.getAction() == MotionEvent.ACTION_UP) {
			postDelayed(hideZoomControl, hideZoomControlDelay);
		}
		gestureDetector.onTouchEvent(e);
		return super.onTouchEvent(e);
	}
	
	public void setZoomControlButtons(View zoomIn, View zoomOut) {
		zoomInButton = zoomIn;
		zoomOutButton = zoomOut;
		post(showZoomControl);
		
		zoomInButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				getController().zoomIn();
				post(showZoomControl);
				postDelayed(hideZoomControl, hideZoomControlDelay);
			}
		});
		zoomOutButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				getController().zoomOut();
				post(showZoomControl);
				postDelayed(hideZoomControl, hideZoomControlDelay);
			}
		});
	}
	
	public void finish() {
		getOverlays().clear();
		
		zoomInButton = null;
		zoomOutButton = null;
	}
	
	public void setHideZoomControlDelay(int delay) {
		hideZoomControlDelay = delay;
	}
}
