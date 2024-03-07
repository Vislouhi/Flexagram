package org.flexatar;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.UserConfig;

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
