package org.flexatar;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;

import java.io.File;


public class FlexatarCell extends RelativeLayout {
    private final ImageView icnFlx;
    private final float ratio;
    private final float imageWidth;
    private final File flexatarFile;
    private final String flexatarType;
    private CheckBoxCell checkBoxCell;

    public FlexatarCell(@NonNull Context context, Drawable drawable, File flexatarFile) {
        super(context);
        this.flexatarFile = flexatarFile;
        flexatarType = flexatarFile.getName().split("___")[0];
        icnFlx = new ImageView(context);
        icnFlx.setContentDescription("flexatar button");
        icnFlx.setBackground(Theme.createSelectorDrawable(ColorUtils.setAlphaComponent(Color.WHITE, (int) (255 * 0.3f))));
        icnFlx.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(0), AndroidUtilities.dp(0), AndroidUtilities.dp(0));

        icnFlx.setImageDrawable(drawable);
        ratio = (float)drawable.getMinimumHeight()/(float)drawable.getMinimumWidth();
        imageWidth = 150;
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(AndroidUtilities.dp(imageWidth), AndroidUtilities.dp(imageWidth));
//        layoutParams.gravity = Gravity.CENTER;
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        icnFlx.setLayoutParams(layoutParams);
        icnFlx.setId( View.generateViewId());
//            icnFlx.requestLayout();
        addView(icnFlx);
        checkBoxCell = new CheckBoxCell(getContext(), 1);
        checkBoxCell.setPadding(AndroidUtilities.dp(2),AndroidUtilities.dp(2),AndroidUtilities.dp(2),AndroidUtilities.dp(0));
        checkBoxCell.setBackgroundDrawable(Theme.getSelectorDrawable(false));

        RelativeLayout.LayoutParams checkBoxLayoutParams = new RelativeLayout.LayoutParams(AndroidUtilities.dp(50),AndroidUtilities.dp(50));
        checkBoxLayoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, icnFlx.getId());
        checkBoxLayoutParams.bottomMargin =  AndroidUtilities.dp(0);
        checkBoxLayoutParams.addRule(RelativeLayout.ALIGN_START, icnFlx.getId());
        checkBoxLayoutParams.setMarginStart( AndroidUtilities.dp(imageWidth/ratio+4));

        checkBoxCell.setLayoutParams(checkBoxLayoutParams);


    }
    public void setChecked(boolean checked, boolean animated){
        checkBoxCell.setChecked(checked,animated);
    }
    public boolean isBuiltin(){
        return flexatarType.equals("builtin");
    }
    public void addCheckbox(){
        if (flexatarType.equals("builtin")) return;
        if (indexOfChild(checkBoxCell) == -1)
            addView(checkBoxCell);
    }
    public void removeCheckbox(){
        removeView(checkBoxCell);
    }
    public boolean isChecked(){
        return checkBoxCell.isChecked();
    }

    public void deleteFlexatarFile() {
        FlexatarStorageManager.deleteFromStorage(getContext(),flexatarFile);
    }
}
