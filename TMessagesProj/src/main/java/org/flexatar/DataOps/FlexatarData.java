package org.flexatar.DataOps;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import org.flexatar.AnimationUnit;
import org.flexatar.FlexatarCommon;
import org.flexatar.GLM;
import org.flexatar.InterUnit;
import org.flexatar.VBO;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlexatarData {
    private final LengtBasedDict mouthData;
    public final float mouthRatio;
    public Map<String, Map<String, List<byte[]>>> flxData = new HashMap<>();
    public int mouthVtxCount;
    public ByteBuffer mouthUvBB;
    public ByteBuffer mouthIdxBB;
    public int mouthIdxCount;
    public List<byte[]> mouthBlendshapes;
    public ByteBuffer[] mouthBlendshapeBB;
    public float[][][] lipAnchors;
    public float[] lipSize;
    public float[] teethGap;
    public Bitmap[] mouthBitmaps;

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
        mouthData = new LengtBasedDict(this.flxData.get("mouth").get("mouthData").get(0));

        String flxInfoString = new String(this.flxData.get("mouth").get("FlxInfo").get(0), StandardCharsets.UTF_8);
        try {
            JSONObject flxInfo = new JSONObject(flxInfoString);
            JSONArray bbox = flxInfo.getJSONArray("bbox");
            mouthRatio = 1f/(float)(flxInfo.getDouble("camFovX")/flxInfo.getDouble("camFovY")*
                    bbox.getDouble(3)/bbox.getDouble(2));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        repackMandalaBlendshapeMouth();
        repackMandalaBlendshape();
        makeMandalaVtx();
        prepareGlBuffers();
    }

    public ByteBuffer[] headBB = new ByteBuffer[5];
    public Bitmap[] headBitmaps = new Bitmap[5];
    public List<List<float[]>> mouthPoints;
    private static int[] idxOfInterest = {88,97,50,43,54,46};

    private void prepareGlBuffers(){
//         ===========HEAD PART==============
        mouthPoints = new ArrayList<>();

        for (int i = 0; i < idxOfInterest.length; i++) {
            List<float[]> vtxMandala = new ArrayList<>();
            mouthPoints.add(vtxMandala);
        }

        for (int i = 0; i < 5; i++) {
            ByteBuffer headBlendshapeBB = Data.dataToBuffer(flxData.get("exp0").get("mandalaBlendshapes").get(i));
            headBB[i] = headBlendshapeBB;
            FloatBuffer fb = headBlendshapeBB.asFloatBuffer();
            int c = 0;
            for(int idx:idxOfInterest){
                float[] vtx = { fb.get(idx * 4), fb.get(idx*4 + 1), fb.get(idx*4 + 2), 1f };
                mouthPoints.get(c).add(vtx);
                c+=1;
            }
            headBlendshapeBB.position(0);
        }

        for (int i = 0; i < 5; i++) {
            byte[] imgData = flxData.get("exp0").get("mandalaTextureBlurBkg").get(i);
            InputStream inputStream = new ByteArrayInputStream(imgData);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            headBitmaps[i] = bitmap;
        }
//         ===========MOUTH PART==============
        mouthUvBB = Data.dataToBuffer(mouthData.dict.get("uv"));
        mouthVtxCount = mouthUvBB.capacity() / 4 / 2;
        mouthIdxBB = Data.dataToBuffer(mouthData.dict.get("index"));
        mouthIdxCount = mouthIdxBB.capacity()/2;
        mouthBlendshapeBB = new ByteBuffer[5];
        for (int i = 0; i < 5; i++) {
            mouthBlendshapeBB[i] = Data.dataToBuffer(mouthBlendshapes.get(i));

        }
        float[] lipAnchorsFlat = Data.dataToFloatArray(mouthData.dict.get("lip_anchors"));
        lipAnchors = new float[5][2][2];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k <2; k++) {
                    lipAnchors[i][j][k] = lipAnchorsFlat[i*2*2 + j*2 + k];
                }
            }
        }
        lipSize = Data.dataToFloatArray(mouthData.dict.get("lip_size"));
        float[] teethGapRaw = Data.dataToFloatArray(mouthData.dict.get("teeth_gap"));
        teethGap = new float[2];
        teethGap[0] = 1f - teethGapRaw[1];
        teethGap[1] = 1f - (teethGapRaw[1]+teethGapRaw[3]);

        mouthBitmaps = new Bitmap[5];
        for (int i = 0; i < 5; i++) {
            byte[] imgData = flxData.get("mouth").get("mandalaTexture").get(i);
            InputStream inputStream = new ByteArrayInputStream(imgData);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            mouthBitmaps[i] = bitmap;
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
    private void repackMandalaBlendshapeMouth(){
        byte[] mandalaBlendshapes = mouthData.dict.get("marker_list");
        int blockLength = mandalaBlendshapes.length/5;
        byte[][] repack = new byte[5][blockLength];

        for (int i = 0; i < blockLength/8; i++) {
            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 8; k++) {
                    repack[j][i*8+k] = mandalaBlendshapes[i*40 + j * 8 +k];
                }
            }
        }
        List<byte[]> repackList = new ArrayList<>();
        for (int j = 0; j < 5; j++) {
            repackList.add(repack[j]);
        }
        this.mouthBlendshapes = repackList;
