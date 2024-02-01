package org.flexatar;

import android.view.View;

import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;

public class ItemModel {
    public final static int ACTION_CELL = 0;
    public final static int PROGRESS_CELL = 1;
    public final static int FLEXATAR_CELL = 2;
    public final static int DELIMITER = 3;
    private int cellType;
    private boolean isChecked=false;

    public String getProgressTime() {
        return progressTime;
    }

    private String progressTime;

    public String getErrorCode() {
        return errorCode;
    }

    private String errorCode;

    public void setFlexatarProgressCell(FlexatarProgressCell flexatarProgressCell) {
        this.flexatarProgressCell = new WeakReference<>(flexatarProgressCell);
    }

    private WeakReference<FlexatarProgressCell> flexatarProgressCell = null;


    public FlexatarProgressCell.DismissListener getDismissListener() {
        return dismissListener;
    }

    private FlexatarProgressCell.DismissListener dismissListener;

    public JSONObject getTicket() {
        return ticket;
    }

    private JSONObject ticket;

    public View.OnClickListener getOnClickListener() {
        return onClickListener;
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    private View.OnClickListener onClickListener;

    public void setImageResource(int imageResource) {
        this.imageResource = imageResource;
    }

    public int getImageResource() {
        return imageResource;
    }
    private int imageResource;

    private String nameText;

    public String getNameText() {
        return nameText;
    }

    public void setNameText(String nameText) {
        this.nameText = nameText;
    }

    public File getFlexatarFile() {
        return flexatarFile;
    }

    public void setFlexatarFile(File flexatarFile) {
        this.flexatarFile = flexatarFile;
    }

    private File flexatarFile;


    public ItemModel(int cellType) {
        this.cellType = cellType;
    }


    public int getCellType() {
        return cellType;
    }

    public void setTicket(JSONObject ticket) {
        this.ticket=ticket;
    }

    public void setDismissListener(FlexatarProgressCell.DismissListener dismissListener) {
        this.dismissListener=dismissListener;
    }

    public void setTime(String time) {
        progressTime = time;
        if(flexatarProgressCell == null)return;
        flexatarProgressCell.get().setTime(time);
    }

    public void setError(String errorCode) {
        this.errorCode=errorCode;
        if (flexatarProgressCell!=null)
            flexatarProgressCell.get().setError(errorCode);

    }

    public void setChecked(boolean b) {
        isChecked = b;
    }
    public boolean isChecked() {
        return isChecked;
    }


}
