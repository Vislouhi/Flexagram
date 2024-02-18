package org.flexatar;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.GraySectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FoldUpFlexatarChooseView extends LinearLayout {
    private Runnable onRemoveViewListener;
//    private String flxId;

    public FoldUpFlexatarChooseView(@NonNull Context context,String flxId) {
        super(context);
        setOrientation(VERTICAL);

        GraySectionCell dividerCell = new GraySectionCell(context);

        addView(dividerCell,LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT));
        RecyclerView recyclerView = new RecyclerView(getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new Adapter(getContext(),flxId));
        addView(recyclerView,LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT));
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        dividerCell.setText(LocaleController.getString("MyFlexatars", R.string.MyFlexatars),
                LocaleController.getString("Close", R.string.Close),v->{
            animateClose();
        });
    }

    public void addOnRemoveViewListener(Runnable onRemoveViewListener) {
        this.onRemoveViewListener=onRemoveViewListener;
    }



    public interface OnFlexatarChosenListener{
        void onChosen(File flexatarFile);
    }
    private OnFlexatarChosenListener onFlexatarChosenListener;
    public void addOnFlexatarChosenListener(OnFlexatarChosenListener onFlexatarChosenListener) {
        this.onFlexatarChosenListener = onFlexatarChosenListener;
    }

    public class Adapter  extends RecyclerView.Adapter<Adapter.ViewHolder>{
        private final Context mContext;
        private final List<File> flxFiles;
        private final String flxId;
        public Adapter(Context context,String flxId) {
            super();
            this.flxId=flxId;
            mContext = context;
            Log.d("FLX_INJECT","current id is "+flxId);
            List<String> hiddenRecords = FlexatarStorageManager.getHiddenRecords(mContext);
            flxFiles = FlexatarStorageManager.getFlexatarFileListExcept(mContext,new ArrayList<String>(){{
                add(flxId);addAll(hiddenRecords);addAll(FlexatarStorageManager.getGroups(mContext));
            }});
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            FlexatarCell cell = new FlexatarCell(mContext);
            return new Adapter.ViewHolder(cell);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FlexatarCell cell = (FlexatarCell) holder.itemView;
            cell.loadFromFile(flxFiles.get(position));
            cell.setOnClickListener(v->{
                String chosenId = cell.getFlexatarFile().getName().replace(".flx","");
                FlexatarStorageManager.addGroupRecord(mContext,flxId,chosenId);
                FlexatarStorageManager.addStorageHiddenRecord(mContext,chosenId);
//                int cellIdx = flxFiles.indexOf(cell.getFlexatarFile());
                FoldUpFlexatarChooseView.this.onFlexatarChosenListener.onChosen(flxFiles.get(position));
                FoldUpFlexatarChooseView.this.animateClose();

                /*flxFiles.remove(cellIdx);
                AndroidUtilities.runOnUIThread(()->{
                    notifyItemRemoved(cellIdx);
                });*/

            });

        }

        @Override
        public int getItemCount() {
            return flxFiles.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }

    }


    private void animateClose(){

        ValueAnimator animator = ValueAnimator.ofInt(getHeight(), 0);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                // Update the height of the view
                int animatedValue = (int) animation.getAnimatedValue();
                FrameLayout.LayoutParams param = new FrameLayout.LayoutParams(LayoutHelper.MATCH_PARENT, animatedValue, Gravity.BOTTOM);
                setLayoutParams(param);
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animator) {

            }

            @Override
            public void onAnimationEnd(@NonNull Animator animator) {
                ((FrameLayout)getParent()).removeView(FoldUpFlexatarChooseView.this);
                onRemoveViewListener.run();
            }

            @Override
            public void onAnimationCancel(@NonNull Animator animator) {

            }

            @Override
            public void onAnimationRepeat(@NonNull Animator animator) {

            }
        });
        animator.setDuration(250); // Adjust the duration as needed
        animator.start();
    }
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        int currentOrientation = getResources().getConfiguration().orientation;
        boolean isPortrait = currentOrientation == Configuration.ORIENTATION_PORTRAIT;

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
//        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        Log.d("FLX_INJECT","screen height "+screenHeight);
        Log.d("FLX_INJECT","popup height "+screenHeight*2/3);
        ValueAnimator animator = ValueAnimator.ofInt(0, isPortrait ? screenHeight*2/3:((View)getParent()).getHeight());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                // Update the height of the view
                int animatedValue = (int) animation.getAnimatedValue();
                FrameLayout.LayoutParams param = new FrameLayout.LayoutParams(LayoutHelper.MATCH_PARENT, animatedValue, Gravity.BOTTOM);
                setLayoutParams(param);
            }
        });
        animator.setDuration(250); // Adjust the duration as needed
        animator.start();
    }
}