//        Data.printFloatArray(Data.dataToFloatArray(repack[0]));

//                Data.dataToFloatArray()
//        this.mouthData.dict.put("marker_list",repackList);

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

    public static class MouthPivots {
        public float[] topPivot = {0f,0f};
        public float[] botPivot = {0f,0f};
        public float lipSize = 1f;

        public MouthPivots(float[] topPivot, float[] botPivot, float lipSize){
            this.topPivot=topPivot;
            this.botPivot=botPivot;
            this.lipSize=lipSize;
        }
        public MouthPivots(MouthPivots p1,MouthPivots p2,float weight){
            topPivot = GLM.addv2(GLM.mulSv2(p1.topPivot,weight),GLM.mulSv2(p2.topPivot,1f - weight));
            botPivot = GLM.addv2(GLM.mulSv2(p1.botPivot,weight),GLM.mulSv2(p2.botPivot,1f - weight));
            lipSize = p1.lipSize * weight + p2.lipSize * (1f - weight);
        }
    }

    public MouthPivots calcMouthPivots(InterUnit interUnit){
        return calcMouthPivots(this, interUnit);
        /*float[] topPivot = {0f,0f};
        float[] botPivot = {0f,0f};
        float lipSizeLoc = 0f;
        for (int i = 0; i < 3; i++) {
            int idx = interUnit.idx[i];
            float w = interUnit.weights[i];
            topPivot = GLM.addv2(GLM.mulSv2(lipAnchors[idx][0],w),topPivot);
            botPivot = GLM.addv2(GLM.mulSv2(lipAnchors[idx][1],w),botPivot);
            lipSizeLoc += w * lipSize[idx];
        }
        return new MouthPivots(topPivot,botPivot,lipSizeLoc);*/
    }
    public MouthPivots calcMouthPivots(FlexatarData flxData,float weight,InterUnit interUnit){
        MouthPivots p1 = calcMouthPivots(this, interUnit);
        MouthPivots p2 = calcMouthPivots(flxData,interUnit);
        return new MouthPivots(p1,p2,weight);

    }
    public static MouthPivots calcMouthPivots(FlexatarData flxData,InterUnit interUnit){
        float[] topPivot = {0f,0f};
        float[] botPivot = {0f,0f};
        float lipSizeLoc = 0f;
        for (int i = 0; i < 3; i++) {
            int idx = interUnit.idx[i];
            float w = interUnit.weights[i];
            topPivot = GLM.addv2(GLM.mulSv2(flxData.lipAnchors[idx][0],w),topPivot);
            botPivot = GLM.addv2(GLM.mulSv2(flxData.lipAnchors[idx][1],w),botPivot);
            lipSizeLoc += w * flxData.lipSize[idx];
        }
        return new MouthPivots(topPivot,botPivot,lipSizeLoc);
    }
    public List<float[]> calcMouthKeyVtx(InterUnit interUnit, float[] viewModel, float[] zRot, float[] extraRotMat, AnimationUnit animUnit, float screenRatio, float[] speechState){
        List<float[]> keyVtxList = new ArrayList<>();
        int c = 0;
        for (List<float[]> vMan : this.mouthPoints) {

            float[] vtx = this.calcMouthKeyVtx(vMan, interUnit, viewModel, zRot, extraRotMat, animUnit, screenRatio, speechState, c == 2);
            keyVtxList.add(vtx);
//                Log.d("KEY_VtX",Arrays.toString(vtx));
            c += 1;
        }
        return keyVtxList;
    }
    private float[] calcMouthKeyVtx(List<float[]> vtxMandala, InterUnit interUnit, float[] viewModel, float[] zRot, float[] extraRotMat, AnimationUnit animUnit, float screenRatio, float[] speechState, boolean calcSpeech){
        float[] resultVtx = {0f,0f,0f,0f};
        for (int i = 0; i < 3; i++) {
            resultVtx = GLM.add(GLM.mulS(vtxMandala.get(interUnit.idx[i]),interUnit.weights[i]),resultVtx);
//            float[] centerBlendshape = vtxMandala.get(0);
//            float[] w1 = {0.8f,1f,1f,1f};
//            float[] w2 = {0.2f,0.00f,0f,0f};
//            resultVtx = GLM.add(GLM.mul(resultVtx,w1),GLM.mul(centerBlendshape,w2));
        }
        if (calcSpeech) {
            for (int i = 0; i < 5; i++) {
                resultVtx[1] += speechState[i] * FlexatarCommon.speechBspKeyBshp[i] * 0.3f;
            }
        }

        Matrix.multiplyMV(resultVtx,0,extraRotMat,0,resultVtx,0);
        Matrix.multiplyMV(resultVtx,0,viewModel,0,resultVtx,0);
        resultVtx[0] = (float)Math.atan(resultVtx[0]/resultVtx[2]) * 5f;
        resultVtx[1] = (float)Math.atan(resultVtx[1]/resultVtx[2]) * 5f;
        resultVtx[1] -= 4f;
        Matrix.multiplyMV(resultVtx,0,zRot,0,resultVtx,0);
        resultVtx[1] += 4f;
        resultVtx[0] += animUnit.tx;
        resultVtx[1] -= animUnit.ty;
        resultVtx[0] *= 0.8f+animUnit.scale;
        resultVtx[1] *= 0.8f+animUnit.scale;
        resultVtx[1] *= -screenRatio;
        return resultVtx;

    }

    public static byte[] removeMouth(LengthBasedFlxUnpack dataLB){
        String currentPartName = "exp0";
        Data flxData = null;
        for (int i = 0; i < dataLB.hPacks.size(); i++) {
            String headerName = dataLB.hPacks.get(i);
            byte[] headerBody = dataLB.hPacksByte.get(i);
            byte[] body = dataLB.bPacks.get(i);

            if (headerName.equals("Delimiter")) {
                String str = new String(body, StandardCharsets.UTF_8);
                try {
                    JSONObject jsonObject = new JSONObject(str);
                    currentPartName = jsonObject.getString("type");

                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
            if (!currentPartName.equals("mouth") && !currentPartName.equals("end")) {
                if (flxData == null) {
                    flxData = new Data(headerBody);
                    flxData = flxData.encodeLengthHeader().add(flxData);

                }else {
                    Data header = new Data(headerBody);
                    header = header.encodeLengthHeader().add(header);
                    flxData = flxData.add(header);
                }
                Data bodyData = new Data(body);
                bodyData = bodyData.encodeLengthHeader().add(bodyData);
                flxData = flxData.add(bodyData);
            }
        }
        return flxData.value;
    }

}
