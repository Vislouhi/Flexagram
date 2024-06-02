package org.flexatar;

import android.util.Log;

import org.telegram.messenger.ApplicationLoader;

import java.io.File;

public class CacheController {
    private static File cacheDir;
    public static File cacheDir(){
        if (cacheDir!=null) return cacheDir;
        cacheDir = new File(ApplicationLoader.applicationContext.getCacheDir(), "flx_cache");
        if (!cacheDir.exists()){
            if (cacheDir.mkdir()){
                Log.d("FLX_INJECT","flx cache dir created");
            }else{
                Log.d("FLX_INJECT","flx cache dir failed to create");
            }
        }
        return cacheDir;
    }
    public static File makeDirInCache(String dirName) {
        File dir = new File(cacheDir(), dirName);
        if (!dir.exists()){
            if (dir.mkdir()){
                Log.d("FLX_INJECT","flx cache dir created");
            }else{
                Log.d("FLX_INJECT","flx cache dir failed to create");
            }
        }
        return dir;
    }
    public static File cacheDirPhoto(){
        return makeDirInCache("photo");
    }
    public static File cacheDirVideo(){
        return makeDirInCache("video");
    }
}
