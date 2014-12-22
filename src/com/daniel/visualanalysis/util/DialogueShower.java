package com.daniel.visualanalysis.util;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

@SuppressLint("NewApi")
public class DialogueShower extends DialogFragment {

	/* Strings for requesting type of dialogue*/
	public static final String select_calculation = "USER_INPUT_REQUEST";
	public static final String integral_result = "INTEGRAL_RESULT";
	public static final String derivative_result = "DERIVATIVE_RESULT";
	public static final String select_image = "IMAGE_REQUEST";
	public static final String settings = "SETTINGS";
	
	public float output;
	public float input0;
	public float input1;
	
	private String typeOfDialogue;

	/* Will use default constructor*/
	
	public void setArguments(float out, float in0, float in1){
		output = out;
		input0 = in0;
		input1 = in1;
		typeOfDialogue="INTEGRAL_RESULT";
	}
	
	public void setArguments(float out, float in0){
		output = out;
		input0 = in0;
		typeOfDialogue="DERIVATIVE_RESULT";
	}
	
	public void setRequest(String typeOfRequest){
		typeOfDialogue = typeOfRequest;
	}
		
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		CharSequence message = null;
		if (typeOfDialogue.equals(derivative_result)){
			message = "f'("+Float.toString(input0)+") = "+Float.toString(output);
		}
		else if (typeOfDialogue.equals(integral_result)){
			message = "Integral from "+Float.toString(input0)+" to "+Float.toString(input1)+" = "+Float.toString(output);
		}
		else if (typeOfDialogue.equals(select_calculation)){
			CharSequence[] settings = {	"Derivative",
										"Integral"	};
			builder.setItems(settings, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int chosenCalculation) {
					switch(chosenCalculation){
					case 0:
						broadcastSelection("DERIVATE");
						return;
					case 1:
						broadcastSelection("INTEGRATE");
						return;
					default:
						Log.e("Dialogue shower","Error in user selection");
					}
				}
			});
		}
		else if (typeOfDialogue.equals(select_image)){
			CharSequence[] options = {	"Camera",
										"Open File"	};
			builder.setItems(options, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int chosenCalculation) {
					switch(chosenCalculation){
					case 0:
						broadcastSelection("CAMERA");
						return;
					case 1:
						broadcastSelection("LOAD");
						return;
					default:
						Log.e("Dialogue shower","Error in user selection");
					}
				}
			});			
		}
		else if (typeOfDialogue.equals(settings)){
			CharSequence[] options = {	"Lagrange Interpolation",
										"Spline Interpolation",
										"LOESS Interpolation"	};
			builder.setItems(options, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int chosenSetting) {
					switch (chosenSetting) {
					case 0:
						broadcastSelection("LAGRANGE");
						return;
					case 1:
						broadcastSelection("SPLINE");
						return;
					case 2:
						broadcastSelection("LOESS");
						return;
					default:
						return;
					}
				}
			});
		}
		else{
			Log.e("DialogueShower", "Data hasn't been added to alertDialog");
			NullPointerException ex = new NullPointerException();
			throw ex;
		}
		
		builder.setMessage(message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dismiss();
			}
		});
		
		return builder.create();

	}
	
	private void broadcastSelection(String action){
		Intent intent;
		if (action.equals("DERIVATE") || action.equals("INTEGRATE")){
			intent = new Intent("SELECTION_BROADCAST");
			intent.putExtra("action", action);
			LocalBroadcastManager.getInstance(null).sendBroadcast(intent);
		}
		else if (action.equals("CAMERA") || action.equals("LOAD")){
			intent = new Intent("IMAGE_SELECTION");
			intent.putExtra("action", action);
			LocalBroadcastManager.getInstance(null).sendBroadcast(intent);
		}
		else if (action.equals("LAGRANGE") || action.equals("SPLINE") || action.equals("LOESS")){
			intent = new Intent("SETTINGS_BROADCAST");
			intent.putExtra("action", action);
			LocalBroadcastManager.getInstance(null).sendBroadcast(intent);
		}
	}


}
