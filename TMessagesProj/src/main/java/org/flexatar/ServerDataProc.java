package org.flexatar;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.UserConfig;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ServerDataProc {

    public static String[] getFlexatarLinkList(String jsonString,String fClass){
        try {
            JSONObject list = new JSONObject(jsonString);
            JSONArray listElements = list.getJSONArray(fClass);
            String[] ret = new String[listElements.length()];
             for (int i = 0; i < listElements.length(); i++) {
                 ret[i] = listElements.getJSONObject(i).getString("ftar");

            }
             return ret;
        } catch (JSONException e) {
            return new String[0];
        }

    }
    public static String[] getFlexatarIdList(String jsonString,String fClass){
        try {
            JSONObject list = new JSONObject(jsonString);
            JSONArray listElements = list.getJSONArray(fClass);
            String[] ret = new String[listElements.length()];
            for (int i = 0; i < listElements.length(); i++) {
                ret[i] = listElements.getJSONObject(i).getString("id");

            }
            return ret;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }

    public static String genDeleteRout(String ftar){
        int lastSlashIndex = ftar.lastIndexOf('/');
        return ftar.substring(0, lastSlashIndex);
    }
    public static String routToFileName(String rout,String prefix){
        return prefix+rout+".flx";
    }
    public static String fileNameToRout(String rout){
        if (!rout.startsWith(FlexatarStorageManager.FLEXATAR_PREFIX)) return null;
        Log.d("FLX_INJECT","fileNameToRout "+rout);
        String flxId = rout.replace(FlexatarStorageManager.FLEXATAR_PREFIX, "");
        flxId = flxId.replace(".flx","");
        Log.d("FLX_INJECT","fileNameToRout "+flxId);

        long telegramID = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser().id;
        rout = "private/1.00/tg/"+telegramID+"/"+flxId+"/"+flxId +".p";
        return rout;
    }
    public static String fileNameToMetaRout(String rout){
        if (!rout.startsWith(FlexatarStorageManager.FLEXATAR_PREFIX)) return null;
        Log.d("FLX_INJECT","fileNameToRout "+rout);
        String flxId = rout.replace(FlexatarStorageManager.FLEXATAR_PREFIX, "");
        flxId = flxId.replace(".flx","");
        Log.d("FLX_INJECT","fileNameToRout "+flxId);

        long telegramID = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser().id;
        rout = "private/1.00/tg/"+telegramID+"/"+flxId+"/"+flxId +".m";
        return rout;
    }
    public static class FlexatarChatCellInfo{
        public String ftar;
        public boolean download;
        public String code;
        public String verify;

        public boolean isSet(){
            return ftar!=null || verify!=null;
        }

        public String getFileName() {
            if (ftar == null)return null;
            String[] splitFtar = ftar.split("/");
            String prefix = FlexatarStorageManager.FLEXATAR_PREFIX;
            if(splitFtar[0].equals("public"))
                prefix = FlexatarStorageManager.PUBLIC_PREFIX;
            String fid = splitFtar[splitFtar.length - 1].replace(".p", "");
            return prefix + fid + ".flx";
        }
    }
    public static FlexatarChatCellInfo parseFlexatarCellUrl(String urlStr){
        try {
            FlexatarChatCellInfo ret = new FlexatarChatCellInfo();
            String queryString = urlStr.split("\\?")[1];
            String[] queryPairs = queryString.split("&");
//            Log.d("FLX_INJECT", "queryString " + queryString);
//            Log.d("FLX_INJECT", "queryPairs " + queryPairs.length);

            for (String pair :  queryPairs){
                String[] splited = pair.split("=");
                if (splited.length<=1) continue;
                if (splited[0].equals("ftar")){
                    String notFullUrl = splited[1];
                    String[] splited1 = notFullUrl.split("/");
                    notFullUrl += "/"+splited1[splited1.length-1]+".p";
                    ret.ftar = notFullUrl;
                }
                if (splited[0].equals("verify")){
                    ret.verify = splited[1];
                }
                if (splited[0].equals("download")){
                    ret.download = splited[1].equals("true");
                }
                if (splited[0].equals("code")){
                    ret.code = splited[1];
                }
            }

            return ret.isSet() ? ret : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
//        String[] urlSplit = url.split("\\?");
//        if (urlSplit.length<2) return null;
//        urlSplit[1].
    }
    /*public static class FlexatarListResponse{

        private final String[] linksPublic;
        private final String[] idsPublic;
        private final String[] linksPrivate;
        private final String[] idsPrivate;

        public ArrayList<String> getPublicLinksToDownload() {
            return publicLinksToDownload;
        }

        public ArrayList<String> getPublicIdsToDownload() {
            return publicIdsToDownload;
        }

        public ArrayList<String> getPrivateLinksToDownload() {
            return privateLinksToDownload;
        }

        public ArrayList<String> getPrivateIdsToDownload() {
            return privateIdsToDownload;
        }

        private final ArrayList<String> publicLinksToDownload;
        private final ArrayList<String> publicIdsToDownload;
        private final ArrayList<String> privateLinksToDownload;
        private final ArrayList<String> privateIdsToDownload;

        public FlexatarListResponse(String response){
            Log.d("FLX_INJECT", "FlexatarListResponse: "+response);
            linksPublic = ServerDataProc.getFlexatarLinkList(response, "public");
            idsPublic = ServerDataProc.getFlexatarIdList(response, "public");
            linksPrivate = ServerDataProc.getFlexatarLinkList(response, "private");
            idsPrivate = ServerDataProc.getFlexatarIdList(response, "private");

            List<String> fidsPublic = FlexatarStorageManager.getSavedFids(FlexatarStorageManager.PUBLIC_PREFIX);
            publicLinksToDownload = new ArrayList<>();
            publicIdsToDownload = new ArrayList<>();
            for (int i = 0; i < idsPublic.length; i++) {
                if (!fidsPublic.contains(idsPublic[i])){
                    publicLinksToDownload.add(linksPublic[i]);
                    publicIdsToDownload.add(idsPublic[i]);
                }
            }

            List<String> fidsPrivate = FlexatarStorageManager.getSavedFids(FlexatarStorageManager.FLEXATAR_PREFIX);
            privateLinksToDownload = new ArrayList<>();
            privateIdsToDownload = new ArrayList<>();
            for (int i = 0; i < idsPrivate.length; i++) {
                if (!fidsPrivate.contains(idsPrivate[i])){
                    privateLinksToDownload.add(linksPrivate[i]);
                    privateIdsToDownload.add(idsPrivate[i]);
                }
            }
        }
        public boolean hasPrivate(){
            return privateLinksToDownload.size()>0;
        }
        public boolean hasPublic(){
            return publicLinksToDownload.size()>0;
        }
    }*/

}
