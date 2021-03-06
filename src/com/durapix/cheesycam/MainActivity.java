package com.durapix.cheesycam;

import static android.widget.Toast.makeText;
import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognitionService;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

public class MainActivity extends Activity implements RecognitionListener , MediaScannerConnectionClient{
	private Camera mCamera;
	
    public String[] allFiles;
    private String SCAN_PATH ;
    private static final String FILE_TYPE = "*/*";
    private MediaScannerConnection conn;
    
	// private CameraPreview mCameraPreview;
	private static final String KWS_SEARCH = "wakeup";
	private static final String FORECAST_SEARCH = "forecast";
	private static final String DIGITS_SEARCH = "digits";
	private static final String MENU_SEARCH = "menu";
	private static final String KEYPHRASE = "cheese";
	private static String imagePath = "";
	private LinearLayout L1;
	private static final String TAG = "CamTestActivity";
	Preview preview;
	Button buttonClick;
	String fileName;
	Activity act;
	Context ctx;

	private SpeechRecognizer recognizer;
	private HashMap<String, Integer> captions;

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);

		// Prepare the data for UI
		captions = new HashMap<String, Integer>();
		captions.put(KWS_SEARCH, R.string.kws_caption);
		captions.put(MENU_SEARCH, R.string.menu_caption);
		captions.put(DIGITS_SEARCH, R.string.digits_caption);
		captions.put(FORECAST_SEARCH, R.string.forecast_caption);
		setContentView(R.layout.main);
		((TextView) findViewById(R.id.caption_text))
				.setText("Preparing the voice recognizer, Please wait a moment ..");

		L1 = (LinearLayout) findViewById(R.id.main_lay);
		makeText(getApplicationContext(), "Loading .. Please wait .. ", Toast.LENGTH_LONG).show();
		
		File folder = new File(Environment.getExternalStorageDirectory() + "/CheesyCam");
        allFiles = folder.list();
        SCAN_PATH=Environment.getExternalStorageDirectory().toString()+"/CheesyCam/"+allFiles[0];

