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


import java.util.ArrayList;


public class FlexatarCabinetActivity extends BaseFragment  {

//    private FlexatarCabinetActivity.ListAdapter listViewAdapter;
//    private FlexatarCabinetActivity.EmptyTextProgressView emptyView;
    private LinearLayoutManager layoutManager;
//    private RecyclerListView listView;
    private ImageView floatingButton;
    private FlickerLoadingView flickerLoadingView;

    private NumberTextView selectedDialogsCountTextView;
    private ArrayList<View> actionModeViews = new ArrayList<>();

    private ActionBarMenuItem otherItem;

    private ArrayList<FlexatarCabinetActivity.CallLogRow> calls = new ArrayList<>();
    private boolean loading;
    private boolean firstLoaded;
    private boolean endReached;

    private ArrayList<Long> activeGroupCalls;

    private ArrayList<Integer> selectedIds = new ArrayList<>();

    private int prevPosition;
    private int prevTop;
    private boolean scrollUpdated;
    private boolean floatingHidden;
    private final AccelerateDecelerateInterpolator floatingInterpolator = new AccelerateDecelerateInterpolator();

    private Drawable greenDrawable;
    private Drawable greenDrawable2;
    private Drawable redDrawable;
    private ImageSpan iconOut, iconIn, iconMissed;
    private TLRPC.User lastCallUser;
    private TLRPC.Chat lastCallChat;

    private Long waitingForCallChatId;

    private boolean openTransitionStarted;

    private static final int TYPE_OUT = 0;
    private static final int TYPE_IN = 1;
    private static final int TYPE_MISSED = 2;

    private static final int delete_all_calls = 1;
    private static final int delete = 2;
    private FlexatarIconsVerticalScroll flexatarIconsView;
    private FlexatarPreview flexatarPreview;
    private ActionBarMenuItem keepMetaDataCahnges;


    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();


        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();

    }
    @Override
    public boolean onBackPressed() {
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        if (flexatarPreview != null && frameLayout.indexOfChild(flexatarPreview) != -1) {
            frameLayout.removeView(flexatarPreview);

            keepMetaDataCahnges.setVisibility(View.GONE);
            keepMetaDataCahnges.setEnabled(false);
            return false;
        }else {
            return super.onBackPressed();
        }

    }

    @Override
    public View createView(Context context) {


        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        actionBar.setBackButtonDrawable(new BackDrawable(false));
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("FlexatarMenuName", R.string.FlexatarMenuName));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    if (flexatarPreview != null && frameLayout.indexOfChild(flexatarPreview) != -1) {
                        frameLayout.removeView(flexatarPreview);

                        keepMetaDataCahnges.setVisibility(View.GONE);
                        keepMetaDataCahnges.setEnabled(false);
                    } else {
                        if (actionBar.isActionModeShowed()) {
                            hideActionMode(true);
                            flexatarIconsView.removeCheckBoxes();
                        } else {
                            finishFragment();
                        }
                    }
                } else if (id == delete_all_calls) {
                    showDeleteAlert(true);
                } else if (id == delete) {
                    showDeleteAlert(false);
                }else if (id == 10) {
                    if (flexatarPreview != null && frameLayout.indexOfChild(flexatarPreview) != -1) {
                        frameLayout.removeView(flexatarPreview);

                        keepMetaDataCahnges.setVisibility(View.GONE);
                        keepMetaDataCahnges.setEnabled(false);
                        FlexatarStorageManager.FlexatarMetaData metaData = flexatarPreview.getNewMetaData();
                        if (metaData != null) {
                            FlexatarStorageManager.FlexatarMetaData oldMetData = flexatarPreview.getFlexatarCell().getMetaData();
                            if (metaData.name != null) {
                                oldMetData.name = metaData.name;
                                flexatarPreview.getFlexatarCell().setName(metaData.name);
                            }
                            if (metaData.mouthCalibration != null) {
                                oldMetData.mouthCalibration = metaData.mouthCalibration;
                            }
                            if (metaData.amplitude != null) {
                                oldMetData.amplitude = metaData.amplitude;
                            }


                        }
                    }
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        keepMetaDataCahnges = menu.addItem(10, R.drawable.background_selected);
        keepMetaDataCahnges.setVisibility(View.GONE);
        keepMetaDataCahnges.setEnabled(false);
//        otherItem.setContentDescription(LocaleController.getString("AccDescrMoreOptions", R.string.AccDescrMoreOptions));
//        otherItem.addSubItem(delete_all_calls, R.drawable.msg_delete, LocaleController.getString("DeleteAllCalls", R.string.DeleteAllCalls));



        flickerLoadingView = new FlickerLoadingView(context);
        flickerLoadingView.setViewType(FlickerLoadingView.CALL_LOG_TYPE);
        flickerLoadingView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        flickerLoadingView.showDate(false);
//        emptyView = new FlexatarCabinetActivity.EmptyTextProgressView(context, flickerLoadingView);
//        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        flexatarIconsView = new FlexatarIconsVerticalScroll(context,this);
        flexatarIconsView.setLayoutParams(new LinearLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        flexatarIconsView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        flexatarIconsView.setOnNewFlexatarChosenListener((l)->{
            presentFragment(new FlexatarCameraCaptureFragment());
        });
        flexatarIconsView.setOnShowFlexatarListener((flexatarCell) -> {
            keepMetaDataCahnges.setVisibility(View.VISIBLE);
            keepMetaDataCahnges.setEnabled(true);

            flexatarPreview = new FlexatarPreview(context,flexatarCell,this);
            flexatarPreview.setClickable(false);

            frameLayout.addView(flexatarPreview,LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT));


        });
        flexatarIconsView.setOnViewInstructionsChosenListener((v)->{
            FlexatarInstructionFragment flexatarInstructionFragment = new FlexatarInstructionFragment();
            flexatarInstructionFragment.setOnTryInterfacePressed((v1)->{

                presentFragment(new FlexatarCameraCaptureFragment(true));
                flexatarInstructionFragment.finishPage();
            });
            presentFragment(flexatarInstructionFragment);
        });
        frameLayout.addView(flexatarIconsView);



        return fragmentView;
    }

    private void showPlusFlexatarAlert(boolean all) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Choose");
        builder.setMessage(LocaleController.getString("DeleteAllCallsText", R.string.DeleteAllCallsText));
