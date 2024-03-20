package org.flexatar;

import static org.flexatar.FlexatarNotificator.ChosenStateForRoundVideo.MIX;
import static org.flexatar.FlexatarNotificator.ChosenStateForRoundVideo.MORPH;
import static org.flexatar.FlexatarNotificator.ChosenStateForRoundVideo.HYBRID;
import static org.flexatar.FlexatarNotificator.ChosenStateForRoundVideo.NO;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import org.flexatar.DataOps.FlexatarData;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;
import org.webrtc.EglBase;
import org.webrtc.EglBase14;
import org.webrtc.EglBase14Impl;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CountDownLatch;


public class FlexatarVideoEncoder {
    private static final String MIME_TYPE = "video/avc";
    private static final int FRAME_RATE = 20;               // 15fps
    private static final int IFRAME_INTERVAL = 10;          // 10 seconds between I-frames
    private final String outputPath;
    private final File aacFile;
    private final Runnable completion;
    private final int account;
    private EglBase eglBase = null;
    private FlxDrawer.GroupMorphState mState;
    private MediaMuxer mMuxer;
    private FlexatarVideoEncoder.CodecInputSurface mInputSurface;
    private MediaCodec mEncoder;
    private MediaCodec.BufferInfo mBufferInfo;
    private int mTrackIndex;
    private boolean mMuxerStarted;
    private MediaExtractor extractor;
    private int audioTrack;

