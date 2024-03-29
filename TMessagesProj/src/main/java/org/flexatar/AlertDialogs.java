package org.flexatar;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.opengl.GLSurfaceView;
import android.os.Build;
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

import org.flexatar.DataOps.Data;
import org.flexatar.DataOps.FlexatarData;
import org.flexatar.DataOps.LengthBasedFlxUnpack;
import org.flexatar.DataOps.LengthBasedUnpack;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieDrawable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class AlertDialogs {
    public static Dialog showNotEnoughPhotosAlert(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("Info", R.string.Info));
        builder.setMessage(LocaleController.getString("NotEnoughPhotos", R.string.NotEnoughPhotos));


        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> {


        });
        return builder.create();
    }

    public static interface OnNameReady{
        void onNameReady(String name);
    }
    public static Dialog askFlexatarNameDialog(Context context,String initialName,OnNameReady listener){
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
        Dialog alertDialog = builder.create();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG);
//        }

        return alertDialog;

    }
    public static AlertDialog askToCompleteInstructions(Context context){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("Info", R.string.Info));
        builder.setMessage(LocaleController.getString("CompleteInstruction", R.string.CompleteInstruction));


        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> {


        });
        return builder.create();
    }
    public static AlertDialog sayImpossibleToPerform(Context context){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("Info", R.string.Info));
        builder.setMessage(LocaleController.getString("ImpossibleToPerform", R.string.ImpossibleToPerform));


        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> {


        });
        return builder.create();
    }

    public static AlertDialog sayImposableToDelete(Context context){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("Info", R.string.Info));
        builder.setMessage(LocaleController.getString("ImpossibleToDeleteGroup", R.string.ImpossibleToDeleteGroup));


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

    public static AlertDialog showImpossibleToDelete(Context context){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("FlexatarInfo", R.string.FlexatarInfo));
        builder.setMessage(LocaleController.getString("ImpossibleToDelete", R.string.ImpossibleToDelete));


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

    public static AlertDialog askToPutFlexatarToGallery(Context context,File flexatarFile){
//        Context context = ApplicationLoader.applicationContext;
        int account = UserConfig.selectedAccount;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("FlexatarInfo",R.string.FlexatarInfo));
//        builder.setTitle(LocaleController.getString("VerifyAlertCap", R.string.VerifyAlertCap));
        builder.setMessage(LocaleController.getString("AddFlexatarToGallery",R.string.AddFlexatarToGallery));


        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> {
           FlexatarStorageManager.addToStorage(context,account,flexatarFile);
           File videoFile = new File(flexatarFile.getAbsolutePath().replace(".flx",".mp4"));
            if (flexatarFile.exists()) flexatarFile.delete();
            if (videoFile.exists()) videoFile.delete();

        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), (dialogInterface, i) -> {

        });
        return builder.create();
    }
    public static AlertDialog askToDeleteFlexatarFromCloud(Context context,File tmpFile,File galleryFile){
//        Context context = ApplicationLoader.applicationContext;
        int account = UserConfig.selectedAccount;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("FlexatarInfo",R.string.FlexatarInfo));
