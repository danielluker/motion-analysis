package com.daniel.visualanalysis.util;

import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/***
 * This class is instantiated in a background thread, and waits for GPS status
 * updates. When updates are received, they are broadcasted as intents, which
 * LocationScreen class receives. 
 * @author daniel
 *
 */
public class LocationUpdater implements LocationListener {

	public static final String LOCATION_SERVICE = "LOCATION_UPDATE";
	public boolean LISTENING;
	public boolean LOCATION_FOUND;
	
	
	public LocationUpdater(){
		LOCATION_FOUND = false;
		LISTENING = false;
	}
	
	@Override
	public void onLocationChanged(Location location) {
		Log.d("LOCATION_SERVICE", "on location changed");
		if (!LOCATION_FOUND){
			LOCATION_FOUND = true;
			broadcastState(true);
		}
		broadcastLocation(location);	
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		switch (status){
		case LocationProvider.OUT_OF_SERVICE:
			Log.e(LOCATION_SERVICE, provider+" unavailable");
			broadcastState(false);
			LISTENING = false;
			break;
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			Log.e(LOCATION_SERVICE, provider+" temporarily unavailable");
			broadcastState(false);
			break;
		case LocationProvider.AVAILABLE:
			broadcastState(true);
		}
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.d(LOCATION_SERVICE, provider+" enabled");
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.e(LOCATION_SERVICE, provider+" disabled");
		LISTENING = false;
	}
	
	private void broadcastState(boolean active){
		Log.d(LOCATION_SERVICE, "Broadcasting GPS status from LocationUpdater to LocationScreen");
		Intent message = new Intent("STATUS_UPDATE");
		if (active)
			message.putExtra("ACTIVE", true);
		else
			message.putExtra("ACTIVE", false);
		LocalBroadcastManager.getInstance(null).sendBroadcast(message);
	}

	private void broadcastLocation(Location location){
		Log.d(LOCATION_SERVICE, "Broadcasting location from LocationUpdater to LocationScreen");
		Intent message = new Intent("LOCATION_UPDATE");
		message.putExtra("NEW_LOCATION", location);
		LocalBroadcastManager.getInstance(null).sendBroadcast(message);
	}
	
}
