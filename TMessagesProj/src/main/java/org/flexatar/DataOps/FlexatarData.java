package org.flexatar.DataOps;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.util.Log;

import org.flexatar.VBO;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlexatarData {
    public Map<String, Map<String, List<byte[]>>> flxData = new HashMap<>();
    public FlexatarData(LengthBasedFlxUnpack dataLB){
        String currentPartName = "exp0";
        Map<String, List<byte[]>> currentPart = new HashMap<>();
        flxData.put(currentPartName,currentPart);

        for (int i = 0; i < dataLB.hPacks.size(); i++) {
            String headerName = dataLB.hPacks.get(i);
            byte[] body = dataLB.bPacks.get(i);
            if (headerName.equals("Delimiter")){
                String str = new String(body, StandardCharsets.UTF_8);
                try {
                    JSONObject jsonObject = new JSONObject(str);
                    currentPartName = jsonObject.getString("type");
                    if (!currentPartName.equals("end")) {
                        currentPart = new HashMap<>();
                        flxData.put(currentPartName, currentPart);
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
//                Log.d("====DEB====", " delimiter " + str);

            }else{
                if (currentPart.containsKey(headerName)){
                    currentPart.get(headerName).add(body);
                }else{
                    List<byte[]> dataList = new ArrayList<>();
                    dataList.add(body);
                    currentPart.put(headerName,dataList);

                }
            }
        }
        if (!checkMouthExists()){
            Log.d("====DEB====","mouth not found");
            flxData.remove("mouth");
            currentPart = new HashMap<>();
            flxData.put("mouth",currentPart);
            byte[] moutData = new LengthBasedUnpack(AssetAccess.dataFromFile("flexatar/FLX_mouth_collection.dat")).bPacks.get(0);
            LengthBasedFlxUnpack packLBMouth = new LengthBasedFlxUnpack(moutData);
            for (int i = 0; i < packLBMouth.hPacks.size(); i++) {
                String headerName = packLBMouth.hPacks.get(i);
                byte[] body = packLBMouth.bPacks.get(i);
//                Log.d("====DEB====","headerName " +headerName);
                if (currentPart.containsKey(headerName)){
                    currentPart.get(headerName).add(body);
                }else{
                    List<byte[]> dataList = new ArrayList<>();
                    dataList.add(body);
                    currentPart.put(headerName,dataList);

                }

            }
//            flxData.remove("mouth");
        }
        repackMandalaBlendshape();
        makeMandalaVtx();
        prepareGlBuffers();
    }

    public ByteBuffer[] headBB = new ByteBuffer[5];
    public Bitmap[] headBitmaps = new Bitmap[5];
    private void prepareGlBuffers(){
        for (int i = 0; i < 5; i++) {
            headBB[i] = Data.dataToBuffer(flxData.get("exp0").get("mandalaBlendshapes").get(i));
        }
        for (int i = 0; i < 5; i++) {
            byte[] imgData = flxData.get("exp0").get("mandalaTextureBlurBkg").get(i);
            InputStream inputStream = new ByteArrayInputStream(imgData);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            headBitmaps[i] = bitmap;
        }
    }
    private void repackMandalaBlendshape(){
        byte[] mandalaBlendshapes = this.flxData.get("exp0").get("mandalaBlendshapes").get(0);
        int blockLength = mandalaBlendshapes.length/5;
        byte[][] repack = new byte[5][blockLength];

        for (int i = 0; i < blockLength/16; i++) {
            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 16; k++) {
                    repack[j][i*16+k] = mandalaBlendshapes[i*80 + j * 16 +k];
                }
            }
        }
        List<byte[]> repackList = new ArrayList<>();
        for (int j = 0; j < 5; j++) {
            repackList.add(repack[j]);
        }
//        Data.printFloatArray(Data.dataToFloatArray(repack[0]));

//                Data.dataToFloatArray()
        this.flxData.get("exp0").put("mandalaBlendshapes",repackList);

    }

    public float[][][] mandalaTriangles;
    public float[][][] mandalaBorder;
    public int[] mandalaFaces;
    private void makeMandalaVtx(){
        float[] trianglesFloat = Data.dataToFloatArray(this.flxData.get("exp0").get("mandalaCheckpoints").get(0));
        int[] mandalaFaces = Data.dataToIntArray(this.flxData.get("exp0").get("mandalaFaces").get(0));
        this.mandalaFaces = mandalaFaces;
        int vtxInFace = 3;
        int facesCount = mandalaFaces.length/vtxInFace;
        int vtxSize = 2;

        mandalaTriangles = new float[mandalaFaces.length/3][vtxInFace][vtxSize];
        for (int i = 0; i < facesCount; i++) {
            for (int j = 0; j < vtxInFace; j++) {
                for (int k = 0; k < vtxSize; k++) {
                    mandalaTriangles[i][j][k] = trianglesFloat[mandalaFaces[i*vtxInFace+j]*vtxSize+k];
                }
//
            }
        }
        int[] mandalaBorderIdx = Data.dataToIntArray(this.flxData.get("exp0").get("mandalaBorder").get(0));
        mandalaBorder = new float[mandalaBorderIdx.length][2][vtxSize];
        for (int i = 0; i < mandalaBorderIdx.length - 1; i++) {
            for (int j = 0; j < vtxSize; j++) {
                mandalaBorder[i][0][j] = trianglesFloat[mandalaBorderIdx[i]*2+j];
                mandalaBorder[i][1][j] = trianglesFloat[mandalaBorderIdx[i+1]*2+j];
            }

        }
        int lastIdx = mandalaBorderIdx.length - 1;
        for (int j = 0; j < vtxSize; j++) {
            mandalaBorder[lastIdx][0][j] = trianglesFloat[mandalaBorderIdx[lastIdx] * 2 + j];
            mandalaBorder[lastIdx][1][j] = trianglesFloat[mandalaBorderIdx[0] * 2 + j];
        }
//        float[] testPoint = new float[2];
//        testPoint[0] = 0.45f;
//        testPoint[1] = 0.8f;
//        InterUnit.makeInterUnit(testPoint, mandalaTriangles,mandalaFaces,mandalaBorder);

//        for (int v:mandalaBorderIdx){ Log.d("====DEB====", " imgData " + v);}

    }
    private boolean checkMouthExists(){
        if (flxData.containsKey("mouth")){
            if (flxData.get("mouth").containsKey("mouthData")){
                return true;
            }
        }
        return false;
    }



}
