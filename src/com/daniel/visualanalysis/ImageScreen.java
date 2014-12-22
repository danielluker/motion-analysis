package com.daniel.visualanalysis;

import java.io.File;
import java.util.ArrayList;

import com.daniel.visualanalysis.util.CubeSplineInterpolator;
import com.daniel.visualanalysis.util.Interpolator;
import com.daniel.visualanalysis.util.LocalRegressionInterpolator;
import com.daniel.visualanalysis.util.PolynomialInterpolator;
import com.daniel.visualanalysis.util.DialogueShower;
import com.daniel.visualanalysis.R;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class ImageScreen extends Activity {
		
	/* INSTANCE VARIABLES */
	public static final int ACTION_TAKE_PHOTO = 1;
	public static final int ACTION_LOAD_PHOTO = 2;
	public static final int SETTINGS = 3;
	
	public boolean PHOTO_SET=false;
	public float bitmap_x_min;
	public float bitmap_x_max;
	public float bitmap_y_min;
	public float bitmap_y_max;
	public float scale;
	
	public ArrayList<Float> mIntegralPoints=new ArrayList<Float>();
	
	public Paint BLACK_PAINT;
	
	public ImageView mImageView;
	public Canvas mCanvas;
	public Bitmap mCurrentBitmap;
	
	private Interpolator mInterpolator;
	private Path mPath;
	
	private String mCurrentPhotoPath;
	private File mCurrentPhotoFile; 
	private String ACTION;
	private String TYPE_OF_INTERPOLATION;

	private static final String TAG = "ImageScreen";
	private static final int STEP_SIZE = 200;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); // use creator from the inherited class
		setContentView(R.layout.image_screen);
		
		BLACK_PAINT = new Paint(); BLACK_PAINT.setColor(getResources().getColor(R.color.black_overlay));
		
		mImageView = (ImageView) findViewById(R.id.imageView1);
		
		bitmap_x_min=0;
		bitmap_x_max=mImageView.getWidth();
		bitmap_y_min=0;
		bitmap_y_max=mImageView.getHeight();
		
		Button picBtn = (Button) findViewById(R.id.importPhotoButton);
		Button clearBtn = (Button) findViewById(R.id.clearButton);
		Button polyBtn = (Button) findViewById(R.id.polyButton);
		Button restoreBtn = (Button) findViewById(R.id.restoreImageButton);
		Button calcBtn = (Button) findViewById(R.id.calculateButton);
		Button setBtn = (Button) findViewById(R.id.settingsButton);

		mImageView.setOnTouchListener(mImageViewOnTouchListener);
		picBtn.setOnClickListener(mPicOnClickListener);
		clearBtn.setOnClickListener(mClearBtnOnClickListener);
		polyBtn.setOnClickListener(mPolyBtnOnClickListener);
		restoreBtn.setOnClickListener(mRestorePicOnClickListener);
		calcBtn.setOnClickListener(mCalcBtnOnClickListener);
		setBtn.setOnClickListener(mSettingsBtnOnClickListener);

		// Default interpolation = LAGRANGE
		TYPE_OF_INTERPOLATION = "LAGRANGE";
		mInterpolator = new PolynomialInterpolator(false);
		
		// Camera vs. Load
		LocalBroadcastManager.getInstance(this).registerReceiver(mSelectionReceiver, new IntentFilter("IMAGE_SELECTION"));
		// Derivative vs. Integral
		LocalBroadcastManager.getInstance(this).registerReceiver(mInstructionReceiver, new IntentFilter("SELECTION_BROADCAST"));
		// Polynomial vs. Spline
		LocalBroadcastManager.getInstance(this).registerReceiver(mTypeOfInterpolationReceiver, new IntentFilter("SETTINGS_BROADCAST"));

	}
	
