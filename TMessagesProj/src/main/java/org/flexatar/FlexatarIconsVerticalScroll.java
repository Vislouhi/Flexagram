package org.flexatar;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import com.google.android.exoplayer2.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.DividerCell;
import org.telegram.ui.Cells.GraySectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.List;

public class FlexatarIconsVerticalScroll extends ScrollView {
    private final TextCell textCellNewFlxByPhoto;
    private final LinearLayout flxIconsLayout;
    private final Context context;
    private final BaseFragment parentFragment;
    private GraySectionCell dividerCellFlexatarInProgress;

    public void setOnNewFlexatarChosenListener(OnClickListener newByPhotoListener){
        textCellNewFlxByPhoto.setOnClickListener(newByPhotoListener);
    }
    public FlexatarIconsVerticalScroll(Context context, BaseFragment parentFragment) {
        super(context);
        this.parentFragment=parentFragment;
        this.context=context;
        flxIconsLayout = new LinearLayout(context);
        flxIconsLayout.setOrientation(LinearLayout.VERTICAL);
//        flxIconsLayout.setBackgroundColor(Color.BLACK);
        addView(flxIconsLayout, LayoutHelper.createLinear( LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL));


        textCellNewFlxByPhoto = new TextCell(context);
        textCellNewFlxByPhoto.setTextAndIcon("New Flexatar by Photo", R.drawable.msg_addphoto, false);
        flxIconsLayout.addView(textCellNewFlxByPhoto);
//        flxIconsLayout.setOnClickListener((l)->{});

        TextCell textCellNewFlxFromCloud = new TextCell(context);
        textCellNewFlxFromCloud.setTextAndIcon("Load Flexatar from Cloud", R.drawable.msg_download, false);
        flxIconsLayout.addView(textCellNewFlxFromCloud);

        dividerCellFlexatarInProgress = new GraySectionCell(context);
        dividerCellFlexatarInProgress.setText("Flexatars in progress");
//        ValueStorage.clearAllTickets(context);
//        addFlexatarsInProgress();
        GraySectionCell dividerCell = new GraySectionCell(context);
        dividerCell.setText("Flexatars available for calls");
        flxIconsLayout.addView(dividerCell);

        for (int i = 0; i < FlexatarRenderer.icons.size(); i++) {
            ImageView icnFlx = new ImageView(context);
            icnFlx.setContentDescription("flexatar button");
            icnFlx.setBackground(Theme.createSelectorDrawable(ColorUtils.setAlphaComponent(Color.WHITE, (int) (255 * 0.3f))));
            icnFlx.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(0), AndroidUtilities.dp(0), AndroidUtilities.dp(0));
            Bitmap iconBitmap = FlexatarRenderer.icons.get(i);
            icnFlx.setImageDrawable(new BitmapDrawable(context.getResources(),iconBitmap));
            float ratio = (float)iconBitmap.getHeight()/(float)iconBitmap.getWidth();
            float imageWidth = 150;
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(AndroidUtilities.dp(imageWidth), AndroidUtilities.dp(imageWidth * ratio));
            layoutParams.gravity = Gravity.CENTER;
            icnFlx.setLayoutParams(layoutParams);
//            icnFlx.requestLayout();
            flxIconsLayout.addView(icnFlx);
            int finalI = i;
            icnFlx.setOnClickListener((v) -> {


            });

        }

    }

    List<FlexatarProgressCell> flexatarsInProgress = new ArrayList<>();
    Handler handler = new Handler(Looper.getMainLooper());
    private void addFlexatarsInProgress(){
        JSONArray tickets = ValueStorage.getTickets(context);
//        Log.d("FLX_INJECT", "Add new tickets: " + tickets + " " + tickets.length());
        if (tickets !=null && tickets.length()>0) {
            if (flxIconsLayout.indexOfChild(dividerCellFlexatarInProgress) == -1)
                flxIconsLayout.addView(dividerCellFlexatarInProgress,2);
            for (int i = 0; i < tickets.length(); i++) {
                try {
                    int ticketIdx = tickets.length() - 1 - i;
                    JSONObject ticket = tickets.getJSONObject( ticketIdx);
//                    Log.d("FLX_INJECT", "ticket: " + ticket.toString());
                    int finalI = i;
                    FlexatarProgressCell flexatarProgressCell = new FlexatarProgressCell(context, ticket);
                    flexatarProgressCell.addDismissListener((status)->{
                        if (status == FlexatarProgressCell.FLEXATAR_READY) {
                            ValueStorage.removeTicket(context, ticketIdx);
                            handler.post(() -> flxIconsLayout.removeView(flexatarProgressCell));
                            flexatarsInProgress.remove(flexatarProgressCell);
                            if (flexatarsInProgress.size() == 0) {
                                flxIconsLayout.removeView(dividerCellFlexatarInProgress);
                            }
                        }else if (status == FlexatarProgressCell.FLEXATAR_ERROR){
                            ValueStorage.changeTicketStatus(context,ticketIdx,"error");
                            handler.post(() ->{
                                flexatarProgressCell.removeViewAt(1);
                                flexatarProgressCell.addErrorText();
                                    });

                        }

                    });
//                    , () -> {
////                        ValueStorage.removeTicket(context, finalI);
//
//                    });
                    flexatarsInProgress.add(flexatarProgressCell);
                    flexatarProgressCell.setOnClickListener((v)->{
                        showMakeFlexatarErrorAlert(flexatarProgressCell);
                    });

                    flxIconsLayout.addView(flexatarProgressCell,3 + i,
                            LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,0,0,0,4));


                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }


        }
        if (flxIconsLayout.indexOfChild(dividerCellFlexatarInProgress) != -1 && tickets.length() == 0)
            flxIconsLayout.removeView(dividerCellFlexatarInProgress);
    }
    public void updateFlexatarList() {

        flxIconsLayout.removeView(dividerCellFlexatarInProgress);
        for(FlexatarProgressCell flexatarProgressCell : flexatarsInProgress){

            flxIconsLayout.removeView(flexatarProgressCell);
        }
        flexatarsInProgress.clear();
        addFlexatarsInProgress();
    }
    public void showMakeFlexatarErrorAlert(FlexatarProgressCell cell){

        Log.d("FLX_INJECT","getTicketStatus "+cell.getTicketStatus());
        AlertDialog.Builder builder = new AlertDialog.Builder(parentFragment.getParentActivity());

        if(cell.getTicketStatus() == FlexatarProgressCell.FLEXATAR_ERROR) {
            builder.setTitle("There could be following problems:");
            builder.setMessage("1. There was no human faces on the photos.\n" +
                    "2. Head was turned the wrong side. \n" +
                    "3. Flexatar server overloaded.");


            builder.setPositiveButton("Delete", (dialogInterface, i) -> {
                ValueStorage.removeTicketByTime(context, cell.getStartTime());
                flxIconsLayout.removeView(cell);
                flexatarsInProgress.remove(cell);
                if (flexatarsInProgress.size() == 0) {
                    flxIconsLayout.removeView(dividerCellFlexatarInProgress);
                }

            });
            builder.setNegativeButton("Cancel", (dialogInterface, i) -> {

            });
        }else if(cell.getTicketStatus() == FlexatarProgressCell.FLEXATAR_PROGRESS) {
            builder.setTitle("Info:");
            builder.setMessage("Processing, please wait.");
            builder.setPositiveButton("OK", (dialogInterface, i) -> {

            });
        }



        AlertDialog alertDialog = builder.create();
        parentFragment.showDialog(alertDialog);
//        TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
//        if (button != null) {
//            button.setTextColor(Theme.getColor(Theme.key_text_RedBold));
//        }
    }
}
