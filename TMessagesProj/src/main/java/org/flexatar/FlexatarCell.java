package org.flexatar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
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


public class FlexatarCell extends RelativeLayout {
    private final ImageView icnFlx;
    private TextView dateTextView;
    private final LinearLayout textContentLayout;
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
    private DividerCell dividerCell;
    private LinearLayout moveCellView;
    private OnMoveControlPressedListener onMoveControlPressedListener;
    private TextView groupCountTextView;

    public File getFlexatarFile(){
        return flexatarFile;
    }
    public void setName(String name){
        nameTextView.setText(name);
    }
    private boolean isDownload = false;
    public FlexatarCell(@NonNull Context context) {
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



        textContentLayout = new LinearLayout(context);
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



    }
    private boolean isBuiltin = false;
    public void loadFromFile(File flexatarFile){
        loadFromFile(flexatarFile,FlxCellType.FLX_DATE);
    }

    public enum FlxCellType {FLX_DATE,FLX_MOVE};
    public void loadFromFile(File flexatarFile,FlxCellType cellType){
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
        icnFlx.setBackground(null);
        icnFlx.setLayoutParams(layoutParams);
        nameTextView.setText(flexatarMetaData.name);
        if (cellType == FlxCellType.FLX_DATE) {
            if (dateTextView!=null) textContentLayout.removeView(dateTextView);
            if (dividerCell!=null) textContentLayout.removeView(dividerCell);
            if (moveCellView!=null) textContentLayout.removeView(moveCellView);
            if (groupCountTextView!=null) textContentLayout.removeView(groupCountTextView);

            dateTextView = new TextView(getContext());
            dateTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));

            dateTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            dateTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmono.ttf"));
            textContentLayout.addView(dateTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 6, 0, 0));
            String groupId = flexatarFile.getName().replace(".flx","");
            int groupSize = FlexatarStorageManager.getGroupSize(getContext(),groupId);
            if (groupSize>0) {
                groupCountTextView = new TextView(getContext());
                groupCountTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));

                groupCountTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                groupCountTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmono.ttf"));

                groupCountTextView.setText(LocaleController.getString("GroupSize", R.string.GroupSize) +groupSize);
                textContentLayout.addView(groupCountTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 6, 0, 0));
            }


            dividerCell = new DividerCell(getContext());
            dividerCell.setPadding(AndroidUtilities.dp(28), AndroidUtilities.dp(8), AndroidUtilities.dp(28), AndroidUtilities.dp(8));
            textContentLayout.addView(dividerCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 5, Gravity.BOTTOM, 0, 0, 0, 4));

            dateTextView.setText(flexatarMetaData.date);
        } else if (cellType == FlxCellType.FLX_MOVE) {
            if (dateTextView!=null) textContentLayout.removeView(dateTextView);
            if (dividerCell!=null) textContentLayout.removeView(dividerCell);
            if (moveCellView!=null) textContentLayout.removeView(moveCellView);
            if (groupCountTextView!=null) textContentLayout.removeView(groupCountTextView);

            moveCellView = new LinearLayout(getContext());
            moveCellView.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(AndroidUtilities.dp(34), AndroidUtilities.dp(34), 1);
            ImageView moveUpwardsIcon = new ImageView(getContext());
            moveUpwardsIcon.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_actionBarDefault, resourceProvider), PorterDuff.Mode.MULTIPLY));

            moveUpwardsIcon.setImageResource(R.drawable.msg_go_up);
            moveUpwardsIcon.setOnClickListener(v->{
                if (onMoveControlPressedListener!=null) onMoveControlPressedListener.onUp();
            });
            moveCellView.addView(moveUpwardsIcon,params);

            ImageView moveDownwardsIcon = new ImageView(getContext());
            moveDownwardsIcon.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_actionBarDefault, resourceProvider), PorterDuff.Mode.MULTIPLY));

            moveDownwardsIcon.setImageResource(R.drawable.msg_go_down);
            moveDownwardsIcon.setOnClickListener(v->{
                if (onMoveControlPressedListener!=null) onMoveControlPressedListener.onDown();
            });
            moveCellView.addView(moveDownwardsIcon,params);
            ImageView removeIcon = new ImageView(getContext());
            removeIcon.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_actionBarDefault, resourceProvider), PorterDuff.Mode.MULTIPLY));

            removeIcon.setImageResource(R.drawable.msg_round_cancel_m);
            removeIcon.setOnClickListener(v->{
                if (onMoveControlPressedListener!=null) onMoveControlPressedListener.oClose();
            });
            moveCellView.addView(removeIcon,params);
            textContentLayout.addView(moveCellView,LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT,0,12,0,0));

        }
    }
    public void setOnMoveControlPressedListener(OnMoveControlPressedListener l ){
        onMoveControlPressedListener =l;
    }

    public interface OnMoveControlPressedListener{
        void onUp();
        void onDown();
        void oClose();
    }
    public void setLoading(){
        flexatarFile = null;
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
        if (groupCountTextView!=null)groupCountTextView.setText("");
        if (dateTextView!=null) dateTextView.setText("");
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
