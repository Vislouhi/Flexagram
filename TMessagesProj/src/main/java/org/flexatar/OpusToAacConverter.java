package org.flexatar;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import org.flexatar.resampler.Resampler;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DispatchQueue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpusToAacConverter {
    private static final String TAG = "FLX_INJECT";
    private static final String OPUS_FILE_PATH = "/path/to/opus/file.opus";
    private static final String OUTPUT_AAC_FILE_PATH = Environment.getExternalStorageDirectory().getPath() + "/output.aac";
    private MediaMuxer muxer;
    private int trackIndex = -1;
    private int sampleRate;
    private long startTime;
    private Resampler resampler;
    private double resampleFactor;

    private static int sizeCounter = 0;
    private AudioWriter audioWriter = null;
    private AudioReader audioReader = null;
    public List<float[]> speechAnimation = new ArrayList<>();


    public static void testConverter(){
        /*File opusPath = new File(FlexatarStorageManager.createTmpVideoStorage(), "opusTmp.ogg");
        File aacPath = new File(FlexatarStorageManager.createTmpVideoStorage(), "aacTmp.aac");
        OpusToAacConverter converter = new OpusToAacConverter();
        converter.convertOpusToAac(opusPath,aacPath);*/

    }
    public void convertOpusToAac(File opusFile,File aacFile,Runnable onFinish){


        DispatchQueue calcAnimQueue =new DispatchQueue("calcAnimQueue");
        calcAnimQueue.postRunnable(()->{
            SpeechAnimation.loadModelsSync(ApplicationLoader.applicationContext);
            for (int i = 0; i <20; i++) {
                SpeechAnimation.processAudio(animBuffer);
            }
        });
        calcAnimQueue.setPriority(Thread.MAX_PRIORITY);
        DispatchQueue fileReadingQueue =new DispatchQueue("audioReadingQueue");

        CountDownLatch countDownLatch1 = new CountDownLatch(1);
        fileReadingQueue.postRunnable(()->{
            audioReader = new AudioReader(opusFile);
            countDownLatch1.countDown();
            resampleFactor = 16000d / (double) audioReader.sampleRate;
            resampler = new Resampler(true, resampleFactor, 1);
        });

        try {
            countDownLatch1.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        DispatchQueue fileEncodingQueue =new DispatchQueue("audioWritingQueue");

        CountDownLatch countDownLatch = new CountDownLatch(1);

        fileEncodingQueue.postRunnable(()->{
            Log.d(TAG,"start init writer  ");
            long s = System.nanoTime();
            audioWriter = new AudioWriter(aacFile,audioReader.sampleRate,audioReader.channelCount);
            audioWriter.setListener(()->{
                long f = System.nanoTime();
                Log.d(TAG,"writing audio finished in"+((f-s)/1000000L));

            });
            countDownLatch.countDown();
            Log.d(TAG,"audio writer initialized  ");

        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        fileReadingQueue.postRunnable(()-> {
            audioReader.start(new AudioReader.OnAudioBufferReady() {
                @Override
                public void onReady(ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo) {
                    boolean isFinished;
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isFinished = true;
                    } else {
                        isFinished = false;
                    }
                    sizeCounter += bufferInfo.size;
//                    Log.d(TAG, "out buffer avaliblae with size : " + sizeCounter / audioReader.sampleRate / audioReader.channelCount / 2);
                    CountDownLatch latch = new CountDownLatch(1);
                    fileEncodingQueue.postRunnable(()->{
                        audioWriter.write(buffer, bufferInfo,latch);

                    });
                    buffer.position(bufferInfo.offset);
                    buffer.limit(bufferInfo.offset + bufferInfo.size);
                    ShortBuffer shortBuffer = buffer.asShortBuffer();
                    short[] shortArray = new short[bufferInfo.size/2];
                    shortBuffer.get(shortArray);
                    calcAnimQueue.postRunnable(()->{
                        float[] floatBuffer = VPUtil.shortToFloat(shortArray);
                        int lenResampled = (int) (floatBuffer.length*resampleFactor);
                        float[] resampledBuffer = new float[lenResampled];
                        resampler.process(resampleFactor,floatBuffer,0,floatBuffer.length,false,resampledBuffer,0,lenResampled);
                        if (bufferCollector(resampledBuffer)){

                            long f = System.nanoTime();
//                            Log.d(TAG,"resamp proc time "+ ((f-startTime)/1000000L));
                            float[] spst = SpeechAnimation.processAudio(animBuffer);
                            speechAnimation.add(spst);

//                            Log.d(TAG,"animation ready "+ Arrays.toString(spst));
                        }
                        if (isFinished){
                            for (int i = 0; i < 10; i++) {
                                float[] spst = SpeechAnimation.processAudio(new float[I800]);
                                speechAnimation.add(spst);
                            }
                            for (int i = 0; i < 10; i++) {
                                speechAnimation.add(new float[]{0,0,0.05f,0,0});
                            }

                            speechAnimation.subList(0,10).clear();
                            onFinish.run();
//                            new FlexatarVideoEncoder(320,320,speechAnimation);
//                            completion.run();
                            Log.d(TAG,"animation count : "+speechAnimation.size() + " "+speechAnimation.size()/20);
                        }
                    });

                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        });
    }


    public static final int I800 = 800;
    private final float[] animBuffer = new float[I800];
    private final float[] animBufferNext = new float[I800];
    int bufferPosition = 0;
    Object sync = new Object();
    public boolean bufferCollector(float[] part){
        synchronized (sync) {
            if (bufferPosition > I800) {
                bufferPosition -= I800;
                System.arraycopy(animBufferNext, 0, animBuffer, 0, I800);
            }
            int sizeToFill = I800 - bufferPosition;
            if (sizeToFill >= part.length) {
                System.arraycopy(part, 0, animBuffer, bufferPosition, part.length);
                bufferPosition += part.length;
                if (bufferPosition == I800) {
                    bufferPosition = 0;
                    return true;
                }
            } else {
                System.arraycopy(part, 0, animBuffer, bufferPosition, sizeToFill);
                System.arraycopy(part, sizeToFill, animBufferNext, 0, part.length - sizeToFill);
                bufferPosition += part.length;
                return true;
            }
            return false;
        }

    }

}