//		  AdView adView = (AdView) this.findViewById(R.id.adView);
//		    AdRequest adRequest = new AdRequest.Builder().build();
//		    adView.loadAd(adRequest);
        
		// mCamera = getCameraInstance();
		/*
		 * mCameraPreview = new CameraPreview(this, mCamera); FrameLayout
		 * preview = (FrameLayout) findViewById(R.id.camera_preview);
		 * preview.addView(mCameraPreview);
		 */

		// mCamera.takePicture(null, null, mPicture);

		// Recognizer initialization is a time-consuming and it involves IO,
		// so we execute it in async task

		preview = new Preview(this,
				(SurfaceView) findViewById(R.id.surfaceView));
		preview.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		((FrameLayout) findViewById(R.id.preview)).addView(preview);
		preview.setKeepScreenOn(true);

		// buttonClick = (Button) findViewById(R.id.buttonClick);
		//
		// buttonClick.setOnClickListener(new OnClickListener() {
		// public void onClick(View v) {
		// // preview.camera.takePicture(shutterCallback, rawCallback,
		// jpegCallback);
		// camera.takePicture(shutterCallback, rawCallback, jpegCallback);
		// }
		// });

		new AsyncTask<Void, Void, Exception>() {
			@Override
			protected Exception doInBackground(Void... params) {
				try {
					Assets assets = new Assets(MainActivity.this);
					File assetDir = assets.syncAssets();
					setupRecognizer(assetDir);
				} catch (IOException e) {
					return e;
				}
				return null;
			}

			@Override
			protected void onPostExecute(Exception result) {
				if (result != null) {
					((TextView) findViewById(R.id.caption_text))
							.setText("Failed to init recognizer " + result);
				} else {
					switchSearch(KWS_SEARCH);
				}
			}
		}.execute();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection

		switch (item.getItemId()) {
		case R.id.menuitem_show: {

			try {
				dialogShowGalleryBuilder();
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return true;
		}
		case R.id.menuitem_aboutus: {
			dialogAboutBuilder();
			return true;
		}

		case R.id.menuitem_rateus: {
			rate();
			return true;
		}

		default:
			return super.onOptionsItemSelected(item);
		}

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return true;
	}

	public void dialogShowGalleryBuilder() {

		AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(this);
		myAlertDialog.setTitle("Show Photos");
		myAlertDialog
				.setMessage("Captured images are saved at:\n\n /sdcard/CheesyCam/ folder\n\n Do you want to open the Gallery?");
		myAlertDialog.setPositiveButton("OK",
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface arg0, int arg1) {
						finish();
						startScan();
					}
				});
		myAlertDialog.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface arg0, int arg1) {
						//do nothing!
					}
				});
		myAlertDialog.show();
	}
	
	public void dialogAboutBuilder() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("About");
		builder.setMessage(getString(R.string.aboutus));
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	public void rate() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri
				.parse("market://details?id="));
		startActivity(intent);
	}
	
	@Override
	public void onPartialResult(Hypothesis hypothesis) {
		String text = hypothesis.getHypstr();
		if (text.equals(KEYPHRASE)) {
			 stopService(new Intent(this, RecognitionService.class));

			Log.d("CAM", "Captured");
			Log.d("CAM", "Captured at " + imagePath);
			mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
			
			runOnUiThread(new Runnable() {
			    @Override
			    public void run() {
			    	makeText(getBaseContext(), "Photo saving at :) " + fileName, Toast.LENGTH_LONG).show();
			    }
			});
			
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
	// /
			// View v1 = L1.getRootView();
			// v1.setDrawingCacheEnabled(true);
			// Bitmap bm = v1.getDrawingCache();
			//
			// ByteArrayOutputStream stream = new ByteArrayOutputStream();
			// bm.compress(Bitmap.CompressFormat.PNG, 100, stream);
			// byte[] byteArray = stream.toByteArray();
			//
			// try {
			// FileOutputStream outStream = null;
			// // Write to SD Card
			// fileName = String.format("/sdcard/camtest/%d.jpg",
			// System.currentTimeMillis());
			// outStream = new FileOutputStream(fileName);
			// outStream.write(byteArray);
			// outStream.close();
			// Log.d(TAG, "onPictureTaken - wrote bytes: " + byteArray.length);
			//
			// resetCam();
			//
			// } catch (FileNotFoundException e) {
			// e.printStackTrace();
			// } catch (IOException e) {
			// e.printStackTrace();
			// } finally {
			// }
			// Log.d(TAG, "onPictureTaken - jpeg");
		} else if (text.equals(DIGITS_SEARCH))
			switchSearch(DIGITS_SEARCH);
		else if (text.equals(FORECAST_SEARCH))
			switchSearch(FORECAST_SEARCH);
		else
			((TextView) findViewById(R.id.result_text)).setText(text);
	}

	@Override
	public void onResult(Hypothesis hypothesis) {
		((TextView) findViewById(R.id.result_text)).setText("");
		if (hypothesis != null) {
			String text = hypothesis.getHypstr();
			makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onBeginningOfSpeech() {
	}

	@Override
	public void onEndOfSpeech() {
		if (DIGITS_SEARCH.equals(recognizer.getSearchName())
				|| FORECAST_SEARCH.equals(recognizer.getSearchName()))
			switchSearch(KWS_SEARCH);
	}

	private void switchSearch(String searchName) {
		recognizer.stop();
		recognizer.startListening(searchName);
		String caption = getResources().getString(captions.get(searchName));
		((TextView) findViewById(R.id.caption_text)).setText(caption);
	}

	private void setupRecognizer(File assetsDir) {
		File modelsDir = new File(assetsDir, "models");
		recognizer = defaultSetup()
				.setAcousticModel(new File(modelsDir, "hmm/en-us-semi"))
				.setDictionary(new File(modelsDir, "dict/cmu07a.dic"))
				.setRawLogDir(assetsDir).setKeywordThreshold(1e-20f)
				.getRecognizer();
		recognizer.addListener(this);

		// Create keyword-activation search.
		recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
		// Create grammar-based searches.
		File menuGrammar = new File(modelsDir, "grammar/menu.gram");
		recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);
		File digitsGrammar = new File(modelsDir, "grammar/digits.gram");
		recognizer.addGrammarSearch(DIGITS_SEARCH, digitsGrammar);
		// Create language model search.
		File languageModel = new File(modelsDir, "lm/weather.dmp");
		recognizer.addNgramSearch(FORECAST_SEARCH, languageModel);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// preview.camera = Camera.open();
		if (mCamera == null) {
			mCamera = getCameraInstance();
			mCamera.setDisplayOrientation(90);
			mCamera.startPreview();
			preview.setCamera(mCamera);
		}
	}

	@Override
	protected void onPause() {
		if (mCamera != null) {
			mCamera.stopPreview();
			preview.setCamera(null);
			mCamera.release();
			mCamera = null;
		}
		super.onPause();
	}

	private void resetCam() {
		mCamera.startPreview();
		preview.setCamera(mCamera);
	}

	ShutterCallback shutterCallback = new ShutterCallback() {
		public void onShutter() {
			// Log.d(TAG, "onShutter'd");
		}
	};

	PictureCallback rawCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			// Log.d(TAG, "onPictureTaken - raw");
		}
	};

	PictureCallback jpegCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			
			try {
				// Write to SD Card
				File folder = new File(Environment.getExternalStorageDirectory() + "/CheesyCam");
			    String path = folder.getPath();

			    if(!folder.mkdirs() || !folder.exists()){        
			            Log.e(TAG, path + " failed");
			        } else {
			            Log.d(TAG, path + " succeeded");
			        } 
			    
				DateFormat dateFormat = new SimpleDateFormat("MM-dd-HH-mm-ss");
				Calendar cal = Calendar.getInstance();
				String nowTime = dateFormat.format(cal.getTime());
				fileName = String.format(path + "/%s.jpg", "Che-" + nowTime);
				
				if (fileName!=null){
					FileOutputStream outStream = null;
					outStream = new FileOutputStream(fileName);
					outStream.write(data);
					outStream.close();
				}
			
				Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length);
			
				try {
					decodeFile(fileName);
				} catch (Exception e) {
				}
				
				resetCam();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
			}
			Log.d(TAG, "onPictureTaken - jpeg");
		}
	};

	private Camera getCameraInstance() {

		Camera cam = null;
		int cameraCount = 0;

		try {
			// try opening the front cam if exists!

			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			cameraCount = Camera.getNumberOfCameras();
			for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
				Camera.getCameraInfo(camIdx, cameraInfo);
				if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
					try {
						cam = Camera.open(camIdx);
//						CameraPreview.camId = camIdx;
						// setCameraDisplayOrientation(this, camIdx, cam);
						return cam;
					} catch (RuntimeException e) {
						e.getLocalizedMessage();
					}
				}
			}

			cam = Camera.open();
			int now = 0;
			cameraCount = Camera.getNumberOfCameras();
			for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
				now = camIdx;
			}

