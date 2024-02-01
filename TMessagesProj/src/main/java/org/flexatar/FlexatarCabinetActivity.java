package org.flexatar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;

import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import android.widget.TextView;


import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;

import org.telegram.messenger.LocaleController;

import org.telegram.messenger.R;

import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;

import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.NumberTextView;

import org.telegram.ui.Components.RLottieImageView;

import org.telegram.ui.Components.voip.VoIPHelper;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Map;

public class FlexatarCabinetActivity extends BaseFragment  {


    private NumberTextView selectedDialogsCountTextView;
    private ArrayList<View> actionModeViews = new ArrayList<>();

    private ArrayList<Integer> selectedIds = new ArrayList<>();


    private static final int delete_all_calls = 1;
    private static final int delete = 2;
    private ItemModel flexatarInProgressDelimiter;
    private List<ItemModel> itemsAction;
    private List<ItemModel> itemsProgress;
    private List<ItemModel> itemsFlexatar;

    Handler handler = new Handler(Looper.getMainLooper());
    private ItemAdapter itemAdapter;
    private FrameLayout frameLayout;
    private int checkedCount = 0;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();


        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        TicketsController.stop();

    }
    @Override
    public boolean onBackPressed() {
        return super.onBackPressed();

    }

    @Override
    public View createView(Context context) {


        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        frameLayout = (FrameLayout) fragmentView;

        actionBar.setBackButtonDrawable(new BackDrawable(false));
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("FlexatarMenuName", R.string.FlexatarMenuName));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {

                    if (actionBar.isActionModeShowed()) {
                        hideActionMode(true);
                        itemAdapter.removeCheckBoxes();
//                            handler.post(() -> {
//                                itemAdapter.notifyDataSetChanged();
//                            });
                    } else {
                        finishFragment();
                    }

                } else if (id == delete_all_calls) {
                    showDeleteAlert(true);
                } else if (id == delete) {
                    showDeleteAlert(false);
                }
            }
        });

//        ActionBarMenu menu = actionBar.createMenu();

