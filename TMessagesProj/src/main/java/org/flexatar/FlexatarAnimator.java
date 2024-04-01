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
    private float animIdx = 50;
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
    public float headScale = 0f;
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
    private float delta = 1.75f;
    private float[] getAnimVector(int idx){
//        animationPatternIdx = 4;
        float yShift = 0.0f;
        float scaleCorrection = 0.0f;
        float scaleFactor = 0.5f;
        if (animationPatternIdx == 0){
            yShift = 0.4f;
            scaleCorrection = 0.045f;
        }
        else if (animationPatternIdx == 1){
            yShift = 0.0f;
            scaleCorrection = -0.02f;
        }else if (animationPatternIdx == 3){
            yShift = 0.3f;
            scaleCorrection = -0.02f;

        }else if (animationPatternIdx == 4){
            scaleFactor = 0.25f;
            scaleCorrection = -0.02f;
        }
        float tx = 4*FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(idx)[0];
        float ty = 4*FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(idx)[1] - yShift;
        float sc = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(idx)[2]*scaleFactor+scaleCorrection;
        float rx = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(idx)[3];
        float ry = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(idx)[4];
        float rz = -FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(idx)[5]*0.5f;
        float eyebrow = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(idx)[6];
        return new float[]{tx,ty,sc,rx,ry,rz,eyebrow};

    }
    public void next() {

        int firstIdx = (int)animIdx;
        float wInv = animIdx - (float)firstIdx;
        float w = 1f - wInv;
        float[] v1 = getAnimVector(firstIdx);
        float[] v2 = getAnimVector(firstIdx+1);
        float tx = v1[0]*w+v2[0]*wInv;
        float ty = v1[1]*w+v2[1]*wInv;
        float sc = v1[2]*w+v2[2]*wInv;
        float rx = v1[3]*w+v2[3]*wInv;
        float ry = v1[4]*w+v2[4]*wInv;
        float rz = v1[5]*w+v2[5]*wInv;
        float eyebrow = v1[6]*w+v2[6]*wInv;
        point = new float[2];
        point[0] = rx;
        point[1] = ry;


        animUnit = new AnimationUnit(tx, ty, sc+headScale, rz, eyebrow, BlinkGenerator.nextBlinkWeight());
        animIdx += delta;

        if (animIdx >= (FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).size()-1)|| animIdx < 0) {
//            delta = -delta;
//            animIdx += delta;
            animIdx = 0;
            animationPatternIdx+=1;
            if (animationPatternIdx >= FlexatarCommon.emoAnimPatterns.size()){
                animationPatternIdx=0;
            }
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


    /*private int morphStage1Counter = 0;
    private int morphStage2Counter = 0;
    private final int changeDelta = 25*4;
    private final int morphDelta = 25;
    private boolean morphStage = false;
    private int flexatarIdx = 0;

    private void morphSequence(){
        if (!morphStage)
            morphStage1Counter+=1;
        if (morphStage1Counter>changeDelta) {
            morphStage1Counter = 0;
            flexatarIdx+=1;
        }
    }*/
}
