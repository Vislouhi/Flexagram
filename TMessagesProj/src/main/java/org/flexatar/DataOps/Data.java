package org.flexatar.DataOps;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

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

    static long decodeLengthHeader(byte[] data){
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

}
