package org.flexatar;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.google.android.exoplayer2.util.Log;

import org.flexatar.DataOps.FlexatarData;
import org.flexatar.DataOps.LengthBasedFlxUnpack;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Cells.GraySectionCell;
import org.telegram.ui.Cells.MemberRequestCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FlexatarIconsVerticalScroll extends ScrollView {
    private final TextCell textCellNewFlxByPhoto;
    private final LinearLayout flxIconsLayout;
    private final Context context;
    private final FlexatarCabinetActivity parentFragment;
    private final TextCell readInstructionsCell;
    private GraySectionCell dividerCellFlexatarInProgress;
    private List<FlexatarCell> flexatarCells;
    private int checkedCount = 1;
    private OnShowFlexatarListener onShowFlexatarListener;

    public interface OnShowFlexatarListener{
        void onShowFlexatar(FlexatarCell flexatarCell);
    }

    public void setOnShowFlexatarListener(OnShowFlexatarListener onShowFlexatarListener){
        this.onShowFlexatarListener = onShowFlexatarListener;
    }
    public void setOnNewFlexatarChosenListener(OnClickListener newByPhotoListener){
        textCellNewFlxByPhoto.setOnClickListener(newByPhotoListener);
    }
    public void setOnViewInstructionsChosenListener(View.OnClickListener viewInstructionListener){
        readInstructionsCell.setOnClickListener(viewInstructionListener);
    }
    public FlexatarIconsVerticalScroll(Context context, FlexatarCabinetActivity parentFragment) {
        super(context);
        this.parentFragment=parentFragment;
        this.context=context;
        flxIconsLayout = new LinearLayout(context);
        flxIconsLayout.setOrientation(LinearLayout.VERTICAL);
//        flxIconsLayout.setBackgroundColor(Color.BLACK);
        addView(flxIconsLayout, LayoutHelper.createLinear( LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL));

        readInstructionsCell = new TextCell(context);
        readInstructionsCell.setTextAndIcon(LocaleController.getString("ViewInstructions", R.string.ViewInstructions), R.drawable.msg_help, false);
        flxIconsLayout.addView(readInstructionsCell);

        textCellNewFlxByPhoto = new TextCell(context);
        textCellNewFlxByPhoto.setTextAndIcon(LocaleController.getString("NewFlexatarByPhoto", R.string.NewFlexatarByPhoto), R.drawable.msg_addphoto, false);
        flxIconsLayout.addView(textCellNewFlxByPhoto);
//        flxIconsLayout.setOnClickListener((l)->{});

//        TextCell textCellNewFlxFromCloud = new TextCell(context);
//        textCellNewFlxFromCloud.setTextAndIcon(LocaleController.getString("FlexatarFromCloud", R.string.FlexatarFromCloud), R.drawable.msg_download, false);
//        flxIconsLayout.addView(textCellNewFlxFromCloud);

        dividerCellFlexatarInProgress = new GraySectionCell(context);
        dividerCellFlexatarInProgress.setText(LocaleController.getString("FlexatarsInProgress", R.string.FlexatarsInProgress));
//        ValueStorage.clearAllTickets(context);
//        addFlexatarsInProgress();
        GraySectionCell dividerCell = new GraySectionCell(context);
        dividerCell.setText(LocaleController.getString("FlexatarsAvailableForCalls", R.string.FlexatarsAvailableForCalls));
        flxIconsLayout.addView(dividerCell);
        flexatarCells = new ArrayList<>();
        File[] flexatarsInLocalStorage = FlexatarStorageManager.getFlexatarFileList(context);
        for (int i = 0; i < flexatarsInLocalStorage.length; i++) {
            File flexatarFile = flexatarsInLocalStorage[i];


            FlexatarCell flexatarCell = new FlexatarCell(context,flexatarFile);
            flexatarCell.setOnClickListener((v) ->{
                FlexatarCell cell = (FlexatarCell) v;
                if(parentFragment.getActionBar().isActionModeShowed() && !cell.isBuiltin()) {

                    checkedCount += cell.isChecked() ? -1 : 1;
                    cell.setChecked(!cell.isChecked(), true);
                    parentFragment.setCheckedFlexatarsCount();
                }else{
                    onShowFlexatarListener.onShowFlexatar(cell);
                }
            });
            flexatarCell.setOnLongClickListener((v) ->{
                if(!parentFragment.getActionBar().isActionModeShowed()) {
                    parentFragment.showOrUpdateActionMode();
                    parentFragment.getActionBar().showActionMode();
                    makeCheckBoxes();
                    FlexatarCell cell = (FlexatarCell) v;
                    checkedCount = cell.isBuiltin() ? 0 : 1;
                    if (!cell.isBuiltin())
                        cell.setChecked(true, true);
                    parentFragment.setCheckedFlexatarsCount();
                }
                return true;
            });
            flexatarCells.add(flexatarCell);
            flxIconsLayout.addView(flexatarCell,LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT,Gravity.CENTER,0,2,0,2));

        }

    }
    public int getCheckedCount(){

        return checkedCount;
    }
    private void makeCheckBoxes(){
        for (FlexatarCell flexatarCell:flexatarCells)
            flexatarCell.addCheckbox();
    }

    public void removeCheckBoxes(){
        for (FlexatarCell flexatarCell:flexatarCells) {
            flexatarCell.removeCheckbox();
            flexatarCell.setChecked(false,false);
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

    public void deleteSelectedFlexatars() {
        List<FlexatarCell> newFlexatarCells = new ArrayList<>();
        for(FlexatarCell flexatarCell : flexatarCells){
            if (flexatarCell.isChecked()) {
                flexatarCell.deleteFlexatarFile();
                flxIconsLayout.removeView(flexatarCell);

            }else{
                newFlexatarCells.add(flexatarCell);
            }
        }
        flexatarCells = newFlexatarCells;
        File[] flexatarFiles = FlexatarStorageManager.getFlexatarFileList(context);
        boolean found = false;
        for (File f : flexatarFiles){
            if (f.getName().equals(FlexatarUI.chosenFirst.getName())){
                found = true;
            }
        }
        if (!found){
            FlexatarUI.chosenFirst = FlexatarUI.chosenSecond.getName().equals(flexatarFiles[0].getName()) ? flexatarFiles[1] : flexatarFiles[0];
            FlexatarRenderer.currentFlxData = new FlexatarData(new LengthBasedFlxUnpack(FlexatarStorageManager.dataFromFile(FlexatarUI.chosenFirst)));

        }
        found = false;
        for (File f : flexatarFiles){
            if (f.getName().equals(FlexatarUI.chosenSecond.getName())){
                found = true;
            }
        }
        if (!found){
            FlexatarUI.chosenSecond = FlexatarUI.chosenFirst.getName().equals(flexatarFiles[0].getName()) ? flexatarFiles[1] : flexatarFiles[0];
            FlexatarRenderer.altFlxData = new FlexatarData(new LengthBasedFlxUnpack(FlexatarStorageManager.dataFromFile(FlexatarUI.chosenSecond)));

        }
    }
}