//=================================================ON TOUCH LISTENERS===============
	
	@SuppressLint("NewApi")
	Button.OnClickListener mPicOnClickListener = new Button.OnClickListener() {	
		@Override
		public void onClick(View v) {
			DialogueShower showDialogue = new DialogueShower();
			showDialogue.setRequest("IMAGE_REQUEST");
			showDialogue.show(getFragmentManager(), "Requesting user for image selection");
		}
	};
		
	Button.OnClickListener mRestorePicOnClickListener = new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			if((mCurrentPhotoPath==null)||(mCurrentBitmap==null))
				return;
			setPic();
		}
	};
	
	@SuppressLint("NewApi")
	Button.OnClickListener mCalcBtnOnClickListener = new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			DialogueShower showDialogue = new DialogueShower();
			showDialogue.setRequest(DialogueShower.select_calculation);
			showDialogue.show(getFragmentManager(), "Requesting user for selection");
		}
	};
	
	Button.OnClickListener mClearBtnOnClickListener = new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			mInterpolator.clear();
			mCurrentBitmap = Bitmap.createBitmap(mImageView.getWidth(), mImageView.getHeight(), Bitmap.Config.ARGB_8888);
			mImageView.setImageBitmap(mCurrentBitmap);
			PHOTO_SET=false;
			if (TYPE_OF_INTERPOLATION.equals("SPLINE")){
				mInterpolator.addPoint(0, 0);
				mInterpolator.addPoint(mImageView.getWidth(),mImageView.getHeight());
			}
			ACTION=null;
			bitmap_x_min=0;
        	bitmap_x_max=0;
        	bitmap_y_min=0;
        	bitmap_y_max=0;
		}
	};

	
	Button.OnClickListener mPolyBtnOnClickListener = new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			Log.v("create polynomial","detected click");
			if(TYPE_OF_INTERPOLATION.equals("SPLINE") && mInterpolator.numberOfPoints() < 3)
				return;
			if(mInterpolator.numberOfPoints() < 2){
				Toast.makeText(getBaseContext(), R.string.warning1, Toast.LENGTH_SHORT).show();
				return;
			}
			if(TYPE_OF_INTERPOLATION.equals("LOESS") && mInterpolator.numberOfPoints() < 4)
				return;
			mInterpolator.interpolate(mImageView.getWidth());
			Bitmap newBitmap = drawPolynomial(mImageView);
			mImageView.setImageBitmap(newBitmap);
			return;

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
	
	@SuppressLint("NewApi")
	ImageView.OnTouchListener mImageViewOnTouchListener = new ImageView.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {	
			v.performClick();
			if (event.getAction()!=MotionEvent.ACTION_DOWN)
				return false;
			float x = event.getX();
			float y = event.getY();
			Log.v(ACTIVITY_SERVICE,"detected touch at ("+Float.toString(x)+", "+Float.toString(y)+")");
			// Turning the points so that 0,0 is at bottom left corner
			float Y = (float) v.getHeight();
			float correct_y = Y-y;
			if (ACTION==null){
				if (PHOTO_SET){
					Log.d("onTouchListener","bitmap x min ="+Float.toString(bitmap_x_min));
					Log.d("onTouchListener","bitmap x max ="+Float.toString(bitmap_x_max));
					Log.d("onTouchListener","bitmap y min ="+Float.toString(bitmap_y_min));
					Log.d("onTouchListener","bitmap y max ="+Float.toString(bitmap_y_max));
					if (x<bitmap_x_min || x>bitmap_x_max)
						return false;
					if (y<bitmap_y_min || y>bitmap_y_max)
						return false;
					x=(x-bitmap_x_min);
					y=(y-bitmap_y_min);
					correct_y = Y-y; 
					Log.v("onTouchListener","Point in bitmap region!");
				}
				mInterpolator.addPoint(x,correct_y);
				placePoint(x,y); 
				return true;
			}
			else if(ACTION.equals("DERIVATE")){
				Toast.makeText(getApplicationContext(), "Derivative point", Toast.LENGTH_SHORT).show();
				float derivative = mInterpolator.derivativeThirdOrder(x);
				DialogueShower showResult = new DialogueShower();
				showResult.setArguments(derivative, x);
				showResult.show(getFragmentManager(), "Displaying differentiation results");
				return false;
			}
			else if(ACTION.equals("INTEGRATE")){
				switch(mIntegralPoints.size()){
				case 0:
					mIntegralPoints.add(x);
					break;
				case 1:
					mIntegralPoints.add(x);
					float output = mInterpolator.integrate(mIntegralPoints);
					DialogueShower showResult = new DialogueShower();
					showResult.setArguments(output, mIntegralPoints.get(0), x);
					showResult.show(getFragmentManager(), "Displaying integration results");
					paintArea(mIntegralPoints.get(0),x);
					mIntegralPoints.clear();
					break;
				default:
					Log.e(ACTIVITY_SERVICE, "Number of integration points error!");
				}
				Toast.makeText(getApplicationContext(), "Integral point", Toast.LENGTH_SHORT).show();
				return false;
			}
			return false;
		}	
	};

