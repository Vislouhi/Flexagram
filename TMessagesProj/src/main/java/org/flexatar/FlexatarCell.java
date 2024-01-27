package org.flexatar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Cells.DividerCell;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;


public class FlexatarCell extends RelativeLayout {
    private final ImageView icnFlx;
    private final float ratio;
    private final float imageWidth;
    private final File flexatarFile;
    private final String flexatarType;
    private final TextView nameTextView;
    private final FlexatarStorageManager.FlexatarMetaData flexatarMetaData;
    private CheckBoxCell checkBoxCell;

    public File getFlexatarFile(){
        return flexatarFile;
    }
    public void setName(String name){
        nameTextView.setText(name);
    }
    public FlexatarCell(@NonNull Context context,  File flexatarFile) {
        super(context);
        flexatarMetaData = FlexatarStorageManager.getFlexatarMetaData(flexatarFile,true);
        Bitmap iconBitmap = flexatarMetaData.previewImage;
        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(context.getResources(), iconBitmap);

        drawable.setCornerRadius(AndroidUtilities.dp(8));
        this.flexatarFile = flexatarFile;
        flexatarType = flexatarFile.getName().split("___")[0];
        icnFlx = new ImageView(context);
        icnFlx.setContentDescription("flexatar button");
//        icnFlx.setBackground(Theme.createSelectorDrawable(ColorUtils.setAlphaComponent(Color.WHITE, (int) (255 * 0.3f))));
        icnFlx.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(0), AndroidUtilities.dp(0), AndroidUtilities.dp(0));

        icnFlx.setImageDrawable(drawable);
        ratio = (float)drawable.getMinimumHeight()/(float)drawable.getMinimumWidth();
        imageWidth = 100;
        int imageHeight = (int) (imageWidth * ratio);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(AndroidUtilities.dp(imageWidth), AndroidUtilities.dp(imageHeight));
//        layoutParams.gravity = Gravity.CENTER;
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        layoutParams.setMargins(AndroidUtilities.dp(32),0,0,0);

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
        checkBoxLayoutParams.addRule(RelativeLayout.END_OF, icnFlx.getId());
        checkBoxLayoutParams.setMargins(0,0,0,0);
//        checkBoxLayoutParams.setMarginStart( AndroidUtilities.dp(4));

        checkBoxCell.setLayoutParams(checkBoxLayoutParams);



        LinearLayout textContentLayout = new LinearLayout(context);
        textContentLayout.setOrientation(LinearLayout.VERTICAL);
        RelativeLayout.LayoutParams textContenLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
        textContenLayoutParams.addRule(RelativeLayout.END_OF, icnFlx.getId());
        textContenLayoutParams.setMargins(0,0,0,0);
        addView(textContentLayout,textContenLayoutParams);

        nameTextView = new TextView(context);
        nameTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        nameTextView.setText(flexatarMetaData.name);
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        nameTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        textContentLayout.addView(nameTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT,LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL,0,16,0,0));

        TextView dateTextView = new TextView(context);
        dateTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        dateTextView.setText(flexatarMetaData.date);
        dateTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        dateTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmono.ttf"));
        textContentLayout.addView(dateTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT,LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL,0,6,0,0));

        DividerCell dividerCell = new DividerCell(context);
        dividerCell.setPadding(AndroidUtilities.dp(28), AndroidUtilities.dp(8), AndroidUtilities.dp(28), AndroidUtilities.dp(8));
        textContentLayout.addView(dividerCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 5,Gravity.BOTTOM,0,0,0,4));


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

    public FlexatarStorageManager.FlexatarMetaData getMetaData() {
        return  flexatarMetaData;
    }
}
