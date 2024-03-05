package org.flexatar.DataOps;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LengthBasedFlxUnpack {

    public List<String> hPacks = new ArrayList<>();
    public List<byte[]> hPacksByte = new ArrayList<>();
    public List<byte[]> bPacks = new ArrayList<>();
    private final static int MAX_AVAILABLE_LENGTH = 10000000;
    public LengthBasedFlxUnpack(byte[] data){
        int offset = 0;
        int cntr = 0;



        while (offset<data.length) {
            byte[] lengthHeaderBytes = Arrays.copyOfRange(data, offset, offset + 8);
            long lengthHeader = Data.decodeLengthHeader(lengthHeaderBytes);

            if (lengthHeader>MAX_AVAILABLE_LENGTH) break;
            offset += 8;
            byte[] body = Arrays.copyOfRange(data, offset, offset + (int) lengthHeader);
            offset += (int) lengthHeader;
            if (cntr%2 == 1){
                bPacks.add(body);
            }else{
                hPacksByte.add(body);
                String str = new String(body, StandardCharsets.UTF_8);

                try {
//                    Log.d("====DEB====", " header " + str);
                    JSONObject jsonObject = new JSONObject(str);

                    hPacks.add(jsonObject.getString("type"));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

            }

            cntr+=1;
        }


    }
    public boolean validate(int flexatarType){
        if (flexatarType == 0){
            return validateVideo();
        }
        String[] keysMustBe = {
                "Info","PreviewImage","mandalaBlendshapes","mandalaTextureBlurBkg",
                "mandalaCheckpoints","mandalaFaces","mandalaBorder"
        };
        boolean isValid = true;
        for (String key:keysMustBe){
            isValid = isValid && hPacks.contains(key);
        }
        Log.d("FLX_INJECT","flexatar file validation: "+isValid);
        return isValid;
    }
    public boolean validateVideo(){
        String[] keysMustBe = {
                "Info","PreviewImage","video","markers","positions",
                "mandalaCheckpoints","mandalaFaces","mandalaBorder"
        };
        boolean isValid = true;
        for (String key:keysMustBe){
            isValid = isValid && hPacks.contains(key);
        }
        Log.d("FLX_INJECT","video flexatar file validation: "+isValid);
        return isValid;
    }

}
