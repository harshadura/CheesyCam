package com.durapix.cheesycam;

import static android.widget.Toast.makeText;
import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognitionService;
import android.util.Log;
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

public class MainActivity extends Activity implements RecognitionListener {
	private Camera mCamera;
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
	public void onPartialResult(Hypothesis hypothesis) {
		String text = hypothesis.getHypstr();
		if (text.equals(KEYPHRASE)) {
			// stopService(new Intent(this, RecognitionService.class));

			Log.d("CAM", "Captured");
			Log.d("CAM", "Captured at " + imagePath);
			mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);

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
		mCamera = getCameraInstance();
		mCamera.setDisplayOrientation(90);
		mCamera.startPreview();
		preview.setCamera(mCamera);
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
			FileOutputStream outStream = null;
			try {
				// Write to SD Card
				fileName = String.format("/sdcard/camtest/%d.jpg",
						System.currentTimeMillis());
				outStream = new FileOutputStream(fileName);
				outStream.write(data);
				outStream.close();
				Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length);

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
}
