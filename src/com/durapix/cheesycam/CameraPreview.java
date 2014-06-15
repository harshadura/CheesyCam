package com.durapix.cheesycam;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreview extends SurfaceView implements
		SurfaceHolder.Callback {
	private SurfaceHolder mSurfaceHolder;
	private Camera mCamera;

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
	

	
	@Override
	public void surfaceCreated(SurfaceHolder surfaceHolder) {
		try {
//			if(mCamera!=null){
				mCamera.setPreviewDisplay(surfaceHolder);
				mCamera.startPreview();
//			}
		} catch (IOException e) {
			// left blank for now
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
		
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
		mCamera.release();
	}

	@Override
	public void surfaceChanged(SurfaceHolder surfaceHolder, int format,
			int width, int height) {
		// start preview with new settings
		try {
			mCamera.setPreviewDisplay(surfaceHolder);
			mCamera.startPreview();
		} catch (Exception e) {
			// intentionally left blank for a test
		}
	}
}