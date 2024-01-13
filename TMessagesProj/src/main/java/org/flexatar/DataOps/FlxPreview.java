package org.flexatar.DataOps;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FlxPreview {
    public byte[] pathToFlx;
    public byte[] imageData;
    public String userId;
    public Bitmap icon;
    public FlxPreview(byte[] flxPath,byte[] imageData){
        pathToFlx = flxPath;
        String str = new String(pathToFlx, StandardCharsets.UTF_8);
        userId = str.split("/")[1];
//        Log.d("FlxPreview",pathToFlx);
        InputStream inputStream = new ByteArrayInputStream(imageData);
        icon = BitmapFactory.decodeStream(inputStream);
        this.imageData = imageData;

    }
    public FlxPreview(String userId,byte[] imageData){
        pathToFlx = null;
        this.userId = userId;
        this.imageData=imageData;
//        Log.d("FlxPreview",pathToFlx);
        InputStream inputStream = new ByteArrayInputStream(imageData);
        icon = BitmapFactory.decodeStream(inputStream);

    }

}
