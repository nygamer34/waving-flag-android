package com.techionsoft.wavingflag;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.os.SystemClock;
import android.util.Log;

public class Flag {
	/**
	 * Constants - Modify to suit your needs
	 */
	
	private static final boolean IS_WIREFRAME_VISIBLE = false;  //Should enable lines (wireframe) - for testing
	private static final boolean ISPRINTFPS = true;  //Should print FPS - for testing
	
	private static final int m = 45; // horizontal coords; No.of cols = (m-1);
	private static final int n = 45; // vertical coords; No.of rows = (n-1);
	
	private static final float hPercent = 0.66f; // Height of the texture as a percentage of the bitmap
	private static final float wPercent = 1.0f;  // Width of the texute as a percentage of the bitmap

	/**
	 * Variables
	 */
	
	float[][][] points = new float[m][n][3]; // The Array For The Points On The
											 // Grid Of Our "Wave"
	int wiggle_count = 0; // Counter Used To Control How Fast Flag Waves
	float xrot;           // X Rotation
	float yrot;           // Y Rotation
	float zrot;           // Z Rotation
	float hold;
	int[] textures = new int[1]; // Storage for one texture

	//Buffers for Vertices and Textures
	FloatBuffer mVerticesBuffer;
	FloatBuffer mTextureBuffer;

	// Make some calculations for triangle strips
	int trianglesInARow = (m - 1) * 2;  //The number of triangles in a row = no.of columns x 2
	int verticesOfTrianglesInARow = (trianglesInARow - 1) + 3; //Vertices for the triangle strip 

	float mVertices[] = new float[(((n - 1) * verticesOfTrianglesInARow) + (n - 2) * 2) * 3];
	float mTextures[] = new float[(((n - 1) * verticesOfTrianglesInARow) + (n - 2) * 2) * 2];

	// FPS stuff
	long time, frameTime, fpsTime;
	short avgFPS, framerate;

	//Stores the flag bitmap
	Bitmap mBitmap;



	/**
	 * Constructor
	 * @param bitmap the bitmap to be used as the texture for the flag
	 */
	public Flag(Bitmap bitmap) {
		mBitmap = bitmap;
		ByteBuffer vbb = ByteBuffer.allocateDirect(mVertices.length * 4);
		vbb.order(ByteOrder.nativeOrder());
		mVerticesBuffer = vbb.asFloatBuffer();

		ByteBuffer byteBuf = ByteBuffer.allocateDirect(mTextures.length * 4);
		byteBuf.order(ByteOrder.nativeOrder());
		mTextureBuffer = byteBuf.asFloatBuffer();
	}

	/**
	 * Draw a frame of this animation.
	 * @param gl The GL10 object to be used for the drawing
	 * @param surfaceWidth Width of the surface on which the frame is drawm
	 * @param surfaceHeight Height of the surface on which the frame is drawn
	 */
	public void drawFrame(GL10 gl, int surfaceWidth, int surfaceHeight) {
        int x, y;

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);	// Clear The Screen And The Depth Buffer
        gl.glLoadIdentity();									// Reset The View
        gl.glClearColor(0f, 0f, 0f, 0f);               //This Will Clear The Background Color To Black

        gl.glTranslatef(0.0f, 0.0f, -12.0f);

        gl.glRotatef(xrot, 1.0f, 0.0f, 0.0f);
        gl.glRotatef(yrot, 0.0f, 1.0f, 0.0f);
        gl.glRotatef(zrot, 0.0f, 0.0f, 1.0f);
////////////////////////
		if(IS_WIREFRAME_VISIBLE){
			gl.glColor4f(1f, 1f, 1f, 1f);
			gl.glDisable(GL10.GL_TEXTURE_2D);
		}
//////////////////	        
		gl.glDrawArrays (GL10.GL_TRIANGLE_STRIP, 0, mVertices.length/3);
////////////////////////
		if(IS_WIREFRAME_VISIBLE){
			gl.glEnable(GL10.GL_BLEND);
			gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
			gl.glLineWidth(1.0f);
			gl.glColor4f(1f, 0f, 0f, 1.0f);
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVerticesBuffer);
			gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, mVertices.length/3);
			gl.glDisable(GL10.GL_BLEND);
			gl.glColor4f(1f, 1f, 1f, 1f);
			gl.glDisable(GL10.GL_TEXTURE_2D);
		}
