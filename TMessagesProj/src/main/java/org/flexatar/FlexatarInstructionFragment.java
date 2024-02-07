package org.flexatar;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Components.BitmapShaderTools;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.MotionBackgroundDrawable;

import org.telegram.ui.Components.voip.PrivateVideoPreviewDialogNew;

public class FlexatarInstructionFragment extends BaseFragment{
    private final MotionBackgroundDrawable bgGreen = new MotionBackgroundDrawable(0xFF5FD051, 0xFF00B48E, 0xFFA9CC66, 0xFF5AB147, 0, false, true);

    private final BitmapShaderTools bgGreenShaderTools = new BitmapShaderTools(80, 80);
    private final BitmapShaderTools bgBlueVioletShaderTools = new BitmapShaderTools(80, 80);
    private final MotionBackgroundDrawable bgBlueViolet = new MotionBackgroundDrawable(0xFF00A3E6, 0xFF296EF7, 0xFF18CEE2, 0xFF3FB2FF, 0, false, true);
    private TextView positiveButton;
    private View.OnClickListener onViewInstructionsChosenListener = null;

    public FlexatarInstructionFragment(){
        super();
    }



    public void finishPage(){

        finishFragment();
    }



    @Override
    public View createView(Context context) {


        actionBar.setBackButtonDrawable(new BackDrawable(false));
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("Instructions", R.string.Instructions));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishPage();
                }
//                if (id == 10){
//                    showStartUpAlert(true);
//                }
            }
        });
//        ActionBarMenu menu = actionBar.createMenu();
//        ActionBarMenuItem otherItem = menu.addItem(10, R.drawable.msg_help);
//        otherItem.setContentDescription("Help");

        fragmentView = new ScrollView(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));


        ScrollView scrollView = (ScrollView) fragmentView;

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(linearLayout);

        TextView textView1 = FlxLayoutHelper.textViewFactory(context,LocaleController.getString("InstructionText1", R.string.InstructionText1));

        textView1.setGravity(Gravity.CENTER);
        textView1.setPadding(AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(12));

        linearLayout.addView(textView1, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        int[] rotationExampleRes = {R.drawable.face_example_front,R.drawable.face_example_left,R.drawable.face_example_right,R.drawable.face_example_up,R.drawable.face_example_down};
        String[] comments = {
                LocaleController.getString("Front", R.string.Front),
                LocaleController.getString("Left", R.string.Left),
                LocaleController.getString("Right", R.string.Right),
                LocaleController.getString("Up", R.string.Up),
                LocaleController.getString("Down", R.string.Down)
        };
        for (int i = 0; i < rotationExampleRes.length; i++) {
            ImageView imageView = new ImageView(context);
            imageView.setImageResource(rotationExampleRes[i]);

            imageView.setPadding(AndroidUtilities.dp(0),AndroidUtilities.dp(6),AndroidUtilities.dp(48),AndroidUtilities.dp(0));

            TextView textComment = FlxLayoutHelper.textViewFactory(context,comments[i]);
            textComment.setGravity(Gravity.CENTER);
            textComment.setPadding(AndroidUtilities.dp(0),AndroidUtilities.dp(48),AndroidUtilities.dp(0),AndroidUtilities.dp(0));

            linearLayout.addView(FlxLayoutHelper.horizontalLayoutFactory(context,new View[]{textComment,imageView}),LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,AndroidUtilities.dp(72),Gravity.CENTER_HORIZONTAL|Gravity.TOP));
        }

        TextView textView2 = FlxLayoutHelper.textViewFactory(context,LocaleController.getString("InstructionText2", R.string.InstructionText2));

        textView2.setGravity(Gravity.CENTER);
        textView2.setPadding(AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(12));

        linearLayout.addView(textView2, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT, Gravity.TOP));


        int[] failExampleRes = {R.drawable.face_example_rotation_ok,R.drawable.face_example_rotation_fail1,R.drawable.face_example_rotation_fail2};
        String[] failComments = {
                LocaleController.getString("OK", R.string.OK),
                LocaleController.getString("Fail", R.string.Fail),
                LocaleController.getString("Fail", R.string.Fail)
        };
        for (int i = 0; i < failExampleRes.length; i++) {
            ImageView imageView = new ImageView(context);
            imageView.setImageResource(failExampleRes[i]);

            imageView.setPadding(AndroidUtilities.dp(0),AndroidUtilities.dp(6),AndroidUtilities.dp(48),AndroidUtilities.dp(0));

            TextView textComment = FlxLayoutHelper.textViewFactory(context,failComments[i]);
            textComment.setGravity(Gravity.CENTER);
            textComment.setPadding(AndroidUtilities.dp(0),AndroidUtilities.dp(48),AndroidUtilities.dp(0),AndroidUtilities.dp(0));

            linearLayout.addView(FlxLayoutHelper.horizontalLayoutFactory(context,new View[]{textComment,imageView}),LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,AndroidUtilities.dp(72),Gravity.CENTER_HORIZONTAL|Gravity.TOP));
        }

        TextView textView3 = FlxLayoutHelper.textViewFactory(context,LocaleController.getString("InstructionText3", R.string.InstructionText3));

        textView3.setGravity(Gravity.CENTER);
        textView3.setPadding(AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(12));

        linearLayout.addView(textView3, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT, Gravity.TOP));


        int[] mouthExampleRes = {R.drawable.mouth_example_front,R.drawable.mouth_example_left,R.drawable.mouth_example_right,R.drawable.mouth_example_up,R.drawable.mouth_example_down};
        String[] commentsMouth = {
                LocaleController.getString("Front", R.string.Front),
                LocaleController.getString("Left", R.string.Left),
                LocaleController.getString("Right", R.string.Right),
                LocaleController.getString("Up", R.string.Up),
                LocaleController.getString("Down", R.string.Down)
        };
        for (int i = 0; i < mouthExampleRes.length; i++) {
            ImageView imageView = new ImageView(context);
            imageView.setImageResource(mouthExampleRes[i]);

            imageView.setPadding(AndroidUtilities.dp(0),AndroidUtilities.dp(6),AndroidUtilities.dp(48),AndroidUtilities.dp(0));

            TextView textComment = FlxLayoutHelper.textViewFactory(context,commentsMouth[i]);
            textComment.setGravity(Gravity.CENTER);
            textComment.setPadding(AndroidUtilities.dp(0),AndroidUtilities.dp(48),AndroidUtilities.dp(0),AndroidUtilities.dp(0));

            linearLayout.addView(FlxLayoutHelper.horizontalLayoutFactory(context,new View[]{textComment,imageView}),LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,AndroidUtilities.dp(72),Gravity.CENTER_HORIZONTAL|Gravity.TOP));


        }