//        flickerLoadingView = new FlickerLoadingView(context);
//        flickerLoadingView.setViewType(FlickerLoadingView.CALL_LOG_TYPE);
//        flickerLoadingView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
//        flickerLoadingView.showDate(false);
//        emptyView = new FlexatarCabinetActivity.EmptyTextProgressView(context, flickerLoadingView);
//        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        itemsAction = new ArrayList<>();
        itemsProgress = new ArrayList<>();
        itemsFlexatar = new ArrayList<>();
        {
            ItemModel item = new ItemModel(ItemModel.ACTION_CELL);
            item.setImageResource(R.drawable.msg_help);
            item.setNameText(LocaleController.getString("ViewInstructions", R.string.ViewInstructions));
            item.setOnClickListener(v->{
                FlexatarInstructionFragment flexatarInstructionFragment = new FlexatarInstructionFragment();
                flexatarInstructionFragment.setOnTryInterfacePressed((v1)->{

                    presentFragment(new FlexatarCameraCaptureFragment(true));
                    flexatarInstructionFragment.finishPage();
                });
                presentFragment(flexatarInstructionFragment);
            });
            itemsAction.add(item);
        }
        {
            ItemModel item = new ItemModel(ItemModel.ACTION_CELL);
            item.setImageResource(R.drawable.msg_addphoto);
            item.setNameText(LocaleController.getString("NewFlexatarByPhoto", R.string.NewFlexatarByPhoto));
            item.setOnClickListener(v-> {
                if (ValueStorage.checkIfInstructionsComplete(context)) {
                    presentFragment(new FlexatarCameraCaptureFragment());
                }else{
                    showDialog(AlertDialogs.askToCompleteInstructions(context));
                }
            });

            itemsAction.add(item);
        }
        {
            ItemModel item = new ItemModel(ItemModel.DELIMITER);
            item.setNameText(LocaleController.getString("FlexatarsAvailableForCalls", R.string.FlexatarsAvailableForCalls));
            itemsFlexatar.add(item);
        }

        flexatarInProgressDelimiter = new ItemModel(ItemModel.DELIMITER);
        flexatarInProgressDelimiter.setNameText(LocaleController.getString("FlexatarsInProgress", R.string.FlexatarsInProgress));
        itemsProgress.add(flexatarInProgressDelimiter);

        File[] flexatarsInLocalStorage = FlexatarStorageManager.getFlexatarFileList(context);
        for (int i = 0; i < flexatarsInLocalStorage.length; i++) {
            ItemModel item = flexatarItemFactory(flexatarsInLocalStorage[i]);

            itemsFlexatar.add(item);
        }


        RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        itemAdapter = new ItemAdapter(context, itemsAction,itemsProgress,itemsFlexatar);

        itemAdapter.setFlexatarCellOnClickListener((item,cell)->{
//            ItemModel item = (ItemModel) v;
            if(getActionBar().isActionModeShowed() ) {
                Log.d("FLX_INJECT","flx cell pressed");
                if (!cell.isBuiltin()) {
                    item.setChecked(!item.isChecked());

                    checkedCount += cell.isChecked() ? -1 : 1;
                    handler.post(() -> {
                        setCheckedFlexatarsCount();
                    });
                }
            }else{
                presentFragment(new FlexatarPreviewFragment(cell,this));

            }
        });
        itemAdapter.setFlexatarCellOnLongClickListener((item,cell)->{
            if(!getActionBar().isActionModeShowed() ) {
                showOrUpdateActionMode();
                getActionBar().showActionMode();
                item.setChecked(!item.isChecked());
                checkedCount = 1;
                if (cell.isBuiltin()) checkedCount = 0;
                itemAdapter.addCheckBoxes();
                setCheckedFlexatarsCount();
            }

        });

        recyclerView.setAdapter(itemAdapter);
        recyclerView.setLayoutParams(new LinearLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        recyclerView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        frameLayout.addView(recyclerView);



        /*FlexatarServerAccess.downloadBuiltinObserver = file -> {
            ItemModel item = new ItemModel(ItemModel.FLEXATAR_CELL);
            item.setFlexatarFile(file);
            itemsFlexatar.add(item);
            handler.post(()->{
                itemAdapter.notifyDataSetChanged();
            });

        };*/




        return fragmentView;
    }
    Map<String,ItemModel> progressCells = new HashMap<>();
    public void progressCellSetError(String fid,String errorCode){
        if (progressCells.containsKey(fid)){
            progressCells.get(fid).setError(errorCode);
        }
    }
    public void addProgressCell(String fid,TicketsController.Ticket ticket){
        if (progressCells.containsKey(fid)){
            progressCells.get(fid).setTime(ticket.timePassed());
        }else{
            ItemModel progressItem = new ItemModel(ItemModel.PROGRESS_CELL);
            progressItem.setTicket(null);
            progressItem.setTime(ticket.timePassed());
            progressItem.setNameText(ticket.name);
            progressItem.setOnClickListener(v->{
                FlexatarProgressCell flexatarProgressCell = (FlexatarProgressCell) v;
                showMakeFlexatarErrorAlert(flexatarProgressCell,fid,progressItem);
            });
            if (ticket.status.equals("error")){
                progressItem.setError(ticket.errorCode);
            }
            if (ticket.status.equals("in_process")){
                TicketsController.flexatarTaskStart(fid,ticket);
            }
            itemAdapter.addProgressItem( progressItem);
            progressCells.put(fid,progressItem);

        }
    }
    public void removeProgressCell(String fid){
        if (progressCells.containsKey(fid)){
            itemAdapter.removeProgressItem(progressCells.remove(fid));
//            itemsProgress.remove(progressCells.remove(fid));

        }
    }


    private ItemModel flexatarItemFactory(File flexatarFile){
        ItemModel item = new ItemModel(ItemModel.FLEXATAR_CELL);
        item.setFlexatarFile(flexatarFile);
        return item;
    }
    public void showMakeFlexatarErrorAlert(FlexatarProgressCell cell,String fid,ItemModel item){

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());

            builder.setTitle("There could be following problems:");
