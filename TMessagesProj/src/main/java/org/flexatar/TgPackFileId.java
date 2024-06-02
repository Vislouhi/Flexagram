package org.flexatar;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import android.util.Base64;
import android.util.Log;

public class TgPackFileId {
    public static byte[] concat(byte[] array1, byte[] array2) {
        int length1 = array1.length;
        int length2 = array2.length;

        byte[] concatenatedArray = new byte[length1 + length2];

        System.arraycopy(array1, 0, concatenatedArray, 0, length1);
        System.arraycopy(array2, 0, concatenatedArray, length1, length2);

        return concatenatedArray;
    }
    private static int pos_mod(int a,int b){
        int rest = a%b;
        if (rest<0) {
            return rest + Math.abs(b);
        }else{
            return rest;
        }
    }
    public static byte[] packUnsignedInt(int value) {
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);

        buffer.putInt(value);
        return buffer.array();
    }
    private static byte[] pack_tl_string(byte[] string){
        int length = string.length;
        int fill;
        byte[] bytes = new byte[0];
        if (length<=253){
//            Log.d("FLX_INJECT","length : " + length);
//            Log.d("FLX_INJECT","photo id char : " + Character.toString((char)length).getBytes().length);

            bytes = concat(bytes,Character.toString((char)length).getBytes());
//            bytes = concat(bytes,String.valueOf((char) length).getBytes());
            fill = pos_mod(-length-1,4);
        }else{
            bytes = concat(bytes,String.valueOf((char) 254).getBytes(StandardCharsets.UTF_8));
            bytes = concat(bytes,Arrays.copyOfRange(packUnsignedInt(length),0,3));
            fill = pos_mod(-length,4);
        }
        bytes = concat(bytes,string);
        byte[] fillArr = new byte[fill];
        Arrays.fill(fillArr, (byte) 0);
        bytes = concat(bytes,fillArr);
        return bytes;
    }

    private static byte[] rle_encode(byte[] binary){
        byte[] ret = new byte[0];
        int count = 0;
        byte[] tmp = new byte[1];
        for (byte cur : binary){
            if (cur == 0){
                count += 1;
            }else{
                if (count>0 ){

                    tmp[0] = (byte) (0 & 0xFF);
                    ret = concat(ret,tmp);
                    tmp[0] = (byte) (count & 0xFF);
                    ret = concat(ret,tmp);
                    count = 0;
                }
                tmp[0] = cur;
                ret = concat(ret,tmp);
            }
        }
        if (count > 0){
            tmp[0] = (byte) (0 & 0xFF);
            ret = concat(ret,tmp);
            tmp[0] = (byte) (count & 0xFF);
            ret = concat(ret,tmp);
        }
        return ret;
    }
    private static String base64url_encode(byte[] string){
        byte[] encodedBytes = Base64.encode(string, Base64.URL_SAFE);
        return new String(encodedBytes,StandardCharsets.UTF_8);
    }
    private static byte[] packLong(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN); // Set little-endian byte order
        buffer.putLong(value);
        return buffer.array();
    }
    public static String packFileId(long id,int dc_id,long access_hash,byte[] file_reference,String size_type){
        int type_id = 2;
        int type_id_p = type_id | (1 << 25);
        byte[] file_id = new byte[0];
        file_id = concat(file_id,packUnsignedInt(type_id_p));
        file_id = concat(file_id,packUnsignedInt(dc_id));
        file_id = concat(file_id,pack_tl_string(file_reference));
        file_id = concat(file_id,packLong(id));
        file_id = concat(file_id,packLong(access_hash));
        file_id = concat(file_id,packUnsignedInt(1));
        file_id = concat(file_id,packUnsignedInt(type_id));

        byte[] sizeTypeBytes = size_type.getBytes(StandardCharsets.UTF_8);
//        Log.d("FLX_INJECT","sizeTypeBytes length : " + sizeTypeBytes.length);

        byte[] tail = new byte[4-sizeTypeBytes.length];
        Arrays.fill(tail,(byte) 0);
        file_id = concat(file_id,sizeTypeBytes);
        file_id = concat(file_id,tail);

        byte[] tmp = new byte[1];
        tmp[0] = 53;
        file_id = concat(file_id,tmp);
        tmp[0] = 4;
        file_id = concat(file_id,tmp);

//        return base64url_encode(rle_encode(file_id));
        return base64url_encode(rle_encode(file_id)).replace("=","").replace("\n","");
    }
    public static void check(){
        int[] fRef = new int[]{3,0,0,16,206,102,67,104,92,94,142,130,122,15,255,41,33,243,124,201,8,171,136,110,170};
//        int[] fRef = new int[]{1,0,0,12,108,102,68,206,73,141,149,117,202,162,132,157,190,84,208,104,16,37,42,70,179};
        byte[] file_reference = new byte[fRef.length];
        for (int i = 0; i < fRef.length; i++) {
            file_reference[i] = (byte) (fRef[i] & 0xFF);
        }
        String result = packFileId(5339412869357362646L,2,5765837138111146480L,file_reference,"m");
//        String result = packFileId(5339115103569697558L,2,3234066006077291991L,file_reference,"m");
        String sample = "AgACAgIAAxkDAAIQzmZDaFxejoJ6D_8pIfN8yQiriG6qAALW3TEbt2cZSvCp3hFUXgRQAQADAgADbQADNQQ";
//        String sample = "AgACAgIAAxkBAAIMbGZEzkmNlXXKooSdvlTQaBAlKkazAAIW2zEb5lgYStc9Z9DlteEsAQADAgADbQADNQQ";
        Log.d("FLX_INJECT","photo id : " +result);
        Log.d("FLX_INJECT","photo id : "+sample);
        Log.d("FLX_INJECT","verdict : " + sample.equals(result));
    }
}