//////////////////		
		//Right shift the co-ordinates of the column
		// to achieve a waving effect
        if (wiggle_count == 2) {
            for (y = 0; y < n; y++) {
                hold = points[0][y][2];
                for (x = 0; x < m -1; x++) {
                    points[x][y][2] = points[x + 1][y][2];
                }
                points[m-1][y][2] = hold;
            }
            wiggle_count = 0;
            populateVertexBuffer(gl);
        }

        wiggle_count++;

        //Values for rotating the frame along the x,y and z axes
        xrot += 0.3f;
        yrot += 0.2f;
        zrot += 0.4f;
        
		printFPS();		
	}

	/**
	 * Initialize the values
	 * @param gl
	 */
	public void onCreate(GL10 gl){
        try {
            loadGLTextures(gl);
        } catch (IOException e) {
            System.out.println("Failed to load Textures, Bailing!");
            throw new RuntimeException(e);
        }

        gl.glEnable(GL10.GL_TEXTURE_2D);						          // Enable Texture Mapping ( NEW )
        gl.glShadeModel(GL10.GL_SMOOTH);                            //Enables Smooth Color Shading
        gl.glClearColor(0, 0, 0, 0);               //This Will Clear The Background Color To Black
        gl.glClearDepthf(1.0f);                                  //Enables Clearing Of The Depth Buffer
        gl.glEnable(GL10.GL_DEPTH_TEST);                            //Enables Depth Testing
        gl.glDepthFunc(GL10.GL_LEQUAL);                             //The Type Of Depth Test To Do
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);  // Really Nice Perspective Calculations

        //Generate the x,y and z co-ordinates for the quad!! (This is an adaptation of the NeHe waving flag algorithm)
        //  QUADs are not supported in Android (OpenGL ES). So it will have to be converted to triangle strips.
        //    this is done in populateVerterBuffer method and the texture mapping in populateTexBuffer method
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
            	points[x][y][0] = ((x / 5.0f) - 4.5f) * wPercent;
                points[x][y][1] = ((y / 5.0f) - 4.5f) * hPercent;
                points[x][y][2] = (float) (Math.sin((((x / 5.0f) * 40.0f) / 360.0f) * 3.141592654 * 2.0f));
            }
        }
        gl.glDisable(GL10.GL_DEPTH_TEST);  
        gl.glDisable(GL10.GL_TEXTURE_2D);	
		populateVertexBuffer(gl);
		populateTexBuffer(gl);		
		
 	}
	public void initialize(GL10 gl, int surfaceWidth, int surfaceHeight) {
	     if (surfaceHeight == 0){
	    	 surfaceHeight = 1;
	     }
			
        gl.glViewport(0, 0, surfaceWidth, surfaceHeight);        // Reset The Current Viewport And Perspective Transformation
        gl.glMatrixMode(GL10.GL_PROJECTION);                     // Select The Projection Matrix
        gl.glLoadIdentity();                                     // Reset The Projection Matrix
        GLU.gluPerspective(gl, 45.0f, (float) surfaceWidth / (float) surfaceHeight, 0.1f, 100.0f);  // Calculate The Aspect Ratio Of The Window
        gl.glMatrixMode(GL10.GL_MODELVIEW);                      // Select The Modelview Matrix
        gl.glLoadIdentity();                                     // Reset The ModalView Matrix		
	}
    /**
     * Populates the vertex array with the co-ordinates of the triangle strip vertices
     * @param gl the GL10 object to be used for drawing
     */
    private void populateVertexBuffer(GL10 gl){
    	
    	int curVIndex = 0;
    	for(int y =0; y < n-1; y++){
    		mVertices[curVIndex++] = points[0][y][0];
    		mVertices[curVIndex++] = points[0][y][1];
    		mVertices[curVIndex++] = points[0][y][2];
    		if(y>0){
    			//create degenerate vertex
        		mVertices[curVIndex++] = points[0][y][0];
        		mVertices[curVIndex++] = points[0][y][1];
        		mVertices[curVIndex++] = points[0][y][2];  
    		}
    		for(int x = 0; x < m- 1; x++){
    			mVertices[curVIndex++] = points[x][y+1][0];
        		mVertices[curVIndex++] = points[x][y+1][1];
        		mVertices[curVIndex++] = points[x][y+1][2];
        		mVertices[curVIndex++] = points[x+1][y][0];
        		mVertices[curVIndex++] = points[x+1][y][1];
        		mVertices[curVIndex++] = points[x+1][y][2];    
        
    		}
    		mVertices[curVIndex++] = points[m-1][y+1][0];
    		mVertices[curVIndex++] = points[m-1][y+1][1];
    		mVertices[curVIndex++] = points[m-1][y+1][2]; 

    		if(y < n-2){
    			//create another degenerate vertex
        		mVertices[curVIndex++] = points[m-1][y+1][0];
        		mVertices[curVIndex++] = points[m-1][y+1][1];
        		mVertices[curVIndex++] = points[m-1][y+1][2];

    		}
    	}
    	gl.glEnableClientState (GL10.GL_VERTEX_ARRAY);
    	mVerticesBuffer.put(mVertices);
		mVerticesBuffer.position(0);
        gl.glVertexPointer (3, GL10.GL_FLOAT, 0, mVerticesBuffer); 
    }
    
    /**
     * Populate the texture array with the co-ordinates of the texture corresponding to the vertices
     * @param gl the GL10 object to be used for drawing
     */
    private void populateTexBuffer(GL10 gl){
    	int curTIndex = 0;
    	float float_x =0, float_y=0, float_xb=0, float_yb=0;
    	for(int y =0; y < n-1; y++){
    		float_y = (float) (y) / (n-1);
    		float_y *= hPercent;
    		float_yb = (float) (y + 1) / (n-1);
    		float_yb *= hPercent;
    		
    		mTextures[curTIndex++] = 0;
    		mTextures[curTIndex++] = float_y;
    		if(y>0){
    			//create degenerate vertex
    			mTextures[curTIndex++] = 0;
        		mTextures[curTIndex++] = float_y;
    		}
    		for(int x = 0; x < m- 1; x++){
    			 float_x = (float) (x) / (m -1);
    			 float_x *= wPercent;
    			 float_xb = (float) (x + 1) / (m-1);
    			 float_xb *= wPercent;
        		mTextures[curTIndex++] = float_x;
        		mTextures[curTIndex++] = float_yb;
        		mTextures[curTIndex++] = float_xb;
        		mTextures[curTIndex++] = float_y;
    		}
    		mTextures[curTIndex++] = float_xb;
    		mTextures[curTIndex++] = float_yb;
    		if(y < n-2){
    			//create another degenerate vertex
        		mTextures[curTIndex++] = float_xb;
        		mTextures[curTIndex++] = float_yb;

    		}
    	}   
		gl.glEnable(GL10.GL_TEXTURE_2D);
		// Enable the texture state
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);  
		mTextureBuffer.put(mTextures);
		mTextureBuffer.position(0);
		// Point to our buffers
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTextureBuffer); 
    }
    /**
     * Loads the texture from the bitmap
     * @param gl
     * @throws IOException
     */
    private void loadGLTextures(GL10 gl) throws IOException {

        //Create Nearest Filtered Texture
        gl.glGenTextures(1, textures, 0);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);

		// Use the Android GLUtils to specify a two-dimensional texture image
		// from our bitmap
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, mBitmap, 0);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
    }
	/**
	 * Prints the Frames-Per-Second to LogCat every 3 seconds
	 */
	private void printFPS(){
		if(!ISPRINTFPS){
			return;
		}
		time = SystemClock.uptimeMillis();
		if (time >= (frameTime + 1000.0f)) {
			frameTime = time;
			avgFPS += framerate;
			framerate = 0;
		}
		if (time >= (fpsTime + 3000.0f)) {
			fpsTime = time;
			avgFPS /= 3.0f;
			Log.d("FPS: ", Float.toString(avgFPS));
			avgFPS = 0;
		}
		framerate++;
	}
}
