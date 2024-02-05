package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import org.flexatar.FlexatarCellNew;
import org.flexatar.FlexatarStorageManager;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.SharedDocumentCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Components.ChatAttachAlert;
import org.telegram.ui.Components.ChatAttachAlertDocumentLayout;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.FillLastLinearLayoutManager;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.FilteredSearchView;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class ChatAttachAlertFlexatarLayout extends ChatAttachAlert.AttachAlertLayout {
    public interface FlexatarSelectActivityDelegate {
        void didSelectFiles(ArrayList<String> files, String caption, ArrayList<MessageObject> fmessages, boolean notify, int scheduleDate);
        default void didSelectPhotos(ArrayList<SendMessagesHelper.SendingMediaInfo> photos, boolean notify, int scheduleDate) {

        }

        default void startDocumentSelectActivity() {

        }

        default void startMusicSelectActivity() {
        }
    }
    private FlexatarSelectActivityDelegate delegate;
    public void setDelegate(FlexatarSelectActivityDelegate flexatarSelectActivityDelegate) {
        delegate = flexatarSelectActivityDelegate;
    }
    private final RecyclerListView listView;
    private final FillLastLinearLayoutManager layoutManager;
    private final ListAdapter listAdapter;
    private int currentAnimationType;
    private final static int ANIMATION_NONE = 0;
    private final static int ANIMATION_FORWARD = 1;
    private final static int ANIMATION_BACKWARD = 2;
    private boolean scrolling;
    private boolean ignoreLayout =false;

    public ChatAttachAlertFlexatarLayout(ChatAttachAlert alert, Context context, int type, Theme.ResourcesProvider resourcesProvider) {
        super(alert, context, resourcesProvider);

        /*RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        File[] flexatarsInLocalStorage = FlexatarStorageManager.getFlexatarFileList(context);
        ListAdapter listAdapter = new ListAdapter(context, flexatarsInLocalStorage);
        recyclerView.setAdapter(listAdapter);
        recyclerView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        addView(recyclerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));*/
        listView = new RecyclerListView(context, resourcesProvider) {

            Paint paint = new Paint();
            @Override
            protected void dispatchDraw(Canvas canvas) {
                if (currentAnimationType == ANIMATION_FORWARD && getChildCount() > 0) {
                    float top = Integer.MAX_VALUE;
                    for (int i = 0; i < getChildCount(); i++) {
                        if (getChildAt(i).getY() < top) {
                            top = getChildAt(i).getY();
                        }
                    }
                    paint.setColor(Theme.getColor(Theme.key_dialogBackground));
                    //   canvas.drawRect(0, top, getMeasuredWidth(), getMeasuredHeight(), paint);
                }
                super.dispatchDraw(canvas);

            }
        };
        listView.setSectionsType(RecyclerListView.SECTIONS_TYPE_DATE);
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutManager(layoutManager = new FillLastLinearLayoutManager(context, LinearLayoutManager.VERTICAL, false, AndroidUtilities.dp(56), listView) {
            @Override
            public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
                LinearSmoothScroller linearSmoothScroller = new LinearSmoothScroller(recyclerView.getContext()) {
                    @Override
                    public int calculateDyToMakeVisible(View view, int snapPreference) {
                        int dy = super.calculateDyToMakeVisible(view, snapPreference);
                        dy -= (listView.getPaddingTop() - AndroidUtilities.dp(56));
                        return dy;
                    }

                    @Override
                    protected int calculateTimeForDeceleration(int dx) {
                        return super.calculateTimeForDeceleration(dx) * 2;
                    }
                };
                linearSmoothScroller.setTargetPosition(position);
                startSmoothScroll(linearSmoothScroller);
            }
        });
        File[] flexatarsInLocalStorage = FlexatarStorageManager.getFlexatarFileList(context,FlexatarStorageManager.FLEXATAR_PREFIX);

        listAdapter = new ListAdapter(context,flexatarsInLocalStorage);
        listView.setClipToPadding(false);
        listView.setAdapter(listAdapter);
        listView.setPadding(0, 0, 0, AndroidUtilities.dp(48));
        addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setOnItemClickListener((view, position) -> {
            if (flexatarsInLocalStorage.length>0) {
                Log.d("FLX_INJECT", "send flexatar file " + flexatarsInLocalStorage[position]);


                ArrayList<MessageObject> fmessages = new ArrayList<>();

                ArrayList<String> files = new ArrayList<>();
                files.add(FlexatarStorageManager.storePreviewImage(flexatarsInLocalStorage[position]).getAbsolutePath());
                files.add(flexatarsInLocalStorage[position].getAbsolutePath());

                Log.d("FLX_INJECT", "flexatar sent " + files.get(0) + " fmessages " + fmessages.size() + " scheduleDate " + 0 + " cap " + parentAlert.commentTextView.getText().toString());
                Log.d("FLX_INJECT", "flexatar sent " + files.get(1) + " fmessages " + fmessages.size() + " scheduleDate " + 0 + " cap " + parentAlert.commentTextView.getText().toString());

                delegate.didSelectFiles(files, "", fmessages, true, 0);
                parentAlert.dismiss(true);
            }
        });

        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                parentAlert.updateLayout(ChatAttachAlertFlexatarLayout.this, true, dy);
                /*updateEmptyViewPosition();

                if (listView.getAdapter() == searchAdapter) {
                    int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                    int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                    int visibleItemCount = Math.abs(lastVisibleItem - firstVisibleItem) + 1;
                    int totalItemCount = recyclerView.getAdapter().getItemCount();
                    if (visibleItemCount > 0 && lastVisibleItem >= totalItemCount - 10) {
                        searchAdapter.loadMore();
                    }
                }*/
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int offset = AndroidUtilities.dp(13);
                    int backgroundPaddingTop = parentAlert.getBackgroundPaddingTop();
                    int top = parentAlert.scrollOffsetY[0] - backgroundPaddingTop - offset;
                    if (top + backgroundPaddingTop < ActionBar.getCurrentActionBarHeight()) {
                        RecyclerListView.Holder holder = (RecyclerListView.Holder) listView.findViewHolderForAdapterPosition(0);
                        if (holder != null && holder.itemView.getTop() > AndroidUtilities.dp(56)) {
                            listView.smoothScrollBy(0, holder.itemView.getTop() - AndroidUtilities.dp(56));
                        }
                    }
                }
                /*if (newState == RecyclerView.SCROLL_STATE_DRAGGING && searching && listView.getAdapter() == searchAdapter) {
                    AndroidUtilities.hideKeyboard(parentAlert.getCurrentFocus());
                }*/
                scrolling = newState != RecyclerView.SCROLL_STATE_IDLE;
            }
        });
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private File[] flexatarFiles;


        private Context mContext;

        public ListAdapter(Context context, File[] flexatarFiles) {
            mContext = context;
            this.flexatarFiles=flexatarFiles;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public int getItemCount() {

//            return 3;
            return flexatarFiles.length == 0 ? 1 : flexatarFiles.length;
        }

        /*public ChatAttachAlertDocumentLayout.ListItem getItem(int position) {
            int itemsSize = items.size();
            if (position < itemsSize) {
                return items.get(position);
            } else if (history.isEmpty() && !recentItems.isEmpty() && position != itemsSize && position != itemsSize + 1) {
                position -= items.size() + 2;
                if (position < recentItems.size()) {
                    return recentItems.get(position);
                }
            }
            return null;
        }*/

        @Override
        public int getItemViewType(int position) {
            /*if (position == getItemCount() - 1) {
                return 3;
            } else {
                int itemsSize = items.size();
                if (position == itemsSize) {
                    return 2;
                } else if (position == itemsSize + 1) {
                    return 0;
                }
            }*/
            return 1;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (flexatarFiles.length>0) {
                return new RecyclerListView.Holder(new FlexatarCellNew(mContext));
            }else{
                return new RecyclerListView.Holder( new TextCell(mContext));
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (flexatarFiles.length>0) {
                FlexatarCellNew fCell = (FlexatarCellNew) holder.itemView;
                fCell.loadFromFile(flexatarFiles[position]);
            }else{
                ((TextCell) holder.itemView).setTextAndIcon(LocaleController.getString("NoFlexatarsToShare", R.string.NoFlexatarsToShare), R.drawable.filled_unclaimed, false);
            }
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();

        }
    }
    @Override
    int needsActionBar() {
        return 1;
    }

    @Override
    int getCurrentItemTop() {
        if (listView.getChildCount() <= 0) {
            return Integer.MAX_VALUE;
        }
        View child = listView.getChildAt(0);
        RecyclerListView.Holder holder = (RecyclerListView.Holder) listView.findContainingViewHolder(child);
        int top = (int) child.getY() - AndroidUtilities.dp(8);
        int newOffset = top > 0 && holder != null && holder.getAdapterPosition() == 0 ? top : 0;
        if (top >= 0 && holder != null && holder.getAdapterPosition() == 0) {
            newOffset = top;
        }
        return newOffset + AndroidUtilities.dp(13);
    }

    @Override
    public void setTranslationY(float translationY) {
        super.setTranslationY(translationY);
        parentAlert.getSheetContainer().invalidate();
    }

    @Override
    int getListTopPadding() {
        return listView.getPaddingTop();
    }

    @Override
    int getFirstOffset() {
        return getListTopPadding() + AndroidUtilities.dp(5);
    }

    @Override
    void onPreMeasure(int availableWidth, int availableHeight) {
        int padding;
        if (parentAlert.actionBar.isSearchFieldVisible() || parentAlert.sizeNotifierFrameLayout.measureKeyboardHeight() > AndroidUtilities.dp(20)) {
            padding = AndroidUtilities.dp(56);
            parentAlert.setAllowNestedScroll(false);
        } else {
            if (!AndroidUtilities.isTablet() && AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y) {
                padding = (int) (availableHeight / 3.5f);
            } else {
                padding = (availableHeight / 5 * 2);
            }
            padding -= AndroidUtilities.dp(1);
            if (padding < 0) {
                padding = 0;
            }
            parentAlert.setAllowNestedScroll(true);
        }
        if (listView.getPaddingTop() != padding) {
            ignoreLayout = true;
            listView.setPadding(0, padding, 0, AndroidUtilities.dp(48));
            ignoreLayout = false;
        }
//        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) filtersView.getLayoutParams();
//        layoutParams.topMargin = ActionBar.getCurrentActionBarHeight();
    }

    @Override
    int getButtonsHideOffset() {
        return AndroidUtilities.dp(62);
    }

    @Override
    public void requestLayout() {
        if (ignoreLayout) {
            return;
        }
        super.requestLayout();
    }

    @Override
    void scrollToTop() {
        listView.smoothScrollToPosition(0);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }

    }
    @Override
    void onShow(ChatAttachAlert.AttachAlertLayout previousLayout) {

        parentAlert.actionBar.setTitle(LocaleController.getString("SelectFile", R.string.SelectFile));

        layoutManager.scrollToPositionWithOffset(0, 0);
    }
