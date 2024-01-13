package org.flexatar.DataOps;

import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LengtBasedDict {
    public Map<String,byte[]> dict = new HashMap<>();
    public LengtBasedDict(byte[] data){
        int offset = 0;
        int cntr = 0;
        String currentEntry = "";
        while (offset<data.length) {
            byte[] lengthHeaderBytes = Arrays.copyOfRange(data, offset, offset + 8);
            long lengthHeader = Data.decodeLengthHeader(lengthHeaderBytes);
            offset += 8;
            byte[] body = Arrays.copyOfRange(data, offset, offset + (int) lengthHeader);
            offset += (int) lengthHeader;
            if (cntr%2 == 1){
                dict.put(currentEntry,body);
            }else{
                currentEntry = new String(body, StandardCharsets.UTF_8);
                Log.d("====DEB====", " currentEntry " + currentEntry);
            }

            cntr+=1;
        }
    }
}
