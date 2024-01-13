package org.flexatar.DataOps;

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AssetAccess {
    public static String TAG = "AssetAccess";
    public static Context context;
    public static byte[] dataFromFile( String fileName){
        InputStream fileStream;
        int size = 0;
        try {
            fileStream = context.getAssets().open(fileName);
            size = (int) fileStream.available();
        } catch (IOException e) {
            return null;
        }
//        File rootDir = context.getFilesDir();
//        File trgFile = new File(rootDir, fileName);
//        if (!trgFile.exists()){
//            return null;
//        }

        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(fileStream);
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
        Log.d(TAG,"file size " + size);
        return bytes;
    }
    public static ByteBuffer bufferFromFile(String fileName){
        return dataToBuffer(dataFromFile(fileName));
    }
    public static ByteBuffer dataToBuffer(byte[] data){
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length);
//        Log.d("DWNLD","byteArray.length " + byteArray.length);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.put(data);
        byteBuffer.position(0);
//                String decodedString = new String(byteArray, StandardCharsets.UTF_8);
//        Log.d(TAG,"Float" + byteBuffer.asFloatBuffer().get(0));
        return byteBuffer;
    }
}
