package org.flexatar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.util.Log;

import org.flexatar.DataOps.AssetAccess;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.DividerCell;
import org.telegram.ui.Components.LayoutHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

@SuppressLint("ViewConstructor")
public class FlexatarProgressCell extends LinearLayout {

    private final Context context;
    private TextView tmeText;
    private TextView headerText;
    private String errLink;
    private String flexatarId;
    private String flexatarLink;
    private int ticketCurrentStatus = FLEXATAR_PROGRESS;
    private String startTime;
    private DismissListener dismissListener;
    private static int TIMEOUT = 11300;
    private int currentPart;
    private File flexatarReadyFile;
    private String errorCode = null;
    private Timer checkTmer;

    public String getStartTime(){
        return startTime;
    }
    public File getFlexatarReadyFile(){
        return flexatarReadyFile;
    }
    public String getErrorCode(){
        return errorCode;
    }
    public void removeFlexatarOnServer(){
        FlexatarServerAccess.lambdaRequest(ServerDataProc.genDeleteRout(flexatarLink), "DELETE", null, null, null);
    }
    public void setTicket(String name){
        if (startTime!=null) return;
        headerText.setText("Making flexatar : "+ name);

        /*try {
            startTime = ticketData.getString("date");
            flexatarLink = ticketData.getString("ftar");
            errLink = ticketData.getString("err");
            flexatarId = ticketData.getString("id");
            errorCode = ticketData.has("error_code") ? ticketData.getString("error_code") : null;
//            Log.d("FLX_INJECT")
        } catch (JSONException e) {
            return;
        }



        String ticketStatus = null;
        try {
            ticketStatus = ticketData.getString("status");
        } catch (JSONException e) {
//            throw new RuntimeException(e);
        }
        Log.d("FLX_INJECT","ticketStatus "+ticketStatus);
        if (ticketStatus.equals("in_progress")) {
            ticketCurrentStatus = FLEXATAR_PROGRESS;


            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            LocalDateTime startDate = LocalDateTime.parse(startTime,formatter);
            checkTmer = new Timer();
            TimerTask task = new TimerTask() {


                @Override
                public void run() {
                    Log.d("FLX_INJECT","TIMER TICKS");
                    LocalDateTime currentDateTime = LocalDateTime.now();

                    Duration duration = Duration.between(startDate, currentDateTime);
                    long secondsFull = duration.getSeconds();
                    if (secondsFull > TIMEOUT) {
                        Log.d("FLX_INJECT","TICKET_TIMEOUT");
                        checkTmer.cancel();
                        checkTmer.purge();
                        ticketCurrentStatus = FLEXATAR_ERROR;
                        if (dismissListener != null)
                            dismissListener.onStatusChanged(FLEXATAR_ERROR,FlexatarProgressCell.this);
                    }
                    long fullSeconds = duration.getSeconds();
                    long minutes = duration.toMinutes();
                    long seconds = duration.minusMinutes(minutes).getSeconds();

                    String timePassed = String.format(Locale.US, "%02d:%02d", minutes, seconds);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    if (fullSeconds>0 && fullSeconds%3 == 0){

                        downloadRecursive(0, outputStream, new FlexatarServerAccess.CompletionListener() {
                            @Override
                            public void onReady(boolean isComplete) {
                                Log.d("FLX_INJECT","Flexatar Ready");

                                checkTmer.cancel();
                                checkTmer.purge();

                                byte[] flexatarData = outputStream.toByteArray();
                                flexatarReadyFile = FlexatarStorageManager.addToStorage(AssetAccess.context,flexatarData,"user___"+flexatarId);

                                try {
                                    outputStream.close();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                ticketCurrentStatus = FLEXATAR_READY;
                                if (dismissListener != null)
                                    dismissListener.onStatusChanged(FLEXATAR_READY,FlexatarProgressCell.this);
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
                                errorCode = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
                                try {
                                    outputStream.close();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                Log.d("FLX_INJECT","error code" + errorCode);
                                checkTmer.cancel();
                                checkTmer.purge();
                                ticketCurrentStatus = FLEXATAR_ERROR;
                                if (dismissListener != null)
                                    dismissListener.onStatusChanged(FLEXATAR_ERROR,FlexatarProgressCell.this);



                            }

                            @Override
                            public void onFail() {

                            }
                        });

                    }
                    handler.post(() -> tmeText.setText(timePassed + " / (00 : 20)"));

                }
            };
            checkTmer.scheduleAtFixedRate(task, 0, 1000);

           *//* addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View view) {
                    // The view has been attached to the window
                    // This is called when the view is added to the view hierarchy
                    // Do something when attached
                }

                @Override
                public void onViewDetachedFromWindow(View view) {
                    Log.d("FLX_INJECT","onViewDetachedFromWindow");
                    checkTmer.cancel();
                    checkTmer.purge();
                }
            });*//*
        }else if (ticketStatus.equals("error")){
            ticketCurrentStatus = FLEXATAR_ERROR;
            removeViewAt(1);
            addErrorText();
        }*/
    }
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
//        checkTmer.cancel();
//        checkTmer.purge();
    }
    public FlexatarProgressCell(@NonNull Context context,JSONObject ticketData) {
        super(context);
        this.context=context;
    }
    public FlexatarProgressCell(@NonNull Context context) {
        super(context);
        this.context=context;
        setLayoutParams(LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT));
        setOrientation(LinearLayout.VERTICAL);
        headerText = new TextView(context);
        headerText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        headerText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