//    @Override
//    int getSelectedItemsCount() {
//        return selectedFiles.size() + selectedMessages.size();
//    }

    /*@Override
    void sendSelectedItems(boolean notify, int scheduleDate) {
        if (selectedFiles.size() == 0 && selectedMessages.size() == 0 || delegate == null || sendPressed) {
            return;
        }
        sendPressed = true;
        ArrayList<MessageObject> fmessages = new ArrayList<>();
        Iterator<FilteredSearchView.MessageHashId> idIterator = selectedMessages.keySet().iterator();
        while (idIterator.hasNext()) {
            FilteredSearchView.MessageHashId hashId = idIterator.next();
            fmessages.add(selectedMessages.get(hashId));
        }
        ArrayList<String> files = new ArrayList<>(selectedFilesOrder);
        delegate.didSelectFiles(files, parentAlert.commentTextView.getText().toString(), fmessages, notify, scheduleDate);

        parentAlert.dismiss(true);
    }*/
    /*public static class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

        private final Context context;
        private final File[] flexatarFiles;

        public ListAdapter(Context context, File[] flexatarFiles){
            this.context=context;
            this.flexatarFiles=flexatarFiles;
        }
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ListAdapter.ViewHolder(new FlexatarCellNew(context));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FlexatarCellNew fCell = (FlexatarCellNew) holder.itemView;
            fCell.loadFromFile(flexatarFiles[position]);
        }

        @Override
        public int getItemCount() {
            return flexatarFiles.length;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }*/
}
