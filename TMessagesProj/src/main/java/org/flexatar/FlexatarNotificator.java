package org.flexatar;

import android.util.Log;

import java.io.File;

public class FlexatarNotificator {

    public static ChosenStateForRoundVideo chosenStateForRoundVideo;
    public static boolean isMakingFlexatarRoundVideo = false;
    public static int drawerCounter = 0;
    public static void incDrawerCounter(){
        drawerCounter++;
        Log.d("FLX_INJECT", "inc drawer instance count "+drawerCounter);
    }
    public static void decDrawerCounter(){
        drawerCounter--;
        Log.d("FLX_INJECT", "dec drawer instance count "+drawerCounter);
    }
    public static class ChosenStateForRoundVideo{
        public static final int NO = 0;
        public static final int MIX = 1;
        public static final int MORPH = 2;
        public static final int HYBRID = 3;
        public File firstFile;
        public File secondFile;
        public int effect = 0;
        public float mixWeight = 1f;

    }

}
