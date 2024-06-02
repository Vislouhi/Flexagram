package org.flexatar;

import android.content.Context;
import android.util.Log;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class FlxDownloadTaskQueue {
    public enum Action {ADD,DELETE}
    public static class Task {
        public Action action = null;
        public int account;
    }
    private final static List<String> taskList = new CopyOnWriteArrayList<>();
    private static Map<String,List<Task>> tasks = new HashMap<>();
    private static boolean downloadInProgress = false;
    private static final Object sync = new Object();

    private static void fulfillForFlx(String ftar){
        if (tasks.containsKey(ftar)){
            List<Task> currentTasks = tasks.get(ftar);

            for (Task task : currentTasks){
                Log.d("FLX_INJECT","fulfilling task for ftar" +ftar);
                if (task.action == Action.ADD){
                    File ftarCacheFile = new File(CacheController.cacheDir(), ServerDataProc.flxFileNameByRoute(ftar));
                    if (ftarCacheFile.exists()) {
                        FlexatarStorageManager.FlxDesc desc = FlexatarStorageManager.addToStorage(ApplicationLoader.applicationContext, task.account, ftarCacheFile);
                        FlexatarMessageController.FlexatarAddToGalleryListener listener = FlexatarMessageController.getInstance(task.account).flexatarAddToGalleryListener;
                        if (listener!=null){
                            listener.onAddToGallery(desc.file,desc.type);
                        }
                    }
                }else if (task.action == Action.DELETE){
                    Context mContext = ApplicationLoader.applicationContext;
                    File flexatarStorageFolder = FlexatarStorageManager.getFlexatarStorage(mContext,task.account);
                    String fileName = ServerDataProc.flxFileNameByRoute(ftar);
                    File ftarLocalFile = new File(flexatarStorageFolder, fileName);
                    String flxID = fileName.replace(".flx", "");
                    FlexatarStorageManager.removeGroupRecord(mContext,UserConfig.selectedAccount, flxID);
                    FlexatarStorageManager.removeHiddenRecord(mContext,UserConfig.selectedAccount,flxID);
                    FlexatarStorageManager.deleteFromStorage(mContext,task.account,ftarLocalFile);
                }
            }
            tasks.remove(ftar);
        }

    }
    public static void addTask(int account,String ftar,Action action){
        if (!tasks.containsKey(ftar)){
            tasks.put(ftar,new ArrayList<>());
        }
        Task task = new Task();
        task.account=account;
        task.action=action;
        tasks.get(ftar).add(task);

        File ftarCacheFile = new File(CacheController.cacheDir(), ServerDataProc.flxFileNameByRoute(ftar));
        if ((ftarCacheFile.exists() && action == Action.ADD) || action == Action.DELETE){
            Log.d("FLX_INJECT","copy or delete flexatar");
            fulfillForFlx(ftar);
        }else{
            Log.d("FLX_INJECT","no cached flexatar add action to queue flexatar");
            if (action == Action.ADD)
                addDownloadTask(ftar);

        }
    }
    public static void addDownloadTask(String ftar){
        if (taskList.contains(ftar)) return;
        taskList.add(ftar);
        synchronized (sync) {
            if (!downloadInProgress) {
                downloadInProgress = true;
            } else {
                return;
            }
        }
        downloadRecursive(taskList);
    }


    private static void downloadRecursive(List<String> tList){
        String ftar = tList.get(0);
        File ftarCacheFile = new File(CacheController.cacheDir(), ServerDataProc.flxFileNameByRoute(ftar));
        if (ftarCacheFile.exists()){
            Log.d("FLX_INJECT","file already in cache");
            tList.remove(0);
            if (!downloadInProgress){
                downloadRecursive(tList);
            }else{
                synchronized (sync){
                    downloadInProgress = false;
                }
            }
            return;
        }
        Log.d("FLX_INJECT","Start download");
        FlexatarServerAccess.downloadFlexatar(0,ftarCacheFile, ftar, new FlexatarServerAccess.OnReadyOrErrorListener() {
            @Override
            public void onReady(File flexatarFile, int flexatarType) {
                Log.d("FLX_INJECT","Flexatar loaded to cache");
                fulfillForFlx(ftar);
                tList.remove(0);
                if (tList.size()>0){
                    downloadRecursive(tList);
                }else{
                    synchronized (sync){
                        downloadInProgress = false;
                    }
                }
//                    completion.run();
//                    if (flexatarAddToGalleryListener!=null){
//                        flexatarAddToGalleryListener.onAddToGallery(flexatarFile,flexatarType);
//                    }
            }

            @Override
            public void onError() {
                Log.d("FLX_INJECT","Flexatar loaded error");
                fulfillForFlx(ftar);
                tList.remove(0);
                if (tList.size()>0){
                    downloadRecursive(tList);
                }else{
                    synchronized (sync){
                        downloadInProgress = false;
                    }
                }
//                    completion.run();
            }
        });
    }

}
