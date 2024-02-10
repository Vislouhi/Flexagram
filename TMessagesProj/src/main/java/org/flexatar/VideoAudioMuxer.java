package org.flexatar;
import android.annotation.SuppressLint;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class VideoAudioMuxer {
    private static final String TAG = "FLX_INJECT";
    public static void muxing(File fileOut,File videoFile,File audioFile) {

        String outputFile = "";

        try {

            File file = fileOut;
            file.createNewFile();
            outputFile = file.getAbsolutePath();

            MediaExtractor videoExtractor = new MediaExtractor();

            videoExtractor.setDataSource(videoFile.getAbsolutePath());

            MediaExtractor audioExtractor = new MediaExtractor();
            audioExtractor.setDataSource(audioFile.getAbsolutePath());

            Log.d(TAG, "Video Extractor Track Count " + videoExtractor.getTrackCount() );
            Log.d(TAG, "Audio Extractor Track Count " + audioExtractor.getTrackCount() );

            MediaMuxer muxer = new MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

//            videoExtractor.selectTrack(0);
//            MediaFormat videoFormat = videoExtractor.getTrackFormat(0);
//            int videoTrack = muxer.addTrack(videoFormat);

            HashMap<Integer, Integer> indexMap2 = new HashMap<Integer, Integer>(videoExtractor.getTrackCount());
            for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
                videoExtractor.selectTrack(i);
                MediaFormat videoFormat = videoExtractor.getTrackFormat(i);
                int dstIndex2 = muxer.addTrack(videoFormat);
                indexMap2.put(i, dstIndex2);
                Log.d(TAG, "Video Format " + videoFormat.toString() );
            }


            HashMap<Integer, Integer> indexMap = new HashMap<Integer, Integer>(audioExtractor.getTrackCount());
            for (int i = 0; i < audioExtractor.getTrackCount(); i++) {
                audioExtractor.selectTrack(i);
                MediaFormat SoundFormat = audioExtractor.getTrackFormat(i);
                Log.d(TAG, "Audio Format " + SoundFormat.toString() );
                int dstIndex = muxer.addTrack(SoundFormat);
                indexMap.put(i, dstIndex);

            }

//            audioExtractor.selectTrack(0);
//            MediaFormat audioFormat = audioExtractor.getTrackFormat(0);
//            int audioTrack = muxer.addTrack(audioFormat);


//            Log.d(TAG, "Audio Format " + audioFormat.toString() );

            boolean sawEOS = false;
            int frameCount = 0;
            int offset = 100;
            int sampleSize = 256 * 1024;
            ByteBuffer videoBuf = ByteBuffer.allocate(sampleSize);
            ByteBuffer audioBuf = ByteBuffer.allocate(sampleSize);
            MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();


            videoExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);

            muxer.start();

            while (!sawEOS)
            {
                videoBufferInfo.offset = offset;
                videoBufferInfo.size = videoExtractor.readSampleData(videoBuf, offset);


                if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0)
                {
                    Log.d(TAG, "saw input EOS.");
                    sawEOS = true;
                    videoBufferInfo.size = 0;

                }
                else
                {
                    videoBufferInfo.presentationTimeUs = videoExtractor.getSampleTime();
                    videoBufferInfo.flags = videoExtractor.getSampleFlags();
                    int trackIndex = videoExtractor.getSampleTrackIndex();
                    muxer.writeSampleData(indexMap2.get(trackIndex), videoBuf, videoBufferInfo);
                    videoExtractor.advance();


                    frameCount++;
                    Log.d(TAG, "Frame (" + frameCount + ") Video PresentationTimeUs:" + videoBufferInfo.presentationTimeUs +" Flags:" + videoBufferInfo.flags +" Size(KB) " + videoBufferInfo.size / 1024);
                    Log.d(TAG, "Frame (" + frameCount + ") Audio PresentationTimeUs:" + audioBufferInfo.presentationTimeUs +" Flags:" + audioBufferInfo.flags +" Size(KB) " + audioBufferInfo.size / 1024);

                }
            }




            boolean sawEOS2 = false;
            int frameCount2 =0;
            while (!sawEOS2)
            {
                frameCount2++;

                audioBufferInfo.offset = offset;
                audioBufferInfo.size = audioExtractor.readSampleData(audioBuf, offset);

                if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0)
                {
                    Log.d(TAG, "saw input EOS.");
                    sawEOS2 = true;
                    audioBufferInfo.size = 0;
                }
                else
                {
                    audioBufferInfo.presentationTimeUs = audioExtractor.getSampleTime();
                    audioBufferInfo.flags = audioExtractor.getSampleFlags();
                    int trackIndex = audioExtractor.getSampleTrackIndex();
                    muxer.writeSampleData(indexMap.get(trackIndex), audioBuf, audioBufferInfo);
                    audioExtractor.advance();


                    Log.d(TAG, "Frame (" + frameCount + ") Video PresentationTimeUs:" + videoBufferInfo.presentationTimeUs +" Flags:" + videoBufferInfo.flags +" Size(KB) " + videoBufferInfo.size / 1024);
                    Log.d(TAG, "Frame (" + frameCount + ") Audio PresentationTimeUs:" + audioBufferInfo.presentationTimeUs +" Flags:" + audioBufferInfo.flags +" Size(KB) " + audioBufferInfo.size / 1024);

                }
            }


            muxer.stop();
            muxer.release();
            Log.d(TAG, "Muxer ready " );

        } catch (IOException e) {
            Log.d(TAG, "Mixer Error 1 " + e.getMessage());
        } catch (Exception e) {
            Log.d(TAG, "Mixer Error 2 " + e.getMessage());
        }

    }
}
