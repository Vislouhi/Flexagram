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
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.LoadingCell;
import org.telegram.ui.Cells.LocationCell;
import org.telegram.ui.Cells.ProfileSearchCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.CheckBox2;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.NumberTextView;
import org.telegram.ui.Components.ProgressButton;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.voip.VoIPHelper;
import org.telegram.ui.ContactsActivity;

import java.util.ArrayList;
import java.util.Iterator;

public class FlexatarCabinetActivity extends BaseFragment  {

//    private FlexatarCabinetActivity.ListAdapter listViewAdapter;
    private FlexatarCabinetActivity.EmptyTextProgressView emptyView;
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

    private static class EmptyTextProgressView extends FrameLayout {

        private TextView emptyTextView1;
        private TextView emptyTextView2;
        private View progressView;
        private RLottieImageView imageView;

        public EmptyTextProgressView(Context context) {
            this(context, null);
        }

        public EmptyTextProgressView(Context context, View progressView) {
            super(context);

            addView(progressView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            this.progressView = progressView;

            imageView = new RLottieImageView(context);
            imageView.setAnimation(R.raw.utyan_call, 120, 120);
            imageView.setAutoRepeat(false);
            addView(imageView, LayoutHelper.createFrame(140, 140, Gravity.CENTER, 52, 4, 52, 60));
            imageView.setOnClickListener(v -> {
                if (!imageView.isPlaying()) {
                    imageView.setProgress(0.0f);
                    imageView.playAnimation();
                }
            });

            emptyTextView1 = new TextView(context);
            emptyTextView1.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            emptyTextView1.setText(LocaleController.getString("NoRecentCalls", R.string.NoRecentCalls));
            emptyTextView1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            emptyTextView1.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            emptyTextView1.setGravity(Gravity.CENTER);
            addView(emptyTextView1, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 17, 40, 17, 0));

            emptyTextView2 = new TextView(context);
            String help = LocaleController.getString("NoRecentCallsInfo", R.string.NoRecentCallsInfo);
            if (AndroidUtilities.isTablet() && !AndroidUtilities.isSmallTablet()) {
                help = help.replace('\n', ' ');
            }
            emptyTextView2.setText(help);
            emptyTextView2.setTextColor(Theme.getColor(Theme.key_emptyListPlaceholder));
            emptyTextView2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            emptyTextView2.setGravity(Gravity.CENTER);
            emptyTextView2.setLineSpacing(AndroidUtilities.dp(2), 1);
            addView(emptyTextView2, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 17, 80, 17, 0));

            progressView.setAlpha(0f);
            imageView.setAlpha(0f);
            emptyTextView1.setAlpha(0f);
            emptyTextView2.setAlpha(0f);

            setOnTouchListener((v, event) -> true);
        }

        public void showProgress() {
            imageView.animate().alpha(0f).setDuration(150).start();
            emptyTextView1.animate().alpha(0f).setDuration(150).start();
            emptyTextView2.animate().alpha(0f).setDuration(150).start();
            progressView.animate().alpha(1f).setDuration(150).start();
        }

        public void showTextView() {
            imageView.animate().alpha(1f).setDuration(150).start();
            emptyTextView1.animate().alpha(1f).setDuration(150).start();
            emptyTextView2.animate().alpha(1f).setDuration(150).start();
            progressView.animate().alpha(0f).setDuration(150).start();
            imageView.playAnimation();
        }

