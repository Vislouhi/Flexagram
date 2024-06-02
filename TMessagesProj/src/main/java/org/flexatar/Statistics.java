package org.flexatar;

import android.content.Context;
import android.util.Log;

import org.flexatar.DataOps.Data;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.UserConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Statistics {
    private static final String TAG = "FLX_INJECT";
    private static File getStatFolder(){
        Context context = ApplicationLoader.applicationContext;
        File rootDir = context.getFilesDir();
        File statFolder = new File(rootDir,"flx_stat");
        if (!statFolder.exists()) statFolder.mkdir();
        return statFolder;

    }

    private static void addLine(String line){
        String fileName = "" + AccountInstance.getInstance(UserConfig.selectedAccount).getUserConfig().getClientUserId() + ".txt";
        File userStatFile = new File(getStatFolder(),fileName);
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(userStatFile, true);
            fileWriter.append(line);
            fileWriter.append("\n");
//            Log.d(TAG, "Line appended to file successfully.");
        } catch (IOException e) {
            Log.e(TAG, "Error appending line to file: " + e.getMessage());
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing FileWriter: " + e.getMessage());
                }
            }
        }
    }
    public static void addLine(Element element){
        addLine(element.toJson().toString());
    }
    public static void check(){
        addLine("{\"tag\":\"test line\"}");
    }

    public static class Element extends SrializerJson{
        public String type;
//          flx_p - photo flexatar button pressed switch
//          flx_v - video flexatar button pressed switch
//          eff - effect button pressed
//          rnd - round video made
//          fin - call finished
//          stt - call started
        public String time;
        public String flxID1;
        public String flxID2;
        public String effect;
        public Element(String jsonString){
            try {
                this.formJson(jsonString);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

        }
        public Element(String type,String flxID1,String flxID2,String effect){
            this.type = type;
            this.flxID1 = flxID1;
            this.flxID2 = flxID2;
            this.effect = effect;
            LocalDateTime currentDateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            this.time = currentDateTime.format(formatter);

        }
        public boolean isEffect(){
            return this.type.equals("eff");
        }
        public boolean isFlexatar(){
            return this.type.equals("flx_p") || this.type.equals("flx_v");
        }
        public boolean isRound(){
            return this.type.equals("rnd_p") || this.type.equals("rnd_v");
        }
        public boolean isRoundVideo(){
            return this.type.equals("rnd_v");
        }
        public boolean isRoundPhoto(){
            return this.type.equals("rnd_p");
        }
        public String getType(){
            return this.type.equals("flx_p") ? "photo" : "video";
        }
        public boolean isFin(){
            return this.type.equals("fin");
        }
    }
    private static Map<String,String> effectName = new HashMap<>();
    static {
        effectName.put("0", "no");
        effectName.put("1", "mix");
        effectName.put("2", "morph");
        effectName.put("3", "hybrid");
    }

    public static void process(int account){

        String fileName = "" + AccountInstance.getInstance(account).getUserConfig().getClientUserId() + ".txt";
        File userStatFile = new File(getStatFolder(),fileName);
        if (!userStatFile.exists()) return;
        BufferedReader bufferedReader = null;
        List<Element> elements = new ArrayList<>();
        try {
            bufferedReader = new BufferedReader(new FileReader(userStatFile));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                // Process each line
                elements.add(new Element(line));
//                Log.d(TAG, "Read line: " + line);

            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading file: " + e.getMessage());
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing BufferedReader: " + e.getMessage());
                }
            }
        }


        Map<String,FlxRecord> flexatarNoEffectUsage = new HashMap<>();
        Map<String,FlxRecord> flexatarEffectUsage = new HashMap<>();
        Map<String,FlxRecord> flexatarEffectUsage2 = new HashMap<>();
        Map<String,FlxRecord> effectUsage = new HashMap<>();

        Map<String,FlxRecord> flexatarRoundNoEffectUsage = new HashMap<>();
        Map<String,FlxRecord> flexatarRoundEffectUsage = new HashMap<>();
        Map<String,FlxRecord> flexatarRoundEffectUsage2 = new HashMap<>();
        Map<String,FlxRecord> effectRoundUsage = new HashMap<>();

        String currentEffect = "0";
        String currentUsingEffect = null;
        String currentFlxId1 = null;
        String currentFlxId2 = null;
        String currentFlxType = "photo";
        LocalDateTime currentDate = null;
