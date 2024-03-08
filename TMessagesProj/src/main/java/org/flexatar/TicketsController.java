package org.flexatar;

import com.google.android.exoplayer2.util.Log;

import org.flexatar.DataOps.AssetAccess;
import org.flexatar.DataOps.LengthBasedFlxUnpack;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class TicketsController {
    private static Map<String, Timer> ticketTimers = null;
    private static final int TIMEOUT = 30*60;

    public interface TicketObserver{
        void onReady(String id,File file);
        void onError(String id,Ticket ticket);
        void onTimer(String id,Ticket ticket);

        void onStart(String lfid,Ticket ticket);
    }
    public static class Ticket{

        public String date;

        public String id;
        public String ftar;
        public String err;
        public String errorCode;
        public String status;
        public String name;
//        public FlexatarServerAccess.ListElement ftarRecord;

        public Ticket formJson(JSONObject ticket){

            Class<TicketsController.Ticket> clazz = (Class<TicketsController.Ticket>) this.getClass();
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {

                try {
                    String fieldName = field.getName();
                    if (ticket.has(fieldName)){
                        field.set(this,ticket.getString(fieldName));
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }

            return this;
        }
        public JSONObject toJson(String ticketString){

            Class<TicketsController.Ticket> clazz = (Class<TicketsController.Ticket>) this.getClass();
            Field[] fields = clazz.getDeclaredFields();
            JSONObject jsonTicket = null;
            try {
                jsonTicket = new JSONObject(ticketString);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            // Loop through each field
            for (Field field : fields) {

                try {

                    String value = (String) field.get(this);
                    if (value!=null)
                        jsonTicket.put(field.getName(),value);

//                Log.d("FLX_INJECT","Field Name: " + field.getName() + ", Value: " + value);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }

            return jsonTicket;
        }
        public void setDate(){
            LocalDateTime currentDateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            date = currentDateTime.format(formatter);
        }
        public static Ticket[] generateTickets(){
            JSONArray tickets = ValueStorage.getTickets(ApplicationLoader.applicationContext);
            Ticket[] ticketsInst = new Ticket[tickets.length()];

            for (int i = 0; i < tickets.length(); i++) {
                try {
                    ticketsInst[i] =  new Ticket().formJson(tickets.getJSONObject(i));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
            return ticketsInst;
        }
        public String timePassed(){
            LocalDateTime currentDateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime startDate = LocalDateTime.parse(date,formatter);
            Duration duration = Duration.between(startDate, currentDateTime);
            long minutes = duration.toMinutes();
            long seconds = duration.minusMinutes(minutes).getSeconds();


            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }

    }

    private static TicketObserver ticketObserver;
//    public static void clearTimers(){
//        if (ticketTimers == null) return;
//        for(Map.Entry<String,Timer> ent : ticketTimers.entrySet()){
//            ent.getValue().cancel();
//            ent.getValue().purge();
//        }
//        ticketTimers.clear();
//    }
    public static void attachObserver(TicketObserver ticketObserver){
        TicketsController.ticketObserver=ticketObserver;
        Map<String, Ticket> tickets = TicketStorage.getTickets();
        for(Map.Entry<String, Ticket> ticket : tickets.entrySet()){

            TicketsController.ticketObserver.onStart(ticket.getKey(),ticket.getValue());
        }

    }
    public static void removeObserver(){
        TicketsController.ticketObserver = null;
    }
    private static boolean isRunning = false;
    private static Set<String> lfidsPooling = new HashSet<>();
    public static void flexatarTaskStart(String lfid,Ticket ticket){
        if (lfidsPooling.contains(lfid)) return;
        isRunning = true;
        lfidsPooling.add(lfid);

        poolFlexatar(lfid,ticket);
    }
    public static void stop(){
        isRunning = false;
        lfidsPooling.clear();
    }

    private static void poolFlexatar(String lfid,Ticket ticket){
        FlexatarServerAccess.requestJson(FlexatarServiceAuth.getVerification(), "poll/" + ticket.id, "GET", new FlexatarServerAccess.OnRequestJsonReady() {
            @Override
            public void onReady(FlexatarServerAccess.StdResponse response) {
                Map<String, List<FlexatarServerAccess.ListElement>> ftars = FlexatarServerAccess.ListElement.listFactory(response.ftars);

                boolean resultReady = ftars.containsKey("private");
                if (resultReady) {
                    FlexatarServerAccess.ListElement listElement = ftars.get("private").get(0);
//                boolean resultReady = listElement.ftar != null || listElement.err != null;
                    boolean isOk = listElement.ftar != null;
                    String route = isOk ? listElement.ftar : listElement.err;

                    FlexatarServerAccess.requestDataRecursive(
                            FlexatarServiceAuth.getVerification(),
                            route, 0,
                            new ByteArrayOutputStream(),
                            new FlexatarServerAccess.OnDataDownloaded() {
                                @Override
                                public void onReady(boolean finished, ByteArrayOutputStream byteArrayOutputStream) {
                                    if (isOk) {
                                        Log.d("FLX_INJECT", "downloaded " + listElement.ftar);
                                        byte[] flexatarData = byteArrayOutputStream.toByteArray();
                                        int flxType = new LengthBasedFlxUnpack(flexatarData).detectFlxType();
                                        if (flxType!= - 1) {

                                            File flexatarReadyFile;
                                            if (flxType == 1) {
                                                flexatarReadyFile = FlexatarStorageManager.addToStorage(ApplicationLoader.applicationContext, flexatarData, listElement.id);
                                            }else{
                                                flexatarReadyFile = FlexatarStorageManager.addToStorage(ApplicationLoader.applicationContext, flexatarData, listElement.id,"flexatar_",flxType);
                                            }
                                            TicketStorage.removeTicket(lfid);
                                            lfidsPooling.remove(lfid);
                                            if (ticketObserver != null)
                                                ticketObserver.onReady(lfid, flexatarReadyFile);
                                        }else{
                                            ticket.errorCode = "{\"code\":0}";
                                            android.util.Log.d("FLX_INJECT","error code: " + ticket.errorCode);
                                            ticket.status = "error";
                                            TicketStorage.setTicket(lfid, ticket);
                                            FlexatarServerAccess.requestJson(FlexatarServiceAuth.getVerification(), ServerDataProc.genDeleteRout(route), "DELETE", null, null, null);
                                            lfidsPooling.remove(lfid);
                                            if (ticketObserver != null)
                                                ticketObserver.onError(lfid, ticket);
                                        }
                                        try {
                                            byteArrayOutputStream.close();
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    } else {
                                        ticket.errorCode = new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
                                        android.util.Log.d("FLX_INJECT","error code: " + ticket.errorCode);
                                        ticket.status = "error";
                                        TicketStorage.setTicket(lfid, ticket);
                                        FlexatarServerAccess.requestJson(FlexatarServiceAuth.getVerification(), ServerDataProc.genDeleteRout(route), "DELETE", null, null, null);
                                        lfidsPooling.remove(lfid);
                                        if (ticketObserver != null)
                                            ticketObserver.onError(lfid, ticket);
                                        try {
                                            byteArrayOutputStream.close();
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }

                                }

                                @Override
                                public void onError() {
                                    Log.d("FLX_INJECT", "failed to download flexatar");
                                }
                            }
                    );
                }else{
                    if (ticketObserver != null) ticketObserver.onTimer(lfid,ticket);
                    if (isRunning) {
                        try {
                            Thread.sleep(1500);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        if (isRunning)
                            poolFlexatar(lfid, ticket);
                    }
                    else{
                        lfidsPooling.remove(lfid);
                    }
                }


            }
            @Override
            public void onError() {
                Log.d("FLX_INJECT","poll fail" );
            }

        });
    }
}
