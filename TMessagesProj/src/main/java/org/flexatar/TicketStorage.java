package org.flexatar;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.exoplayer2.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class TicketStorage {
    static final String PREF_STORAGE_NAME = "TicketStorage";

    public static synchronized void clearTickets(){
        long userID = UserConfig.getInstance(UserConfig.selectedAccount).clientUserId;

        SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences(PREF_STORAGE_NAME+userID, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
    public static synchronized void removeTicket(String lfid){
        long userID = UserConfig.getInstance(UserConfig.selectedAccount).clientUserId;

        SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences(PREF_STORAGE_NAME+userID, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(lfid);
        editor.apply();
    }
    public static synchronized Map<String, TicketsController.Ticket> getTickets(){
        long userID = UserConfig.getInstance(UserConfig.selectedAccount).clientUserId;

        SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences(PREF_STORAGE_NAME+userID, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = sharedPreferences.getAll();
        Map<String, TicketsController.Ticket> tickets = new HashMap<>();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String key = entry.getKey();
            String value = (String) entry.getValue();
            try {
                tickets.put(key,new TicketsController.Ticket().formJson(new JSONObject(value)));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        return tickets;
    }
    public static synchronized void setTicket(String lfid,TicketsController.Ticket ticket){
        long userID = UserConfig.getInstance(UserConfig.selectedAccount).clientUserId;
        SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences(PREF_STORAGE_NAME+userID, Context.MODE_PRIVATE);
        String ticketString = sharedPreferences.getString(lfid, "{}");
        JSONObject jsonTicket = ticket.toJson(ticketString);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(lfid, jsonTicket.toString());
        editor.apply();
    }
}
