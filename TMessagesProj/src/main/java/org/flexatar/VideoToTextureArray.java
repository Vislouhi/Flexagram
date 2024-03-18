package org.flexatar;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

public class VideoToTextureArray {
    private final int[] videoTexture;
    private SurfaceTexture surfaceTexture;
    private final FlexatarCommon.GlBuffers commonBuffers;
    private MediaExtractor extractor;
    private MediaCodec decoder;
    private int trackIndex=0;
    private Surface outputSurface;
    private boolean outputDone = false;
    private boolean inputDone = false;
    public int videoRotation;
    private boolean currentFrameReady = false;
    public int saveWidth;
    public int saveHeight;
    public float[] textureRotMatrix = new float[16];
    public Runnable onFrameAvailableListener = null;
    public VideoToTextureArray(FlexatarCommon.GlBuffers commonBuffers,File videoFile){
        this.commonBuffers = commonBuffers;
        videoTexture = new int[1];
        GLES20.glGenTextures(1, videoTexture, 0);
        surfaceTexture = new SurfaceTexture(videoTexture[0]);
        this.outputSurface=new Surface(surfaceTexture);

        surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                currentFrameReady = true;
//                Log.d("FLX_INJECT", "frame ready");
                if (onFrameAvailableListener!=null)onFrameAvailableListener.run();
            }
        });



        try {
            File inputFile = videoFile;
//            File inputFile = new File(FlexatarStorageManager.createTmpVideoStorage(), "saved_video.mp4");
            extractor = new MediaExtractor();
            extractor.setDataSource(inputFile.toString());
            trackIndex = selectTrack(extractor);
            if (trackIndex < 0) {
                throw new RuntimeException("No video track found in " + inputFile);
            }
            extractor.selectTrack(trackIndex);

            MediaFormat format = extractor.getTrackFormat(trackIndex);

            Log.d("FLX_INJECT", "Video size is " + format.getInteger(MediaFormat.KEY_WIDTH) + "x" +
                    format.getInteger(MediaFormat.KEY_HEIGHT));

            videoRotation = format.getInteger(MediaFormat.KEY_ROTATION);
            if (videoRotation == 90 || videoRotation == 270){
                saveWidth = format.getInteger(MediaFormat.KEY_HEIGHT);
                saveHeight = format.getInteger(MediaFormat.KEY_WIDTH);
            }else{
                saveWidth = format.getInteger(MediaFormat.KEY_WIDTH);
                saveHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
            }
            Matrix.setRotateM(textureRotMatrix, 0,videoRotation,0,0,1);

            String mime = format.getString(MediaFormat.KEY_MIME);
            decoder = MediaCodec.createDecoderByType(mime);
            decoder.configure(format, outputSurface, null, 0);
            decoder.start();


            outputDone = false;
            inputDone = false;

            createFrameBuffer();
            initVideoProgram();

        }catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
    public void release(){
        int[] videoTexturesToDelete = new int[videoTextures.size()];
        for (int i = 0; i < videoTextures.size(); i++) {
            videoTexturesToDelete[i] = videoTextures.get(i)[0];
        }
        GLES20.glDeleteTextures(videoTexturesToDelete.length,videoTexturesToDelete,0);
        videoTextures.clear();
        extractor = null;
        decoder = null;
        surfaceTexture = null;
        outputSurface = null;
    }
    private int[] renderFrameBuffer = new int[1];
    private void createFrameBuffer(){
        GLES20.glGenFramebuffers(1, renderFrameBuffer, 0);
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status == GLES20.GL_FRAMEBUFFER_COMPLETE) {
            android.util.Log.d("FLX_INJECT", "video texture GL_FRAMEBUFFER_COMPLETE");
        } else {
            android.util.Log.d("FLX_INJECT", "texture GL_FRAMEBUFFER_FAIL");
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }
    private List<int[]> videoTextures = new ArrayList<>();
    public int addVideoTexture(){
        int[] texture = new int[1];

        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        GLES20.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, saveWidth, saveHeight, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        videoTextures.add(texture);
        return texture[0];
    }
    private ShaderProgram textureProgram;
    private void initVideoProgram(){

        textureProgram = new ShaderProgram(ShaderLib.TEX_ROT_VERTEX, ShaderLib.TEX_ROT_FRAGMENT);
        textureProgram.addUniform4f("parSet0");
        textureProgram.addUniform4f("parSet1");
        textureProgram.attribute("uv" , commonBuffers.frameVBO, 2);
        textureProgram.addUniformMatrix4fv("uvRot");

    }
    private boolean isAllFramesReady = false;
    public int currentTextureIdx = -1;
    public int getVideoTexId(){
        return videoTextures.size() == 0 ? -1:videoTextures.get(currentTextureIdx)[0];
    }
    long currentTime = 0;
    public void updateTexture(){
        surfaceTexture.updateTexImage();
    }
    public void draw(){
        draw(true);
    }
    public boolean alFramesLoaded(){
        return isAllFramesReady;
    }
    public void draw(boolean byTimeStep) {
        if (byTimeStep){
            long time = System.nanoTime();
            if (time - currentTime < 40_000_000L) return;
    //        Log.d("FLX_INJECT","time " +(time/1_000_000));
            currentTime = time;
        }
        if (isAllFramesReady){
            currentTextureIdx+=1;
            if (currentTextureIdx>=videoTextures.size()) currentTextureIdx = 0;
            return;
        }
        if (currentFrameReady) {
            surfaceTexture.updateTexImage();
            currentFrameReady=false;
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, renderFrameBuffer[0]);
            int tIdx = addVideoTexture();
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,tIdx , 0);
            GLES20.glViewport(0, 0, saveWidth, saveHeight);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            textureProgram.use();
            textureProgram.bind();
            textureProgram.uniformMatrix4fv("uvRot",textureRotMatrix);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, videoTexture[0]);
            int videoTextureHandle = GLES20.glGetUniformLocation(textureProgram.id, "uSampler");
            GLES20.glUniform1i(videoTextureHandle, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
            textureProgram.unbind();
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            isAllFramesReady = getNextFrame();
            currentTextureIdx = videoTextures.size()-1;
        }
    }
