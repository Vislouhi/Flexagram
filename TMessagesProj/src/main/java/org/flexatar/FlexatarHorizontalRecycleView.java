package org.flexatar;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.flexatar.DataOps.FlexatarData;
import org.flexatar.DataOps.LengthBasedFlxUnpack;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Cells.TextCell;

import java.io.File;

public class FlexatarHorizontalRecycleView extends RecyclerView {
    public FlexatarHorizontalRecycleView(@NonNull Context context,FlexatarUI.FlexatarChooseListener onChooseListener) {
        super(context);
        setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        setAdapter(new Adapter(context,onChooseListener));
    }

    public static class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        private final Context mContext;
        private final File[] flexatarsInLocalStorage;
        private final FlexatarUI.FlexatarChooseListener onChooseListener;
        private OnFlexatarChosen onFlexatarChosenListener = null;

        public Adapter(Context context, FlexatarUI.FlexatarChooseListener onChooseListener){
            mContext = context;
            flexatarsInLocalStorage = FlexatarStorageManager.getFlexatarFileListExcept(context, FlexatarStorageManager.getHiddenRecords(context)).toArray(new File[0]);
//            flexatarsInLocalStorage = FlexatarStorageManager.getFlexatarFileList(context);
            this.onChooseListener = onChooseListener;
        }
        public interface OnFlexatarChosen{
            void onFlexatarChosen(File file);
        }
        public void setAndOverrideOnItemClickListener(OnFlexatarChosen onFlexatarChosenListener){
            this.onFlexatarChosenListener=onFlexatarChosenListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView icnFlx = new ImageView(mContext);
            icnFlx.setContentDescription("flexatar button");
            icnFlx.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(0), AndroidUtilities.dp(0), AndroidUtilities.dp(0));

            return new ViewHolder(icnFlx);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            File flexatarFile = flexatarsInLocalStorage[position];
            Bitmap iconBitmap = FlexatarStorageManager.getFlexatarMetaData(flexatarFile,true).previewImage;
            float ratio = (float)iconBitmap.getHeight()/(float)iconBitmap.getWidth();
            RoundedBitmapDrawable dr = RoundedBitmapDrawableFactory.create(mContext.getResources(), iconBitmap);
            dr.setCornerRadius(AndroidUtilities.dp(8));
            ((ImageView) holder.itemView).setImageDrawable(dr);
            float imageWidth = 70;
            holder.itemView.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtilities.dp(imageWidth), AndroidUtilities.dp(imageWidth*ratio)));
            holder.itemView.setOnClickListener((v) -> {
                if (onFlexatarChosenListener == null) {
                    if (FlexatarStorageManager.callFlexatarChooser.getChosenFirst().getName().equals(flexatarFile.getName())) return;
                    FlexatarStorageManager.callFlexatarChooser.setChosenFlexatar(flexatarFile.getAbsolutePath());
                    FlexatarStorageManager.callFlexatarChooser.getFirstFlxData();
                    FlexatarStorageManager.callFlexatarChooser.getSecondFlxData();

                   /* if (FlexatarUI.chosenFirst.getName().equals(flexatarFile.getName())) return;
                    FlexatarUI.chosenSecond = FlexatarUI.chosenFirst;
                    FlexatarUI.chosenFirst = flexatarFile;
                    FlexatarRenderer.altFlxData = FlexatarRenderer.currentFlxData;
                    byte[] flxBytes = FlexatarStorageManager.dataFromFile(flexatarFile);
                    FlexatarRenderer.currentFlxData = new FlexatarData(new LengthBasedFlxUnpack(flxBytes));*/

                    /*if (FlexatarUI.chosenEffect.equals("Morph")) {
                        FlexatarRenderer.effectsMixWeight = 0;
                    }*/
                    if (onChooseListener != null)
                        onChooseListener.onChoose(((ImageView) holder.itemView));
                }else{
                    onFlexatarChosenListener.onFlexatarChosen(flexatarFile);
                }

            });
        }

        @Override
        public int getItemCount() {
            return flexatarsInLocalStorage.length;
        }
        public static class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
