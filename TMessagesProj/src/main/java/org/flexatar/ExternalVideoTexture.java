package org.flexatar;

import android.graphics.SurfaceTexture;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import org.flexatar.DataOps.AssetAccess;
import org.flexatar.DataOps.FlexatarData;
import org.flexatar.DataOps.FlexatarVData;
import org.flexatar.DataOps.LengthBasedFlxUnpack;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.voip.VideoCapturerDevice;
import org.webrtc.EglBase;
import org.webrtc.EglBase14;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.microedition.khronos.opengles.GL10;

public class ExternalVideoTexture {
    private static final String PARAMETER_SET_0 = "parSet0";
    private static final String PARAMETER_SET_1 = "parSet1";
    private static String TAG = "FLX_INJECT";
    private static int frameCounter = 0;
    private static boolean started = false;
    private static final Object sync = new Object();
    private static final Object sync1 = new Object();
    public static int[] videoTexture;
    public static EGLContext eglContext;
    private static HandlerThread thread;
    private static Handler handler;
    private static EglBase.Context baseContext;
    private static EGLContext sharedContext;
    private static MediaPlayer mediaPlayer;
    public static SurfaceTexture surfaceTexture;
    private static int fps = -1;
    private static long duration = -1L;

    public static int getFrameIdx(){
        /*if (mediaPlayer == null) return 0;
        long mediaTime = mediaPlayer.getTimestamp().getAnchorMediaTimeUs();
        int frameIdx = (int) (318L * mediaTime  / duration);
//        int frameIdx = (int) (mediaTime * fps / 1000000);
//        frameIdx+=1;
        if (frameIdx<0)frameIdx=0;*/
        return frameCounter;

//        Log.d("FLX_INJECT","mediaTimee "+frameIdx);
    }
    public static void startVideoTexture(){
//        EglBase.Context currentBaseContext = VideoCapturerDevice.getEglBase().getEglBaseContext();
//        EGLContext cSharedContext = ((EglBase14.Context) currentBaseContext).getRawContext();
//        if (cSharedContext != sharedContext) {
//            Log.d("FLX_INJECT", "base context changed "+cSharedContext);
////            baseContext = currentBaseContext;
//        }
        synchronized (sync){
            if (started) return;
            started = true;
        }
        thread = new HandlerThread("VideoTexture");
        thread.start();
        handler = new Handler(thread.getLooper());
        handler.post(()->{

            start();
        });

    }
    private static  FlexatarCommon.GlBuffers commonBuffers;