//    public void setOnFrameAvailableListener(SurfaceTexture.OnFrameAvailableListener listener, Handler handler){
//        surfaceTexture.setOnFrameAvailableListener(listener,handler);
//    }
    private MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
    private int inputChunk = 0;

    public boolean getNextFrame(){
        final int TIMEOUT_USEC = 10000;
        boolean VERBOSE = false;
        String TAG = "FLX_INJECT";
        if (outputDone) return false;

        if (VERBOSE) Log.d(TAG, "loop");
        boolean frameExtracted = false;
        while (!frameExtracted) {
            // Feed more data to the decoder.
            if (!inputDone) {
                int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputBufIndex >= 0) {
                    ByteBuffer inputBuf = decoder.getInputBuffer(inputBufIndex);
//                        ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                    // Read the sample data into the ByteBuffer.  This neither respects nor
                    // updates inputBuf's position, limit, etc.
                    int chunkSize = extractor.readSampleData(inputBuf, 0);
                    if (chunkSize < 0) {
                        // End of stream -- send empty frame with EOS flag set.
                        decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                        if (VERBOSE) Log.d(TAG, "sent input EOS");
                    } else {
                        if (extractor.getSampleTrackIndex() != trackIndex) {
                            Log.w(TAG, "WEIRD: got sample from track " +
                                    extractor.getSampleTrackIndex() + ", expected " + trackIndex);
                        }
                        long presentationTimeUs = extractor.getSampleTime();
                        decoder.queueInputBuffer(inputBufIndex, 0, chunkSize,
                                presentationTimeUs, 0 /*flags*/);
                        if (VERBOSE) {
                            Log.d(TAG, "submitted frame " + inputChunk + " to dec, size=" +
                                    chunkSize);
                        }
                        inputChunk++;
                        extractor.advance();
                    }
                } else {
                    if (VERBOSE) Log.d(TAG, "input buffer not available");
                }
            }

            if (!outputDone) {
                int decoderStatus = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (VERBOSE) Log.d(TAG, "no output from decoder available");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = decoder.getOutputFormat();
                    if (VERBOSE) Log.d(TAG, "decoder output format changed: " + newFormat);
                } else if (decoderStatus < 0) {
//                    fail("unexpected result from decoder.dequeueOutputBuffer: " + decoderStatus);
                } else { // decoderStatus >= 0
                    if (VERBOSE) Log.d(TAG, "surface decoder given buffer " + decoderStatus +
                            " (size=" + info.size + ")");
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (VERBOSE) Log.d(TAG, "output EOS");
                        outputDone = true;
                    }

                    boolean doRender = (info.size != 0);
                    frameExtracted=true;
                    decoder.releaseOutputBuffer(decoderStatus, doRender);
                    if (outputDone){
                        extractor.release();
                        decoder.release();
                        surfaceTexture.release();
                        outputSurface.release();
                        extractor = null;
                        decoder = null;
                        surfaceTexture = null;
                        outputSurface = null;
                    }
                }
            }
        }
        return outputDone;
    }
    public void destroy(){
        if (extractor!=null) extractor.release();
        if (decoder!=null)decoder.release();
        if (surfaceTexture!=null)surfaceTexture.release();
        if (outputSurface!=null)outputSurface.release();
        extractor = null;
        decoder = null;
        surfaceTexture = null;
        outputSurface = null;
        isAllFramesReady = true;
        currentTextureIdx = 0;

    }
    private static int selectTrack(MediaExtractor extractor) {

        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
//                if (VERBOSE) {
//                    Log.d(TAG, "Extractor selected track " + i + " (" + mime + "): " + format);
//                }
                return i;
            }
        }

        return -1;
    }
}
