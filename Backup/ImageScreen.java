package com.daniel.visualanalysis;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.daniel.visualanalysis.AlbumStorageDirFactory;
import com.daniel.visualanalysis.R;
import com.daniel.visualanalysis.BaseAlbumDirFactory;
import com.daniel.visualanalysis.FroyoAlbumDirFactory;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class ImageScreen extends Activity {
		
	public static final int ACTION_TAKE_PHOTO = 1;
	public static final int ACTION_LOAD_PHOTO = 2;
	public static final int SETTINGS = 3;
	
	public ImageView mImageView;
	
	private String mCurrentPhotoPath;
	private File mCurrentPhotoFile; 
	private Uri mCurrentPhotoUri;

//	private static final String JPEG_FILE_PREFIX = "IMG_";
//	private static final String JPEG_FILE_SUFFIX = ".jpg";
	private static final String TAG = "ImageScreen";
	
//	private AlbumStorageDirFactory mAlbumStorageDirFactory = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); // use creator from the inherited class
		setContentView(R.layout.image_screen);
		
		mImageView = (ImageView) findViewById(R.id.imageView1);
		Button picBtn = (Button) findViewById(R.id.takePhotoButton);
		Button loadBtn = (Button) findViewById(R.id.openFileButton);
		
		/* Alternative way to set the listeners: avoids setBtnListenerOrDisable methods
		 */
		picBtn.setOnClickListener(mTakePicOnClickListener);
		loadBtn.setOnClickListener(mLoadPicOnClickListener);
		
//		setBtnListenerOrDisable(picBtn,mTakePicOnClickListener,MediaStore.ACTION_IMAGE_CAPTURE);
//		setBtnListenerOrDisable(loadBtn,mLoadPicOnClickListener,Intent.ACTION_PICK);
		
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
//			mAlbumStorageDirFactory = new FroyoAlbumDirFactory();
//		} else {
//			mAlbumStorageDirFactory = new BaseAlbumDirFactory();
//		}	
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.image_screen, menu);
		return true;
	}

//=====================================================FILE MANAGMENT===========
	
//	@SuppressLint("SimpleDateFormat") 
//	private File createImageFile() throws IOException {
//		// Create an image file name
//		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//		String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
//		File albumF = getAlbumDir();
//		File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
//		return imageF;
//	}
//
//	private File getAlbumDir() {
//		File storageDir = null;
//		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {		
//			storageDir = mAlbumStorageDirFactory.getAlbumStorageDir(getAlbumName());
//			if (storageDir != null) {
//				if (! storageDir.mkdirs()) {
//					if (! storageDir.exists()){
//						Log.d("CameraSample", "failed to create directory");
//						return null;
//					}
//				}
//			}	
//		} else {
//			Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
//		}
//		return storageDir;
//	}
//	
//	/* Photo album for this application */
//	private String getAlbumName() {
//		return getString(R.string.album_name);
//	}
	
//=================================================END FILE MANAGMENT===========
	
	Button.OnClickListener mTakePicOnClickListener = new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			
			// Creating file to save the image.
			File cacheFolder = Environment.getExternalStorageDirectory();
			cacheFolder.mkdir();
			mCurrentPhotoFile = new File(cacheFolder, "tempImage_001");
			mCurrentPhotoPath = mCurrentPhotoFile.getAbsolutePath();
			takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mCurrentPhotoFile));

			startActivityForResult(takePictureIntent, ACTION_TAKE_PHOTO);
		}
	};
	
	Button.OnClickListener mLoadPicOnClickListener = new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent loadPictureIntent = new Intent(Intent.ACTION_PICK,
					android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//			loadPictureIntent.setType("image/*");
			
			startActivityForResult(loadPictureIntent, ACTION_LOAD_PHOTO);
		}
	};
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ACTION_TAKE_PHOTO:
			switch (resultCode){
			case Activity.RESULT_OK:
				if (mCurrentPhotoPath != null) {
					setPic();
					galleryAddPic();
					mCurrentPhotoPath = null;
				}
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
				mCurrentPhotoUri = data.getData();
				Bitmap image;
				try {
					image = MediaStore.Images.Media.getBitmap(getContentResolver(), mCurrentPhotoUri);
					BitmapDrawable bmp = new BitmapDrawable(this.getResources(), image);
					mImageView.setImageDrawable(bmp);
				} catch (IOException e) {
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
		
	private void setPic() {
		/* There isn't enough memory to open up more than a couple camera photos */
		/* So pre-scale the target bitmap into which the file is decoded */

		/* Get the size of the ImageView */
		int targetW = mImageView.getWidth();
		int targetH = mImageView.getHeight();

		/* Get the size of the image */
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		int photoW = bmOptions.outWidth;
		int photoH = bmOptions.outHeight;
		
		/* Figure out which way needs to be reduced less */
		int scaleFactor = 1;
		if ((targetW > 0) || (targetH > 0)) {
			scaleFactor = Math.min(photoW/targetW, photoH/targetH);	
		}

		/* Set bitmap options to scale the image decode target */
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;

		/* Decode the JPEG file into a Bitmap */
		Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		
		/* Associate the Bitmap to the ImageView */
		mImageView.setImageBitmap(bitmap);
		mImageView.setVisibility(View.VISIBLE);
	}
	
	private void galleryAddPic() {
	    Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
		File f = mCurrentPhotoFile;
	    Uri contentUri = Uri.fromFile(f);
	    mediaScanIntent.setData(contentUri);
	    this.sendBroadcast(mediaScanIntent);
	}

//============================================================ BUTTON LISTENER =================
	/**
	 * Indicates whether the specified action can be used as an intent. This
	 * method queries the package manager for installed packages that can
	 * respond to an intent with the specified action. If no suitable package is
	 * found, this method returns false.
	 * http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html
	 *
	 * @param context The application's environment.
	 * @param action The Intent action to check for availability.
	 *
	 * @return True if an Intent with the specified action can be sent and
	 *         responded to, false otherwise.
	 */
	public static boolean isIntentAvailable(Context context, String action) {
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list =
				packageManager.queryIntentActivities(intent,
						PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}
	public void setBtnListenerOrDisable(Button btn,Button.OnClickListener onClickListener,String intentName) {
		if (isIntentAvailable(this, intentName)) {
			btn.setOnClickListener(onClickListener);        	
		} else {
			btn.setText( 
					getText(R.string.cannot).toString() + " " + btn.getText());
			btn.setClickable(false);
		}
	}

}
