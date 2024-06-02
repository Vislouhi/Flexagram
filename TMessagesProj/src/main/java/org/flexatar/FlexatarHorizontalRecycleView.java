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
    public FlexatarHorizontalRecycleView(@NonNull Context context,int account,int flexatarType,FlexatarUI.FlexatarChooseListener onChooseListener) {
        super(context);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        setLayoutManager(layoutManager);
        setAdapter(new Adapter(context,account,flexatarType,layoutManager,onChooseListener));

    }

    public static class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        private final Context mContext;
        private File[] flexatarsInLocalStorage;
        private final FlexatarUI.FlexatarChooseListener onChooseListener;
        private final int account;
        private OnFlexatarChosen onFlexatarChosenListener = null;

        public Adapter(Context context,int account,int flexatarType,LinearLayoutManager layoutManager, FlexatarUI.FlexatarChooseListener onChooseListener){
            this.account=account;
            mContext = context;
            if (flexatarType == 0){
                flexatarsInLocalStorage = FlexatarStorageManager.getVideoFlexatarFileList(context,this.account);

            }else if (flexatarType == 1){
                flexatarsInLocalStorage = FlexatarStorageManager.getFlexatarFileListExcept(context,account, FlexatarStorageManager.getHiddenRecords(context,account)).toArray(new File[0]);

            }else{
                flexatarsInLocalStorage = FlexatarStorageManager.getVideoFlexatarFileList(context,this.account);
            }
//            flexatarsInLocalStorage = FlexatarStorageManager.getFlexatarFileList(context);
            this.onChooseListener = onChooseListener;
            FlexatarStorageManager.storageActionListener = (a,code,file) -> {
                if (a!=account) return;
                if (code == FlexatarStorageManager.FLEXATAR_DELETE){
                    ModifyResult result = deleteStringFromArray(flexatarsInLocalStorage, file);

                    flexatarsInLocalStorage = result.array;
                    if (result.idx!=-1)
                        AndroidUtilities.runOnUIThread(()->{
                            notifyItemRemoved(result.idx);
                        });
                }else if (code == FlexatarStorageManager.FLEXATAR_ADD){
                    flexatarsInLocalStorage = addFileToArray(flexatarsInLocalStorage,file);
                    AndroidUtilities.runOnUIThread(()->{

                        notifyItemInserted(0);
                        layoutManager.scrollToPosition(0);
                    });
                }
            };
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
                    if (FlexatarStorageManager.callFlexatarChooser[this.account].getChosenFirst().getName().equals(flexatarFile.getName())) return;
                    FlexatarStorageManager.callFlexatarChooser[this.account].setChosenFlexatar(flexatarFile.getAbsolutePath());
                    FlexatarStorageManager.callFlexatarChooser[this.account].getFirstFlxData();
                    FlexatarStorageManager.callFlexatarChooser[this.account].getSecondFlxData();

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

        // Record to store the result
        private static class ModifyResult {
            public File[] array;
            public int idx;
            public ModifyResult(File[] newArray, int modifiedIndex){
                array=newArray;
                idx=modifiedIndex;
            }
        }

        // Function to delete the given string from the array
        public static ModifyResult deleteStringFromArray(File[] array, File stringToDelete) {
            int index = -1;

            // Find the index of the string to delete
            for (int i = 0; i < array.length; i++) {
                if (array[i].equals(stringToDelete)) {
                    index = i;
                    break;
                }
            }

            // If the string is not found, return the original array and index -1
            if (index == -1) {
                return new ModifyResult(array, index);
            }

            // Create a new array without the deleted string
            File[] newArray = new File[array.length - 1];
            int newArrayIndex = 0;
            for (int i = 0; i < array.length; i++) {
                if (i != index) {
                    newArray[newArrayIndex++] = array[i];
                }
            }

            // Return the result with the new array and the index of the deleted string
            return new ModifyResult(newArray, index);
        }
        public static File[] addFileToArray(File[] array, File fileToAdd) {
            // Create a new array with one more element than the original array
            File[] newArray = new File[array.length + 1];

            // Assign the given File to the first position of the new array
            newArray[0] = fileToAdd;

            // Copy the elements of the original array to the new array starting from the second position
            System.arraycopy(array, 0, newArray, 1, array.length);

            // Return the new array
            return newArray;
        }

    }

}
