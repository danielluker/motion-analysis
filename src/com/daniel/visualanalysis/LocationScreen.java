package com.daniel.visualanalysis;

import java.util.ArrayList;








import com.daniel.visualanalysis.util.*;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.location.*;
import android.os.Bundle;
import android.os.Looper;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class LocationScreen extends FragmentActivity {
	
	public Paint BLACK_PAINT;
	public Paint BLUE_PAINT;
	public Paint RED_PAINT;
	public Paint GREEN_PAINT;
	
	public boolean LOCATION_UPDATING;
	public boolean LOCATION_PROVIDER_ENABLED;
	public boolean PATH_TRACKER;
	public boolean TIME_RUNNING=false;
	public boolean LOCATION_READY=false;

	public ArrayList<Float> mTimePoints;
	public ArrayList<Float> mDistancePoints;
	public ArrayList<Float> mIntegrationPoints=new ArrayList<Float>();
	public float mTotalDistance;
	
	private Location mCurrentLocation;
	private Path mPath;
	
	private long startTime;
	private long totalTime;
	public long GPS_TIME_INTERVAL;
	
	public LocationManager mLocationManager;
	public LocationListener mLocationListener;
	public Interpolator mInterpolator;
	
	public TextView mDistanceDisplay;
	public TextView mTimeDisplay;
	public Bitmap mCurrentBitmap;
	public GoogleMap googleMap;
	
	private String ACTION;
	private String TYPE_OF_INTERPOLATION="LAGRANGE";
	
	public static final int STEP_SIZE = 200;
	private final float BITMAP_OFFSET = 4; 
	private static LocationScreen mLocationScreen;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.location_screen);
		mLocationScreen = this;

		Button mStartButton = (Button) findViewById(R.id.buttonStart);
		Button mStopButton = (Button) findViewById(R.id.buttonStop);
		Button mCalculateButton = (Button) findViewById(R.id.calculateButton);
		Button mSettingsButton = (Button) findViewById(R.id.settingsButton);

		mTimeDisplay = (TextView) findViewById(R.id.time_display);
		mDistanceDisplay = (TextView) findViewById(R.id.distance_display);	
			
		mStartButton.setOnClickListener(mStartOnClickListener);
		mStopButton.setOnClickListener(mStopOnClickListener);
		mCalculateButton.setOnClickListener(mCalcOnClickListener);
		mSettingsButton.setOnClickListener(mSettingsBtnOnClickListener);
		
		GPS_TIME_INTERVAL = 5000;
		
		BLACK_PAINT = new Paint(); BLACK_PAINT.setColor(getResources().getColor(R.color.black_overlay));
		BLUE_PAINT = new Paint(); BLUE_PAINT.setColor(getResources().getColor(R.color.blue));
		RED_PAINT = new Paint(); RED_PAINT.setColor(getResources().getColor(R.color.red));
		GREEN_PAINT = new Paint(); GREEN_PAINT.setColor(getResources().getColor(R.color.green));
		
		mLocationListener = new LocationUpdater();	
		mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		mCurrentLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Log.v("LOCATION_SERVICE", "Initialized mLocationManager");
		
				
		PATH_TRACKER = this.getIntent().getBooleanExtra("pathTracker", false);
		
		// Default interpolation = LAGRANGE
		mInterpolator = new PolynomialInterpolator(true); 

		
		registerBroadcastReceivers();
		
	}
	

	public void startTimer(){
		startTime = System.currentTimeMillis();
		TIME_RUNNING = true;
	}
	
	public void registerBroadcastReceivers(){ 
		LocalBroadcastManager.getInstance(this).registerReceiver(mLocationReceiver, new IntentFilter("LOCATION_UPDATE"));
		LocalBroadcastManager.getInstance(this).registerReceiver(mStatusReceiver, new IntentFilter("STATUS_UPDATE"));
		// Type of calculation: derivative vs. integral
		LocalBroadcastManager.getInstance(this).registerReceiver(mSelectionReceiver, new IntentFilter("SELECTION_BROADCAST"));
		// Type of interpolation: Lagrange vs. Spline.
		LocalBroadcastManager.getInstance(this).registerReceiver(mSettingsReceiver, new IntentFilter("SETTINGS_BROADCAST"));
	}

	public static LocationScreen getInstance(){
		return mLocationScreen;
	}

