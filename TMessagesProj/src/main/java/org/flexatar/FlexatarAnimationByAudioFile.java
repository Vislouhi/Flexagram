package org.flexatar;


import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;


import org.flexatar.resampler.Resampler;
import org.telegram.messenger.ApplicationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class FlexatarAnimationByAudioFile {

    private static final String TAG = "AudioFileReader";

    public static List<float[]> getAnimationPattern(String filePath, File aacAuidoFile) {
        try {
//            AAC ENCODER===========
            MediaCodec aacEncoder = MediaCodec.createEncoderByType("audio/mp4a-latm");
            MediaFormat aacFormat = MediaFormat.createAudioFormat("audio/mp4a-latm", 48000, 1); // Adjust based on your requirements
            aacFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000); // Adjust based on your requirements
            aacFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);

            aacEncoder.configure(aacFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            aacEncoder.start();
            MediaMuxer muxer = new MediaMuxer(aacAuidoFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            int aacTrackIndex = muxer.addTrack(aacFormat);
            muxer.start();


//            AAC ENCODER END===========
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(filePath);

            int audioTrackIndex = selectTrack(extractor);

            if (audioTrackIndex < 0) {
                Log.e(TAG, "No audio track found in the file");
                return null;
            }

            extractor.selectTrack(audioTrackIndex);

            MediaFormat format = extractor.getTrackFormat(audioTrackIndex);
            int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
//            Log.d("FLX_INJECT","channelCount "+ channelCount);
            MediaCodec codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
            codec.configure(format, null, null, 0);
            codec.start();

//            ByteBuffer[] inputBuffers = codec.getInputBuffers();


            short[] shortSignal = new short[0];

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
//            List<ByteBuffer> fileAudieBuffers =
            boolean isEOS = false;
            long aacPresentationTimeUs = 0;
            while ( true) {
                if (!isEOS) {
                    int inputBufferIndex = codec.dequeueInputBuffer(10000);
                    if (inputBufferIndex >= 0) {
                        ByteBuffer inputBuffer = codec.getInputBuffer(inputBufferIndex);
                        int sampleSize = extractor.readSampleData(inputBuffer, 0);
                        Log.d("FLX_INJECT", "sampleSize" + sampleSize);
                        if (sampleSize < 0) {

                            codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            isEOS=true;
                        } else {
                            long presentationTimeUs = extractor.getSampleTime();
                            codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTimeUs, 0);
                            extractor.advance();
                        }
                    }
                }
                int outputBufferIndex = codec.dequeueOutputBuffer(info, 10000);
                if (outputBufferIndex >= 0) {

                    ByteBuffer outputBuffer = codec.getOutputBuffer(outputBufferIndex);
//                    Log.d("FLX_INJECT","isDirect" + outputBuffer.isDirect());
//                    aacEncoder.
                    outputBuffer.position(0);
                    while (true) {
                        int inputBufferIndex = aacEncoder.dequeueInputBuffer(0);
                        if (inputBufferIndex >= 0) {
                            Log.d("FLX_INJECT","inputBufferIndex " + inputBufferIndex);
                            ByteBuffer inputBuffer = aacEncoder.getInputBuffer(inputBufferIndex);
//                            outputBuffer.position(2); // Starting position in the source buffer
                            outputBuffer.limit(outputBuffer.position() + inputBuffer.capacity());
                            inputBuffer.position(0);
                            byte[] tempArray = new byte[inputBuffer.capacity()];
                            outputBuffer.get(tempArray);

                            inputBuffer.put(tempArray);
                            Log.d("FLX_INJECT","pos/cap" + outputBuffer.position() + " " +outputBuffer.capacity());
                            if (outputBuffer.position() == outputBuffer.capacity()){
                                Log.d("FLX_INJECT","break loop");
                                break;
                            }
                            inputBuffer.position(0);
                            aacEncoder.queueInputBuffer(inputBufferIndex, 0, tempArray.length, (long) aacPresentationTimeUs, 0);
                            aacPresentationTimeUs = 1000000L * (tempArray.length / 2) / 48000;
//                            Log.d("FLX_INJECT", "in buf size" + inputBuffer.capacity());
//                            Log.d("FLX_INJECT", "out buf size " + outputBuffer.capacity());
//                            Log.d("FLX_INJECT", "div " + (float) outputBuffer.capacity() / inputBuffer.capacity());
//                        aacEncoder.queueInputBuffer(inputBufferIndex, 0, decodedPcmData.length, 0, 0);



                        }
                        MediaCodec.BufferInfo aacBufferInfo = new MediaCodec.BufferInfo();
                        int aacOutputBufferIndex = aacEncoder.dequeueOutputBuffer(aacBufferInfo, 0);
                        while (aacOutputBufferIndex >= 0) {
                            ByteBuffer aacOutputBuffer = aacEncoder.getOutputBuffer(aacOutputBufferIndex);
                            aacOutputBuffer.position(aacBufferInfo.offset);
                            aacOutputBuffer.limit(aacBufferInfo.offset + aacBufferInfo.size);

                            // Write AAC data to the muxer
                            muxer.writeSampleData(aacTrackIndex, aacOutputBuffer, aacBufferInfo);

                            aacEncoder.releaseOutputBuffer(aacOutputBufferIndex, false);
                            aacOutputBufferIndex = aacEncoder.dequeueOutputBuffer(aacBufferInfo, 0);
                        }
                    }
                    Log.d("FLX_INJECT","Encoding loop broken");
                    outputBuffer.position(0);


                    short[] shortBuffer = convertByteBufferToFloatArray(outputBuffer, info.size);
//                    float[] floatBuffer = VPUtil.shortToFloat(shrtBuffer);
//                    Log.d("FLX_INJECT","audio float "+ Arrays.toString(floatBuffer));
//                    Log.d("FLX_INJECT","audio float len"+ floatBuffer.length);
                    shortSignal = concatenateArrays(shortSignal, shortBuffer);

                    codec.releaseOutputBuffer(outputBufferIndex, false);
//                    Log.d("FLX_INJECT","samplerate "+ info.size);
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
//                        Log.d("FLX_INJECT", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                        break;
                    }
                }

            }

//            Log.d("FLX_INJECT","samplerate "+ sampleRate);
//            Log.d("FLX_INJECT","signal length "+ shortSignal.length);
            float[] floatBuffer = VPUtil.shortToFloat(shortSignal);
//            Log.d("FLX_INJECT","signal length "+ floatBuffer.length);
            double resampleFactor = 16000d / (double) sampleRate;
            int lenResampled = (int) (floatBuffer.length*resampleFactor + 10);
//            Log.d("FLX_INJECT","lenResampled "+ lenResampled);
//            Log.d("FLX_INJECT","resampleFactor "+ resampleFactor);
            float[] resampledBuffer = new float[lenResampled];
            Resampler resampler = new Resampler(true, resampleFactor, 1);
            resampler.process(resampleFactor,floatBuffer,0,floatBuffer.length,true,resampledBuffer,0,lenResampled);
            float[] inBuffer = new float[800];
            SpeechAnimation.loadModelsSync(ApplicationLoader.applicationContext);
            for (int i = 0; i < 20; i++) {
                SpeechAnimation.processAudio(inBuffer);
            }
            List<float[]> result = new ArrayList<>();
            for (int i = 0; i < lenResampled/800; i++) {

                System.arraycopy(resampledBuffer, i*800, inBuffer, 0, 800);

                float[] speechState = SpeechAnimation.processAudio(inBuffer);
                result.add(speechState);
//                Log.d("FLX_INJECT","inBuffer "+ Arrays.toString(inBuffer));
//                Log.d("FLX_INJECT","speechState "+ Arrays.toString(speechState));
//                Log.d("FLX_INJECT","speechState "+ i);
            }



//            Log.d("FLX_INJECT","audio float "+ Arrays.toString(resampledBuffer));
            Log.d("FLX_INJECT","audio animation finished");
            codec.stop();
            codec.release();
            extractor.release();
            aacEncoder.stop();
            aacEncoder.release();
            muxer.stop();
            muxer.release();
//            return floatSignal;
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static int selectTrack(MediaExtractor extractor) {
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

    private static short[] convertByteBufferToFloatArray(ByteBuffer buffer, int length) {
        short[] floatArray = new short[length / 2];
        buffer.asShortBuffer().get(floatArray);
        return floatArray;
    }

    private static short[] concatenateArrays(short[] a, short[] b) {
        int aLen = a.length;
        int bLen = b.length;
        short[] result = new short[aLen + bLen];
        System.arraycopy(a, 0, result, 0, aLen);
        System.arraycopy(b, 0, result, aLen, bLen);
        return result;
    }
}