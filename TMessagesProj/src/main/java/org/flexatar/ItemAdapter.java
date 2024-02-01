package org.flexatar;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.GraySectionCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    private final List<ItemModel> itemsAction;
    private final List<ItemModel> itemsProgress;
    private final List<ItemModel> itemsFlexatar;
    private Context context;
    private ClickListener flexatarCellOnLongClickListener;
    private boolean isCheckBoxes = false;
    Handler handler = new Handler(Looper.getMainLooper());

    public List<File> removeCheckedFlexatars() {
        List<File> files = new ArrayList<>();
        for(ItemModel item : itemsFlexatar){

            if (item.isChecked()){
                files.add(item.getFlexatarFile());
            }
        }
        handler.post(()->{
            Iterator<ItemModel> iterator = itemsFlexatar.iterator();
//            List<File> files = new ArrayList<>();
//            Log.d("FLX_INJECT","size "+itemsProgress.size());
            int counter = itemsAction.size() + (itemsProgress.size() > 1 ? itemsProgress.size() : 0);
//            Log.d("FLX_INJECT","counter start"+counter);
            while (iterator.hasNext()) {
                ItemModel element = iterator.next();
//                Log.d("FLX_INJECT","counter tick"+counter);
                // Add your condition here
                if (element.isChecked()) {

//                    files.add(element.getFlexatarFile());

                        iterator.remove();
//                    Log.d("FLX_INJECT","notifyItemRemoved "+counter);
                        notifyItemRemoved(counter);



                }else{
                    counter += 1;
                }
            }
            removeCheckBoxes();
//
        });
        return files;
    }

    public interface ClickListener{
        void onClick(ItemModel item,FlexatarCellNew cell);
    }
    public void setFlexatarCellOnClickListener(ClickListener flexatarCellOnClickListener) {
        this.flexatarCellOnClickListener = flexatarCellOnClickListener;
    }
    public void setFlexatarCellOnLongClickListener(ClickListener flexatarCellOnLongClickListener) {
        this.flexatarCellOnLongClickListener = flexatarCellOnLongClickListener;
    }

    private ClickListener flexatarCellOnClickListener;

    public ItemAdapter(Context context, List<ItemModel> itemsAction, List<ItemModel> itemsProgress, List<ItemModel> itemsFlexatar) {
        this.context = context;
        this.itemsAction = itemsAction;
        this.itemsProgress = itemsProgress;
        this.itemsFlexatar = itemsFlexatar;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ItemModel.ACTION_CELL) {
            TextCell textCell = new TextCell(context);

            return new ViewHolder(textCell);
        }
        else if (viewType == ItemModel.PROGRESS_CELL) {
            FlexatarProgressCell flexatarProgressCell = new FlexatarProgressCell(context);

            return new ViewHolder(flexatarProgressCell);
        }else if (viewType == ItemModel.FLEXATAR_CELL){
            FlexatarCellNew flexatarCell = new FlexatarCellNew(context);
//            Log.d("FLX_INJECT","create flexatar cell" );
            return new ViewHolder(flexatarCell);
        }else{
            GraySectionCell dividerCell = new GraySectionCell(context);
            return new ViewHolder(dividerCell);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Bind data to the TextView
        ItemModel item = getItem(position);

        switch (holder.getItemViewType()) {
            case ItemModel.ACTION_CELL:
                ((TextCell) holder.itemView).setTextAndIcon(item.getNameText(), item.getImageResource(), false);
                ((TextCell) holder.itemView).setOnClickListener(item.getOnClickListener());
                break;
            case ItemModel.PROGRESS_CELL:
                FlexatarProgressCell cell = (FlexatarProgressCell) holder.itemView;
                cell.setTicket(item.getNameText());
                cell.addDismissListener(item.getDismissListener());
                cell.setOnClickListener(item.getOnClickListener());
                item.setFlexatarProgressCell(cell);
                if (item.getErrorCode() != null){
                    cell.setError(item.getErrorCode());
                }
                if (item.getProgressTime() != null){
                    cell.setTime(item.getProgressTime());
                }

                break;
            case ItemModel.FLEXATAR_CELL:
//                Log.d("FLX_INJECT","add flx cell " + position);
                FlexatarCellNew fCell = (FlexatarCellNew) holder.itemView;
                fCell.loadFromFile(item.getFlexatarFile());
                fCell.setOnClickListener(v->{
                    flexatarCellOnClickListener.onClick(item,fCell);
                    notifyItemChanged(position);
                });

                fCell.setOnLongClickListener(v->{
                    flexatarCellOnLongClickListener.onClick(item,fCell);
//                    int start = itemsAction.size() + (itemsProgress.size() > 1 ? itemsProgress.size() : 0);
//                    notifyItemRangeChanged(start + 1,itemsFlexatar.size()-1);

                    return true;
                });
                fCell.setChecked(item.isChecked(),true);
                if (isCheckBoxes){
                    fCell.addCheckbox();
                }else{
                    fCell.removeCheckbox();
                }
                break;
            case ItemModel.DELIMITER:
                ((GraySectionCell) holder.itemView).setText(item.getNameText());
                break;
        }
//        ItemModel item = itemList.get(position);
//        TextCell textView = (TextCell) holder.itemView;
//        textView.setTextAndIcon(item.getItemName(), R.drawable.msg_help, false);
//        Log.d("FLX_INJECT","onBindViewHolder");
    }

    @Override
    public int getItemCount() {
        int size = itemsAction.size();
        if (itemsProgress.size()>1) size += itemsProgress.size();
        size+=itemsFlexatar.size();
        return size;
    }
    @Override
    public int getItemViewType(int position) {
        return getItem(position).getCellType();

    }

    private ItemModel getItem(int position){
        int p = position;
        if (p<itemsAction.size()){
            return itemsAction.get(p);
        }
        p -= itemsAction.size();
        if (itemsProgress.size()>1){
            if (p<itemsProgress.size()){
                return itemsProgress.get(p);
            }
            p -= itemsProgress.size();
        }


        return itemsFlexatar.get(p);
    }

    public void addCheckBoxes() {
        this.isCheckBoxes = true;
        handler.post(()->{
            int start = itemsAction.size() + (itemsProgress.size() > 1 ? itemsProgress.size() : 0) + 1;
            notifyItemRangeChanged(start,itemsFlexatar.size()-1);
        });
    }

    public void removeCheckBoxes() {
        this.isCheckBoxes = false;
        for (ItemModel item:itemsFlexatar ){
            item.setChecked(false);
        }
        handler.post(()->{
            int start = itemsAction.size() + (itemsProgress.size() > 1 ? itemsProgress.size() : 0) + 1;
            notifyItemRangeChanged(start,itemsFlexatar.size()-1);
        });

    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
    public void addProgressItem(ItemModel item){
        handler.post(()->{
            itemsProgress.add(1,item);
            if (itemsProgress.size()>2)
                notifyItemInserted(itemsAction.size()+1);
            else {
                notifyItemInserted(itemsAction.size());
                notifyItemInserted(itemsAction.size() + 1);
            }

        });

    }
    public void removeProgressItem(ItemModel item){
        handler.post(()->{
            int removeIdx = itemsProgress.indexOf(item);
            itemsProgress.remove(removeIdx);
            notifyItemRemoved(itemsAction.size() + removeIdx);
            if (itemsProgress.size() == 1)  notifyItemRemoved(itemsAction.size());


        });
    }
    public void addFlexatarItem(ItemModel item){
        handler.post(()->{
            itemsFlexatar.add(1,item);
            int start = itemsAction.size() + (itemsProgress.size() > 1 ? itemsProgress.size() : 0) + 1;
            notifyItemInserted(start);

        });
    }

}
