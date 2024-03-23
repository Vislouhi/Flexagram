package org.flexatar;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import android.widget.FrameLayout;
import android.widget.LinearLayout;

import android.widget.TextView;


import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;

import org.telegram.messenger.R;

import org.telegram.messenger.UserConfig;
import org.telegram.messenger.voip.VoIPService;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;

import org.telegram.ui.BasePermissionsActivity;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.NumberTextView;
import org.telegram.ui.Components.ViewPagerFixed;
import org.telegram.ui.LaunchActivity;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlexatarCabinetActivity extends BaseFragment  {

    public static Runnable makeFlexatarFailAction;
    public static boolean needRedrawFlexatarList = false;
    public static boolean needShowMakeFlexatarAlert = false;
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
    private TLRPC.User currentUser;
    private LinearLayout mainLayout;
    private ViewPagerFixed.TabsView tabsView;
    private static int currentTabId = -1;

    private static int getCurrentTabId(){
        if (currentTabId!=-1) return currentTabId;
        String storageName = "flexatar_type_selected_cabintet";
        Context context = ApplicationLoader.applicationContext;
        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        int currentTabId = sharedPreferences.getInt("current_tab", 0);
        return currentTabId;
    }
    private static void setCurrentTabId(int tabId){
        currentTabId = tabId;
        String storageName = "flexatar_type_selected_cabintet";
        Context context = ApplicationLoader.applicationContext;
        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("current_tab", tabId);
        editor.apply();

    }
    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();


        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        FlexatarMessageController.getInstance(UserConfig.selectedAccount).setOnAddToGalleryListener(null);
//        TicketsController.stop();
//        TicketsController.removeObserver();
//        FlexatarServerAccess.downloadBuiltinObserver = null;

    }
    @Override
    public boolean onBackPressed() {
        return super.onBackPressed();

    }
    public void createVideoPhotoTabController(){
        tabsView = new ViewPagerFixed.TabsView(getContext(), true, 3, LaunchActivity.getLastFragment().getResourceProvider()) {
            @Override
            public void selectTab(int currentPosition, int nextPosition, float progress) {
                super.selectTab(currentPosition, nextPosition, progress);

            }
        };
//        tabsView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        tabsView.tabMarginDp = 16;
        tabsView.addTab(0, LocaleController.getString("VideoTab",R.string.VideoTab));
        tabsView.addTab(1, LocaleController.getString("PhotoTab",R.string.PhotoTab));
        tabsView.selectTabWithId(getCurrentTabId(),1f);
        tabsView.setPadding(0,6,0,6);
        tabsView.setDelegate(new ViewPagerFixed.TabsView.TabsViewDelegate() {
            @Override
            public void onPageSelected(int page, boolean forward) {
                if (page == 0){
                    itemAdapter.changeActionCellName(1,LocaleController.getString("NewFlexatarByVideo", R.string.NewFlexatarByVideo));
                }else if (page == 1){
                    itemAdapter.changeActionCellName(1,LocaleController.getString("NewFlexatarByPhoto", R.string.NewFlexatarByPhoto));

                }
                itemAdapter.setUpFlexatarList(page);
                setCurrentTabId(page);

            }

            @Override
            public void onPageScrolled(float progress) {

            }

            @Override
            public void onSamePageSelected() {

            }

            @Override
            public boolean canPerformActions() {
                return true;
            }

            @Override
            public void invalidateBlur() {

            }
        });

        tabsView.finishAddingTabs();
        tabsView.setLayoutParams(LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,32,0,0,0,0));
        mainLayout.addView(tabsView);


    }
    @Override
    public View createView(Context context) {

        makeFlexatarFailAction = ()-> {
//            Log.d("FLX_INJECT","Show dialog impossible to perform");
            AndroidUtilities.runOnUIThread(() -> {
                AlertDialogs.sayImpossibleToPerform(getContext()).show();
//                showDialog(AlertDialogs.sayImpossibleToPerform(getContext()),true,null);
            });
        };
        mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        createVideoPhotoTabController();
        fragmentView = mainLayout;
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));


        frameLayout = new FrameLayout(context);
        mainLayout.addView(frameLayout,LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT));
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
                flexatarInstructionFragment.setOnTryInterfacePressed((v1) -> {
                    if (Build.VERSION.SDK_INT >= 23 && getParentActivity().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        getParentActivity().requestPermissions(new String[]{Manifest.permission.CAMERA},  BasePermissionsActivity.REQUEST_CODE_OPEN_CAMERA);
                        return;
                    }
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
            if (tabsView.getCurrentTabId() == 0){
                item.setNameText(LocaleController.getString("NewFlexatarByVideo", R.string.NewFlexatarByVideo));
            }else if (tabsView.getCurrentTabId() == 1){
                item.setNameText(LocaleController.getString("NewFlexatarByPhoto", R.string.NewFlexatarByPhoto));
            }
            item.setOnClickListener(v-> {
                if (Build.VERSION.SDK_INT >= 23 && getParentActivity().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    getParentActivity().requestPermissions(new String[]{Manifest.permission.CAMERA},  BasePermissionsActivity.REQUEST_CODE_OPEN_CAMERA);
                    return;
                }
                if (FlexatarServiceAuth.getVerification(UserConfig.selectedAccount)!=null && FlexatarServiceAuth.getVerification(UserConfig.selectedAccount).isVerified()) {
                    if (ValueStorage.checkIfInstructionsComplete(context)) {
                        if (tabsView.getCurrentTabId() == 0){
                            presentFragment(new FlexatarVideoCapFragment());
                        }else if (tabsView.getCurrentTabId() == 1){
                            presentFragment(new FlexatarCameraCaptureFragment());

                        }

                    } else {
                        showDialog(AlertDialogs.askToCompleteInstructions(context));
                    }
                }else{
                    showDialog(AlertDialogs.showVerifyInProgress(context));
                    FlexatarServiceAuth.startVerification(UserConfig.selectedAccount, () -> {

                    });
                }
            });

            itemsAction.add(item);
        }
        /*{
            ItemModel item = new ItemModel(ItemModel.ACTION_CELL);
            item.setImageResource(R.drawable.files_gallery);
            item.setNameText(LocaleController.getString("FlexatarWithImages", R.string.FlexatarWithImages));

            item.setOnClickListener(v-> {

                if (FlexatarServiceAuth.getVerification().isVerified()) {
                    presentFragment(new FlexatarByImagesFragment());

                }else{
                    showDialog(AlertDialogs.showVerifyInProgress(context));
                }
            });

            itemsAction.add(item);
        }*/
        {
            ItemModel item = new ItemModel(ItemModel.ACTION_CELL);
            item.setImageResource(R.drawable.msg_download);
            if (FlexatarServerAccess.isDownloadingFlexatars){
                item.setNameText(LocaleController.getString("LoadingFlexatarFromCloud", R.string.LoadingFlexatarFromCloud));
            }else{
                item.setNameText(LocaleController.getString("FlexatarFromCloud", R.string.FlexatarFromCloud));
            }

            item.setOnClickListener(v-> {

                if (FlexatarServiceAuth.getVerification(UserConfig.selectedAccount)!=null && FlexatarServiceAuth.getVerification(UserConfig.selectedAccount).isVerified()) {

                    if (!FlexatarServerAccess.isDownloadingFlexatars) {
                        FlexatarServerAccess.isDownloadingFlexatars = true;
                        ItemModel itemLoader = new ItemModel(ItemModel.FLEXATAR_CELL);
                        itemAdapter.addFlexatarItem(itemLoader);

                        handler.post(() -> {
                            item.setNameText(LocaleController.getString("LoadingFlexatarFromCloud", R.string.LoadingFlexatarFromCloud));
                            itemAdapter.notifyItemChanged(2);
                        });
                        FlexatarServerAccess.downloadCloudFlexatars1(UserConfig.selectedAccount,() -> {
                            FlexatarServerAccess.isDownloadingFlexatars = false;
                            itemAdapter.removeFlexatarCell(1);
                            handler.post(() -> {
                                item.setNameText(LocaleController.getString("FlexatarFromCloud", R.string.FlexatarFromCloud));
                                itemAdapter.notifyItemChanged(2);
                            });
                        },()->{
                            FlexatarServerAccess.isDownloadingFlexatars = false;
                            itemAdapter.removeFlexatarCell(1);
                            handler.post(() -> {
                                item.setNameText(LocaleController.getString("FlexatarFromCloud", R.string.FlexatarFromCloud));
                                itemAdapter.notifyItemChanged(2);
                            });
                            AndroidUtilities.runOnUIThread(() -> {
                                AlertDialogs.sayImpossibleToPerform(getContext()).show();
                            });
                        });
                    }

                }else{
                    showDialog(AlertDialogs.showVerifyInProgress(context));
                    FlexatarServiceAuth.startVerification(UserConfig.selectedAccount, () -> {

                    });
                }
            });

            itemsAction.add(item);
        }

//        ===============DEBUG CELLS============
        if (Config.debugMode) {

            {
                ItemModel item = new ItemModel(ItemModel.ACTION_CELL);
                item.setImageResource(R.drawable.msg_list);
                item.setNameText("Check private storage");
                item.setOnClickListener(v -> {
                    if (FlexatarServiceAuth.getVerification(UserConfig.selectedAccount).isVerified()) {
                        FlexatarServerAccess.requestJson(FlexatarServiceAuth.getVerification(UserConfig.selectedAccount), "list/1.00", "GET", new FlexatarServerAccess.OnRequestJsonReady() {
                                    @Override
                                    public void onReady(FlexatarServerAccess.StdResponse response) {
                                        Log.d("FLX_INJECT", response.ftars);
                                    }

                                    @Override
                                    public void onError() {

                                    }
                                });
                        /*FlexatarServerAccess.lambdaRequest("/list/1.00", "GET", null, null, new FlexatarServerAccess.CompletionListener() {
                            @Override
                            public void onReady(String response) {
                                String[] links = ServerDataProc.getFlexatarLinkList(response, "private");
//                            String[] ids = ServerDataProc.getFlexatarIdList(response, "public");
                                if (links.length == 0) {
                                    Log.d("FLX_INJECT", "private flexatars on server is empty list");
                                    return;
                                }
                                Log.d("FLX_INJECT", "private flexatar on server: " + Arrays.toString(links));


                            }
                        });*/

                    } else {
                        showDialog(AlertDialogs.showVerifyInProgress(context));
                    }
                });

                itemsAction.add(item);
            }

            /*{
                ItemModel item = new ItemModel(ItemModel.ACTION_CELL);
                item.setImageResource(R.drawable.msg_clear);
                item.setNameText("Clear cloud storage");
                item.setOnClickListener(v -> {
                    if (Config.isVerified()) {
                        FlexatarServerAccess.lambdaRequest("/list/1.00", "GET", null, null, new FlexatarServerAccess.CompletionListener() {
                            @Override
                            public void onReady(String response) {
                                String[] links = ServerDataProc.getFlexatarLinkList(response, "private");
//                            String[] ids = ServerDataProc.getFlexatarIdList(response, "public");

                                Log.d("FLX_INJECT", "private flexatar on server " + Arrays.toString(links));
                                for (String link : links)
                                    FlexatarServerAccess.lambdaRequest("/" + ServerDataProc.genDeleteRout(link), "DELETE", null, null, null);

                            *//*if (linksToDownload.size()>0){
                                FlexatarServerAccess.downloadFlexatarListRecursive(FlexatarStorageManager.PUBLIC_PREFIX,linksToDownload,idsToDownload,0);
                            }*//*
                            }
                        });

                    } else {
                        showDialog(AlertDialogs.showVerifyInProgress(context));
                    }
                });

                itemsAction.add(item);
            }*/
            {
                ItemModel item = new ItemModel(ItemModel.ACTION_CELL);
                item.setImageResource(R.drawable.msg_delete);
                item.setNameText("Delete local files");
                item.setOnClickListener(v -> {
                    if (FlexatarServiceAuth.getVerification(UserConfig.selectedAccount).isVerified()) {
                        File[] localFlexatars = FlexatarStorageManager.getFlexatarFileList(context,UserConfig.selectedAccount, FlexatarStorageManager.FLEXATAR_PREFIX);
                        for (File file : localFlexatars) {
                            FlexatarStorageManager.deleteFromStorage(context,UserConfig.selectedAccount, file, false);
                        }
                        File[] localFlexatars1 = FlexatarStorageManager.getVideoFlexatarFileList(context,UserConfig.selectedAccount);
                        for (File file : localFlexatars1) {
                            FlexatarStorageManager.deleteFromStorage(context,UserConfig.selectedAccount, file, false);
                        }
                        Log.d("FLX_INJECT", "local flexatars deleted");

                    } else {
                        showDialog(AlertDialogs.showVerifyInProgress(context));
                    }
                });

                itemsAction.add(item);
            }
            {
                ItemModel item = new ItemModel(ItemModel.ACTION_CELL);
                item.setImageResource(R.drawable.msg_delete);
                item.setNameText("Try send to bot");
                item.setOnClickListener(v -> {
//                    if (Config.isVerified()) {

                        Log.d("FLX_INJECT", "Try send to bot");
////                        FlexatarServiceAuth.clearVerificationData();
//                        if (!FlexatarServiceAuth.loadVerificationData()) {
                            Log.d("FLX_INJECT", "Request auth");
                            /*FlexatarServiceAuth.auth(new FlexatarServiceAuth.OnAuthListener() {
                                @Override
                                public void onReady() {
                                    Log.d("FLX_INJECT","auth ready");
                                }

                                @Override
                                public void onError() {
                                    Log.d("FLX_INJECT","auth error");
                                }
                            });*/


//                    } else {
//                        showDialog(AlertDialogs.showVerifyInProgress(context));
//                    }
                });

                itemsAction.add(item);
            }
            /*{
                ItemModel item = new ItemModel(ItemModel.ACTION_CELL);
                item.setImageResource(R.drawable.msg_delete);
                item.setNameText("Try to make video");
                item.setOnClickListener(v -> {
                    if (Config.isVerified()) {

//                        EncodeAndMuxTest videoWriter = new EncodeAndMuxTest();
//                        videoWriter.testEncodeVideoToMp4(animationPattern);

                    } else {
                        showDialog(AlertDialogs.showVerifyInProgress(context));
                    }
                });

                itemsAction.add(item);
            }*/
        }


//======================END DEBUG CELLS===
        {
            ItemModel item = new ItemModel(ItemModel.DELIMITER);
            item.setNameText(LocaleController.getString("FlexatarsAvailableForCalls", R.string.FlexatarsAvailableForCalls));
            itemsFlexatar.add(item);
        }

        flexatarInProgressDelimiter = new ItemModel(ItemModel.DELIMITER);
        flexatarInProgressDelimiter.setNameText(LocaleController.getString("FlexatarsInProgress", R.string.FlexatarsInProgress));
        itemsProgress.add(flexatarInProgressDelimiter);




        RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        itemAdapter = new ItemAdapter(context,UserConfig.selectedAccount, itemsAction,itemsProgress,itemsFlexatar);

        itemAdapter.setUpFlexatarList( tabsView.getCurrentTabId());

        itemAdapter.setFlexatarCellOnClickListener((item,cell)->{
//            ItemModel item = (ItemModel) v;
            if(getActionBar().isActionModeShowed() ) {
                Log.d("FLX_INJECT","flx cell pressed");
                if (!cell.isBuiltin()) {
                    String groupId = item.getFlexatarFile().getName().replace(".flx", "");
                    int groupSize = FlexatarStorageManager.getGroupSize(getContext(),UserConfig.selectedAccount, groupId);
                    if (groupSize == 0) {
                        item.setChecked(!item.isChecked());

                        checkedCount += cell.isChecked() ? -1 : 1;
                        handler.post(() -> {
                            setCheckedFlexatarsCount();
                        });
                    }else{
                        showDialog(AlertDialogs.sayImposableToDelete(getContext()));
                    }
                }
            }else{
                if (cell.getFlexatarFile()!=null)
                    presentFragment(new FlexatarPreviewFragment(cell,this));

            }
        });
        itemAdapter.setFlexatarCellOnLongClickListener((item,cell)->{
            boolean isCallActivity = VoIPService.getSharedInstance() != null;

            if (isCallActivity) {
                Log.d("FLX_INJECT","forbiden to delete");
                showDialog(AlertDialogs.showImpossibleToDelete(getContext()));
                return;
            }
            if(!getActionBar().isActionModeShowed() ) {
                showOrUpdateActionMode();
                getActionBar().showActionMode();

                String groupId = item.getFlexatarFile().getName().replace(".flx", "");
                int groupSize = FlexatarStorageManager.getGroupSize(getContext(),UserConfig.selectedAccount, groupId);
                if (groupSize == 0) {
                    item.setChecked(!item.isChecked());
                    checkedCount = 1;
                    if (cell.isBuiltin()) checkedCount = 0;

                }else{
                    showDialog(AlertDialogs.sayImposableToDelete(getContext()));
                    checkedCount = 0;
                }
                itemAdapter.addCheckBoxes();
                setCheckedFlexatarsCount();
            }

        });
        itemAdapter.setResourceProvider(getResourceProvider());

        recyclerView.setAdapter(itemAdapter);
        recyclerView.setLayoutParams(new LinearLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        recyclerView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        frameLayout.addView(recyclerView);



       /* FlexatarServerAccess.downloadBuiltinObserver = new FlexatarServerAccess.DownloadBuiltinObserver() {
            @Override
            public void start() {
//                ItemModel item = new ItemModel(ItemModel.FLEXATAR_CELL);
//                itemAdapter.addFlexatarItem(item);
            }

            @Override
            public void onError() {

//                itemAdapter.removeFlexatarCell(1);
            }

            @Override
            public void downloaded(File file,int flexatarType) {
//                itemAdapter.removeFlexatarCell(1);
                if (flexatarType == tabsView.getCurrentTabId()) {
                    ItemModel item = new ItemModel(ItemModel.FLEXATAR_CELL);
                    item.setFlexatarFile(file);
                    itemAdapter.addFlexatarItem(item, FlexatarServerAccess.isDownloadingFlexatars ? 2 : 1);
                }


            }
        };*/

        FlexatarMessageController.getInstance(UserConfig.selectedAccount).setOnAddToGalleryListener(new FlexatarMessageController.FlexatarAddToGalleryListener() {
            @Override
            public void onAddToGallery(File flexatarFile, int flexatarType) {
                if (flexatarType == tabsView.getCurrentTabId()) {
                    ItemModel item = new ItemModel(ItemModel.FLEXATAR_CELL);
                    item.setFlexatarFile(flexatarFile);
                    itemAdapter.addFlexatarItem(item,  1);
                }
            }
        });

        return fragmentView;
    }
    Map<String,ItemModel> progressCells = new HashMap<>();
    /*public void progressCellSetError(String fid,String errorCode){
        if (progressCells.containsKey(fid)){
            progressCells.get(fid).setError(errorCode);
        }
    }
    public void addProgressCell(String fid,TicketsController.Ticket ticket){
        if (progressCells.containsKey(fid)){
            progressCells.get(fid).setTime(ticket.timePassed());
            progressCells.get(fid).setStartTime(ticket.date);
        }else{
            ItemModel progressItem = new ItemModel(ItemModel.PROGRESS_CELL);
            progressItem.setTicket(null);
            progressItem.setTime(ticket.timePassed());
            progressItem.setStartTime(ticket.date);
            progressItem.setNameText(ticket.name);
            progressItem.setOnClickListener(v->{
//                FlexatarProgressCell flexatarProgressCell = (FlexatarProgressCell) v;
                showMakeFlexatarErrorAlert(fid,progressItem);
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
    }*/


    public static ItemModel flexatarItemFactory(File flexatarFile){
        ItemModel item = new ItemModel(ItemModel.FLEXATAR_CELL);
        item.setFlexatarFile(flexatarFile);
        return item;
    }
    /*public void showMakeFlexatarErrorAlert(String fid,ItemModel item){

//        Log.d("FLX_INJECT","item.getErrorCode() " +item.getErrorCode());
        if (item.getErrorCode() != null ){
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(LocaleController.getString("FollowingProblemTitle", R.string.FollowingProblemTitle));


            try {
                int code = new JSONObject(item.getErrorCode()).getInt("code");
                if (code == 1){
                    builder.setMessage(LocaleController.getString("YouDidntFollowInstruction", R.string.YouDidntFollowInstruction));
                }else if (code == 2){
                    builder.setMessage(LocaleController.getString("PhotosDoNotContainFace", R.string.PhotosDoNotContainFace));
                }else if (code == 3){
                    builder.setMessage(LocaleController.getString("Timeout", R.string.Timeout));
                }else{
                    builder.setMessage(LocaleController.getString("UnknownProblem", R.string.UnknownProblem));
                }

            } catch (JSONException ignored) {}

            builder.setPositiveButton("Delete", (dialogInterface, i) -> {

                TicketStorage.removeTicket(fid);
                itemAdapter.removeProgressItem(item);


            });
            AlertDialog alertDialog = builder.create();
            showDialog(alertDialog);
        }


    }*/
    private void showDeleteAlert(boolean all) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());


            builder.setTitle(LocaleController.getString("DeleteFlexatar",R.string.DeleteFlexatar));
            builder.setMessage(LocaleController.getString("DeleteFlexatarMsg",R.string.DeleteFlexatarMsg));

        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialogInterface, i) -> {

            List<File> filesToDelete = itemAdapter.removeCheckedFlexatars();

            for(File f:filesToDelete )
                FlexatarStorageManager.deleteFromStorage(getContext(),UserConfig.selectedAccount,f);
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

        if (needRedrawFlexatarList) {
            itemAdapter.setUpFlexatarList( tabsView.getCurrentTabId());
            FlexatarCabinetActivity.needRedrawFlexatarList = false;
        }
        if (needShowMakeFlexatarAlert){
            needShowMakeFlexatarAlert = false;
            AlertDialog dialog = AlertDialogs.sayFlexatarStartMaking(getContext(), () -> {

            });
            showDialog(dialog);

        }
        Log.d("FLX_INJECT","resume flexatar cabinet");
       /* TicketsController.attachObserver( new TicketsController.TicketObserver() {
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
        });*/
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

