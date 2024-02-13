package org.flexatar;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.flexatar.DataOps.Data;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FlexatarByImagesFragment extends BaseFragment {
    private static final int PICK_IMAGE_REQUEST = 666;
    public void finishPage(){

        finishFragment();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("ImagesForFlexatarTitle", R.string.ImagesForFlexatarTitle));

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishPage();
                }

            }
        });
        RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new Adapter(context,this ));
        recyclerView.setLayoutParams(new LinearLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        recyclerView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));


        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration(){
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                if (view instanceof FrameLayout) {
//                    int padding = (parent.getWidth() - view.getMeasuredWidth()) / 2;
                    // Apply margin to all sides of the view
                    outRect.set(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6));
                }
                /*view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        view.getViewTreeObserver().removeOnPreDrawListener(this);

                        if (view instanceof ImageView) {
                            int padding = (parent.getWidth() - view.getMeasuredWidth()) / 2;
                            // Apply margin to all sides of the view
                            outRect.set(padding, AndroidUtilities.dp(0), 0, AndroidUtilities.dp(0));
                        }

                        return true;
                    }
                });*/

            }
        });


        fragmentView = recyclerView;
//        fragmentView = new RecyclerView(getContext());
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        return fragmentView;
    }
    public interface OnImagesForFlexatarChosen {
        void onChosen(Intent data);
    }
    public static OnImagesForFlexatarChosen imageChosenListener;
    public static class Adapter  extends RecyclerView.Adapter<Adapter.ViewHolder>{

        private final Context mContext;
        private final int buttonsCount = 2;
        private final BaseFragment parent;
        List<File> imagesUri = new ArrayList<>();

        public Adapter(Context context,BaseFragment parent){
            mContext = context;
            this.parent = parent;
        }
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0){
                TextCell cell = new TextCell(mContext);
                return new Adapter.ViewHolder(cell);
            }else{
                FrameLayout cell = new FrameLayout(mContext);
                ImageView img = new ImageView(mContext){
                    final Path roundedPath = new Path();
                    final RectF roundedRect = new RectF();
                    final int cornerRadius = AndroidUtilities.dp(12);
                    @Override
                    protected void onDraw(Canvas canvas) {
                        int width = getWidth();
                        int height = getHeight();
                        roundedPath.reset();
                        roundedRect.set(0, 0, width, height);
                        roundedPath.addRoundRect(roundedRect, cornerRadius, cornerRadius, Path.Direction.CW);
                        canvas.clipPath(roundedPath);
                        super.onDraw(canvas);
                    }
                };
                cell.addView(img,LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT,LayoutHelper.WRAP_CONTENT,Gravity.CENTER_HORIZONTAL));

                RectF bgRect = new RectF();
                Paint paint = new Paint();
                paint.setARGB(200, 0, 0, 0);
                ImageView closePanelIcon = new ImageView(mContext){
                    @Override
                    protected void onDraw(Canvas canvas) {
                        bgRect.set(0, 0, getWidth(), getHeight());
                        canvas.drawRoundRect(bgRect, dp(getWidth()/2), dp(getHeight()/2), paint);
                        super.onDraw(canvas);
                    }
                };
                closePanelIcon.setImageResource(R.drawable.input_clear);
                cell.addView(closePanelIcon,LayoutHelper.createFrame(38,38,Gravity.TOP | Gravity.RIGHT,0,12,12,0));
                FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.WRAP_CONTENT);

                cell.setLayoutParams(p);
                return new Adapter.ViewHolder(cell);
            }
        }
        private TextCell addImageCell;
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (holder.itemView instanceof TextCell){
                TextCell cell = (TextCell) holder.itemView;
                if (position == 0){
                    addImageCell = cell;
                    String countText = "( "+imagesUri.size()+" / 5 )";
                    cell.setTextAndValueAndIcon(LocaleController.getString("SelectImageFromGallery",R.string.SelectImageFromGallery),countText, R.drawable.files_gallery, true);
                    cell.setOnClickListener(v->{
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("image/*");
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                        imageChosenListener = data ->{
                            if (data == null) return;
                            Object lock = new Object();
                            if (data.getClipData() != null) {
                                int count = data.getClipData().getItemCount();
                                if (count>0) {

                                    for (int i = 0; i < count; i++) {
                                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                                        resizeImageForFlexatar(mContext,imageUri,720,file->{


                                            AndroidUtilities.runOnUIThread(()->{
                                                synchronized (lock) {
                                                    imagesUri.add(file);
                                                    String countText1 = "( "+imagesUri.size()+" / 5 )";
                                                    addImageCell.setTextAndValueAndIcon(LocaleController.getString("SelectImageFromGallery",R.string.SelectImageFromGallery),countText1,R.drawable.files_gallery,true);
//                                                    notifyItemChanged(0);

                                                    notifyItemInserted(buttonsCount + imagesUri.size() - 1);
                                                }
                                            });

                                        });

                                    }

                                }
                            } else if (data.getData() != null) {

                                Uri imageUri = data.getData();
                                resizeImageForFlexatar(mContext,imageUri,720,file->{

                                    AndroidUtilities.runOnUIThread(()->{
                                        synchronized (lock) {
                                            imagesUri.add(file);
                                            String countText1 = "( "+imagesUri.size()+" / 5 )";

                                            addImageCell.setTextAndValueAndIcon(LocaleController.getString("SelectImageFromGallery",R.string.SelectImageFromGallery),countText1,R.drawable.files_gallery,true);
//                                            notifyItemChanged(0);

                                            notifyItemInserted(buttonsCount + imagesUri.size() - 1);
                                        }
                                    });
                                });
                            }
                        };
                        parent.startActivityForResult(intent, PICK_IMAGE_REQUEST);
                    });
                }else if (position == 1){
                    cell.setTextAndIcon(LocaleController.getString("MakeFlexatar",R.string.MakeFlexatar), R.drawable.menu_flexatar, true);
                    cell.setOnClickListener(v->{
                        if (imagesUri.size() != 5)
                            parent.showDialog(AlertDialogs.showNotEnoughPhotosAlert(mContext));
                        else{
                            packDataForFlexatar(imagesUri);
                            parent.finishFragment();
                        }
                    });
                }

            } else if (holder.itemView instanceof FrameLayout) {
                ImageView cell = (ImageView) ((FrameLayout) holder.itemView).getChildAt(0);
                File file = imagesUri.get(position - buttonsCount);
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                cell.setImageBitmap(bitmap);
                ImageView close = (ImageView) ((FrameLayout) holder.itemView).getChildAt(1);
                close.setOnClickListener(v->{
                    int newPosition = imagesUri.indexOf(file);
                    imagesUri.remove(file);
                    file.delete();
                    String countText1 = "( "+imagesUri.size()+" / 5 )";
                    addImageCell.setTextAndValueAndIcon(LocaleController.getString("SelectImageFromGallery",R.string.SelectImageFromGallery),countText1,R.drawable.files_gallery,true);
//                    notifyItemChanged(0);
                    notifyItemRemoved(newPosition + buttonsCount);
                });

            }
        }

        @Override
        public int getItemCount() {
            return buttonsCount+imagesUri.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
        @Override
        public int getItemViewType(int position) {
            return (position == 0 || position == 1)? 0 : 1;

        }
        public interface ImageConversionReadyListener{
            void onReady(File file);
        }
        public static void resizeImageForFlexatar(Context context, Uri uri, int targetWidth,ImageConversionReadyListener listener) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                int orientation = 0;
                try {
                    InputStream inputStream = context.getContentResolver().openInputStream(uri);
                    orientation = getExifOrientation(inputStream);
                    inputStream.close();
                } catch (IOException e) {
                    return;
                }
                try {
                    InputStream inputStream = context.getContentResolver().openInputStream(uri);

                    Bitmap bitmapOrig = BitmapFactory.decodeStream(inputStream);
                    if (inputStream != null)
                        inputStream.close();
                    float scaleFactor = (float) targetWidth / bitmapOrig.getWidth();

                    if (orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                        scaleFactor = (float) targetWidth / bitmapOrig.getHeight();
                    }
                    Bitmap bitmap = Bitmap.createScaledBitmap(bitmapOrig, (int) (bitmapOrig.getWidth() * scaleFactor), (int) (bitmapOrig.getHeight() * scaleFactor), false);
                    bitmapOrig.recycle();
                    bitmap = rotateBitmap(bitmap, orientation);
                    File imageFile = new File(FlexatarStorageManager.createFlexatarSendImageStorage(context), UUID.randomUUID().toString() + ".jpg");
                    FileOutputStream outputStream = new FileOutputStream(imageFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    outputStream.close();
                    listener.onReady(imageFile);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        public static Drawable createThumbnail(Context context, Uri uri, int thumbnailSize) {
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                Bitmap bitmapOrig = BitmapFactory.decodeStream(inputStream);
                if (inputStream!=null)
                    inputStream.close();
//                inputStream = context.getContentResolver().openInputStream(uri);
//                int orientation = getExifOrientation(inputStream);
//                if (inputStream!=null)
//                    inputStream.close();
//
//                bitmapOrig = rotateBitmap(bitmapOrig, orientation);
                float scaleFactor = (float)thumbnailSize/bitmapOrig.getWidth();
                Bitmap bitmap = Bitmap.createScaledBitmap(bitmapOrig, thumbnailSize, (int) (bitmapOrig.getHeight() * scaleFactor), false);
                bitmapOrig.recycle();
                RoundedBitmapDrawable dr = RoundedBitmapDrawableFactory.create(context.getResources(), bitmap);
                dr.setCornerRadius(AndroidUtilities.dp(24));

                return dr;

            } catch (FileNotFoundException e) {
                return null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        private static int getExifOrientation(InputStream inputStream) {
            try {
                // Create an ExifInterface from the InputStream
                ExifInterface exifInterface = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    exifInterface = new ExifInterface(inputStream);
                    // Read the orientation from the Exif data
                    return exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                }
                return ExifInterface.ORIENTATION_UNDEFINED;

            } catch (IOException e) {
                e.printStackTrace();
                return ExifInterface.ORIENTATION_UNDEFINED;
            }
        }

        private static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateBitmapDeg(bitmap, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateBitmapDeg(bitmap, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateBitmapDeg(bitmap, 270);
                default:
                    return bitmap;
            }
        }

        private static Bitmap rotateBitmapDeg(Bitmap bitmap, int degrees) {
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.postRotate(degrees);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        private static void packDataForFlexatar(List<File> files){
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("rotation", 0);
                jsonObject.put("teeth_top", 0);
                jsonObject.put("teeth_bottom", 0);
                jsonObject.put("name", "flexatarName");
                jsonObject.put("mouth_only", false);
                jsonObject.put("unordered", true);
                LocalDateTime currentDateTime = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                jsonObject.put("date", currentDateTime.format(formatter));

            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            Data sendData = new Data(jsonObject.toString());
            sendData = sendData.encodeLengthHeader().add(sendData);
            List<byte[]> imagesCollector = new ArrayList<>();
            for (File f : files){
                imagesCollector.add(FlexatarStorageManager.dataFromFile(f));
            }
            for (int i = 0; i < imagesCollector.size(); i++) {
                Data cData = new Data(imagesCollector.get(i));
                cData = cData.encodeLengthHeader().add(cData);
                sendData = sendData.add(cData);
            }
            File saveFile = new File(FlexatarStorageManager.createFlexatarSendImageStorage(ApplicationLoader.applicationContext),"input_face.bin");
            FlexatarStorageManager.dataToFile(sendData.value,saveFile);

        }
    }

}
