package org.flexatar;

import org.flexatar.DataOps.FlexatarData;

import java.util.Timer;
import java.util.TimerTask;

public class FlexatarAnimator {
    private Timer timer;
    private int animationPatternIdx = 0;
//    InterUnit interUnit;
    int animIdx = 0;
    public AnimationUnit animUnit;
    private float[] point = {0.5f,0.45f};
    public boolean isActive = false;
    public FlexatarAnimator(){
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
    public void start(){
        synchronized (mutexObject) {
            if (!isActive) {
                timer = new Timer();

                TimerTask task = new TimerTask() {


                    @Override
                    public void run() {

                        float tx = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(animIdx)[0];
                        float ty = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(animIdx)[1] + 0.2f;
                        float sc = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(animIdx)[2];
                        float rx = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(animIdx)[3];
                        float ry = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(animIdx)[4];
                        float rz = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(animIdx)[5];
                        float eyebrow = FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).get(animIdx)[6];
                        point = new float[2];
                        point[0] = rx;
                        point[1] = ry;


                        animUnit = new AnimationUnit(tx, ty, sc, rz, eyebrow, BlinkGenerator.nextBlinkWeight());
                        animIdx += 2;

                        if (animIdx >= FlexatarCommon.emoAnimPatterns.get(animationPatternIdx).size()) {
                            animIdx = 0;
                        }
                        if (FlexatarRenderer.isEffectsOn && FlexatarRenderer.effectID == 1){
                            FlexatarRenderer.effectsMixWeight += 0.0025f;
                            if (FlexatarRenderer.effectsMixWeight>1){FlexatarRenderer.effectsMixWeight = 0;}
                        }
                        if (FlexatarRenderer.isEffectsOn && FlexatarRenderer.isMorphEffect){
                            FlexatarRenderer.effectsMixWeight += 0.005f;
                            if (FlexatarRenderer.effectsMixWeight>1){FlexatarRenderer.effectsMixWeight = 1;}
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
    public InterUnit getInterUnit(FlexatarData flxData){
        return InterUnit.makeInterUnit(point,flxData.mandalaTriangles,flxData.mandalaFaces,flxData.mandalaBorder);
    }
    public void release(){
        synchronized (mutexObject) {
            timer.cancel();
            timer.purge();
            isActive = false;
        }
    }
}
