package com.daniel.visualanalysis;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class SelectScreen1 extends Activity{

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.select_screen_1);
	}
	
	public void changeScreen_Image(View v){
		Intent changeScreenIntent = new Intent(this, ImageScreen.class);
    	startActivity(changeScreenIntent);
	}
	
	public void changeScreen_Location(View v){
		Log.d(ACTIVITY_SERVICE, "Creating intent for LocationSelect.class");
		Intent changeScreenIntent = new Intent(this, LocationSelect.class);
    	startActivity(changeScreenIntent);
	}

}