//=================================================LISTENERS===============
	/***
	 * 09/XI/2014
	 * ~ Improving this method by moving location listening and updating to background thread
	 * 
	 */
	Button.OnClickListener mStartOnClickListener = new Button.OnClickListener(){
		@Override
		public void onClick(View v){
			//start collecting gps information, as defined by GPS_TIME_INTERVAL	
			mDistancePoints = new ArrayList<Float>();
			mTimePoints = new ArrayList<Float>();
//			if (LOCATION_UPDATING = collectLocation(GPS_TIME_INTERVAL)){
				// Adding initial point (0,0) for polynomial
			collectLocationInBackground(GPS_TIME_INTERVAL);
				mTotalDistance = 0;
				mInterpolator.addPoint(0,mTotalDistance);
				mCurrentLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				v.setClickable(false);
				findViewById(R.id.buttonStop).setClickable(true);
//			}
//			else
//				Toast.makeText(getApplicationContext(), R.string.error_location, Toast.LENGTH_LONG).show();
		}
	};
	
	/***
	 * Implemented the listener
	 */
	Button.OnClickListener mStopOnClickListener = new Button.OnClickListener(){
		@Override
		public void onClick(View v){
			mLocationManager.removeUpdates(mLocationListener);
			findViewById(R.id.buttonStart).setClickable(true);
			v.setClickable(false);
			updateGraph();
			CharSequence cs0 = "Total distance = "+Float.toString(mTotalDistance)+"m";
			CharSequence cs1 = "Total time = "+Long.toString(totalTime/1000)+"s";
			mDistanceDisplay.setText(cs0);
			mTimeDisplay.setText(cs1);
			return;
		}
	};

	Button.OnClickListener mCalcOnClickListener = new Button.OnClickListener(){
		@SuppressLint("NewApi")
		@Override
		public void onClick(View v){
			DialogueShower showDialogue = new DialogueShower();
			showDialogue.setRequest("USER_INPUT_REQUEST");
			showDialogue.show(getFragmentManager(), "Requesting user for type of calculation");
		}
	};
	
	@SuppressLint("NewApi")
	Button.OnClickListener mSettingsBtnOnClickListener = new Button.OnClickListener(){	
		@Override
		public void onClick(View v) {
			DialogueShower showDialogue = new DialogueShower();
			showDialogue.setRequest(DialogueShower.settings);
			showDialogue.show(getFragmentManager(), "Requesting user for selection");
			return;
		}
	};
	
	/*
	@SuppressLint("NewApi")
	ImageView.OnTouchListener mImageViewOnTouchListener = new ImageView.OnTouchListener() {	
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			v.performClick();
			if (event.getAction()!=MotionEvent.ACTION_DOWN){
				return false;
			}
			float x = event.getX();
			x=x*(totalTime/mImageView.getWidth());
			Log.v(ACTIVITY_SERVICE, "Detected touch at x = "+Float.toString(x)+", Action = "+ACTION);
			if (!mInterpolator.isSolved())
				return false;
			if (ACTION==null)
				return false;
			else if (ACTION.equals("DERIVATE")){
				float out = mInterpolator.derivativeThirdOrder(x);
				Log.v("Calculation", "Value of derivative = "+Float.toString(out));
				DialogueShower showDialogue = new DialogueShower();
				showDialogue.setArguments(out*1000, x/1000);
				showDialogue.show(getFragmentManager(),"Displaying derivate result");
				return true;
			}
			else if (ACTION.equals("INTEGRATE")){
				
				switch (mIntegrationPoints.size()){
				case 0:
					mIntegrationPoints.add(x);
					return true;
				case 1:
					mIntegrationPoints.add(x);
					float out = mInterpolator.integrate(mIntegrationPoints);
					DialogueShower showDialogue = new DialogueShower();
					showDialogue.setArguments(out/10000, mIntegrationPoints.get(0)/1000, x/1000);
					showDialogue.show(getFragmentManager(),"Displaying integral result");
					paintArea(mIntegrationPoints.get(0),mIntegrationPoints.get(1));
					mIntegrationPoints.clear();
					return true;
				default:
					Log.e("ACTIVITY_SERVICE", "Error in integration points... attempting solution");
					mIntegrationPoints.clear();
				}
			}
			return false;				
		}
	};
	*/
	
	private BroadcastReceiver mLocationReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.v(ACTIVITY_SERVICE, "Received broadcast from LocationUpdater");
			
			// Decoding the broadcast
			Bundle extras = intent.getExtras();
			Location NEW_LOCATION = (Location) extras.get("NEW_LOCATION");	
			
			// Updating total distance and displaying it
			if(mCurrentLocation!=null){
				mTotalDistance += mCurrentLocation.distanceTo(NEW_LOCATION);		
				CharSequence displayDist = Float.toString(mTotalDistance);
				Log.d(ACTIVITY_SERVICE, "displayDist ="+displayDist);
				mDistanceDisplay.setText(displayDist);
			}

			// Updating current location
			mCurrentLocation = NEW_LOCATION;
			
			// Setting up initial timer
			if (!TIME_RUNNING)
				startTimer();	
			
			// Calculating total time and displaying it
			long cTime = System.currentTimeMillis(); 
			long dTime = cTime-startTime;
			CharSequence displayTime = Long.toString(dTime);
			mTimeDisplay.setText(displayTime);
			totalTime = dTime;
			
			// Adding point to polynomial
			if (dTime!=0){
				mInterpolator.addPoint(dTime, mTotalDistance);
			}
			
			// Adding points to local store
			mDistancePoints.add(mTotalDistance);
			mTimePoints.add((float) totalTime);
			
			// Updating the graph
			updateGraph();
		}
	};
	
	private BroadcastReceiver mStatusReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(ACTIVITY_SERVICE, "Received GPS status from LocationUpdater");
			if (intent.getBooleanExtra("ACTIVE", false)){
				Toast.makeText(getBaseContext(), R.string.gps_success, Toast.LENGTH_LONG).show();	
			}
			else
				Toast.makeText(getBaseContext(), R.string.gps_wait, Toast.LENGTH_LONG).show();
		}
	};
	
	private BroadcastReceiver mSelectionReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {		
			ACTION = intent.getStringExtra("action");
			Log.d("Broadcast","Received selection broadcast, action: "+ACTION);
			if (ACTION.equals("DERIVATE"))
				Toast.makeText(getBaseContext(), R.string.derivative_instruction, Toast.LENGTH_SHORT).show();
			else if (ACTION.equals("INTEGRATE"))
				Toast.makeText(getBaseContext(), R.string.integrate_instruction, Toast.LENGTH_SHORT).show();
			else{
				Log.e("Broadcast", "Error receiving selection broadcast");
				return;
			}
		}
	};
	
	private BroadcastReceiver mSettingsReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {		
			String received = intent.getStringExtra("action");
			Log.d("Broadcast","Received selection broadcast, action: "+received);
			if (received.equals("LAGRANGE")){
				Toast.makeText(getBaseContext(), R.string.select_lagrange, Toast.LENGTH_SHORT).show();
				if (TYPE_OF_INTERPOLATION.equals("SPLINE")){
					mInterpolator = new PolynomialInterpolator(false);
					TYPE_OF_INTERPOLATION = received;
				}
			}
			else if (received.equals("SPLINE"))
				Toast.makeText(getBaseContext(), R.string.select_spline, Toast.LENGTH_SHORT).show();
				if (TYPE_OF_INTERPOLATION.equals("LAGRANGE")){
//					mInterpolator = new CubeSplineInterpolator(false);
					TYPE_OF_INTERPOLATION = received;
					}			
			else{
				Log.e("Broadcast", "Error receiving selection broadcast, mTypeOfInerpolationReceiver");
				return;
			}
		}
	};
	
	/***
	 * Method which queries the GPS for locations. This is a bad implementaion, since it runs on the UI
	 * thread. Deprecated in favour of collectLocationInBackground()
	 * @param timeInterval : Time in between GPS satellite data 
	 * @return Boolean representing success of operation
	 */
	@Deprecated
	public boolean collectLocation(long timeInterval){
		//First we initialise the LocationListener with subclass locationlistener
		try{
			Log.v(LOCATION_SERVICE,"Requesting GPS updates...");
			// Debugging location manager
			Log.d(LOCATION_SERVICE, "GPS enabled = "+mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,timeInterval,0,mLocationListener);
		}
		catch(NullPointerException ex){
			Log.e(LOCATION_SERVICE, "NullPointerException on method mLocationManager.requestLocationUpdates(...)");
			return false;
		}
		catch(IllegalArgumentException ex){
			Log.e(LOCATION_SERVICE,"LocationProvider unavailable or LocationListener null");
			return false;
		}
		catch(SecurityException ex){
			Log.e(LOCATION_SERVICE,"Location permission unavailable!");
			return false;
		}
		Log.d(LOCATION_SERVICE,"Location provider status: positive");
		Toast.makeText(getBaseContext(), R.string.location_start, Toast.LENGTH_LONG).show();
		return true;		
	}
	
	/***
	 * Begin query of GPS satellite location. Work in background thread
	 * @param timeInterval 
	 */
	public void collectLocationInBackground(final long timeInterval){
		Thread backgroundCollector = new Thread(){
			@Override
			public void run(){
				try{
					Log.v(LOCATION_SERVICE,"Requesting GPS updates...");
					// Debugging location manager
					Log.d(LOCATION_SERVICE, "GPS enabled = "+mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
					mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,timeInterval,0,mLocationListener);
				}
				catch(NullPointerException ex){
					Log.e(LOCATION_SERVICE, "NullPointerException on method mLocationManager.requestLocationUpdates(...)");
					return;
				}
				catch(IllegalArgumentException ex){
					Log.e(LOCATION_SERVICE,"LocationProvider unavailable or LocationListener null");
					return;
				}
				catch(SecurityException ex){
					Log.e(LOCATION_SERVICE,"Location permission unavailable!");
					return;
				}
				Log.d(LOCATION_SERVICE,"Location provider status: positive");
				Toast.makeText(getBaseContext(), R.string.location_start, Toast.LENGTH_LONG).show();
			}
		};
		backgroundCollector.run();
	}
	
	private void updateGraph(){
		if (mInterpolator.numberOfPoints()<3)
			return;
//		mCurrentBitmap = drawPoints(mImageView);
//		mInterpolator.interpolateDistance(mImageView.getWidth(),mImageView.getHeight(),totalTime,mTotalDistance);
//		mInterpolator2.interpolateDistance(mImageView.getWidth(),mImageView.getHeight(),totalTime,mTotalDistance);
//		mInterpolator3.interpolateDistance(mImageView.getWidth(),mImageView.getHeight(),totalTime,mTotalDistance);
//		mCurrentBitmap = drawPolynomial(mImageView);
//		mImageView.setImageBitmap(mCurrentBitmap);
	}

    public Handler getHandler() {
        return new Handler(Looper.getMainLooper()) {
            public void handleMessage(Message m) {
            	googleMap = (GoogleMap) m.obj;
            }
        };
    }
	
	//================================================RENDERING======================
	
	
	public Bitmap drawPolynomial(View v){		
		Log.v(ACTIVITY_SERVICE, "Drawing Polynomial...");
		Canvas canvas = new Canvas(mCurrentBitmap);
		drawPath(mInterpolator, BLACK_PAINT, canvas);
//		drawPath(mInterpolator2, RED_PAINT, canvas);
//		drawPath(mInterpolator3, GREEN_PAINT, canvas);
		return mCurrentBitmap;
	}
	
	private void drawPath(Interpolator cInterpolator, Paint color, Canvas mCanvas){
		mPath = new Path();
		float startX = cInterpolator.f_visual(0).x;
		float startY = cInterpolator.f_visual(0).y;
		mPath.moveTo(startX, startY);
		
		float nextX = 0;
		float nextY = 0;
		for (int i = 1; i < STEP_SIZE; i++){
			nextX = cInterpolator.f_visual(i).x;
			nextY = cInterpolator.f_visual(i).y;
			mPath.lineTo(nextX, nextY);
		}
		for (int j = STEP_SIZE-1; j>=0; j--){
			nextX = cInterpolator.f_visual(j).x;
			nextY = cInterpolator.f_visual(j).y - BITMAP_OFFSET;
			mPath.lineTo(nextX, nextY);
		}
		mCanvas.drawPath(mPath, color); 
	}
	/*
	private Bitmap drawPoints(View v){
		Log.v(ACTIVITY_SERVICE, "Drawing Points...");
		mCurrentBitmap = Bitmap.createBitmap(mImageView.getWidth(), mImageView.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(mCurrentBitmap);
		
		float y_max = ((float)mImageView.getHeight());
		float x_scale = ((float)mImageView.getWidth())/((float)totalTime);
		float y_scale = y_max/((float)mTotalDistance);
		
		mPath = new Path();
		float startX = 0;
		float startY = y_max;
		mPath.moveTo(startX, startY);
		
		float nextX = 0;
		float nextY = 0;
		for (int i = 1; i < mInterpolator.numberOfPoints(); i++){
			nextX = mTimePoints.get(i)*x_scale;
			nextY = y_max-mDistancePoints.get(i)*y_scale;
			mPath.lineTo(nextX, nextY);
		}
		mPath.lineTo(nextX, nextY-BITMAP_OFFSET);
		for (int j = mInterpolator.numberOfPoints()-1;j>=0;j--){
			nextX = mTimePoints.get(j)*x_scale;
			nextY = y_max-mDistancePoints.get(j)*y_scale-BITMAP_OFFSET;
			mPath.lineTo(nextX, nextY);
		}
		canvas.drawPath(mPath, BLUE_PAINT); 		
		return mCurrentBitmap;
	}
	
	private void paintArea(float a_data, float b_data){
		Log.v("ACTIVITY_SERVICE","Drawing integration area");
		if (!mInterpolator.isSolved())
			return;
		float y_min = (float) mImageView.getHeight();
		float x_scale = ((float)mImageView.getWidth())/((float)totalTime);
		float y_scale = ((float)mImageView.getHeight())/((float)mTotalDistance);
		
		Bitmap newBitmap = Bitmap.createBitmap(mCurrentBitmap);
		Canvas canvas = new Canvas(newBitmap);
		float a_view = a_data*x_scale;
		float b_view = b_data*x_scale;
		mPath = new Path();
		mPath.moveTo(a_view, y_min);
		mPath.lineTo(a_view, y_min-mInterpolator.f(a_data)*y_scale);
		float n = 1/(float)STEP_SIZE;
		for (int i=0;i<STEP_SIZE*(b_view-a_view);i++){
			mPath.lineTo(a_view+i*n, y_min-mInterpolator.f(a_data+(i*n/x_scale))*y_scale);
		}
		mPath.lineTo(b_view, y_min);
		mPath.lineTo(a_view, y_min);
		canvas.clipPath(mPath);
		canvas.drawRGB(0x01, 0x01, 0x01);
		mCurrentBitmap = newBitmap;
		mImageView.setImageBitmap(newBitmap);
		
	}
	*/
}