//        if (all) {
//            builder.setTitle(LocaleController.getString("DeleteAllCalls", R.string.DeleteAllCalls));
//            builder.setMessage(LocaleController.getString("DeleteAllCallsText", R.string.DeleteAllCallsText));
//        } else {
//            builder.setTitle(LocaleController.getString("DeleteCalls", R.string.DeleteCalls));
//            builder.setMessage(LocaleController.getString("DeleteSelectedCallsText", R.string.DeleteSelectedCallsText));
//        }

        /*final boolean[] checks = new boolean[]{false};
        FrameLayout frameLayout = new FrameLayout(getParentActivity());
        CheckBoxCell cell = new CheckBoxCell(getParentActivity(), 1);
        cell.setBackgroundDrawable(Theme.getSelectorDrawable(false));
        cell.setText(LocaleController.getString("DeleteCallsForEveryone", R.string.DeleteCallsForEveryone), "", false, false);
        cell.setPadding(LocaleController.isRTL ? AndroidUtilities.dp(8) : 0, 0, LocaleController.isRTL ? 0 : AndroidUtilities.dp(8), 0);
        frameLayout.addView(cell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.TOP | Gravity.LEFT, 8, 0, 8, 0));
        cell.setOnClickListener(v -> {
            CheckBoxCell cell1 = (CheckBoxCell) v;
            checks[0] = !checks[0];
            cell1.setChecked(checks[0], true);
        });*/

