package com.techionsoft.wavingflag;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class WavingFlagActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		// Set activity to Full-screen
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// Create an OpenGL view.
		GLSurfaceView view = new GLSurfaceView(this);

		// Creating and attaching the renderer.
		Bitmap flagBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.flag);
		WavingFlagRenderer renderer = new WavingFlagRenderer(flagBitmap);
		view.setRenderer(renderer);
		setContentView(view);
    }
}