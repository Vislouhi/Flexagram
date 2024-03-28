package org.flexatar;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.google.android.exoplayer2.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class FlexatarViewRenderer implements GLSurfaceView.Renderer{
    public FlxDrawer drawer;
    public boolean isRounded = false;
    private int width;
    private int height;

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        Log.d("FLX_INJECT","onSurfaceCreated ");
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        this.width=width;
        this.height=height;
        GLES20.glViewport(0, 0, width, height);
        drawer.screenRatio = (float) width/ (float)height;
        drawer.setSize(width,height);
//        Log.d("FLX_INJECT","drawer.screenRatio "+drawer.screenRatio);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);


        if (isRounded)
            drawer.drawRounded();
        else
            drawer.draw();
    }
}
