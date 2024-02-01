package org.flexatar;

import android.content.Context;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import org.flexatar.DataOps.FlexatarData;
import org.flexatar.DataOps.LengthBasedFlxUnpack;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.voip.VoIPBackgroundProvider;

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
        FlxDrawerNew drawer = new FlxDrawerNew();
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
        FlxDrawerNew drawer = new FlxDrawerNew();
        renderer.drawer = drawer;
        drawer.setFlexatarData(flexatarData);

        surfaceView.setRenderer(renderer);

        surfaceView.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        PopupWindow popupWindow = new PopupWindow(
                surfaceView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT

        );
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = Math.min(size.x, size.y);

        int windowWidth = width - AndroidUtilities.dp(24);
        int xOffset = AndroidUtilities.dp(12);

        popupWindow.setWidth(windowWidth);
//        popupView.setOnCloseListener(() -> {
//            popupWindow.dismiss();
//            Log.d("FLX_INJECT","popup dismissed");
//        });
        popupWindow.showAsDropDown(location, xOffset, AndroidUtilities.dp(48));
//        popupWindow.showAtLocation(location, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, AndroidUtilities.dp(46));
        return popupWindow;
    }

}