//        builder.setTitle(LocaleController.getString("VerifyAlertCap", R.string.VerifyAlertCap));
        builder.setMessage(LocaleController.getString("AskDeleteFromCloud",R.string.AskDeleteFromCloud));


        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialogInterface, i) -> {
            if (tmpFile.exists())tmpFile.delete();
            if (galleryFile.exists()){
                FlexatarStorageManager.deleteFromStorage(context,account,galleryFile,true);
            }

        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), (dialogInterface, i) -> {

        });
        return builder.create();
    }
    public static AlertDialog sayAlreadyInGallery(Context context){
//        Context context = ApplicationLoader.applicationContext;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("FlexatarInfo",R.string.FlexatarInfo));
//        builder.setTitle(LocaleController.getString("VerifyAlertCap", R.string.VerifyAlertCap));
        builder.setMessage(LocaleController.getString("FlexatarInGallery",R.string.FlexatarInGallery));


        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> {

        });

        return builder.create();
    }
    public static AlertDialog sayFlexatarStartMaking(Context context,Runnable completion){
//        Context context = ApplicationLoader.applicationContext;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("FlexatarInfo",R.string.FlexatarInfo));
//        builder.setTitle(LocaleController.getString("VerifyAlertCap", R.string.VerifyAlertCap));
        builder.setMessage(LocaleController.getString("FlexatarWillBeReady",R.string.FlexatarWillBeReady));


        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> {
            if (completion!=null) completion.run();
        });

        return builder.create();
    }
    public static AlertDialog sayFlexatarConnectionError(Context context,Runnable completion){
//        Context context = ApplicationLoader.applicationContext;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("FlexatarInfo",R.string.FlexatarInfo));
//        builder.setTitle(LocaleController.getString("VerifyAlertCap", R.string.VerifyAlertCap));
        builder.setMessage(LocaleController.getString("FlexatarConnectionErr",R.string.FlexatarConnectionErr));


        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> {
            if (completion!=null) completion.run();
        });

        return builder.create();
    }
    public static AlertDialog sayFlexatarNotFound(Context context){

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("FlexatarInfo",R.string.FlexatarInfo));
        builder.setMessage(LocaleController.getString("FlexatarNotFound",R.string.FlexatarNotFound));
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> {

        });

        return builder.create();
    }
   /* public static AlertDialog askToSaveFlexatarToGallery(Context context, File flexatarFile){
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
    }*/
    public static GLSurfaceView createFlxSurface(File file){
        FlexatarData flexatarDataTmp = FlexatarData.factory(file);
        GLSurfaceView surfaceView = new GLSurfaceView(ApplicationLoader.applicationContext);
        surfaceView.setEGLContextClientVersion(2);
        FlexatarViewRenderer renderer = new FlexatarViewRenderer();
        renderer.isRounded = true;
        FlxDrawer drawer = new FlxDrawer();

        drawer.onFrameStartListener.set( ()-> new FlxDrawer.RenderParams(){{
            if (flexatarDataTmp.flxDataType == FlexatarData.FlxDataType.PHOTO) {
                flexatarData = flexatarDataTmp;
                flexatarType=1;
            }else if (flexatarDataTmp.flxDataType == FlexatarData.FlxDataType.VIDEO) {
                flexatarDataVideo = flexatarDataTmp;
                flexatarType=0;
                drawer.prepareVideoTextures();
            }
            mixWeight = 1;
            effectID = 0;
            isEffectsOn = false;
        }});

        renderer.drawer = drawer;


        surfaceView.setBackgroundColor(Color.TRANSPARENT);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        surfaceView.getHolder().setFormat(android.graphics.PixelFormat.RGBA_8888);
        surfaceView.setRenderer(renderer);
        return surfaceView;
    }

    public interface OnAddToGalleryListener{
        void onAdd(File flexatarFile);
    }
    public interface OnDeleteFromGalleryListener{
        void onDelete(File tmpFile,File galleryFile);
    }

    public static class ImageViewRoundBkg extends ImageView{
        private final RectF bgRect = new RectF();
        private final Paint paint = new Paint();

        private final float scale = 0.57f;
        public ImageViewRoundBkg(Context context) {
            super(context);
            paint.setARGB(100, 0, 0, 0);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            Drawable drawable = getDrawable();

            bgRect.set(0, 0, getWidth(), getHeight());
            canvas.drawRoundRect(bgRect, getWidth()/2, getHeight()/2, paint);
            float w = getWidth();
            float h = getHeight();
            int left = (int) ((w-w*scale)/2);
            int right = (int) (left + w*scale);
            int top = (int) ((h-h*scale)/2);
            int bottom = (int) (top + h*scale);
            drawable.setBounds(left, top, right, bottom);
            drawable.draw(canvas);
//                super.onDraw(canvas);
        }
    }
    public static PopupWindow importFlexatarPopup(Context context, View location,Theme.ResourcesProvider resourceProvider,
                                                  ServerDataProc.FlexatarChatCellInfo ftarInfo,int fragW,int fragH,Runnable onDismiss,OnAddToGalleryListener onAddToGallery,Runnable alreadyInGallery,OnDeleteFromGalleryListener onDeleteFromGallery,Runnable flexatarNotFound) {
        int account = UserConfig.selectedAccount;
//        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
//

        int width = Math.min(fragW, fragH);

        int windowWidth = width/2;
        int windowHeight = (int)(windowWidth * 1.6f);
        int buttonSize = (int) ((float)windowWidth/4.5f);

        FrameLayout layout = new FrameLayout(context);



        ImageView declineIcon = new ImageView(context);
        declineIcon.setImageResource(R.drawable.cancel_big);
        FrameLayout.LayoutParams par1 = LayoutHelper.createFrame(46, 46, Gravity.BOTTOM | Gravity.RIGHT, 0, 0, 12, 0);
        par1.width = buttonSize;
        par1.height = buttonSize;

        layout.addView(declineIcon,par1);


        ImageViewRoundBkg acceptIcon = new ImageViewRoundBkg(context);
        FrameLayout.LayoutParams par2 = LayoutHelper.createFrame(46,46,Gravity.BOTTOM |Gravity.LEFT,12,0,12,0);
        par2.width = buttonSize;
        par2.height = buttonSize;
        acceptIcon.setImageResource(R.drawable.msg_addfolder);
        layout.addView(acceptIcon,par2);

        ImageViewRoundBkg deleteIcon = new ImageViewRoundBkg(context);
        deleteIcon.setImageResource(R.drawable.msg_delete);
        FrameLayout.LayoutParams par3 = LayoutHelper.createFrame(46,46,Gravity.BOTTOM |Gravity.CENTER,0,0,0,0);
        par3.width = buttonSize;
        par3.height = buttonSize;
        //        String pathMetaData = ftarInfo.ftar.replace(".p",".m");
        File tmpFlxFile = new File(FlexatarStorageManager.createTmpLoadFlexatarStorage(ApplicationLoader.applicationContext,account),ftarInfo.getFileName());
        File galleryFile = new File(FlexatarStorageManager.getFlexatarStorage(ApplicationLoader.applicationContext,account),ftarInfo.getFileName());
        File fileToReadFlx = galleryFile.exists() ? galleryFile : tmpFlxFile;
        boolean needDeleteIcon = true;
        if (tmpFlxFile.exists()) {
            Log.d("FLX_INJECT","flx tmp name " +tmpFlxFile.getName());
            if (tmpFlxFile.getName().startsWith("public"))needDeleteIcon=false;
            acceptIcon.setOnClickListener(v -> {
                onAddToGallery.onAdd(tmpFlxFile);

            });


        }
        if (galleryFile.exists()) {
            Log.d("FLX_INJECT","flx gal name " +galleryFile.getName());
            if (galleryFile.getName().startsWith("public"))needDeleteIcon=false;

            acceptIcon.setOnClickListener(v -> {
                alreadyInGallery.run();

            });
        }
        if ((tmpFlxFile.exists() || galleryFile.exists())&&needDeleteIcon){
            layout.addView(deleteIcon,par3);

            deleteIcon.setOnClickListener(v->{
                onDeleteFromGallery.onDelete(tmpFlxFile,galleryFile);

            });
        }
        PopupWindow popupWindow = new PopupWindow(
                layout,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT

        );
        AtomicReference<Boolean> isDialogOpened = new AtomicReference<>(true);
        if( fileToReadFlx.exists()) {
            FrameLayout.LayoutParams params = LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT,Gravity.TOP);
            params.height = windowHeight - AndroidUtilities.dp(48);
            layout.addView(createFlxSurface(fileToReadFlx), params);
        }else{
            RLottieDrawable downloadDrawable = new RLottieDrawable(R.raw.download_progress, "download_progress", AndroidUtilities.dp(36), AndroidUtilities.dp(36), false, null);
            ImageView downloadAnimation = new ImageView(context);
            downloadAnimation.setImageDrawable(downloadDrawable);
            downloadDrawable.setAutoRepeat(1);
            downloadDrawable.start();

            int fillColor = Theme.getColor(Theme.key_actionBarDefault, resourceProvider);
            int cornerRadius = AndroidUtilities.dp(20);

            RoundRectShape roundRectShape = new RoundRectShape(new float[]{cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius, cornerRadius}, null, null);
            ShapeDrawable shapeDrawable = new ShapeDrawable(roundRectShape);
            shapeDrawable.getPaint().setColor(fillColor);
            downloadAnimation.setBackground(shapeDrawable);

            FrameLayout.LayoutParams param = LayoutHelper.createFrame(46, 46, Gravity.CENTER, 0, 0, 0, 0);
            param.width = windowWidth / 2;
            param.height = windowHeight / 2;
            layout.addView(downloadAnimation, param);

            FlexatarServerAccess.downloadFlexatar(UserConfig.selectedAccount,tmpFlxFile, ftarInfo.ftar, new FlexatarServerAccess.OnReadyOrErrorListener() {
                @Override
                public void onReady(File flexatarFile,int flexatarType) {
                    AndroidUtilities.runOnUIThread(()->{
                        if (!isDialogOpened.get())return;
                        downloadDrawable.stop();
                        layout.removeView(downloadAnimation);
                        FrameLayout.LayoutParams params = LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT,Gravity.TOP);
                        params.height = windowHeight - AndroidUtilities.dp(48);
                        if (tmpFlxFile.exists()) {
                            layout.addView(createFlxSurface(tmpFlxFile), params);
                            if (!tmpFlxFile.getName().startsWith("public")) {
                                layout.addView(deleteIcon, par3);
                                deleteIcon.setOnClickListener(v->{
                                    onDeleteFromGallery.onDelete(tmpFlxFile,galleryFile);

                                });
                            }
                        }
                        else if (galleryFile.exists()) {
                            layout.addView(createFlxSurface(galleryFile), params);
                            if (!galleryFile.getName().startsWith("public")) {
                                layout.addView(deleteIcon, par3);
                                deleteIcon.setOnClickListener(v->{
                                    onDeleteFromGallery.onDelete(tmpFlxFile,galleryFile);

                                });
                            }
                        }


                        acceptIcon.setOnClickListener(v->{
                            onAddToGallery.onAdd(tmpFlxFile);

                        });
                    });
                }

                @Override
                public void onError() {
                    AndroidUtilities.runOnUIThread(()-> {
                        downloadDrawable.stop();
                        layout.removeView(downloadAnimation);
                        popupWindow.dismiss();
                        flexatarNotFound.run();
                    });
                }
            });

        }



        popupWindow.setWidth(windowWidth);
        popupWindow.setHeight(windowHeight);

        declineIcon.setOnClickListener(v->{
            popupWindow.dismiss();
        });


        popupWindow.showAtLocation(location, Gravity.CENTER, 0, 0);
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                if (onDismiss!=null) onDismiss.run();
                isDialogOpened.set(false);
            }
        });

        return popupWindow;
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