//===========================================BROADCAST RECEIVERS====================
	
	private BroadcastReceiver mSelectionReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			String ACTION = intent.getStringExtra("action");
			if (ACTION.equals("CAMERA")){
				Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				// Creating file to save the image.
				File cacheFolder = Environment.getExternalStorageDirectory();
				cacheFolder.mkdir();
				mCurrentPhotoFile = new File(cacheFolder, "tempImage_001");
				mCurrentPhotoPath = mCurrentPhotoFile.getAbsolutePath();
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mCurrentPhotoFile));
				Log.d("CAMERA_REQUEST", "Initiating camera request");
				startActivityForResult(takePictureIntent, ACTION_TAKE_PHOTO);
			}
			else if (ACTION.equals("LOAD")){
				Intent loadPictureIntent = new Intent(Intent.ACTION_PICK,
						android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);		
				startActivityForResult(loadPictureIntent, ACTION_LOAD_PHOTO);
			}
			else{
				Log.e("Broadcast", "Error receiving selection broadcast");
				return;
			}
			mImageView.setClickable(true);
		}
	};

	private BroadcastReceiver mInstructionReceiver = new BroadcastReceiver(){
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
			mImageView.setClickable(true);
		}
	};
	
	private BroadcastReceiver mTypeOfInterpolationReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			String received = intent.getStringExtra("action");
			Log.d("Broadcast","Received selection broadcast, action: "+received);
			if (received.equals("LAGRANGE")){
				Toast.makeText(getBaseContext(), R.string.select_lagrange, Toast.LENGTH_SHORT).show();
				if (!TYPE_OF_INTERPOLATION.equals("LAGRANGE")){
					mInterpolator = new PolynomialInterpolator(false);
					mCurrentBitmap = Bitmap.createBitmap(mImageView.getWidth(), mImageView.getHeight(), Bitmap.Config.ARGB_8888);
					mImageView.setImageBitmap(mCurrentBitmap);
					PHOTO_SET=false;
					TYPE_OF_INTERPOLATION = received;
				}
			}
			else if (received.equals("SPLINE")){
				Toast.makeText(getBaseContext(), R.string.select_spline, Toast.LENGTH_SHORT).show();
				if (!TYPE_OF_INTERPOLATION.equals("SPLINE")){
					mInterpolator = new CubeSplineInterpolator(false);
					mCurrentBitmap = Bitmap.createBitmap(mImageView.getWidth(), mImageView.getHeight(), Bitmap.Config.ARGB_8888);
					mImageView.setImageBitmap(mCurrentBitmap);
					PHOTO_SET=false;
					TYPE_OF_INTERPOLATION = received;
					// If we use spline interpolation, we need to add (0,0) and (max_x,max_y)
					mInterpolator.addPoint(0,0);
					mInterpolator.addPoint(mImageView.getWidth(),mImageView.getHeight());
				}	
			}
			else if (received.equals("LOESS")){
				Toast.makeText(getBaseContext(), R.string.select_loess, Toast.LENGTH_SHORT).show();
				if (!TYPE_OF_INTERPOLATION.equals("LOESS")){
					mInterpolator = new LocalRegressionInterpolator(false);
					mCurrentBitmap = Bitmap.createBitmap(mImageView.getWidth(), mImageView.getHeight(), Bitmap.Config.ARGB_8888);
					mImageView.setImageBitmap(mCurrentBitmap);
					PHOTO_SET=false;
					TYPE_OF_INTERPOLATION = received;
					// If we use spline interpolation, we need to add (0,0) and (max_x,max_y)
					mInterpolator.addPoint(0,0);
					mInterpolator.addPoint(mImageView.getWidth(),mImageView.getHeight());
				}	
			}
			else{
				Log.e("Broadcast", "Error receiving selection broadcast, mTypeOfInerpolationReceiver");
				return;
			}
		}
	};


	//=================================================ACTIVITY RESULT===================

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d("ACTIVITY_RESULT", "Returning activity result...");
		switch (requestCode) {
		case ACTION_TAKE_PHOTO:
			switch (resultCode){
			case Activity.RESULT_OK:
				if (mCurrentPhotoPath != null) 
					setPic();
				else
					Log.e(TAG, "mCurrentPhotoPath = null");
				break;
			case Activity.RESULT_CANCELED:
				Log.w(TAG, "Photo cancelled by user");
				break;
			default:
				Log.e(TAG, "Camera error!");
				break;
			}
			break;
		case ACTION_LOAD_PHOTO:
			switch (resultCode){
			case Activity.RESULT_OK:
				Uri mCurrentPhotoUri = data.getData();
				Bitmap image;
				try {
					image = MediaStore.Images.Media.getBitmap(getContentResolver(), mCurrentPhotoUri);
					mCurrentBitmap = image;
					File photoFile = new File(getRealPathFromURI(mCurrentPhotoUri));
					mCurrentPhotoPath = photoFile.getAbsolutePath();
					setPic();
				} catch (Exception e) {
					e.printStackTrace();
					Log.e(TAG,"IOException onActivityResult, LOAD_PHOTO");
				}
				break;
			case Activity.RESULT_CANCELED:
				Log.w(TAG, "Load cancelled by user");
				break;
			default:
				Log.e(TAG, "Load error!");
				break;
			}
			break;
		case SETTINGS:
			
		}
		
	}
		
