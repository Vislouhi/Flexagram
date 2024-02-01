package org.flexatar;

import com.google.android.exoplayer2.util.Log;

import org.flexatar.DataOps.AssetAccess;
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
        public String id;
        public String date;
        public String ftar;
        public String err;
        public String errorCode;
        public String status;
        public String name;
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
    private static boolean isRunning = false;
    private static Set<String> lfidsPooling = new HashSet<>();
    public static void flexatarTaskStart(String lfid,Ticket ticket){
        if (lfidsPooling.contains(lfid)) return;
        isRunning = true;
        lfidsPooling.add(lfid);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        poolFlexatar(lfid,ticket,outputStream,true);
    }
    public static void stop(){
        isRunning = false;
    }
    private static void poolFlexatar(String lfid,Ticket ticket, ByteArrayOutputStream outputStream,boolean poolErr){
        if (poolErr)
            FlexatarServerAccess.lambdaRequest(ticket.err, "GET", null, outputStream, new FlexatarServerAccess.CompletionListener() {
                @Override
                public void onReady(boolean isComplete) {
                    ticket.errorCode = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
                    ticket.status = "error";
                    TicketStorage.setTicket(lfid,ticket);
                    FlexatarServerAccess.lambdaRequest(ServerDataProc.genDeleteRout(ticket.ftar), "DELETE", null, null, null);
                    lfidsPooling.remove(lfid);
                    if (ticketObserver != null) ticketObserver.onError(lfid,ticket);
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Log.d("FLX_INJECT","error code" + ticket.errorCode);

                }

                @Override
                public void onFail() {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    if (ticketObserver != null) ticketObserver.onTimer(lfid,ticket);
                    if (isRunning)
                        poolFlexatar(lfid,ticket,outputStream,!poolErr);
                    else{
                        lfidsPooling.remove(lfid);
                    }
                    Log.d("FLX_INJECT","Error not ready");
                }
            });
        else
            FlexatarServerAccess.downloadFlexatarRecursive(ticket.ftar,0, outputStream, new FlexatarServerAccess.CompletionListener() {
                @Override
                public void onReady(boolean isComplete) {
                    Log.d("FLX_INJECT","Flexatar Ready");

                    byte[] flexatarData = outputStream.toByteArray();
                    File flexatarReadyFile = FlexatarStorageManager.addToStorage(ApplicationLoader.applicationContext, flexatarData, ticket.ftar);
                    TicketStorage.removeTicket(lfid);
                    lfidsPooling.remove(lfid);
                    if (ticketObserver != null) ticketObserver.onReady(lfid,flexatarReadyFile);
//                    if (ticketObserver != null) ticketObserver.onReady(ticket.fid,flexatarReadyFile);
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onFail() {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    if (ticketObserver != null) ticketObserver.onTimer(lfid,ticket);
                    if (isRunning)
                        poolFlexatar(lfid,ticket,outputStream,!poolErr);
                    else{
                        lfidsPooling.remove(lfid);
                    }
                    Log.d("FLX_INJECT","flexatar not ready");

                }
            });
    }


    /*public static void initiateTimers(){
        if (ticketTimers == null) ticketTimers = new HashMap<>();

        JSONArray tickets = ValueStorage.getTickets(ApplicationLoader.applicationContext);
        for (int i = 0; i < tickets.length(); i++) {
            try {
                JSONObject ticket = tickets.getJSONObject(i);
                String flexatarId = ticket.getString("id");
                String startTime = ticket.getString("date");
                String flexatarLink = ticket.getString("ftar");
                String errLink = ticket.getString("err");
                String errorCode = ticket.has("error_code") ? ticket.getString("error_code") : null;
                String ticketStatus = ticket.getString("status");
                if (ticketTimers.containsKey(flexatarId)) continue;
                if (!ticketStatus.equals("in_progress")) continue;

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                LocalDateTime startDate = LocalDateTime.parse(startTime,formatter);
                Timer checkTimer = new Timer();
                ticketTimers.put(flexatarId,checkTimer);
                TimerTask task = new TimerTask() {


                    @Override
                    public void run() {
                        Log.d("FLX_INJECT","TIMER TICKS");
                        LocalDateTime currentDateTime = LocalDateTime.now();

                        Duration duration = Duration.between(startDate, currentDateTime);
                        long secondsFull = duration.getSeconds();
                        if (secondsFull > TIMEOUT) {
                            Log.d("FLX_INJECT","TICKET_TIMEOUT");
                            checkTimer.cancel();
                            checkTimer.purge();
                            ticketTimers.remove(flexatarId);
                            ValueStorage.changeTicketStatus(ApplicationLoader.applicationContext,flexatarId,"error","timeout");

                            if (ticketObserver != null) ticketObserver.onError(flexatarId,"timeout");

                        }
                        long fullSeconds = duration.getSeconds();
                        long minutes = duration.toMinutes();
                        long seconds = duration.minusMinutes(minutes).getSeconds();

                        String timePassed = String.format(Locale.US, "%02d:%02d", minutes, seconds);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        if (fullSeconds>0 && fullSeconds%3 == 0){

                            FlexatarServerAccess.downloadFlexatarRecursive(flexatarLink,0, outputStream, new FlexatarServerAccess.CompletionListener() {
                                @Override
                                public void onReady(boolean isComplete) {
                                    Log.d("FLX_INJECT","Flexatar Ready");

                                    checkTimer.cancel();
                                    checkTimer.purge();
                                    ticketTimers.remove(flexatarId);

                                    byte[] flexatarData = outputStream.toByteArray();
                                    File flexatarReadyFile = FlexatarStorageManager.addToStorage(ApplicationLoader.applicationContext, flexatarData, "user___" + flexatarId);
                                    ValueStorage.removeTicket(ApplicationLoader.applicationContext,flexatarId);
                                    if (ticketObserver != null) ticketObserver.onReady(flexatarId,flexatarReadyFile);
                                    try {
                                        outputStream.close();
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
//                                    ticketCurrentStatus = FLEXATAR_READY;
//                                    if (dismissListener != null)
//                                        dismissListener.onStatusChanged(FLEXATAR_READY,FlexatarProgressCell.this);
                                }

                                @Override
                                public void onFail() {
                                    Log.d("FLX_INJECT","flexatar not ready");
//                                ticketCurrentStatus = FLEXATAR_ERROR;
//                                if (dismissListener != null)
//                                    dismissListener.onStatusChanged(FLEXATAR_ERROR);
                                }
                            });
                            FlexatarServerAccess.lambdaRequest(errLink, "GET", null, outputStream, new FlexatarServerAccess.CompletionListener() {
                                @Override
                                public void onReady(boolean isComplete) {
                                    String errorCode = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
                                    if (ticketObserver != null) ticketObserver.onError(flexatarId,errorCode);
                                    try {
                                        outputStream.close();
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    Log.d("FLX_INJECT","error code" + errorCode);
                                    checkTimer.cancel();
                                    checkTimer.purge();
                                    ticketTimers.remove(flexatarId);
                                    ValueStorage.changeTicketStatus(ApplicationLoader.applicationContext,flexatarId,"error",errorCode);

//                                    ticketCurrentStatus = FLEXATAR_ERROR;
//                                    if (dismissListener != null)
//                                        dismissListener.onStatusChanged(FLEXATAR_ERROR,FlexatarProgressCell.this);



                                }

                                @Override
                                public void onFail() {

                                }
                            });

                        }
                        if (ticketObserver != null) ticketObserver.onTimer(flexatarId,timePassed);
                    }
                };
                checkTimer.scheduleAtFixedRate(task, 0, 1000);

            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

        }
    }*/

}
