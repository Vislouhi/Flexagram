package org.flexatar;

import android.content.Context;
import android.view.Gravity;
import android.view.View;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SeekBarView;

public class FlexatarCalibrationCell {

    private final SeekBarView seekBar;
    private final HeaderCell sliderHeader;
    private OnDragListener onDragListener;

    public SeekBarView getSeekBar(){
        return seekBar;
    }
    public HeaderCell getHeaderCell(){
        return sliderHeader;
    }

    public void setProgress(float v) {
        seekBar.setProgress(v);
    }

    public interface OnDragListener{
        void onDrag(float progress);
    }
    public void setOnDragListener(OnDragListener listener){
        onDragListener = listener;
    }
    public  FlexatarCalibrationCell(Context context, String text){
        sliderHeader = new HeaderCell(context);
        sliderHeader.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        sliderHeader.setText(text);
        seekBar = new SeekBarView(context);
        seekBar.setReportChanges(true);
        int stepCount = 30;
        seekBar.setSeparatorsCount(stepCount);
        seekBar.setProgress(0.5f);
        seekBar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        seekBar.setDelegate(new SeekBarView.SeekBarViewDelegate() {
            @Override
            public void onSeekBarDrag(boolean stop, float progress) {
                if (onDragListener != null){
                    onDragListener.onDrag(progress);
                }
            }

            @Override
            public void onSeekBarPressed(boolean pressed) {
            }

            @Override
            public CharSequence getContentDescription() {
                return "No";
            }

            @Override
            public int getStepsCount() {
                return stepCount;
            }
        });
    }
}
