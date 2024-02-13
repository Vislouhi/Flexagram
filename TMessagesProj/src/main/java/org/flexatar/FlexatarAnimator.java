package org.flexatar;

import com.google.android.exoplayer2.util.Log;

import org.flexatar.DataOps.FlexatarData;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class FlexatarAnimator {
    private Timer timer;
    private int animationPatternIdx = 0;
//    InterUnit interUnit;
    private int animIdx = 50;
    public AnimationUnit animUnit;
    private float[] point = {0.5f,0.45f};
    public boolean isActive = false;
    public FlexatarAnimator(){
        Random random = new Random();
        random.setSeed(System.nanoTime());
        animIdx = 50 + random.nextInt(200);
       /* timer = new Timer();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {

                float tx = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(animIdx)[0];
                float ty = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(animIdx)[1]+0.2f;
                float sc = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(animIdx)[2];
                float rx = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(animIdx)[3];
                float ry = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(animIdx)[4];
                float rz = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(animIdx)[5];
                float eyebrow = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(animIdx)[6];
                float[] point = new float[2];
                point[0] = rx;
                point[1] = ry;

                interUnit = InterUnit.makeInterUnit(point,flxData.mandalaTriangles,flxData.mandalaFaces,flxData.mandalaBorder);
                animUnit = new AnimationUnit(tx,ty,sc,rz,eyebrow,BlinkGenerator.nextBlinkWeight());
                animIdx+=2;

                if (animIdx >= FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).size()){animIdx = 0;}

                // Code to be executed repeatedly
//                System.out.println("Task executed at regular interval.");
            }
        };

        // Schedule the task to run every 1000 milliseconds (1 second)
        timer.scheduleAtFixedRate(task, 0, 40);*/

    }
    private static final Object mutexObject = new Object();
//    private float effectsWeightStep = 0.05f;
    public int usageCounter = 0;
    public int counter5 = 0;
    public void start(){
        usageCounter++;
        synchronized (mutexObject) {
            if (!isActive) {
                timer = new Timer();

                TimerTask task = new TimerTask() {


                    @Override
                    public void run() {

                       next();
                        counter5++;
                        if (counter5>5){
                            counter5 = 0;
                            if (usageCounter == 0){
                                release();
                            }
                            usageCounter = 0;
                        }
                        // Code to be executed repeatedly
//                System.out.println("Task executed at regular interval.");
                    }
                };

                // Schedule the task to run every 1000 milliseconds (1 second)
                timer.scheduleAtFixedRate(task, 0, 40);
                isActive = true;




            }
        }
    }
    public static final Object mandalaTriangleMutex = new Object();
    public InterUnit getInterUnit(FlexatarData flxData){
        synchronized (mandalaTriangleMutex) {
            return InterUnit.makeInterUnit(point, flxData.mandalaTriangles, flxData.mandalaFaces, flxData.mandalaBorder);
        }
    }
    public void release(){
        synchronized (mutexObject) {
            isActive = false;
            if (timer == null) return;
            timer.cancel();
            timer.purge();

        }
    }
    private int delta = 2;
    public void next() {
        float tx = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(animIdx)[0];
        float ty = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(animIdx)[1] + 0.0f;
        float sc = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(animIdx)[2];
        float rx = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(animIdx)[3];
        float ry = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(animIdx)[4];
        float rz = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(animIdx)[5];
        float eyebrow = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(animIdx)[6];
        point = new float[2];
        point[0] = rx;
        point[1] = ry;


        animUnit = new AnimationUnit(tx, ty, sc, rz, eyebrow, BlinkGenerator.nextBlinkWeight());
        animIdx += delta;

        if (animIdx >= FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).size() || animIdx<0) {
            delta = -delta;
            animIdx += delta;
        }

        /*if (FlexatarRenderer.isEffectsOn && FlexatarRenderer.effectID == 1){
            FlexatarRenderer.effectsMixWeight += 0.0025f;
            if (FlexatarRenderer.effectsMixWeight>1){FlexatarRenderer.effectsMixWeight = 0;}
        }
        if (FlexatarRenderer.isEffectsOn && FlexatarRenderer.isMorphEffect){
            FlexatarRenderer.effectsMixWeight += 0.005f;
            if (FlexatarRenderer.effectsMixWeight>1){FlexatarRenderer.effectsMixWeight = 1;}
        }*/
//        Log.d("FLX_INJECT", "animator working");
    }

    public void reverse() {
        delta = -delta;
    }
}