//            builder.setMessage("1. There was no human faces on the photos.\n" +
//                    "2. Head was turned the wrong side. \n" +
//                    "3. Flexatar server overloaded.");

            builder.setMessage(cell.getErrorCode());
            builder.setPositiveButton("Delete", (dialogInterface, i) -> {

                TicketStorage.removeTicket(fid);
                itemAdapter.removeProgressItem(item);


            });
        AlertDialog alertDialog = builder.create();
        showDialog(alertDialog);
    }
    private void showDeleteAlert(boolean all) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());


            builder.setTitle("Delete flexatars.");
            builder.setMessage("Do you want to delete selected flexatars from local storage?");

        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialogInterface, i) -> {

            List<File> filesToDelete = itemAdapter.removeCheckedFlexatars();

            for(File f:filesToDelete )
                FlexatarStorageManager.deleteFromStorage(getContext(),f);
            hideActionMode(false);
//            itemAdapter.removeCheckBoxes();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        AlertDialog alertDialog = builder.create();
        showDialog(alertDialog);
        TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }


    private void hideActionMode(boolean animated) {
        actionBar.hideActionMode();
        selectedIds.clear();

    }



    public void createActionMode() {
        if (actionBar.actionModeIsExist(null)) {
            return;
        }
        final ActionBarMenu actionMode = actionBar.createActionMode();

        selectedDialogsCountTextView = new NumberTextView(actionMode.getContext());
        selectedDialogsCountTextView.setTextSize(18);
        selectedDialogsCountTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        selectedDialogsCountTextView.setTextColor(Theme.getColor(Theme.key_actionBarActionModeDefaultIcon));

        actionMode.addView(selectedDialogsCountTextView, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, 72, 0, 0, 0));
        selectedDialogsCountTextView.setOnTouchListener((v, event) -> true);

        actionModeViews.add(actionMode.addItemWithWidth(delete, R.drawable.msg_delete, AndroidUtilities.dp(54), LocaleController.getString("Delete", R.string.Delete)));
    }



    public void showOrUpdateActionMode() {
        boolean updateAnimated = false;
        if (actionBar.isActionModeShowed()) {
            if (selectedIds.isEmpty()) {
                hideActionMode(true);
                return;
            }
            updateAnimated = true;
        } else {
            createActionMode();
            actionBar.showActionMode();

            AnimatorSet animatorSet = new AnimatorSet();
            ArrayList<Animator> animators = new ArrayList<>();
            for (int a = 0; a < actionModeViews.size(); a++) {
                View view = actionModeViews.get(a);
                view.setPivotY(ActionBar.getCurrentActionBarHeight() / 2);
                AndroidUtilities.clearDrawableAnimation(view);
                animators.add(ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.1f, 1.0f));
            }
            animatorSet.playTogether(animators);
            animatorSet.setDuration(200);
            animatorSet.start();
        }

        selectedDialogsCountTextView.setNumber(checkedCount, updateAnimated);
    }
    public void setCheckedFlexatarsCount(){
        selectedDialogsCountTextView.setNumber(checkedCount, true);
    }



    @Override
    public void onResume() {
        super.onResume();
        TicketsController.attachObserver( new TicketsController.TicketObserver() {
            @Override
            public void onReady(String lfid, File file) {
                removeProgressCell(lfid);
                itemAdapter.addFlexatarItem(flexatarItemFactory(file));
            }

            @Override
            public void onError(String lfid, TicketsController.Ticket ticket) {
                progressCellSetError(lfid,ticket.errorCode);
            }

            @Override
            public void onTimer(String lfid, TicketsController.Ticket ticket) {
                addProgressCell(lfid,ticket);
            }

            @Override
            public void onStart(String lfid, TicketsController.Ticket ticket) {
                addProgressCell(lfid,ticket);
            }
        });
    }

    /*@Override
    public void onRequestPermissionsResultFragment(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 101 || requestCode == 102 || requestCode == 103) {
            boolean allGranted = true;
            for (int a = 0; a < grantResults.length; a++) {
                if (grantResults[a] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (grantResults.length > 0 && allGranted) {
                if (requestCode == 103) {
                    VoIPHelper.startCall(lastCallChat, null, null, false, getParentActivity(), FlexatarCabinetActivity.this, getAccountInstance());
                } else {
                    TLRPC.UserFull userFull = lastCallUser != null ? getMessagesController().getUserFull(lastCallUser.id) : null;
                    VoIPHelper.startCall(lastCallUser, requestCode == 102, requestCode == 102 || userFull != null && userFull.video_calls_available, getParentActivity(), null, getAccountInstance());
                }
            } else {
                VoIPHelper.permissionDenied(getParentActivity(), null, requestCode);
            }
        }
    }*/





    /*@Override
    public void onTransitionAnimationStart(boolean isOpen, boolean backward) {
        super.onTransitionAnimationStart(isOpen, backward);
        if (isOpen) {
            openTransitionStarted = true;
        }
    }*/

    @Override
    public boolean needDelayOpenAnimation() {
        return true;
    }


    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();



        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));

//        themeDescriptions.add(new ThemeDescription(flexatarIconsView, ThemeDescription.FLAG_BACKGROUND, new Class[]{FlexatarIconsVerticalScroll.class}, new String[]{"flexatarIconsView"}, null, null, null, Theme.key_avatar_backgroundPink));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));


//        themeDescriptions.add(new ThemeDescription(emptyView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{FlexatarCabinetActivity.EmptyTextProgressView.class}, new String[]{"emptyTextView1"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
//        themeDescriptions.add(new ThemeDescription(emptyView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{FlexatarCabinetActivity.EmptyTextProgressView.class}, new String[]{"emptyTextView2"}, null, null, null, Theme.key_emptyListPlaceholder));


//        themeDescriptions.add(new ThemeDescription(floatingButton, ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_chats_actionIcon));
//        themeDescriptions.add(new ThemeDescription(floatingButton, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_chats_actionBackground));
//        themeDescriptions.add(new ThemeDescription(floatingButton, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, null, Theme.key_chats_actionPressedBackground));
//
//        themeDescriptions.add(new ThemeDescription(flickerLoadingView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));


        return themeDescriptions;
    }
}