//        TextCell tryInterfaceCell = new TextCell(context);
//        tryInterfaceCell.setTextAndIcon(LocaleController.getString("TryCaptureInterface", R.string.TryCaptureInterface), R.drawable.msg_addphoto, true);
//        linearLayout.addView(tryInterfaceCell);
        positiveButton = new TextView(getContext()) {
            private final Paint whitePaint = new Paint();
            private final Paint[] gradientPaint = new Paint[1];

            {
                bgGreen.setBounds(0, 0, 80, 80);
                bgBlueViolet.setBounds(0, 0, 80, 80);
                bgGreenShaderTools.setBounds(0, 0, 80, 80);
                bgBlueVioletShaderTools.setBounds(0, 0, 80, 80);
                bgGreen.setAlpha(255);
                bgBlueViolet.setAlpha(255);
                bgGreenShaderTools.getCanvas().drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                bgBlueVioletShaderTools.getCanvas().drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                bgGreen.draw(bgGreenShaderTools.getCanvas());
                bgBlueViolet.draw(bgBlueVioletShaderTools.getCanvas());
                whitePaint.setColor(Color.WHITE);
            }

            @Override
            protected void onSizeChanged(int w, int h, int oldw, int oldh) {
                super.onSizeChanged(w, h, oldw, oldh);
                for (int a = 0; a < gradientPaint.length; a++) {
                    if (a == 0) {
                        gradientPaint[a] = bgGreenShaderTools.paint;
                    } else if (a == 1) {
                        gradientPaint[a] = bgBlueVioletShaderTools.paint;
                    } else {
                        gradientPaint[a] = bgGreenShaderTools.paint;
                    }
                }
            }

            @Override
            protected void onDraw(Canvas canvas) {
                bgGreenShaderTools.setBounds(-getX(), -getY(), FlexatarInstructionFragment.this.fragmentView.getWidth() - getX(), FlexatarInstructionFragment.this.fragmentView.getHeight() - getY());
                bgBlueVioletShaderTools.setBounds(-getX(), -getY(), FlexatarInstructionFragment.this.fragmentView.getWidth() - getX(), FlexatarInstructionFragment.this.fragmentView.getHeight() - getY());

                AndroidUtilities.rectTmp.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
                gradientPaint[0].setAlpha(255);
                int round = AndroidUtilities.dp(8) + (int) ((AndroidUtilities.dp(26) - AndroidUtilities.dp(8)) * (1f - 1));
                canvas.drawRoundRect(AndroidUtilities.rectTmp, round, round, gradientPaint[0]);
//                if (0 > 0 && 0 + 1 < gradientPaint.length) {
//                    gradientPaint[strangeCurrentPage + 1].setAlpha((int) (255 * pageOffset));
//                    canvas.drawRoundRect(AndroidUtilities.rectTmp, round, round, gradientPaint[strangeCurrentPage + 1]);
//                }
//                if (openProgress1 < 1f) {
//                    whitePaint.setAlpha((int) (255 * (1f - 0)));
//                    canvas.drawRoundRect(AndroidUtilities.rectTmp, round, round, whitePaint);
//                }
                super.onDraw(canvas);

//                if (positiveButtonDrawText) {
                    int xPos = (getWidth() / 2);
                    int yPos = (int) ((getHeight() / 2) - ((positiveButton.getPaint().descent() + positiveButton.getPaint().ascent()) / 2));
                    canvas.drawText(LocaleController.getString("TryCaptureInterface", R.string.TryCaptureInterface), xPos, yPos, positiveButton.getPaint());
//                }
            }
        };
        positiveButton.setMaxLines(1);
        positiveButton.setEllipsize(null);
        positiveButton.setMinWidth(AndroidUtilities.dp(64));
        positiveButton.setTag(Dialog.BUTTON_POSITIVE);
        positiveButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        positiveButton.setTextColor(Theme.getColor(Theme.key_voipgroup_nameText));
        positiveButton.setGravity(Gravity.CENTER);
        positiveButton.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
        positiveButton.getPaint().setTextAlign(Paint.Align.CENTER);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            positiveButton.setForeground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8), Color.TRANSPARENT, ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_voipgroup_nameText), (int) (255 * 0.3f))));
        }
        positiveButton.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
        positiveButton.setOnClickListener(onViewInstructionsChosenListener);
        linearLayout.addView(positiveButton,LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT,12,12,12,12));


        return fragmentView;
    }
    public void setOnTryInterfacePressed(View.OnClickListener listener){
        this.onViewInstructionsChosenListener = listener;
        if (positiveButton == null) return;
        positiveButton.setOnClickListener(listener);
    }
    @Override
    public void onFragmentDestroy() {

        super.onFragmentDestroy();
        finishPage();
    }







}
