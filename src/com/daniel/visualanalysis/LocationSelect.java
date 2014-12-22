package com.daniel.visualanalysis;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class LocationSelect extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.location_select);
		ImageView mImageView = (ImageView) findViewById(R.id.imageView1);
		mImageView.setImageResource(R.drawable.gps_satellite);
	}

//	public void changeScreen_PathTracker(View v){
//		Intent changeScreenIntent = new Intent(this, LocationScreen.class);
//		changeScreenIntent.putExtra("pathTracker", true);
//    	startActivity(changeScreenIntent);
//	}
	
	public void changeScreen_DistanceTracker(View v){
		Intent changeScreenIntent = new Intent(this, LocationScreen.class);
    	startActivity(changeScreenIntent);
	}
	
	public static Bitmap decodeSampledBitmapFromResource(String pathName,
            int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth,
                reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(pathName, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and
            // keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
	
}