//        layoutParams.setMargins(AndroidUtilities.dp(4),AndroidUtilities.dp(12),AndroidUtilities.dp(12),0);
        headerText.setLayoutParams(layoutParams);
        addView(headerText);

        tmeText = new TextView(context);
        tmeText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        tmeText.setText("");
        tmeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        tmeText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        tmeText.setLayoutParams(layoutParams);
        addView(tmeText);

        DividerCell dividerCell = new DividerCell(context);
                    dividerCell.setPadding(AndroidUtilities.dp(28), AndroidUtilities.dp(8), AndroidUtilities.dp(28), AndroidUtilities.dp(8));
        addView(dividerCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 5,0,0,0,4));

//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
//        String currentDate = dateFormat.format(new Date());
//        LocalDate.parse(currentDate)
//        Duration.between()
    }
    public void downloadRecursive(int part,ByteArrayOutputStream outputStream,FlexatarServerAccess.CompletionListener listener){
        FlexatarServerAccess.lambdaRequest(flexatarLink+"?part="+part, "GET", null, outputStream, new FlexatarServerAccess.CompletionListener() {
            @Override
            public void onReady(boolean isComplete) {
               if (isComplete){
                   listener.onReady(true);
               }else{
                   downloadRecursive(part+1,outputStream,listener);

               }
            }

            @Override
            public void onFail() {
                listener.onFail();
            }
        });

    }
    public void addDismissListener(DismissListener dismissListener){
        this.dismissListener=dismissListener;
    }
    public static int FLEXATAR_READY = 0;
    public static int FLEXATAR_ERROR = 1;
    public static int FLEXATAR_PROGRESS = 2;
    public int getTicketStatus(){
        return ticketCurrentStatus;
    }
    public void addErrorText() {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL;

        TextView errorText = new TextView(context);
        errorText.setTextColor(Theme.getColor(Theme.key_avatar_nameInMessageRed));
        errorText.setText("Finished with error");
        errorText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        errorText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        errorText.setLayoutParams(layoutParams);
        addView(errorText,1);
    }
    Handler handler = new Handler(Looper.getMainLooper());
    public void setTime(String time) {
        handler.post(() -> tmeText.setText(time + " / (00 : 20)"));
    }

    public void setError(String errorCode) {
        if (errorCode == null) {
            if (this.errorCode!=null){
                removeViewAt(1);
                addView(tmeText,1);
            }
        }else{
            this.errorCode = errorCode;
            handler.post(() -> {
                removeViewAt(1);
                addErrorText();
            });
        }

    }

    public interface DismissListener{
        void onStatusChanged(int status,FlexatarProgressCell cell);
    }
}