//			Preview.camId = now;

			// setCameraDisplayOrientation(this, cameraCount, cam);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return cam;
	}
	
	@Override
    public void onBackPressed() {
        Intent startAc = new Intent(this, MainActivity.class);
        this.startActivity(startAc);
        finish();
    }
	
	
	public  Bitmap decodeFile(String path) {//you can provide file path here 
	    int orientation;
	    try {
	        if (path == null) {
	            return null;
	        }
	        // decode image size 
	        BitmapFactory.Options o = new BitmapFactory.Options();
	        o.inJustDecodeBounds = true;
	        // Find the correct scale value. It should be the power of 2.
	        final int REQUIRED_SIZE = 70;
	        int width_tmp = o.outWidth, height_tmp = o.outHeight;
	        int scale = 0;
	        while (true) {
	            if (width_tmp / 2 < REQUIRED_SIZE
	                    || height_tmp / 2 < REQUIRED_SIZE)
	                break;
	            width_tmp /= 2;
	            height_tmp /= 2;
	        scale++;
	        }
	        // decode with inSampleSize
	        BitmapFactory.Options o2 = new BitmapFactory.Options();
	        o2.inSampleSize = scale;
	        Bitmap bm = BitmapFactory.decodeFile(path, o2);
	        Bitmap bitmap = bm;

	        ExifInterface exif = new ExifInterface(path);

	        orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);

	        Log.e("ExifInteface .........", "rotation ="+orientation);

	        //exif.setAttribute(ExifInterface.ORIENTATION_ROTATE_90, 90);

	        Log.e("orientation", "" + orientation);
	        Matrix m = new Matrix();

	        if ((orientation == ExifInterface.ORIENTATION_ROTATE_180)) {
	            m.postRotate(180);
	            //m.postScale((float) bm.getWidth(), (float) bm.getHeight());
	            // if(m.preRotate(90)){
	            Log.e("in orientation", "" + orientation);
	            bitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(),bm.getHeight(), m, true);
	            return bitmap;
	        } else if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
	            m.postRotate(90); 
	            Log.e("in orientation", "" + orientation);
	            bitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(),bm.getHeight(), m, true);
	            return bitmap;
	        }
	        else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
	            m.postRotate(270);
	            Log.e("in orientation", "" + orientation);
	            bitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(),bm.getHeight(), m, true);
	            return bitmap;
	        } 
	        return bitmap;
	    } catch (Exception e) {
	        return null;
	    }
	}
	
	  private void startScan()
	    {
	        if(conn!=null)
	        {
	            conn.disconnect();
	        }

	        conn = new MediaScannerConnection(this, this);
	        conn.connect();
	    }


	    public void onMediaScannerConnected()
	    {
	        conn.scanFile(SCAN_PATH, FILE_TYPE);    
	    }


	    public void onScanCompleted(String path, Uri uri)
	    {
	        try
	        {
	            if (uri != null) 
	            {
	                Intent intent = new Intent(Intent.ACTION_VIEW);
	                intent.setData(uri);
	                startActivity(intent);
	            }
	        }
	        finally 
	        {
	            conn.disconnect();
	            conn = null;
	        }
	    }
}
