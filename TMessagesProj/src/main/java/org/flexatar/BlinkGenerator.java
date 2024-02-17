package org.flexatar;

import java.util.Random;

public class BlinkGenerator {
    public static int blinkPauseCounter = 100;
    public static int blinkCounter = 0;
    static Random random = new Random();

    public static float nextBlinkWeight(){
        float blinkWeight = 0;
        try {

            if (blinkPauseCounter == 0) {
                if (FlexatarCommon.blinkPattren.length == blinkCounter) {
                    blinkCounter = 0;
                    blinkPauseCounter = random.nextInt(100) + 50;
                } else {
//                        blinkWeight = 1f;
                    blinkWeight = FlexatarCommon.blinkPattren[blinkCounter];
                    blinkCounter += 1;
                }

            } else {
                blinkPauseCounter -= 1;
            }
        }catch (ArrayIndexOutOfBoundsException e){
            blinkWeight = 0;
            blinkCounter = 0;
            blinkPauseCounter = random.nextInt(100) + 50;
        }
        return blinkWeight;
    }
}
