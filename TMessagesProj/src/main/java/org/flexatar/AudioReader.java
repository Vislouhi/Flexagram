package org.flexatar;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioReader extends MediaCodec.Callback {
    private static final String TAG = "FLX_INJECT";

    private final MediaExtractor extractor;
    private OnAudioBufferReady audioBufferListener;
    private MediaCodec opusCodec;
    public int channelCount;
    public int sampleRate;
    public interface OnAudioBufferReady{
        void onReady(ByteBuffer buffer,MediaCodec.BufferInfo bufferInfo);
    }

    public AudioReader(File file){

        extractor = new MediaExtractor();
        try {
            extractor.setDataSource(file.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int audioTrackIndex = selectTrack(extractor);

        if (audioTrackIndex < 0) {
            Log.e(TAG, "No audio track found in the file");
            return;
        }
        extractor.selectTrack(audioTrackIndex);
        MediaFormat format = extractor.getTrackFormat(audioTrackIndex);
        sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        try {
            opusCodec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        opusCodec.setCallback(this);
        opusCodec.configure(format, null, null, 0);

    }
    public void start(OnAudioBufferReady audioBufferListener){
        this.audioBufferListener=audioBufferListener;
        opusCodec.start();
    }
    public static int selectTrack(MediaExtractor extractor) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int index) {
        ByteBuffer inputBuffer = opusCodec.getInputBuffer(index);
        int sampleSize = extractor.readSampleData(inputBuffer, 0);
        if (sampleSize < 0) {
//                    Log.d(TAG, "input buffers finished");
            opusCodec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);


        } else {
//                    Log.d(TAG, "input buffer encoded size: " + sampleSize);
            long presentationTimeUs = extractor.getSampleTime();
            opusCodec.queueInputBuffer(index, 0, sampleSize, presentationTimeUs, 0);
            extractor.advance();

        }
    }

    private final Object sync = new Object();
    @Override
    public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int index, @NonNull MediaCodec.BufferInfo bufferInfo) {
        synchronized (sync) {
            ByteBuffer outputBuffer = opusCodec.getOutputBuffer(index);
            audioBufferListener.onReady(outputBuffer,bufferInfo);
            opusCodec.releaseOutputBuffer(index, false);
            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                opusCodec.stop();
                opusCodec.release();
                Log.d(TAG,"input audio file read finished");
            }
        }
    }

    @Override
    public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {

    }

    @Override
    public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {

    }
}
