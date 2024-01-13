package org.flexatar.DataOps;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LengthBasedFlxUnpack {

    public List<String> hPacks = new ArrayList<>();
    public List<byte[]> bPacks = new ArrayList<>();
    public LengthBasedFlxUnpack(byte[] data){
        int offset = 0;
        int cntr = 0;
        while (offset<data.length) {
            byte[] lengthHeaderBytes = Arrays.copyOfRange(data, offset, offset + 8);
            long lengthHeader = Data.decodeLengthHeader(lengthHeaderBytes);
            offset += 8;
            byte[] body = Arrays.copyOfRange(data, offset, offset + (int) lengthHeader);
            offset += (int) lengthHeader;
            if (cntr%2 == 1){
                bPacks.add(body);
            }else{
                String str = new String(body, StandardCharsets.UTF_8);
                try {
                    JSONObject jsonObject = new JSONObject(str);
//                    Log.d("====DEB====", " header " + jsonObject.getString("type"));
                    hPacks.add(jsonObject.getString("type"));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

            }

            cntr+=1;
        }


    }

}
