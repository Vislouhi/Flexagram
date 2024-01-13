package org.flexatar.DataOps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LengthBasedUnpack {


    public List<byte[]> bPacks = new ArrayList<>();
    public LengthBasedUnpack(byte[] data){
        int offset = 0;
        while (offset<data.length) {
            byte[] lengthHeaderBytes = Arrays.copyOfRange(data, offset, offset + 8);
            long lengthHeader = Data.decodeLengthHeader(lengthHeaderBytes);
            offset += 8;
            byte[] body = Arrays.copyOfRange(data, offset, offset + (int) lengthHeader);
            offset += (int) lengthHeader;
            bPacks.add(body);
        }

    }

}
