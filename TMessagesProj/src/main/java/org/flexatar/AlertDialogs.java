package org.flexatar;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.flexatar.DataOps.FlexatarData;
import org.flexatar.DataOps.LengthBasedFlxUnpack;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;

public class AlertDialogs {
    public static interface OnNameReady{
        void onNameReady(String name);
    }
    public static AlertDialog askFlexatarNameDialog(Context context,String initialName,OnNameReady listener){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(LocaleController.getString("FlexatarName", R.string.FlexatarName));
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        final EditText editText = new EditText(context);
        editText.setText(initialName);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        editText.setHint(LocaleController.getString("EnterFlexatarsName", R.string.EnterFlexatarsName));
        int pad = AndroidUtilities.dp(12);
        linearLayout.setPadding(pad, pad, pad, pad);
        linearLayout.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
        builder.setView(linearLayout);
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> {
            listener.onNameReady(editText.getText().toString());
        });

        return builder.create();

    }
    public static AlertDialog askToCompleteInstructions(Context context){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("Info", R.string.Info));
        builder.setMessage(LocaleController.getString("CompleteInstruction", R.string.CompleteInstruction));


        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> {


        });
        return builder.create();
    }
    public static AlertDialog showVerifyInProgress(Context context){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("VerifyAlertCap", R.string.VerifyAlertCap));
        builder.setMessage(LocaleController.getString("VerifyAlertMes", R.string.VerifyAlertMes));


        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> {


        });
        return builder.create();
    }

    public static AlertDialog askToMakeFlexatarVideo(Context context){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("FlexatarInfo",R.string.FlexatarInfo));
//        builder.setTitle(LocaleController.getString("VerifyAlertCap", R.string.VerifyAlertCap));
        builder.setMessage(LocaleController.getString("FlexatarRoundQuestion",R.string.FlexatarRoundQuestion));


        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> {
            Config.chosenAudioWithFlexatar = true;
            Config.signalRecordAudioSemaphore();
            Config.runChooseFlexatarForAudioCallback();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), (dialogInterface, i) -> {
            Config.chosenAudioWithFlexatar = false;
            Config.signalRecordAudioSemaphore();
        });
        return builder.create();
    }
    public static AlertDialog askToSaveFlexatarToGallery(Context context, File flexatarFile){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("Info", R.string.Info));
        builder.setMessage(LocaleController.getString("CompleteInstruction", R.string.CompleteInstruction));
        byte[] flxBytes = FlexatarStorageManager.dataFromFile(flexatarFile);
        LengthBasedFlxUnpack unpackedFlexatar = new LengthBasedFlxUnpack(flxBytes);
        FlexatarData flexatarData = new FlexatarData(unpackedFlexatar);

        GLSurfaceView surfaceView = new GLSurfaceView(context);
        surfaceView.setEGLContextClientVersion(2);
        FlexatarViewRenderer renderer = new FlexatarViewRenderer();
        FlxDrawer drawer = new FlxDrawer();
        renderer.drawer = drawer;
        drawer.setFlexatarData(flexatarData);

        surfaceView.setRenderer(renderer);

        builder.setView(surfaceView);
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> {


        });
        return builder.create();
    }

    public static PopupWindow flexatarPopup(Context context, View location, File flexatarFile){

        byte[] flxBytes = FlexatarStorageManager.dataFromFile(flexatarFile);
        LengthBasedFlxUnpack unpackedFlexatar = new LengthBasedFlxUnpack(flxBytes);
        FlexatarData flexatarData = new FlexatarData(unpackedFlexatar);

        GLSurfaceView surfaceView = new GLSurfaceView(context);
        surfaceView.setEGLContextClientVersion(2);
        FlexatarViewRenderer renderer = new FlexatarViewRenderer();
        renderer.isRounded = true;
        FlxDrawer drawer = new FlxDrawer();

        renderer.drawer = drawer;
        drawer.setFlexatarData(flexatarData);

        surfaceView.setBackgroundColor(Color.TRANSPARENT);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        surfaceView.getHolder().setFormat(android.graphics.PixelFormat.RGBA_8888);
//        surfaceView.setZOrderOnTop(true);


        surfaceView.setRenderer(renderer);




        FrameLayout layout = new FrameLayout(context);
        ImageView decline = new ImageView(context);
        decline.setImageResource(R.drawable.cancel_big);

        layout.addView(surfaceView);
        layout.addView(decline,LayoutHelper.createFrame(46,46,Gravity.TOP |Gravity.RIGHT,0,12,12,0));

        TextView addView = new TextView(context);
        addView.setTextColor(Color.WHITE);
        addView.setText(LocaleController.getString("ImportFlexatar", R.string.ImportFlexatar));
        addView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        addView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));

        layout.addView(addView,LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT,LayoutHelper.WRAP_CONTENT,Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM,0,0,0,18));
        PopupWindow popupWindow = new PopupWindow(
                layout,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT

        );
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = Math.min(size.x, size.y);

        int windowWidth = width/2;
        int windowHeight = (int)(windowWidth * 1.4f);
//        int xOffset = AndroidUtilities.dp(12);

        popupWindow.setWidth(windowWidth);
        popupWindow.setHeight(windowHeight);

        decline.setOnClickListener(v->{
            popupWindow.dismiss();
        });
        addView.setOnClickListener(v->{
            popupWindow.dismiss();
            Log.d("FLX_INJECT","file name "+flexatarFile.getName() );
//            FlexatarStorageManager.addToStorage(context,f,fid);

        });
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                FlexatarRenderer.animator.release();
            }
        });
//        popupView.setOnCloseListener(() -> {
//            popupWindow.dismiss();
//            Log.d("FLX_INJECT","popup dismissed");
//        });
//        popupWindow.showAsDropDown(location, xOffset, AndroidUtilities.dp(48));
        popupWindow.showAtLocation(location, Gravity.CENTER, 0, 0);
        return popupWindow;
    }

}