//        builder.setView(frameLayout);
        builder.setPositiveButton("Cancel", (dialogInterface, i) -> {

        });
        /*builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        AlertDialog alertDialog = builder.create();
        showDialog(alertDialog);
        TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }*/
    }
    private void showDeleteAlert(boolean all) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());


            builder.setTitle("Delete flexatars.");
            builder.setMessage("Do you want to delete selected flexatars from local storage?");

        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialogInterface, i) -> {
            flexatarIconsView.deleteSelectedFlexatars();
            flexatarIconsView.removeCheckBoxes();
            hideActionMode(false);
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        AlertDialog alertDialog = builder.create();
        showDialog(alertDialog);
        TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }

    /*private void deleteAllMessages(boolean revoke) {
        TLRPC.TL_messages_deletePhoneCallHistory req = new TLRPC.TL_messages_deletePhoneCallHistory();
        req.revoke = revoke;
        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (response != null) {
                TLRPC.TL_messages_affectedFoundMessages res = (TLRPC.TL_messages_affectedFoundMessages) response;
                TLRPC.TL_updateDeleteMessages updateDeleteMessages = new TLRPC.TL_updateDeleteMessages();
                updateDeleteMessages.messages = res.messages;
                updateDeleteMessages.pts = res.pts;
                updateDeleteMessages.pts_count = res.pts_count;
                final TLRPC.TL_updates updates = new TLRPC.TL_updates();
                updates.updates.add(updateDeleteMessages);
                getMessagesController().processUpdates(updates, false);
                if (res.offset != 0) {
                    deleteAllMessages(revoke);
                }
            }
        });
    }*/

    private void hideActionMode(boolean animated) {
        actionBar.hideActionMode();
        selectedIds.clear();

    }

    private boolean isSelected(ArrayList<TLRPC.Message> messages) {
        for (int a = 0, N = messages.size(); a < N; a++) {
            if (selectedIds.contains(messages.get(a).id)) {
                return true;
            }
        }
        return false;
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
        selectedDialogsCountTextView.setNumber(flexatarIconsView.getCheckedCount(), updateAnimated);
    }
    public void setCheckedFlexatarsCount(){
        selectedDialogsCountTextView.setNumber(flexatarIconsView.getCheckedCount(), true);
    }

    private void hideFloatingButton(boolean hide) {
        if (floatingHidden == hide) {
            return;
        }
        floatingHidden = hide;
        ObjectAnimator animator = ObjectAnimator.ofFloat(floatingButton, "translationY", floatingHidden ? AndroidUtilities.dp(100) : 0).setDuration(300);
        animator.setInterpolator(floatingInterpolator);
        floatingButton.setClickable(!hide);
        animator.start();
    }


    @Override
    public void onResume() {
        super.onResume();
        flexatarIconsView.updateFlexatarList();

    }

    @Override
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
    }



    private static class CallLogRow {
        public TLRPC.User user;
        public ArrayList<TLRPC.Message> calls;
        public int type;
        public boolean video;
    }

    @Override
    public void onTransitionAnimationStart(boolean isOpen, boolean backward) {
        super.onTransitionAnimationStart(isOpen, backward);
        if (isOpen) {
            openTransitionStarted = true;
        }
    }

    @Override
    public boolean needDelayOpenAnimation() {
        return true;
    }


    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();



        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));

        themeDescriptions.add(new ThemeDescription(flexatarIconsView, ThemeDescription.FLAG_BACKGROUND, new Class[]{FlexatarIconsVerticalScroll.class}, new String[]{"flexatarIconsView"}, null, null, null, Theme.key_avatar_backgroundPink));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));


//        themeDescriptions.add(new ThemeDescription(emptyView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{FlexatarCabinetActivity.EmptyTextProgressView.class}, new String[]{"emptyTextView1"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
//        themeDescriptions.add(new ThemeDescription(emptyView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{FlexatarCabinetActivity.EmptyTextProgressView.class}, new String[]{"emptyTextView2"}, null, null, null, Theme.key_emptyListPlaceholder));


        themeDescriptions.add(new ThemeDescription(floatingButton, ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_chats_actionIcon));
        themeDescriptions.add(new ThemeDescription(floatingButton, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_chats_actionBackground));
        themeDescriptions.add(new ThemeDescription(floatingButton, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, null, Theme.key_chats_actionPressedBackground));

        themeDescriptions.add(new ThemeDescription(flickerLoadingView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));


        return themeDescriptions;
    }
}