//==============================================MODIFYING THE IMAGEVIEW=============
	
	public String getRealPathFromURI(Uri uri) {
	    Cursor cursor = getContentResolver().query(uri, null, null, null, null); 
	    cursor.moveToFirst(); 
	    int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA); 
	    return cursor.getString(idx); 
	}
	
	/***
	 * Scales and sets image located at mCurrentPhotoPath onto ImageView
	 */
	private void setPic() {
        Log.d("CAMERA_REQUEST","Rendering image...");
        /* There isn't enough memory to open up more than a couple camera photos */
        /* So pre-scale the target bitmap into which the file is decoded */
 
        /* Get the size of the ImageView */
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight(); 
        /* Set bitmap options to scale the image decode target */
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
//        bmOptions.inPurgeable = true;
        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        /* Get the size of the image */
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        int tempW = photoW;
        int tempH = photoH;
        if (photoW > photoH){
            tempW = photoH;
            tempH = photoW;
        }
        int inSampleSize = 1;
        if (tempH > targetH || tempW > targetW){
            final int halfHeight = tempH / 2;
            final int halfWidth = tempW / 2;
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width smaller than the requested height and width.
            while ((halfHeight / inSampleSize) > targetH
                    && (halfWidth / inSampleSize) > targetW) {
                inSampleSize *= 2;
            }
        }
        /* Decode the JPEG file into a Bitmap */
        bmOptions.inSampleSize = inSampleSize;
        bmOptions.inJustDecodeBounds= false;
        bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        
        /* Rotate if the width is greater than the height*/
        if (photoW > photoH){
            Matrix matrix = new Matrix();
            matrix.postRotate(270);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap = rotatedBitmap; 
        }
        float output_width = bitmap.getWidth();
        float output_height = bitmap.getHeight();
        float aspect_ratio_pic = output_height/output_width;
        float aspect_ratio_iv = mImageView.getHeight()/mImageView.getWidth();
        Log.v("set pic","output width = "+Float.toString(output_width));
        Log.v("set pic","output height = "+Float.toString(output_height));
        mCurrentBitmap = bitmap; 
    	int objectiveX;
    	int objectiveY;
        if (aspect_ratio_pic>aspect_ratio_iv){
        	scale = mImageView.getHeight()/output_height;
        	Log.d("scale",Float.toString(scale));
 	        bitmap_x_min = ( mImageView.getWidth()-scale*(output_width))/2;
        	bitmap_x_max = mImageView.getWidth()-bitmap_x_min;
        	bitmap_y_min=0;
        	bitmap_y_max=mImageView.getHeight();
        	objectiveX = (int) (scale*(output_width));
        	objectiveY = mImageView.getHeight();
        }
        else{
        	scale = mImageView.getWidth()/output_width;
        	Log.d("scale",Float.toString(scale));
 	        bitmap_y_min = (mImageView.getHeight()-scale*(output_height))/2;
        	bitmap_y_max = mImageView.getHeight()-bitmap_y_min; 
        	bitmap_x_min=0;
        	bitmap_x_max=mImageView.getWidth();
        	objectiveY = (int) (scale*(output_height));
        	objectiveX = mImageView.getWidth();
        }
        /* Associate the Bitmap to the ImageView */
        mCurrentBitmap = Bitmap.createScaledBitmap(bitmap, objectiveX, objectiveY, false);
        mImageView.setImageBitmap(bitmap);//scaledBitmap
        mImageView.setVisibility(View.VISIBLE);
        PHOTO_SET = true;
    }
	
	public void placePoint(float x_coord, float y_coord) {
		Log.d("point", "painting point ("+Float.toString(x_coord)+", "+Float.toString(y_coord)+")");
		Log.d("point", "mCurrentBitmap==null :"+(mCurrentBitmap==null));
		
		if (mCurrentBitmap==null)
			mCurrentBitmap = Bitmap.createBitmap(mImageView.getWidth(), mImageView.getHeight(),Bitmap.Config.ARGB_8888);
				
		Bitmap mutableBitmap = mCurrentBitmap.copy(Bitmap.Config.ARGB_8888, true);
		Canvas canvas = new Canvas(mutableBitmap);
		if (PHOTO_SET){
//			x_coord = x_coord/scale;
//			y_coord = y_coord/scale;
		}
		canvas.drawPoint(x_coord, x_coord, BLACK_PAINT);
		canvas.drawCircle(x_coord, y_coord, 3, BLACK_PAINT);
		Log.d("point","drawing ("+Float.toString(x_coord)+", "+Float.toString(y_coord)+")");
		mCurrentBitmap = mutableBitmap;
		mImageView.setImageBitmap(mutableBitmap);
	}

	public Bitmap drawPolynomial(View v){
		float offset = 4;
		float y_invert = (float) mImageView.getHeight();
		if (mCurrentBitmap==null){
			Toast.makeText(getBaseContext(), R.string.warning2, Toast.LENGTH_SHORT).show();
			return null;
		}
		Bitmap newBitmap = Bitmap.createBitmap(mCurrentBitmap);
		Canvas canvas = new Canvas(newBitmap);

		mPath = new Path();
		float startX = mInterpolator.f_visual(0).x;
		float startY = y_invert - mInterpolator.f_visual(0).y;
		mPath.moveTo(startX, startY);
		
		float nextX = 0;
		float nextY = 0;
		for (int i = 1; i < STEP_SIZE; i++){
			nextX = mInterpolator.f_visual(i).x;
			nextY = y_invert - mInterpolator.f_visual(i).y;
			mPath.lineTo(nextX, nextY);
		}
		for (int j = STEP_SIZE-1; j>=0; j--){
			nextX = mInterpolator.f_visual(j).x;
			nextY = y_invert - mInterpolator.f_visual(j).y - offset;
			mPath.lineTo(nextX, nextY);
		}
		canvas.drawPath(mPath, BLACK_PAINT);
		mCurrentBitmap = newBitmap;
		return newBitmap;
	}
	
	private void paintArea(float a, float b){
		if (!mInterpolator.isSolved())
			return;
		float y = (float) mImageView.getHeight();
		
		Bitmap newBitmap = Bitmap.createBitmap(mCurrentBitmap);
		Canvas canvas = new Canvas(newBitmap);
		if (PHOTO_SET){
			a+=1;
			y-=6;
		}
		mPath = new Path();
		mPath.moveTo(a, y);
		mPath.lineTo(a, mInterpolator.f(a));
		float n = 1/(float)STEP_SIZE;
		for (int i=0;i<STEP_SIZE*(b-a);i++)
			mPath.lineTo(a+i*n, y-mInterpolator.f(a+i*n));
		mPath.lineTo(b, y);
		mPath.lineTo(a, y);
		canvas.clipPath(mPath);
		canvas.drawRGB(0x01, 0x01, 0x01);
		mCurrentBitmap = newBitmap;
		mImageView.setImageBitmap(newBitmap);
	}
}