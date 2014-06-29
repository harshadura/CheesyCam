package com.durapix.cheesycam;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Build;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

public class CameraPreview extends SurfaceView implements
		SurfaceHolder.Callback {
	private SurfaceHolder mSurfaceHolder;
	private Camera mCamera;
	public static int camId;
	
	// Constructor that obtains context and camera
	@SuppressWarnings("deprecation")
	public CameraPreview(Context context, Camera camera) {
		super(context);
		this.mCamera = camera;
		this.mSurfaceHolder = this.getHolder();
		this.mSurfaceHolder.addCallback(this);
		this.mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	// Constructor that obtains context and camera
	@SuppressWarnings("deprecation")
	public CameraPreview(Context context, Camera camera, boolean flag) {
		super(context);
		this.mCamera = camera;
		this.mSurfaceHolder = this.getHolder();
		this.mSurfaceHolder.addCallback(this);
		this.mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	
		//Get Camera Params for customisation
		Camera.Parameters parameters = mCamera.getParameters();
		//Check Whether device supports AutoFlash, If you YES then set AutoFlash
		List<String> flashModes = parameters.getSupportedFlashModes();
		if (flashModes.contains(android.hardware.Camera.Parameters.FLASH_MODE_TORCH))
		{
		     parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
		}
		mCamera.setParameters(parameters);
		
	}
	
	public static boolean hasGingerbread() {
return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
}
	
	@Override
	public void surfaceCreated(SurfaceHolder surfaceHolder) {
		try {
			if(mCamera!=null){
				mCamera.setPreviewDisplay(surfaceHolder);
				mCamera.startPreview();
			}
		} catch (IOException e) {
			// left blank for now
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
		
		if(mCamera!=null){
			
			//Get Camera Params for customisation
			Camera.Parameters parameters = mCamera.getParameters();
			//Check Whether device supports AutoFlash, If you YES then set AutoFlash
			List<String> flashModes = parameters.getSupportedFlashModes();
			if (flashModes.contains(android.hardware.Camera.Parameters.FLASH_MODE_OFF))
			{
			     parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
			}
			mCamera.setParameters(parameters);
			
			mCamera.stopPreview();
            mCamera.setPreviewCallback(null);

            mCamera.release();
            mCamera = null;
        }
	}

	@Override
	public void surfaceChanged(SurfaceHolder surfaceHolder, int format,
			int width, int height) {
		// start preview with new settings
		try {
			mCamera.setPreviewDisplay(surfaceHolder);
			mCamera.startPreview();
			setCameraDisplayOrientation(camId, mCamera);
		} catch (Exception e) {
			// intentionally left blank for a test
		}
	}
	
	    public void setCameraDisplayOrientation(int cameraId, android.hardware.Camera camera) {
	         int rotation = ((WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
	                 .getRotation();
	         int degrees = 0;
	         switch (rotation) {
	             case Surface.ROTATION_0: degrees = 0; break;
	             case Surface.ROTATION_90: degrees = 90; break;
	             case Surface.ROTATION_180: degrees = 180; break;
	             case Surface.ROTATION_270: degrees = 270; break;
	         }

	         int result;
	         if (hasGingerbread()) {
	             android.hardware.Camera.CameraInfo info =
	                     new android.hardware.Camera.CameraInfo();
	             android.hardware.Camera.getCameraInfo(cameraId, info);
	             if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
	                 result = (info.orientation + degrees) % 360;
	                 result = (360 - result) % 360;  // compensate the mirror
	             } else {  // back-facing
	                 result = (info.orientation - degrees + 360) % 360;
	             }
	         } else {
	             // on API 8 and lower devices
	             if (getContext().getResources().getConfiguration().orientation !=Configuration.ORIENTATION_LANDSCAPE) {
	                 result = 90;
	             } else {
	                 result = 0;
	             }
	         }
	         try {
	             camera.setDisplayOrientation(result);
	         } catch (Exception e) {
	             // may fail on old OS versions. ignore it.
	             e.printStackTrace();
	         }
	     }
}