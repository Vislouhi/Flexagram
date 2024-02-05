package org.flexatar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Cells.DividerCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieDrawable;

import java.io.File;


public class FlexatarCellNew extends RelativeLayout {
    private final ImageView icnFlx;
    private final TextView dateTextView;
    private float ratio;
    private float imageWidth;
    private File flexatarFile;
    private String flexatarType;
    private final TextView nameTextView;
    private FlexatarStorageManager.FlexatarMetaData flexatarMetaData;
    private CheckBoxCell checkBoxCell;
    private boolean isPublic;
    private RLottieDrawable downloadDrawable;
    private Theme.ResourcesProvider resourceProvider;

    public File getFlexatarFile(){
        return flexatarFile;
    }
    public void setName(String name){
        nameTextView.setText(name);
    }
    private boolean isDownload = false;
    public FlexatarCellNew(@NonNull Context context) {
        super(context);

        RectF rectF = new RectF();
        Path path = new Path();
        icnFlx = new ImageView(context)/*{
            @Override
            protected void onDraw(Canvas canvas) {
                if (isDownload){
                    rectF.set(0, 0, getWidth(), getHeight());
                    path.addRoundRect(rectF, 20, 20, Path.Direction.CW);
                    canvas.clipPath(path);
                    super.onDraw(canvas);
                }else
                    super.onDraw(canvas);
            }
        }*/;

        icnFlx.setContentDescription("flexatar button");
//        icnFlx.setBackground(Theme.createSelectorDrawable(ColorUtils.setAlphaComponent(Color.WHITE, (int) (255 * 0.3f))));
        icnFlx.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(0), AndroidUtilities.dp(0), AndroidUtilities.dp(0));



        icnFlx.setId( View.generateViewId());
//            icnFlx.requestLayout();
        addView(icnFlx);

        checkBoxCell = new CheckBoxCell(getContext(), 1);
        checkBoxCell.setPadding(AndroidUtilities.dp(2),AndroidUtilities.dp(2),AndroidUtilities.dp(2),AndroidUtilities.dp(0));
        checkBoxCell.setBackgroundDrawable(Theme.getSelectorDrawable(false));

        LayoutParams checkBoxLayoutParams = new LayoutParams(AndroidUtilities.dp(50),AndroidUtilities.dp(50));
        checkBoxLayoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, icnFlx.getId());
        checkBoxLayoutParams.bottomMargin =  AndroidUtilities.dp(0);
        checkBoxLayoutParams.addRule(RelativeLayout.END_OF, icnFlx.getId());
        checkBoxLayoutParams.setMargins(0,0,0,0);
//        checkBoxLayoutParams.setMarginStart( AndroidUtilities.dp(4));

        checkBoxCell.setLayoutParams(checkBoxLayoutParams);



        LinearLayout textContentLayout = new LinearLayout(context);
        textContentLayout.setOrientation(LinearLayout.VERTICAL);
        LayoutParams textContenLayoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
        textContenLayoutParams.addRule(RelativeLayout.END_OF, icnFlx.getId());
        textContenLayoutParams.setMargins(0,0,0,0);
        addView(textContentLayout,textContenLayoutParams);

        nameTextView = new TextView(context);
        nameTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));

        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        nameTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        textContentLayout.addView(nameTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT,LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL,0,16,0,0));

        dateTextView = new TextView(context);
        dateTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));

        dateTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        dateTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmono.ttf"));
        textContentLayout.addView(dateTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT,LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL,0,6,0,0));

        DividerCell dividerCell = new DividerCell(context);
        dividerCell.setPadding(AndroidUtilities.dp(28), AndroidUtilities.dp(8), AndroidUtilities.dp(28), AndroidUtilities.dp(8));
        textContentLayout.addView(dividerCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 5,Gravity.BOTTOM,0,0,0,4));


    }
    private boolean isBuiltin = false;
    public void loadFromFile(File flexatarFile){
        this.flexatarFile = flexatarFile;
        isBuiltin =  flexatarFile.getName().startsWith(FlexatarStorageManager.BUILTIN_PREFIX);
        isPublic = flexatarFile.getName().startsWith(FlexatarStorageManager.PUBLIC_PREFIX) ;
//        flexatarType = flexatarFile.getName().split("___")[0];
        flexatarMetaData = FlexatarStorageManager.getFlexatarMetaData(flexatarFile,true);
        Bitmap iconBitmap = flexatarMetaData.previewImage;
        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getContext().getResources(), iconBitmap);

        drawable.setCornerRadius(AndroidUtilities.dp(8));

        icnFlx.setImageDrawable(drawable);
        ratio = (float)drawable.getMinimumHeight()/(float)drawable.getMinimumWidth();
        imageWidth = 100;
        int imageHeight = (int) (imageWidth * ratio);
        LayoutParams layoutParams = new LayoutParams(AndroidUtilities.dp(imageWidth), AndroidUtilities.dp(imageHeight));
