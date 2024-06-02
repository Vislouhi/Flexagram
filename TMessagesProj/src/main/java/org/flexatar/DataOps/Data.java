package org.flexatar.DataOps;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Data {
    public byte[] value;

    public Data(String str){
        value = str.getBytes(StandardCharsets.UTF_8);
    }
    public Data(byte[] val){
        value = val;
    }
    public Data add(Data data){
        int length1 = value.length;
        int length2 = data.value.length;
        byte[] result = new byte[length1 + length2];
        System.arraycopy(value, 0, result, 0, length1);
        System.arraycopy(data.value, 0, result, length1, length2);
        return new Data(result);
    }
    public Data encodeLengthHeader(){
        long longValue = value.length;
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.order(ByteOrder.nativeOrder());
        buffer.putLong(longValue);
        return new Data(buffer.array());
    }

    public static long decodeLengthHeader(byte[] data){
        ByteBuffer bb = ByteBuffer.allocate(8).order( ByteOrder.nativeOrder() );
        bb.put(data);
        bb.position(0);
        return bb.asLongBuffer().get();
    }
    public static ByteBuffer dataToBuffer(byte[] data){
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length);
//        Log.d("DWNLD","byteArray.length " + byteArray.length);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.put(data);
        byteBuffer.position(0);
//                String decodedString = new String(byteArray, StandardCharsets.UTF_8);
//        Log.d("IntStorManager","Float" + byteBuffer.asFloatBuffer().get(0));
        return byteBuffer;
    }
    public static ByteBuffer floatArrayToBytebuffer(float[] array){
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(array.length*4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer fb = byteBuffer.asFloatBuffer();
        for (int i = 0; i < array.length; i++) {
            fb.put(array[i]);
        }
        fb.position(0);
        byteBuffer.position(0);
        return byteBuffer;
    }
    public static float[] dataToFloatArray(byte[] data){
        ByteBuffer byteBuffer = ByteBuffer.allocate(data.length);
//        Log.d("DWNLD","byteArray.length " + byteArray.length);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.put(data);
        byteBuffer.position(0);
        int floatCount = data.length / 4;
        float[] floatArray = new float[floatCount];
        for (int i = 0; i < floatCount; i++) {
            floatArray[i] = byteBuffer.getFloat();
        }
//                String decodedString = new String(byteArray, StandardCharsets.UTF_8);
//        Log.d("===DEB===","dataToFloatArray" + byteBuffer.getFloat());
        return floatArray;
    }
    public static float[] bufferFloatArray(ByteBuffer byteBuffer){

        byteBuffer.position(0);
        int floatCount = byteBuffer.capacity() / 4;
        float[] floatArray = new float[floatCount];
        for (int i = 0; i < floatCount; i++) {
            floatArray[i] = byteBuffer.getFloat();
        }
//                String decodedString = new String(byteArray, StandardCharsets.UTF_8);
//        Log.d("===DEB===","dataToFloatArray" + byteBuffer.getFloat());
        return floatArray;
    }
    public static void printFloatArray(float[] arr){
        for (int i = 0; i < arr.length; i++) {
            Log.d("printFloatArray","" + arr[i]);
        }
    }
    public static int[] dataToIntArray(byte[] data){
        ByteBuffer byteBuffer = ByteBuffer.allocate(data.length);
//        Log.d("DWNLD","byteArray.length " + byteArray.length);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.put(data);
        byteBuffer.position(0);
        int floatCount = data.length / 8;
        int[] floatArray = new int[floatCount];
        for (int i = 0; i < floatCount; i++) {
            floatArray[i] = (int)byteBuffer.getLong();
        }
//                String decodedString = new String(byteArray, StandardCharsets.UTF_8);
//        Log.d("===DEB===","dataToFloatArray" + byteBuffer.getFloat());
        return floatArray;
    }
    public static String readFlxId(File file){
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[8];
            int bytesRead = fileInputStream.read(buffer, 0, 8);
            int packetLength = dataToIntArray(buffer)[0];
            buffer = new byte[packetLength];
            bytesRead = fileInputStream.read(buffer, 0, packetLength);
            buffer = new byte[8];
            bytesRead = fileInputStream.read(buffer, 0, 8);
            packetLength = dataToIntArray(buffer)[0];
            buffer = new byte[packetLength];
            bytesRead = fileInputStream.read(buffer, 0, packetLength);
            fileInputStream.close();
            String str = new String(buffer, StandardCharsets.UTF_8);
            JSONObject jsonObject = new JSONObject(str);
            return jsonObject.getString("fid");
//            Log.d("readFlxHeader",str);


        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }

    }
    public static byte[] unpackPreviewImage(Context context, String fileName){
        try {
//            FileInputStream fileInputStream = new FileInputStream(file);
            InputStream fileInputStream;
//            int size = 0;
            try {
                fileInputStream = context.getAssets().open(fileName);
//                size = (int) fileInputStream.available();
            } catch (IOException e) {
                return null;
            }
            boolean isHeader = true;
            String currentType = "";
            while (true) {
                byte[] buffer = new byte[8];
                int bytesRead = fileInputStream.read(buffer, 0, 8);
                if (bytesRead<=0) break;

                int packetLength = dataToIntArray(buffer)[0];
                buffer = new byte[packetLength];
                bytesRead = fileInputStream.read(buffer, 0, packetLength);
                if(isHeader) {
                    String str = new String(buffer, StandardCharsets.UTF_8);
                    JSONObject jsonObject = new JSONObject(str);
                    currentType = jsonObject.getString("type");
//                    Log.d("unpackPreviewImage", jsonObject.toString());
                }
                if (currentType.equals("PreviewImage")&&!isHeader){
                    fileInputStream.close();
                    return buffer;
                }
                isHeader = !isHeader;
            }
        } catch (IOException | JSONException e) {
            return null;
        }
        return null;
    }

    public List<byte[]> split(int firstSize, int partSize){
        if (value.length <= firstSize){
            return new ArrayList<byte[]>(){{add(value);}};
        }else{
            int leftSize = value.length-firstSize;
            int partCount = leftSize/partSize;
            int lastSize = leftSize-partCount*partSize;
            if (lastSize>0){
                partCount+=1;
            }
            List<byte[]> ret = new ArrayList<>();
            byte[] firstPart = new byte[firstSize];
            System.arraycopy(value, 0, firstPart, 0, firstSize);
            ret.add(firstPart);
            for (int i = 0; i < partCount-1; i++) {
                byte[] part = new byte[partSize];
                System.arraycopy(value, firstSize+partSize*i, part, 0, partSize);
                ret.add(part);
            }
            if (lastSize>0){
                byte[] part = new byte[lastSize];
                System.arraycopy(value, firstSize+partSize*(partCount-1), part, 0, lastSize);
                ret.add(part);
            }
            return ret;

        }

    }

}