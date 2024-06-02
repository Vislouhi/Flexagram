package org.flexatar;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

public class AudioWriter extends MediaCodec.Callback {
    private static final String TAG = "FLX_INJECT";

    private final MediaCodec aacEncoder;
    private final MediaMuxer muxer;
    private final int sampleRate;
    private int trackIndex = -1;
    private final CountDownLatch muxerReadyLatch = new CountDownLatch(1);
    private Runnable onFinish;

    public AudioWriter(File file, int sampleRate, int channelCount){
        this.sampleRate=sampleRate;
        try {
            muxer = new MediaMuxer(file.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            aacEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        aacEncoder.setCallback(this);
        MediaFormat aacFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate,channelCount); // Adjust based on your requirements
        aacFormat.setInteger(MediaFormat.KEY_BIT_RATE, 64*1024); // Adjust based on your requirements
        aacFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectHE);
        aacEncoder.configure(aacFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        aacEncoder.start();

//        trackIndex = muxer.addTrack(aacFormat);
//        muxer.start();
//        muxerReadyLatch.countDown();
//        Log.d(TAG,"muxerReadyLatch.countDown()");

    }
    public void setListener(Runnable onFinish){
        this.onFinish=onFinish;
    }
    long presentationTime =0;
    public void write(ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo,CountDownLatch latch){
        try {
//            Log.d(TAG,"muxerReadyLatch.await()");

            muxerReadyLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Integer bufferIndex = null;
        try {
            bufferIndex = bufferIndexQueue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        ByteBuffer inputBuffer = aacEncoder.getInputBuffer(bufferIndex);
        inputBuffer.position(bufferInfo.offset);
        inputBuffer.limit(bufferInfo.offset + bufferInfo.size);
        buffer.position(bufferInfo.offset);
        buffer.limit(bufferInfo.offset + bufferInfo.size);

        byte[] tempArray = new byte[bufferInfo.size];
        buffer.get(tempArray);
//        Log.d(TAG,"before write latch");
        latch.countDown();
//        Log.d(TAG,"after write latch");
        inputBuffer.put(tempArray);
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            aacEncoder.queueInputBuffer(bufferIndex, 0, 0, presentationTime, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        }else {
            aacEncoder.queueInputBuffer(bufferIndex, 0, bufferInfo.size, presentationTime, 0);
            presentationTime += 1000000L * (bufferInfo.size / 2) / sampleRate;
        }

    }
    private final ArrayBlockingQueue<Integer> bufferIndexQueue = new ArrayBlockingQueue<Integer>(32);
    @Override
    public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int i) {

        bufferIndexQueue.offer(i);
    }

    @Override
    public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int i, @NonNull MediaCodec.BufferInfo bufferInfo) {


        ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(i);
        outputBuffer.position(bufferInfo.offset);
        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
        muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo);
        mediaCodec.releaseOutputBuffer(i, false);
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            mediaCodec.stop();
            mediaCodec.release();
            try {
                muxer.stop();
            }catch (IllegalStateException ignored){}
            muxer.release();
            if (onFinish!=null)onFinish.run();
        }
    }

    @Override
    public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {

    }

    @Override
    public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {
//        Log.d(TAG,"onOutputFormatChanged");
        if (trackIndex == -1) {
            trackIndex = muxer.addTrack(mediaFormat);
            muxer.start();
            muxerReadyLatch.countDown();
//            Log.d(TAG,"muxerReadyLatch.countDown()");
        }
    }
}
