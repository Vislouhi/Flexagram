package org.flexatar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
            throw new RuntimeException(e);
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
    public static String routToFileName(String rout){
        return rout.replace("/","__");
    }
    public static String fileNameToRout(String rout){
        return rout.replace("__","/");
    }
}