    private static void initCommonBuffers() {
        if (commonBuffers == null) {
            FlexatarRenderer.speechState = new float[]{0, 0, 0.05f, 0, 0};
            commonBuffers = FlexatarCommon.bufferFactory();

        }
    }
    private static int[] renderFrameBuffer;
    private static int[] renderTexture;
    private static void initFrameBuffer(){
        if (renderFrameBuffer != null) return;
        renderFrameBuffer = new int[1];
        renderTexture = new int[1];
        GLES20.glGenFramebuffers(1, renderFrameBuffer, 0);
        GLES20.glGenTextures(1, renderTexture, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, renderTexture[0]);
        GLES20.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 512, 512, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status == GLES20.GL_FRAMEBUFFER_COMPLETE) {
            android.util.Log.d("FlxDrawer", "GL_FRAMEBUFFER_COMPLETE");
        } else {
            android.util.Log.d("FlxDrawer", "GL_FRAMEBUFFER_FAIL");
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }
    private static List<int[]> videoTextures = new ArrayList<>();
    public static int addVideoTexture(){
        int[] texture = new int[1];

        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        GLES20.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, 512, 512, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        videoTextures.add(texture);
        return texture[0];
    }
    public static int getTextureFrame(){
        startVideoTexture();
        if (renderTexture == null) return -2;
        return renderTexture[0];
    }
    private static FlexatarVData flxvData;
    private static ShaderProgram videoProgram;
    private static ShaderProgram textureProgram;
    private static void initVideoProgram(){
        if (videoProgram != null) return;
        flxvData = new FlexatarVData(new LengthBasedFlxUnpack(AssetAccess.dataFromFile("flexatar/test_flx.v")));

        videoProgram = new ShaderProgram(ShaderLib.VIDEO_VERTEX, ShaderLib.VIDEO_FRAGMENT);
        videoProgram.addUniform4f("parSet0");
        videoProgram.addUniform4f("parSet1");


    }
    private static void initRotTextureProgram(){
        if (textureProgram != null) return;

        textureProgram = new ShaderProgram(ShaderLib.TEX_ROT_VERTEX, ShaderLib.VIDEO_FRAGMENT);
        textureProgram.addUniform4f("parSet0");
        textureProgram.addUniform4f("parSet1");
        textureProgram.attribute("uv" , commonBuffers.frameVBO, 2);
        textureProgram.addUniformMatrix4fv("uvRot");

    }
    public static void drawTexture(){
        initFrameBuffer();
        initCommonBuffers();
        initRotTextureProgram();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, renderFrameBuffer[0]);
        int tIdx = addVideoTexture();
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tIdx);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,tIdx , 0);
        renderTexture[0] = tIdx;
        GLES20.glViewport(0, 0, 512, 512);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        textureProgram.use();
        textureProgram.bind();
        textureProgram.uniformMatrix4fv("uvRot",textureRotMatrix);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, ExternalVideoTexture.videoTexture[0]);
        int videoTextureHandle = GLES20.glGetUniformLocation(textureProgram.id, "uSampler");
        GLES20.glUniform1i(videoTextureHandle, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        textureProgram.unbind();

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }
    public static void draw(){

        initFrameBuffer();
        initCommonBuffers();
        initVideoProgram();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, renderFrameBuffer[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, renderTexture[0], 0);

        GLES20.glViewport(0, 0, 512, 512);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
//        float[] speechState = FlexatarRenderer.speechState;
        float[] speechState = new float[]{0,0,-1,0,0};

        videoProgram.use();
        videoProgram.bind();
//        videoProgram.uniform4f("sizePosition", 1, 1, 0, 0);
        int positionHandle = GLES20.glGetAttribLocation(videoProgram.id, "uv");
        flxvData.markerBB.position(FlexatarCommon.videoStride*ExternalVideoTexture.getFrameIdx());
//        flxvData.markerBB.position(FlexatarCommon.videoStride*frameCounter);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(
                positionHandle,
                2,
                GLES20.GL_FLOAT,
                false,
                0,
                flxvData.markerBB);
        for (int i = 0; i < 3; i++) {
            int positionHandle1 = GLES20.glGetAttribLocation(videoProgram.id, "speechBuff"+i);
            GLES20.glEnableVertexAttribArray(positionHandle1);
            GLES20.glVertexAttribPointer(
                    positionHandle1,
                    4,
                    GLES20.GL_FLOAT,
                    false,
                    0,
                    FlexatarCommon.speechBshBB[1]);
        }


        videoProgram.uniform4f(PARAMETER_SET_0,  speechState[0], speechState[1], speechState[2], speechState[3]);
        videoProgram.uniform4f(PARAMETER_SET_1, speechState[4], 0, 0, 0);


        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, ExternalVideoTexture.videoTexture[0]);
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, videoTexture[0]);
        int videoTextureHandle = GLES20.glGetUniformLocation(videoProgram.id, "uSampler");
        GLES20.glUniform1i(videoTextureHandle, 0);
        commonBuffers.idxVBO.bind();
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, commonBuffers.idxCount, GLES20.GL_UNSIGNED_SHORT, 0);

        commonBuffers.idxVBO.unbind();
        videoProgram.unbind();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

    }

    private static void start(){
        EGLDisplay eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            Log.e(TAG, "Unable to get EGL display");
            return;
        }

        int[] version = new int[2];
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            Log.e(TAG, "Unable to initialize EGL");
            return;
        }

        int[] attribList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE
        };

        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];

        if (!EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, configs.length, numConfigs, 0)) {
            Log.e(TAG, "Unable to find suitable EGL config");
            return;
        }

        EGLConfig eglConfig = configs[0];

        int[] contextAttribs = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        baseContext = VideoCapturerDevice.getEglBase().getEglBaseContext();
        sharedContext = ((EglBase14.Context) baseContext).getRawContext();
        eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, sharedContext, contextAttribs, 0);
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            Log.e(TAG, "Unable to create EGL context");
            return;
        }
        int[] surfaceAttribs = {
                EGL14.EGL_WIDTH, 1,
                EGL14.EGL_HEIGHT, 1,
                EGL14.EGL_NONE
        };
        EGLSurface eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, surfaceAttribs, 0);
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            Log.e(TAG, "Unable to create EGL pbuffer surface");
            return;
        }

        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            Log.e(TAG, "Unable to make EGL current");
        }
        videoTexture = new int[1];
        GLES20.glGenTextures(1, videoTexture, 0);
        surfaceTexture = new SurfaceTexture(videoTexture[0]);

        surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
//                Log.d("FLX_INJECT","video frame avalibale "+frameCounter);
                synchronized (sync1) {
                    surfaceTexture.updateTexImage();
//                    draw();
                    drawTexture();

                }
//                handler.postDelayed(()->{
//                    if (!VideoToTextureArray.getNextFrame()){
//                        timerSwitchTextures();
//                    }
//                },100);


//                handler.post(()->{
//                    surfaceTexture.updateTexImage();
//                });
//                    surfaceTexture.updateTexImage();

                frameCounter+=1;
            }
        },handler);

        Surface surface = new Surface(surfaceTexture);
//        VideoToTextureArray.decode(surface);
//        VideoToTextureArray.getNextFrame();
//        Log.d("FLX_INJECT","video rotation "+VideoToTextureArray.videoRotation);
//        Matrix.setIdentityM(textureRotMatrix, 0);
//        Matrix.setRotateM(textureRotMatrix, 0,VideoToTextureArray.videoRotation,0,0,1);

        frameCounter = 0;

    }
    public static void updateTex(){
        if (surfaceTexture==null) return;
        handler.post(()->{
            surfaceTexture.updateTexImage();
        });

    }
    public static int texCounter = 0;
    public static void timerSwitchTextures(){
        handler.postDelayed(()->{
            renderTexture[0] = videoTextures.get(texCounter)[0];
            texCounter+=1;
            if (texCounter >= videoTextures.size())texCounter=0;
            timerSwitchTextures();
        },100);
    }
    public static float[] textureRotMatrix = new float[16];
}