        @Override
        public boolean hasOverlappingRendering() {
            return false;
        }
    }







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
    public View createView(Context context) {
//        greenDrawable = getParentActivity().getResources().getDrawable(R.drawable.ic_call_made_green_18dp).mutate();
//        greenDrawable.setBounds(0, 0, greenDrawable.getIntrinsicWidth(), greenDrawable.getIntrinsicHeight());
//        greenDrawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_calls_callReceivedGreenIcon), PorterDuff.Mode.MULTIPLY));
//        iconOut = new ImageSpan(greenDrawable, ImageSpan.ALIGN_BOTTOM);
//        greenDrawable2 = getParentActivity().getResources().getDrawable(R.drawable.ic_call_received_green_18dp).mutate();
//        greenDrawable2.setBounds(0, 0, greenDrawable2.getIntrinsicWidth(), greenDrawable2.getIntrinsicHeight());
//        greenDrawable2.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_calls_callReceivedGreenIcon), PorterDuff.Mode.MULTIPLY));
//        iconIn = new ImageSpan(greenDrawable2, ImageSpan.ALIGN_BOTTOM);
//        redDrawable = getParentActivity().getResources().getDrawable(R.drawable.ic_call_received_green_18dp).mutate();
//        redDrawable.setBounds(0, 0, redDrawable.getIntrinsicWidth(), redDrawable.getIntrinsicHeight());
//        redDrawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_fill_RedNormal), PorterDuff.Mode.MULTIPLY));
//        iconMissed = new ImageSpan(redDrawable, ImageSpan.ALIGN_BOTTOM);

        actionBar.setBackButtonDrawable(new BackDrawable(false));
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Flexatar");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    if (actionBar.isActionModeShowed()) {
                        hideActionMode(true);
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

        ActionBarMenu menu = actionBar.createMenu();
        otherItem = menu.addItem(10, R.drawable.ic_ab_other);
        otherItem.setContentDescription(LocaleController.getString("AccDescrMoreOptions", R.string.AccDescrMoreOptions));
        otherItem.addSubItem(delete_all_calls, R.drawable.msg_delete, LocaleController.getString("DeleteAllCalls", R.string.DeleteAllCalls));

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        flickerLoadingView = new FlickerLoadingView(context);
        flickerLoadingView.setViewType(FlickerLoadingView.CALL_LOG_TYPE);
        flickerLoadingView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        flickerLoadingView.showDate(false);
        emptyView = new FlexatarCabinetActivity.EmptyTextProgressView(context, flickerLoadingView);
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        flexatarIconsView = new FlexatarIconsVerticalScroll(context,this);
        flexatarIconsView.setLayoutParams(new LinearLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        flexatarIconsView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        flexatarIconsView.setOnNewFlexatarChosenListener((l)->{
            presentFragment(new FlexatarCameraCaptureFragment());
        });

        frameLayout.addView(flexatarIconsView);

        if (loading) {
            emptyView.showProgress();
        } else {
            emptyView.showTextView();
        }

        floatingButton = new ImageView(context);
        floatingButton.setVisibility(View.VISIBLE);
        floatingButton.setScaleType(ImageView.ScaleType.CENTER);

        Drawable drawable = Theme.createSimpleSelectorCircleDrawable(AndroidUtilities.dp(56), Theme.getColor(Theme.key_chats_actionBackground), Theme.getColor(Theme.key_chats_actionPressedBackground));
        if (Build.VERSION.SDK_INT < 21) {
            Drawable shadowDrawable = context.getResources().getDrawable(R.drawable.floating_shadow).mutate();
            shadowDrawable.setColorFilter(new PorterDuffColorFilter(0xff000000, PorterDuff.Mode.MULTIPLY));
            CombinedDrawable combinedDrawable = new CombinedDrawable(shadowDrawable, drawable, 0, 0);
            combinedDrawable.setIconSize(AndroidUtilities.dp(56), AndroidUtilities.dp(56));
            drawable = combinedDrawable;
        }
        floatingButton.setBackgroundDrawable(drawable);
        floatingButton.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chats_actionIcon), PorterDuff.Mode.MULTIPLY));
        floatingButton.setImageResource(R.drawable.msg_filled_plus);
        floatingButton.setContentDescription(LocaleController.getString("Call", R.string.Call));
        if (Build.VERSION.SDK_INT >= 21) {
            StateListAnimator animator = new StateListAnimator();
            animator.addState(new int[]{android.R.attr.state_pressed}, ObjectAnimator.ofFloat(floatingButton, "translationZ", AndroidUtilities.dp(2), AndroidUtilities.dp(4)).setDuration(200));
            animator.addState(new int[]{}, ObjectAnimator.ofFloat(floatingButton, "translationZ", AndroidUtilities.dp(4), AndroidUtilities.dp(2)).setDuration(200));
            floatingButton.setStateListAnimator(animator);
            floatingButton.setOutlineProvider(new ViewOutlineProvider() {
                @SuppressLint("NewApi")
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, AndroidUtilities.dp(56), AndroidUtilities.dp(56));
                }
            });
        }
//        frameLayout.addView(floatingButton, LayoutHelper.createFrame(Build.VERSION.SDK_INT >= 21 ? 56 : 60, Build.VERSION.SDK_INT >= 21 ? 56 : 60, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.BOTTOM, LocaleController.isRTL ? 14 : 0, 0, LocaleController.isRTL ? 0 : 14, 14));
        floatingButton.setOnClickListener(v -> {
//            presentFragment(contactsFragment);
            /*Bundle args = new Bundle();
            args.putBoolean("destroyAfterSelect", true);
            args.putBoolean("returnAsResult", true);
            args.putBoolean("onlyUsers", true);
            args.putBoolean("allowSelf", false);
            ContactsActivity contactsFragment = new ContactsActivity(args);
            contactsFragment.setDelegate((user, param, activity) -> {
                TLRPC.UserFull userFull = getMessagesController().getUserFull(user.id);
                VoIPHelper.startCall(lastCallUser = user, false, userFull != null && userFull.video_calls_available, getParentActivity(), null, getAccountInstance());
            });
            presentFragment(contactsFragment);*/
        });
        return fragmentView;
    }
//    @Override
//    public void onResume() {
//
//        super.onResume();
//    }
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

        if (all) {
            builder.setTitle(LocaleController.getString("DeleteAllCalls", R.string.DeleteAllCalls));
            builder.setMessage(LocaleController.getString("DeleteAllCallsText", R.string.DeleteAllCallsText));
        } else {
            builder.setTitle(LocaleController.getString("DeleteCalls", R.string.DeleteCalls));
            builder.setMessage(LocaleController.getString("DeleteSelectedCallsText", R.string.DeleteSelectedCallsText));
        }
        final boolean[] checks = new boolean[]{false};
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
        });
        builder.setView(frameLayout);
        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialogInterface, i) -> {
            if (all) {
//                deleteAllMessages(checks[0]);
                calls.clear();
                loading = false;
                endReached = true;
                otherItem.setVisibility(View.GONE);

            } else {
                getMessagesController().deleteMessages(new ArrayList<>(selectedIds), null, null, 0, checks[0], false);
            }
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

    private void createActionMode() {
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



    private void showOrUpdateActionMode() {
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
        selectedDialogsCountTextView.setNumber(selectedIds.size(), updateAnimated);
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


        themeDescriptions.add(new ThemeDescription(emptyView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{FlexatarCabinetActivity.EmptyTextProgressView.class}, new String[]{"emptyTextView1"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(emptyView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{FlexatarCabinetActivity.EmptyTextProgressView.class}, new String[]{"emptyTextView2"}, null, null, null, Theme.key_emptyListPlaceholder));


        themeDescriptions.add(new ThemeDescription(floatingButton, ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_chats_actionIcon));
        themeDescriptions.add(new ThemeDescription(floatingButton, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_chats_actionBackground));
        themeDescriptions.add(new ThemeDescription(floatingButton, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, null, Theme.key_chats_actionPressedBackground));

        themeDescriptions.add(new ThemeDescription(flickerLoadingView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));


        return themeDescriptions;
    }
}

