package org.flexatar;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ValueStorage {
    static final String PREF_STORAGE_NAME = "FlexatarStorage";
    private static final String DONT_SHOW_FLEXATAR_PHOTO_INSTRUCTIONS = "flexatarPhotoInst";
    private static final String MAKE_FLEXATAR_TICKETS = "flexatarTickets";
    private static Object ticketMutex = new Object();
    public static boolean checkIfDontShowFlexatarPhotoInstructions(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_STORAGE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(DONT_SHOW_FLEXATAR_PHOTO_INSTRUCTIONS, false);
    }
    public static void setfDontShowFlexatarPhotoInstructions(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_STORAGE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(DONT_SHOW_FLEXATAR_PHOTO_INSTRUCTIONS, true);
        editor.apply();
    }
    public static synchronized void clearAllTickets(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_STORAGE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(MAKE_FLEXATAR_TICKETS, "[]");
        editor.apply();
    }
    public static synchronized void changeTicketStatus(Context context,int index,String status){

        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_STORAGE_NAME, Context.MODE_PRIVATE);
        String flexatarTicketsString = sharedPreferences.getString(MAKE_FLEXATAR_TICKETS, "[]");
        try {
            JSONArray jsonArray =  new JSONArray(flexatarTicketsString);
            jsonArray.getJSONObject(index).put("status", status);

            Log.d("FLX_INJECT","changeTicketStatus "+jsonArray.toString());
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(MAKE_FLEXATAR_TICKETS, jsonArray.toString());
            editor.apply();
        } catch (JSONException e) {
//            throw new RuntimeException(e);
        }
    }
    public static synchronized void addTicket(Context context,String ticket){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_STORAGE_NAME, Context.MODE_PRIVATE);
        String flexatarTicketsString = sharedPreferences.getString(MAKE_FLEXATAR_TICKETS, "[]");

        Log.d("FLX_INJECT","flexatarTicketsString " + flexatarTicketsString);

//        if (flexatarTicketsString.length() > 0){
            try {
                JSONArray jsonArray =  new JSONArray(flexatarTicketsString);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("link",ticket);
                LocalDateTime currentDateTime = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String startTime = currentDateTime.format(formatter);
                jsonObject.put("date", startTime);
                jsonObject.put("status", "in_progress");
//                Log.d("FLX_INJECT", "before " + jsonArray.toString());
//                if (jsonArray.length()>0)
//                    jsonArray.put(jsonArray.getJSONObject(0));
                jsonArray.put(jsonObject);
                flexatarTicketsString = jsonArray.toString();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(MAKE_FLEXATAR_TICKETS, flexatarTicketsString);
                editor.apply();
//                Log.d("FLX_INJECT", "after " + flexatarTicketsString);
            } catch (JSONException e) {
//                throw new RuntimeException(e);
            }


    }
    public static synchronized JSONArray getTickets(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_STORAGE_NAME, Context.MODE_PRIVATE);
        String flexatarTicketsString = sharedPreferences.getString(MAKE_FLEXATAR_TICKETS, "");
        try {
            return new JSONArray(flexatarTicketsString);
        } catch (JSONException e) {
            return null;
//            throw new RuntimeException(e);
        }
    }
    public static synchronized void removeTicket(Context context,int idx){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_STORAGE_NAME, Context.MODE_PRIVATE);
        String flexatarTicketsString = sharedPreferences.getString(MAKE_FLEXATAR_TICKETS, "");
        try {
            JSONArray array = new JSONArray(flexatarTicketsString);
            array.remove(idx);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(MAKE_FLEXATAR_TICKETS, array.toString());
            editor.apply();
        } catch (JSONException e) {
            return;
//            throw new RuntimeException(e);
        }
    }
    public static synchronized void removeTicketByTime(Context context,String time){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_STORAGE_NAME, Context.MODE_PRIVATE);
        String flexatarTicketsString = sharedPreferences.getString(MAKE_FLEXATAR_TICKETS, "");
        try {
            JSONArray array = new JSONArray(flexatarTicketsString);

            for (int i = 0; i < array.length(); i++) {
                if (array.getJSONObject(i).getString("date").equals(time)) {
                    array.remove(i);
                    break;
                }
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(MAKE_FLEXATAR_TICKETS, array.toString());
            editor.apply();
        } catch (JSONException e) {
            return;
//            throw new RuntimeException(e);
        }
    }
}
