package org.flexatar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.DividerCell;
import org.telegram.ui.Components.LayoutHelper;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

@SuppressLint("ViewConstructor")
public class FlexatarProgressCell extends LinearLayout {

    private final Context context;
    private int ticketCurrentStatus = FLEXATAR_PROGRESS;
    private String startTime;
    private DismissListener dismissListener;
    private static int TIMEOUT = 30;

    public String getStartTime(){
        return startTime;
    }
    public FlexatarProgressCell(@NonNull Context context, JSONObject ticketData) {
        super(context);
        this.context=context;

        setOrientation(LinearLayout.VERTICAL);
        TextView headerText = new TextView(context);
        headerText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        headerText.setText("Making flexatar");
        headerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        headerText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
//        layoutParams.setMargins(AndroidUtilities.dp(4),AndroidUtilities.dp(12),AndroidUtilities.dp(12),0);
        headerText.setLayoutParams(layoutParams);
        addView(headerText);


        try {
            startTime = ticketData.getString("date");
        } catch (JSONException e) {
            return;
        }


        Handler handler = new Handler(Looper.getMainLooper());
        String ticketStatus = null;
        try {
            ticketStatus = ticketData.getString("status");
        } catch (JSONException e) {
//            throw new RuntimeException(e);
        }
        Log.d("FLX_INJECT","ticketStatus "+ticketStatus);
        if (ticketStatus.equals("in_progress")) {
            ticketCurrentStatus = FLEXATAR_PROGRESS;
            TextView tmeText = new TextView(context);
            tmeText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            tmeText.setText("");
            tmeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            tmeText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            tmeText.setLayoutParams(layoutParams);
            addView(tmeText);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            LocalDateTime startDate = LocalDateTime.parse(startTime,formatter);
            Timer checkTmer = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    LocalDateTime currentDateTime = LocalDateTime.now();

                    Duration duration = Duration.between(startDate, currentDateTime);
                    long secondsFull = duration.getSeconds();
                    if (secondsFull > TIMEOUT) {
                        checkTmer.cancel();
                        checkTmer.purge();
                        ticketCurrentStatus = FLEXATAR_ERROR;
                        if (dismissListener != null)
                            dismissListener.onStatusChanged(FLEXATAR_ERROR);
                    }
                    long minutes = duration.toMinutes();
                    long seconds = duration.minusMinutes(minutes).getSeconds();
                    String timePassed = String.format(Locale.US, "%02d:%02d", minutes, seconds);
                    handler.post(() -> tmeText.setText(timePassed + " / (00 : 20)"));
//                Log.d("FLX_INJECT","timePassed "+timePassed);
                }
            };
            checkTmer.scheduleAtFixedRate(task, 0, 1000);

            addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View view) {
                    // The view has been attached to the window
                    // This is called when the view is added to the view hierarchy
                    // Do something when attached
                }

                @Override
                public void onViewDetachedFromWindow(View view) {
                    checkTmer.cancel();
                    checkTmer.purge();
                }
            });
        }else if (ticketStatus.equals("error")){
            ticketCurrentStatus = FLEXATAR_ERROR;
            addErrorText();
        }
        DividerCell dividerCell = new DividerCell(context);
                    dividerCell.setPadding(AndroidUtilities.dp(28), AndroidUtilities.dp(8), AndroidUtilities.dp(28), AndroidUtilities.dp(8));
        addView(dividerCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 5,0,0,0,4));

//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
//        String currentDate = dateFormat.format(new Date());
//        LocalDate.parse(currentDate)
//        Duration.between()
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

    public interface DismissListener{
        void onStatusChanged(int status);
    }
}