    public FlexatarVideoEncoder(int account,int mWidth, int mHeight, List<float[]> animationPattern,File videoFile,File aacFile,Runnable completion){
        this.aacFile=aacFile;
        this.completion=completion;
        this.account=account;

        FlexatarStorageManager.FlexatarChooser chooser = FlexatarStorageManager.roundFlexatarChooser[account];
        int flxType = chooser.getFlxType();
        FlxDrawer flxDrawer;
        flxDrawer = new FlxDrawer();
        flxDrawer.setSize(mWidth,mHeight);
        flxDrawer.setPromo();
        flxDrawer.setRealtimeAnimation(false);
        flxDrawer.setTgRoundVideo();

        FlexatarData firstFlx;
        FlexatarData secondFlx;
        FlexatarData videoFlx;
        if (flxType==1){
            firstFlx = FlexatarData.factory(chooser.getChosenFirst());
            secondFlx = chooser.getEffectIndex() == NO ? null : FlexatarData.factory(chooser.getChosenSecond());
            videoFlx = null;
        }else if (flxType==0){
            firstFlx = null;
            secondFlx = null;
            videoFlx = FlexatarData.factory(chooser.getChosenVideo());
        }else{
            firstFlx = null;
            secondFlx = null;
            videoFlx = null;
        }
        /*flxDrawer.onFrameStartListener.set( ()-> new FlxDrawer.RenderParams(){{
            mixWeight = 1f;
            effectID = 0;
            isEffectsOn = false;
            flexatarData = firstFlx;
        }});*/

//        flxDrawer.setFlexatarData(FlexatarData.factory(chooser.getChosenFirst()));
        final HandlerThread thread = new HandlerThread("video_flexatar_textures_thread");
        thread.start();
        final Handler handler = new Handler(thread.getLooper());
        CountDownLatch latch = new CountDownLatch(1);

        handler.post(()->{

            if (flxType == 0) {
                synchronized (EglBase.lock) {
                    eglBase = EglBase.create(null, EglBase.CONFIG_PIXEL_BUFFER);
                    try {
                        // Both these statements have been observed to fail on rare occasions, see BUG=webrtc:5682.
                        eglBase.createDummyPbufferSurface();
                        eglBase.makeCurrent();
                    } catch (RuntimeException e) {
                        // Clean up before rethrowing the exception.
                        eglBase.release();
                        handler.getLooper().quit();
                        throw e;
                    }
                }
                flxDrawer.flxvData = videoFlx;

                flxDrawer.onVideoFrameAvailableListener = new Runnable() {
                    private int counter = 0;
                    @Override
                    public void run() {
                        flxDrawer.videoToTextureArray.draw(false);
//                        if (flxDrawer.videoToTextureArray.alFramesLoaded() )
                        if (flxDrawer.videoToTextureArray.alFramesLoaded() || counter >= animationPattern.size()) {
                            flxDrawer.videoToTextureArray.destroy();
                            latch.countDown();
                            Log.d("FLX_INJECT", "texture obtained");
                        }

                        counter+=1;
                    }
                };



                Log.d("FLX_INJECT","request texture");
                flxDrawer.prepareVideoTextures();

            }else{
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        flxDrawer.screenRatio = (float) mWidth / (float) mHeight;
       /* if (chooser.getEffectIndex() == MIX){
//            flxDrawer.setFlexatarDataAlt(FlexatarData.factory(chooser.getChosenSecond()));
//            flxDrawer.setisEffectOnVal(true);
//            flxDrawer.setEffectIdVal(0);
//            flxDrawer.setMixWeightVal(FlexatarNotificator.chosenStateForRoundVideo.mixWeight);
            flxDrawer.onFrameStartListener.set( ()-> new FlxDrawer.RenderParams(){{
                mixWeight = chooser.getMixWeight();
                effectID = 0;
                isEffectsOn = true;
                flexatarData = firstFlx;
                flexatarDataAlt = secondFlx;

            }});
        }
        else if (chooser.getEffectIndex() == MORPH){
//            flxDrawer.setFlexatarDataAlt(FlexatarData.factory(chooser.getChosenSecond()));
//            flxDrawer.setisEffectOnVal(true);
//            flxDrawer.setEffectIdVal(0);
//            flxDrawer.setMixWeightVal(1f);
            flxDrawer.onFrameStartListener.set( ()-> new FlxDrawer.RenderParams(){{
                mixWeight = 1f;
                effectID = 0;
                isEffectsOn = true;
                flexatarData = firstFlx;
                flexatarDataAlt = secondFlx;

            }});
        }
        else if (chooser.getEffectIndex() == HYBRID){
//            flxDrawer.setFlexatarDataAlt(FlexatarData.factory(chooser.getChosenSecond()));
//            flxDrawer.setisEffectOnVal(true);
//            flxDrawer.setEffectIdVal(1);
//            flxDrawer.setMixWeightVal(0f);
            flxDrawer.onFrameStartListener.set( ()-> new FlxDrawer.RenderParams(){{
                mixWeight = 0f;
                effectID = 0;
                isEffectsOn = true;
                flexatarData = firstFlx;
                flexatarDataAlt = secondFlx;

            }});
        }*/
        String groupId = chooser.getChosenFirst().getName().replace(".flx", "");
        List<File> groupFiles = FlexatarStorageManager.getFlexatarGroupFileList(ApplicationLoader.applicationContext,account, groupId);
        if (groupFiles.size() != 0) {
            groupFiles.add(chooser.getChosenFirst());
        }
        mState = new FlxDrawer.GroupMorphState();
        TimerAutoDestroy.OnTimerListener<FlxDrawer.GroupMorphState> onTimerListener =
                x ->{
                    if (x.flexatarData == null){
                        x.flexatarData = firstFlx;
                    }

                    if (!x.morphStage)
                        x.counter+=1;
                    if (x.counter>x.changeDelta){
                        x.counter = 0;

                        if (x.flexatarCounter>=groupFiles.size()){
                            x.flexatarCounter=0;
                        }
                        FlexatarData.asyncFactory(groupFiles.get(x.flexatarCounter),fData->{
                            x.morphStage = true;
                            x.mixWeight = 0;
                            x.effectID = 0;
                            x.isEffectsOn = true;
                            x.flexatarDataAlt = x.flexatarData;
                            x.flexatarData = fData;

                            x.flexatarCounter+=1;
                        });
                    }
                    if (x.morphStage){
                        x.morphCounter+=1;
                        double w = (1d + Math.cos(Math.PI + Math.PI * (double) x.morphCounter / x.morphDelta)) / 2;
                        x.mixWeight = (float)w;
                        x.effectID = 0;
                        x.isEffectsOn = true;
                        if (x.morphCounter>x.morphDelta){
                            x.morphCounter = 0;
                            x.morphStage =false;
                            x.mixWeight = 1;
                            x.effectID = 0;
                            x.isEffectsOn = false;

                        }
                    }

                    return x;
                };

        try {
            mBufferInfo = new MediaCodec.BufferInfo();
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 1000000);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
            try {
                mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mInputSurface = new FlexatarVideoEncoder.CodecInputSurface(mEncoder.createInputSurface(),eglBase);
            mEncoder.start();
            mTrackIndex = -1;
            mMuxerStarted = false;
            outputPath = videoFile.toString();

            try {
                mMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            } catch (IOException ioe) {
                throw new RuntimeException("MediaMuxer creation failed", ioe);
            }

            mInputSurface.makeCurrent();
            long presentationTime = 0;
//            Log.d("FLX_INJECT","flxType "+flxType);
//            Log.d("FLX_INJECT","videoFlx "+videoFlx);

            for (int i = 0; i < animationPattern.size(); i++) {
                drainEncoder(false);

                int finalI = i;
                flxDrawer.onFrameStartListener.set( ()-> new FlxDrawer.RenderParams(){{
                    flexatarType=flxType;
                    flexatarData = firstFlx;
                    flexatarDataAlt = secondFlx;
                    flexatarDataVideo = videoFlx;
                    if (flxType == 1) {
                        if (chooser.getEffectIndex() == NO) {
                            mixWeight = 1f;
                            effectID = 0;
                            isEffectsOn = false;
                            if (groupFiles.size() > 0) {
                                mState = onTimerListener.onTic(mState);
                                mixWeight = mState.mixWeight;
                                effectID = mState.effectID;
                                isEffectsOn = mState.isEffectsOn;
                                flexatarData = mState.flexatarData;
                                flexatarDataAlt = mState.flexatarDataAlt;
                            }
                        } else if (chooser.getEffectIndex() == MIX) {
                            mixWeight = chooser.getMixWeight();
                            effectID = 0;
                            isEffectsOn = true;

                        } else if (chooser.getEffectIndex() == MORPH) {

                            mixWeight = 1f - (float) finalI / (float) animationPattern.size();
                            effectID = 0;
                            isEffectsOn = true;

                        } else if (chooser.getEffectIndex() == HYBRID) {
                            mixWeight = (float) finalI * 0.005f;
                            effectID = 1;
                            isEffectsOn = true;
                        }
                    }


                }});


                /*if (chooser.getEffectIndex() == MORPH){

                    float weight = 1f - (float) i / (float) animationPattern.size();
                    flxDrawer.setMixWeightVal(weight);
                }else
                if (chooser.getEffectIndex() == HYBRID){
                    float weight = (float) i * 0.005f;
                    flxDrawer.setMixWeightVal(weight);
                }*/

                if (i == animationPattern.size()/2)
                    flxDrawer.builtinAnimator.reverse();


                GLES20.glClearColor(0f,0f,0f,0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                flxDrawer.setSpeechState(animationPattern.get(i));

                if (flxType == 0) {
                    flxDrawer.videoToTextureArray.draw(false);
                }

                flxDrawer.draw();
                int error = GLES20.glGetError();
                if (error != GLES20.GL_NO_ERROR) {
                    Log.e("FLX_INJECT", "Error openg: " + error);
                }
                // Generate a new frame of input.
//                generateSurfaceFrame(i);
                mInputSurface.setPresentationTime(presentationTime);
                presentationTime += 1000_000L * 50L;

                // Submit it to the encoder.  The eglSwapBuffers call will block if the input
                // is full, which would be bad if it stayed full until we dequeued an output
                // buffer (which we can't do, since we're stuck here).  So long as we fully drain
                // the encoder before supplying additional input, the system guarantees that we
                // can supply another frame without blocking.
//                if (VERBOSE) Log.d(TAG, "sending frame " + i + " to encoder");
                mInputSurface.swapBuffers();
            }

            // send end-of-stream to encoder, and drain remaining output
            drainEncoder(true);
        }
        finally {
            // release encoder, muxer, and input Surface
            releaseEncoder();
            handler.post(()-> {
                if (eglBase!=null)
                    eglBase.release();
            });

        }
    }
    public void addAudioTrack(){
        extractor = new MediaExtractor();
//        File file = new File(FlexatarStorageManager.createTmpVideoStorage(), "aacTmp.aac");
        try {
            extractor.setDataSource(aacFile.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int audioTrackIndex = AudioReader.selectTrack(extractor);
        extractor.selectTrack(audioTrackIndex);
        MediaFormat format = extractor.getTrackFormat(audioTrackIndex);
        audioTrack = mMuxer.addTrack(format);


    }
    public void writeAudio(){
        int sampleSize = 256 * 1024;
        int offset = 100;
        ByteBuffer audioBuf = ByteBuffer.allocate(sampleSize);

        MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();

        extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        boolean sawEOS = false;
        while (!sawEOS)
        {
            audioBufferInfo.offset = offset;
            audioBufferInfo.size = extractor.readSampleData(audioBuf, offset);
            if (audioBufferInfo.size < 0)
            {
                Log.d("FLX_INJECT", "saw input EOS.");
                sawEOS = true;
                audioBufferInfo.size = 0;
            }else{
                audioBufferInfo.presentationTimeUs = extractor.getSampleTime();
                audioBufferInfo.flags = extractor.getSampleFlags();
                mMuxer.writeSampleData(audioTrack, audioBuf, audioBufferInfo);
                extractor.advance();
            }
        }
        extractor.release();
    }
    private void releaseEncoder() {
//        if (VERBOSE) Log.d(TAG, "releasing encoder objects");
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mInputSurface != null) {
            mInputSurface.release();
            mInputSurface = null;
        }
        if (mMuxer != null) {
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
        completion.run();
    }
    private void drainEncoder(boolean endOfStream) {
        final int TIMEOUT_USEC = 10000;
//        if (VERBOSE) Log.d(TAG, "drainEncoder(" + endOfStream + ")");

        if (endOfStream) {
//            if (VERBOSE) Log.d(TAG, "sending EOS to encoder");
            mEncoder.signalEndOfInputStream();
        }

//        ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
        while (true) {
            int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break;      // out of while
                } else {
//                    if (VERBOSE) Log.d(TAG, "no output available, spinning to await EOS");
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat newFormat = mEncoder.getOutputFormat();
//                Log.d(TAG, "encoder output format changed: " + newFormat);

                // now that we have the Magic Goodies, start the muxer
                mTrackIndex = mMuxer.addTrack(newFormat);
                addAudioTrack();
                mMuxer.start();
                mMuxerStarted = true;
                writeAudio();
            } else if (encoderStatus < 0) {
//                Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
//                        encoderStatus);
                // let's ignore it
            } else {
                ByteBuffer encodedData = mEncoder.getOutputBuffer(encoderStatus);
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                            " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
//                    if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    if (!mMuxerStarted) {
                        throw new RuntimeException("muxer hasn't started");
                    }

                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                    mMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
//                    if (VERBOSE) Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer");
                }

                mEncoder.releaseOutputBuffer(encoderStatus, false);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
//                        Log.w(TAG, "reached end of stream unexpectedly");
                    } else {
//                        if (VERBOSE) Log.d(TAG, "end of stream reached");
                    }
                    break;      // out of while
                }
            }
        }
    }
    private static class CodecInputSurface {
        private static final int EGL_RECORDABLE_ANDROID = 0x3142;
        private final EglBase sharedContext;

        private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
        private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;

        private Surface mSurface;

        /**
         * Creates a CodecInputSurface from a Surface.
         */
        public CodecInputSurface(Surface surface,EglBase sharedContext) {
            if (surface == null) {
                throw new NullPointerException();
            }
            mSurface = surface;
            this.sharedContext = sharedContext;

            eglSetup();
        }

        /**
         * Prepares EGL.  We want a GLES 2.0 context and a surface that supports recording.
         */
        private void eglSetup() {
            synchronized (EglBase.lock) {
                mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
                if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
                    throw new RuntimeException("unable to get EGL14 display");
                }
                int[] version = new int[2];
                if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
                    throw new RuntimeException("unable to initialize EGL14");
                }

                // Configure EGL for recording and OpenGL ES 2.0.
                int[] attribList = {
                        EGL14.EGL_RED_SIZE, 8,
                        EGL14.EGL_GREEN_SIZE, 8,
                        EGL14.EGL_BLUE_SIZE, 8,
                        EGL14.EGL_ALPHA_SIZE, 8,
                        EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                        EGL_RECORDABLE_ANDROID, 1,
                        EGL14.EGL_NONE
                };
                EGLConfig[] configs = new EGLConfig[1];
                int[] numConfigs = new int[1];
                EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length,
                        numConfigs, 0);
                checkEglError("eglCreateContext RGB888+recordable ES2");

                // Configure context for OpenGL ES 2.0.
                int[] attrib_list = {
                        EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                        EGL14.EGL_NONE
                };
                EGLContext shared = EGL14.EGL_NO_CONTEXT;
                if (sharedContext != null) {

                    shared = ((EglBase14.Context) sharedContext.getEglBaseContext()).getRawContext();
                }
                mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], shared,
                        attrib_list, 0);
                checkEglError("eglCreateContext");

                // Create a window surface, and attach it to the Surface we received.
                int[] surfaceAttribs = {
                        EGL14.EGL_NONE
                };
                mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], mSurface,
                        surfaceAttribs, 0);
                checkEglError("eglCreateWindowSurface");
            }
        }

        /**
         * Discards all resources held by this class, notably the EGL context.  Also releases the
         * Surface that was passed to our constructor.
         */
        public void release() {
            if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_CONTEXT);
                EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
                EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
                EGL14.eglReleaseThread();
                EGL14.eglTerminate(mEGLDisplay);
            }

            mSurface.release();

            mEGLDisplay = EGL14.EGL_NO_DISPLAY;
            mEGLContext = EGL14.EGL_NO_CONTEXT;
            mEGLSurface = EGL14.EGL_NO_SURFACE;

            mSurface = null;
        }

        /**
         * Makes our EGL context and surface current.
         */
        public void makeCurrent() {
            EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext);
            checkEglError("eglMakeCurrent");
        }

        /**
         * Calls eglSwapBuffers.  Use this to "publish" the current frame.
         */
        public boolean swapBuffers() {
            boolean result = EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface);
            checkEglError("eglSwapBuffers");
            return result;
        }

        /**
         * Sends the presentation time stamp to EGL.  Time is expressed in nanoseconds.
         */
        public void setPresentationTime(long nsecs) {
            EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, nsecs);
            checkEglError("eglPresentationTimeANDROID");
        }

        /**
         * Checks for EGL errors.  Throws an exception if one is found.
         */
        private void checkEglError(String msg) {
            int error;
            if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
                throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
            }
        }
    }
}
