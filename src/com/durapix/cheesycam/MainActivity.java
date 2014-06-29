package com.durapix.cheesycam;

import static android.widget.Toast.makeText;
import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognitionService;
import android.util.Log;
import android.view.Surface;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

public class MainActivity extends Activity implements
        RecognitionListener {
	private Camera mCamera;
	private CameraPreview mCameraPreview;
    private static final String KWS_SEARCH = "wakeup";
    private static final String FORECAST_SEARCH = "forecast";
    private static final String DIGITS_SEARCH = "digits";
    private static final String MENU_SEARCH = "menu";
    private static final String KEYPHRASE = "cheese";
    private static String imagePath = "";
    
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

   	    mCamera = getCameraInstance();
		mCameraPreview = new CameraPreview(this, mCamera);
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.addView(mCameraPreview);
		
//		mCamera.takePicture(null, null, mPicture);
		
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task

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

/*	private Camera tryOpenFrontFacingCameraElseBackCamOpen() {
	    int cameraCount = 0;
	    Camera cam = null;
	    Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
	    cameraCount = Camera.getNumberOfCameras();
	    for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
	        Camera.getCameraInfo(camIdx, cameraInfo);
	        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
	            try {
	                cam = Camera.open(camIdx);
	                return cam;
	            } catch (RuntimeException e) {
	                 e.getLocalizedMessage();
	            }
	        }
	    }
	    

	    return cam;
	}
	*/
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE)){
        	stopService(new Intent(this, RecognitionService.class));
        	
//        	//Get Camera Params for customisation
//        	Camera.Parameters parameters = mCamera.getParameters();
//
//        	//Check Whether device supports AutoFlash, If you YES then set AutoFlash
//        	List<String> flashModes = parameters.getSupportedFlashModes();
//        	if (flashModes.contains(android.hardware.Camera.Parameters.FLASH_MODE_AUTO))
//        	{
//        	     parameters.setFlashMode(Parameters.FLASH_MODE_AUTO);
//        	}
//        	mCamera.setParameters(parameters);
//        	mCamera.startPreview();
        	
//            mCamera = getCameraInstance();
//     		mCameraPreview = new CameraPreview(this, mCamera, true);
//     		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
//     		preview.addView(mCameraPreview);
        	
     		mCamera.takePicture(null, null, mPicture);

        	Log.d("CAM", "Captured");
        	Log.d("CAM", "Captured at " + imagePath);
        	
    
//        	Intent i=new Intent(PocketSphinxActivity.this, Custom_CameraActivity.class);
//            finish();
//            stopService(new Intent(this, RecognitionService.class));
//            startActivity(i);
        }
//            switchSearch(MENU_SEARCH);
        else if (text.equals(DIGITS_SEARCH))
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
    public void onDestroy()
    {

if(mCamera!=null){
	mCamera.stopPreview();
	mCamera.setPreviewCallback(null);

	mCamera.release();
	mCamera = null;
        }


        super.onDestroy();
    }
    
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
		                CameraPreview.camId= camIdx;
//		                setCameraDisplayOrientation(this, camIdx, cam);
		                return cam;
		            } catch (RuntimeException e) {
		                 e.getLocalizedMessage();
		            }
		        }
		    }
		    
		   cam = Camera.open();
		   int now=0 ;
		   cameraCount = Camera.getNumberOfCameras();
		    for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
		        now = camIdx;
		    }
		    
		    CameraPreview.camId= now;

//		   setCameraDisplayOrientation(this, cameraCount, cam);

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return cam;
	}

	PictureCallback mPicture = new PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			File pictureFile = getOutputMediaFile();
			imagePath =  pictureFile.getAbsolutePath();
		
			
			if (pictureFile == null) {
				return;
			}
			try {
				FileOutputStream fos = new FileOutputStream(pictureFile);
				fos.write(data);
				fos.close();
			} catch (FileNotFoundException e) {

			} catch (IOException e) {
			}
		}
	};

	private static File getOutputMediaFile() {
		File mediaStorageDir = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				"MyCameraApp");
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				Log.d("MyCameraApp", "failed to create directory");
				return null;
			}
		}
		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());
		File mediaFile;
		mediaFile = new File(mediaStorageDir.getPath() + File.separator
				+ "IMG_" + timeStamp + ".jpg");

		return mediaFile;
	}
	
	@Override
    public void onBackPressed(){
//		mCamera.stopPreview();
//		mCameraPreview.setCamera(null);
//		mCamera.release();
//		mCamera = null;
//           super.onBackPressed();
    }
	
//	@Override
//	protected void onPause() {
//		if(mCamera != null) {
//			mCamera.stopPreview();
////			mCameraPreview.setCamera(null);
//			mCamera.release();
//		}
//		super.onPause();
//	}
	

}
