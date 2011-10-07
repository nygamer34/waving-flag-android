package com.techionsoft.wavingflag;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.opengl.GLSurfaceView.Renderer;

public class WavingFlagRenderer implements Renderer  {

	Flag flag;
	private int surfaceWidth;
	private int surfaceHeight;

	public WavingFlagRenderer(Bitmap bitmap){
		flag = new Flag(bitmap);
		
	}
	@Override
	public void onDrawFrame(GL10 gl) {
		flag.drawFrame(gl, surfaceWidth, surfaceHeight);

	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		flag.initialize(gl, width, height);
		this.surfaceWidth = width;
		this.surfaceHeight = height;

	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		flag.onCreate(gl);

	}

}