//        layoutParams.gravity = Gravity.CENTER;
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        layoutParams.setMargins(AndroidUtilities.dp(32),0,0,0);

        icnFlx.setLayoutParams(layoutParams);
        nameTextView.setText(flexatarMetaData.name);
        dateTextView.setText(flexatarMetaData.date);
    }

    public void setLoading(){
        downloadDrawable = new RLottieDrawable(R.raw.download_progress, "download_progress", AndroidUtilities.dp(36), AndroidUtilities.dp(36), false, null);
//        downloadDrawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_actionBarDefaultIcon), PorterDuff.Mode.MULTIPLY));

        icnFlx.setImageDrawable(downloadDrawable);
        downloadDrawable.setAutoRepeat(1);
        downloadDrawable.start();

//        ratio = (float)downloadDrawable.getMinimumHeight()/(float)downloadDrawable.getMinimumWidth();
        imageWidth = 100;
//        int imageHeight = (int) (imageWidth * ratio);
        LayoutParams layoutParams = new LayoutParams(AndroidUtilities.dp(imageWidth), AndroidUtilities.dp(imageWidth));
//        layoutParams.gravity = Gravity.CENTER;
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        layoutParams.setMargins(AndroidUtilities.dp(32),0,0,0);
//        icnFlx.setBackgroundColor(Color.BLACK);
        int cornerRadius = AndroidUtilities.dp(20);
        int fillColor = Theme.getColor( Theme.key_actionBarDefault, resourceProvider);
//        int fillColor = Theme.key_actionBarActionModeDefault; // Replace with your desired color

        // Create a rounded rectangle shape
        RoundRectShape roundRectShape = new RoundRectShape(new float[]{cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius}, null, null);

        // Create a ShapeDrawable with the rounded rectangle shape
        ShapeDrawable shapeDrawable = new ShapeDrawable(roundRectShape);

        // Set the fill color
        shapeDrawable.getPaint().setColor(fillColor);
        icnFlx.setBackground(shapeDrawable);

        icnFlx.setLayoutParams(layoutParams);
        nameTextView.setText(LocaleController.getString("Downloading", R.string.Downloading));
        dateTextView.setText("");
        isDownload = true;

    }
    public void setChecked(boolean checked, boolean animated){
//        Log.d("FLX_INJECT","flx cell set checked");
        checkBoxCell.setChecked(checked,animated);
    }
    public boolean isBuiltin(){
        return isBuiltin;
    }
    public boolean isPublic(){
        return isPublic;
    }
    public void addCheckbox(){
        if (isBuiltin) {
            if (indexOfChild(checkBoxCell) != -1) removeCheckbox();
            return;
        }
        if (indexOfChild(checkBoxCell) == -1)
            addView(checkBoxCell);
    }
    public void removeCheckbox(){
        if (indexOfChild(checkBoxCell) != -1) removeView(checkBoxCell);
    }
    public boolean isChecked(){
        return checkBoxCell.isChecked();
    }

    /*public void deleteFlexatarFile() {
        FlexatarStorageManager.deleteFromStorage(getContext(),flexatarFile);
    }*/

    public FlexatarStorageManager.FlexatarMetaData getMetaData() {
        return  flexatarMetaData;
    }

    public void setResourceProvider(Theme.ResourcesProvider resourceProvider) {
        this.resourceProvider=resourceProvider;
    }
}
