package org.flexatar;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.flexatar.DataOps.AssetAccess;
import org.flexatar.DataOps.FlexatarData;
import org.flexatar.DataOps.LengthBasedFlxUnpack;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Config {
    public static long authBotId = 6818271084L;
    public static final boolean debugMode = true;

    public static final String version = "pre-historic";


    public static CountDownLatch stopRecordingAudioSemaphore = null;
    public static Runnable sendAudioCallback = null;
    public static Runnable chooseFlexatarForAudioCallback = null;

    public static CountDownLatch startSendFlexatarRoundSemaphore = null;
    public static boolean chosenAudioWithFlexatar;
    public static boolean chosenSendFlexatarRoundVideo;
    public static boolean sendFlexatarRoundVideoCanceled;

    public static void runAudioCallback(){
        if (sendAudioCallback != null) sendAudioCallback.run();
    }
    public static void runChooseFlexatarForAudioCallback(){
        if (chooseFlexatarForAudioCallback != null) chooseFlexatarForAudioCallback.run();
    }
    public static void signalRecordAudioSemaphore(){
        if (Config.stopRecordingAudioSemaphore !=null)
            Config.stopRecordingAudioSemaphore.countDown();
    }
    public static void signalSendFlexatarSemaphore(){
        if (Config.startSendFlexatarRoundSemaphore !=null)
            Config.startSendFlexatarRoundSemaphore.countDown();
    }

    public static String token = null;
    public static String verify = null;
    public static String storage = null;
    public static String stat = null;



}