//        LocalDateTime currentEffectDate = null;
//        JSONArray roundsPhoto = new JSONArray();
//        JSONArray roundsVideo = new JSONArray();
        for (Element el : elements){
            if (el.isRound()) {
                String rndFlxId1 = el.flxID1;
                String rndFlxId2 = el.flxID2;
                String rndType = el.isRoundPhoto() ? "photo":"video";
                Map<String, FlxRecord> collector;
                if (currentEffect.equals("0")) {
                    collector = flexatarRoundNoEffectUsage;
                } else {
                    collector = flexatarRoundEffectUsage;

                    if (!flexatarRoundEffectUsage.containsKey(rndFlxId2)) {
                        flexatarRoundEffectUsage.put(rndFlxId2, new FlxRecord(rndType));
                    }
                    flexatarRoundEffectUsage.get(rndFlxId2).usageTime += 1;
                }
                if (!collector.containsKey(rndFlxId1)) {
                    collector.put(rndFlxId1, new FlxRecord(rndType));
                }
                collector.get(rndFlxId1).usageTime += 1;

                if (el.isRoundPhoto()) {
                    String rndEff = el.effect;
                    if (!effectRoundUsage.containsKey(rndEff)) {
                        effectRoundUsage.put(rndEff, new FlxRecord("photo"));
                    }
                    effectRoundUsage.get(rndEff).usageTime += 1;
                }


                continue;
            }
            LocalDateTime newDate = parseDate(el.time);
            Map<String, FlxRecord> collector;
            if (currentFlxId1!=null) {
                if (currentEffect.equals("0")) {
                    collector = flexatarNoEffectUsage;
                } else {

                    collector = flexatarEffectUsage;
                }
                if (!collector.containsKey(currentFlxId1)) {
                    collector.put(currentFlxId1, new FlxRecord(currentFlxType));

                }
                collector.get(currentFlxId1).usageTime += duration(currentDate, newDate);
            }

            if (currentFlxId2!=null){
                if (!currentEffect.equals("0")) {
                    if (!flexatarEffectUsage2.containsKey(currentFlxId2)) {
                        flexatarEffectUsage2.put(currentFlxId2, new FlxRecord("photo"));
                    }
                    if (currentDate != null) {
                        flexatarEffectUsage2.get(currentFlxId2).usageTime += duration(currentDate, newDate);
                    }
                }
            }


            if (currentUsingEffect!=null){
                if (!effectUsage.containsKey(currentUsingEffect)) {
                    effectUsage.put(currentUsingEffect, new FlxRecord("photo"));
                }
                effectUsage.get(currentUsingEffect).usageTime += duration(currentDate, newDate);
            }
            if (el.isEffect())
                currentEffect = el.effect;
            currentUsingEffect = currentEffect;
//            currentEffectDate = newDate;

            if (el.isFlexatar()) {
//                currentUsingEffect = currentEffect;
                currentFlxId1 = el.flxID1;
                currentFlxId2 = el.flxID2;
                currentFlxType = el.getType();

            }
            if (el.isFin()) {
                currentFlxId1 = null;
                currentFlxId2 = null;
                currentUsingEffect = null;
            }
            if (currentFlxType.equals("video")) currentUsingEffect = null;

            currentDate = newDate;

        }
        for (Map.Entry<String,FlxRecord> entry : flexatarEffectUsage2.entrySet()){
            if (flexatarEffectUsage.containsKey(entry.getKey())){
                flexatarEffectUsage.get(entry.getKey()).usageTime += entry.getValue().usageTime;
            }
        }

        JSONObject statJsonCalls = makeJsonUsageRecord(flexatarNoEffectUsage,flexatarEffectUsage,effectUsage);
        JSONObject statJsonRound = makeJsonUsageRecord(flexatarRoundNoEffectUsage,flexatarRoundEffectUsage,effectRoundUsage);
        JSONObject fullStat = new JSONObject();
        try {
            fullStat.put("call",statJsonCalls);
            fullStat.put("round",statJsonRound);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }


        Log.d(TAG, "full stat  "+fullStat);
        Data data = new Data(fullStat.toString());

        FlexatarServerAccess.requestJson(FlexatarServiceAuth.getVerification(account), "statistics/usage", "POST", data.value, "application/json", new FlexatarServerAccess.OnRequestJsonReady() {

            @Override
            public void onReady(FlexatarServerAccess.StdResponse response) {
                Log.d("FLX_INJECT","flx marketing request success ");
                userStatFile.delete();
            }

            @Override
            public void onError() {
                Log.d("FLX_INJECT","flx marketing request fail ");
            }
        });
    }
    private static JSONObject makeJsonUsageRecord(Map<String,FlxRecord> flexatarNoEffectUsage,Map<String,FlxRecord> flexatarEffectUsage,Map<String,FlxRecord> effectUsage){
        JSONArray singlePhoto = new JSONArray();
        JSONArray singleVideo = new JSONArray();


        for (Map.Entry<String,FlxRecord> entry : flexatarNoEffectUsage.entrySet()){
            JSONObject entryJson = createJsonEntry(entry);
            if (entry.getValue().isPhoto()) {
                singlePhoto.put(entryJson);
            }else {
                singleVideo.put(entryJson);
            }
        }

        JSONArray effectPhoto = new JSONArray();
        JSONArray effectVideo = new JSONArray();

        for (Map.Entry<String,FlxRecord> entry : flexatarEffectUsage.entrySet()){
            JSONObject entryJson = createJsonEntry(entry);
            if (entry.getValue().isPhoto()) {
                effectPhoto.put(entryJson);
            }else {
                effectVideo.put(entryJson);
            }
        }


        JSONObject effectsJson = new JSONObject();

        for (Map.Entry<String,FlxRecord> entry : effectUsage.entrySet()){
            String eff = effectName.get(entry.getKey());
            try {
                effectsJson.put(eff,entry.getValue().usageTime);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
//            Log.d(TAG, "effect : "+entry.getKey()+ " used for : " +entry.getValue().usageTime);
        }
        JSONObject statJson = new JSONObject();
        try {
            statJson.put("single_photo",singlePhoto);
            statJson.put("single_video",singleVideo);
            statJson.put("effect_photo",effectPhoto);
            statJson.put("effect_video",effectVideo);
            statJson.put("effect_usage",effectsJson);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return statJson;
    }
    private static class FlxRecord{
        public String id;
        public String type;
        public long usageTime = 0L;
        public FlxRecord(String type){
            this.type=type;
        }
        public boolean isPhoto(){
            return type.equals("photo");
        }
    }

    public static LocalDateTime parseDate(String date){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime startDate = LocalDateTime.parse(date,formatter);
        return startDate;
    }
    public static long duration(LocalDateTime t1,LocalDateTime t2){
        return Duration.between(t1, t2).getSeconds();
    }
    public static JSONObject createJsonEntry(Map.Entry<String,FlxRecord> entry){
        String[] idSplit = splitStringByFirstUnderscore(entry.getKey());
        JSONObject entryJson = new JSONObject();
        try {
            entryJson.put("type",idSplit[0].equals("flexatar") ? "private" : idSplit[0]);
            entryJson.put("id",idSplit[1]);
            entryJson.put("time",entry.getValue().usageTime);
            return entryJson;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    public static String[] splitStringByFirstUnderscore(String input) {
        int index = input.indexOf('_');
        if (index != -1) {
            String beforeUnderscore = input.substring(0, index);
            String afterUnderscore = input.substring(index + 1);
            return new String[]{beforeUnderscore, afterUnderscore};
        } else {
            // If no underscore is found, return the original string and an empty string
            return new String[]{input, ""};
        }
    }
}
//    LocalDateTime currentDateTime = LocalDateTime.now();
//    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//    LocalDateTime startDate = LocalDateTime.parse(date,formatter);
//    Duration duration = Duration.between(startDate, currentDateTime);