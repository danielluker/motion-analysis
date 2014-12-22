package com.daniel.visualanalysis;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;

public class FirstScreen extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.first_screen);
	}
	
	public void onClick(View v){
		
		Intent changeScreen = new Intent(this, LocationScreen.class);
		startActivity(changeScreen);
	}
	
}
