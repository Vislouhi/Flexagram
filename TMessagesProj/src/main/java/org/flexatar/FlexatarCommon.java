package org.flexatar;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;

import org.flexatar.DataOps.AssetAccess;
import org.flexatar.DataOps.Data;
import org.flexatar.DataOps.LengthBasedUnpack;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FlexatarCommon {
    static VBO uvVBO;
    static VBO idxVBO;
    static int idxCount;
    static int vtxCount;
    public static List<List<float[]>> flxPatterns = new ArrayList<>();
    public static VBO[] speechBshVBO = new VBO[3];
    public static VBO eyebrowVBO;
    public static float[] blinkPattren;
    public static float[] speechBspKeyBshp;
    public static int mouthLineMaskId;
    public static float[] uvKeyPoint;
    private static Bitmap mouthLineBitmap;
    public static Bitmap blackFrame;
    public static ByteBuffer frameBB;

    public static void prepare(){
        prepareAnimationPatterns();
        prepareEmoAnimation();
        prepareBlinkPattern();
        prepareGlBuffers();
    }


    static void prepareBlinkPattern(){
        blinkPattren = Data.dataToFloatArray(AssetAccess.dataFromFile("flexatar/FLX_blink_pattern.dat"));

    }
    static void prepareAnimationPatterns(){

        LengthBasedUnpack animPacks = new LengthBasedUnpack(AssetAccess.dataFromFile("flexatar/FLX_pattern_collection.dat"));
        for (byte[] pack:animPacks.bPacks) {
            float[] currentPatternFlat = Data.dataToFloatArray(pack);
            List<float[]> currentPattern = new ArrayList<>();
            for (int i = 0; i < currentPatternFlat.length / 10 - 1; i++) {
                currentPattern.add(Arrays.copyOfRange(currentPatternFlat, i*10, (i+1)*10));

            }

            flxPatterns.add(currentPattern);
        }

    }
    public static List<List<float[]>> emoAnimPatterns = new ArrayList<>();
//    public static Map<String,List<float[]>> emoAnimPatterns = new HashMap<>();

    static void prepareEmoAnimation(){
        String[] animIds = {"neu","hap","sup","sad","ang"};
        for (String animId:animIds){
            String fName = "flexatar/FLX_"+animId+".anim";
            float[] currentPatternFlat = Data.dataToFloatArray(AssetAccess.dataFromFile(fName));
            List<float[]> currentPattern = new ArrayList<>();
            for (int i = 0; i < currentPatternFlat.length / 10 - 1; i++) {
                currentPattern.add(Arrays.copyOfRange(currentPatternFlat, i*10, (i+1)*10));

            }

            emoAnimPatterns.add(currentPattern);
        }
    }
    private static byte[][] repackSpeechBlendshape(byte[] data){

        int blockLength = data.length/5;
        byte[][] repack = new byte[3][blockLength*2];

        for (int i = 0; i < blockLength/8; i++) {
            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 8; k++) {
                    if (j == 0) {
                        repack[0][i * 16 + k] = data[i * 40 + j * 8 + k];
                    }
                    if (j == 1) {
                        repack[0][i * 16 + k + 8] = data[i * 40 + j * 8 + k];
                    }
                    if (j == 2) {
                        repack[1][i * 16 + k] = data[i * 40 + j * 8 + k];
                    }
                    if (j == 3) {
                        repack[1][i * 16 + k + 8] = data[i * 40 + j * 8 + k];
                    }
                    if (j == 4) {
                        repack[2][i * 16 + k] = data[i * 40 + j * 8 + k];
                    }
                }
            }
        }
        return repack;
    }
    public static class GlBuffers{
        public final VBO eyebrowVBO;
        public final VBO uvVBO;
        public final VBO idxVBO;
        public final VBO[] speechBshVBO;
        public final int idxCount;
        public final TextureArray mouthLineTexture;
        public final TextureArray frameTexture;
        public final VBO frameVBO;

        public GlBuffers(VBO eyebrowVBO, VBO uvVBO, VBO idxVBO, VBO[] speechBshVBO,int idxCount,TextureArray mouthLineTexture,VBO frameVBO,TextureArray frameTexture) {
            this.eyebrowVBO = eyebrowVBO;
            this.uvVBO = uvVBO;
            this.idxVBO = idxVBO;
            this.speechBshVBO = speechBshVBO;
            this.idxCount=idxCount;
            this.mouthLineTexture=mouthLineTexture;
            this.frameVBO=frameVBO;
            this.frameTexture=frameTexture;

        }
        public void release(){
            eyebrowVBO.destroy();
            uvVBO.destroy();
            idxVBO.destroy();
            for (VBO vbo:speechBshVBO) {
                vbo.destroy();
            }

        }

    }
    private static ByteBuffer eyebrowBB;
    private static ByteBuffer[] speechBshBB = new ByteBuffer[3];
    private static ByteBuffer uvBB;
    private static ByteBuffer idxBB;
    private static void prepareGlBuffers(){
        eyebrowBB = AssetAccess.bufferFromFile("flexatar/FLX_bkg_anim_blendshapes.dat");
        byte[][] speechBshpData = repackSpeechBlendshape(new LengthBasedUnpack(AssetAccess.dataFromFile("flexatar/FLX_speech_bsh.dat")).bPacks.get(0));
        int keyIdx = 50;
        speechBspKeyBshp = new float[5];

        for (int i = 0; i < 3; i++) {
            ByteBuffer speechBshBuffer = Data.dataToBuffer(speechBshpData[i]);
            FloatBuffer speechBshFloat = speechBshBuffer.asFloatBuffer();
            if (i==0){
                speechBspKeyBshp[0] = speechBshFloat.get(keyIdx * 4 + 1);
                speechBspKeyBshp[1] = speechBshFloat.get(keyIdx * 4 + 3);
            }
            if (i==1){
                speechBspKeyBshp[2] = speechBshFloat.get(keyIdx * 4 + 1);
                speechBspKeyBshp[3] = speechBshFloat.get(keyIdx * 4 + 3);
            }
            if (i==2){
                speechBspKeyBshp[4] = speechBshFloat.get(keyIdx * 4 + 1);

            }

            speechBshBuffer.position(0);
            speechBshBB[i] = speechBshBuffer;
        }
        uvBB = AssetAccess.bufferFromFile("flexatar/FLX_mesh_uv.dat");
        idxBB = AssetAccess.bufferFromFile("flexatar/FLX_mesh_idx.dat");
        FloatBuffer uvFB = uvBB.asFloatBuffer();
        int keIdx = 50;
        uvKeyPoint = new float[]{uvFB.get(keIdx * 2), uvFB.get(keIdx * 2 + 1)-0.15f,1f};
        uvBB.position(0);
        byte[] imgData = AssetAccess.dataFromFile("flexatar/FLX_mouth_line_mask.dat");
        InputStream inputStream = new ByteArrayInputStream(imgData);
        mouthLineBitmap = BitmapFactory.decodeStream(inputStream);
        blackFrame = BitmapFactory.decodeStream(new ByteArrayInputStream(AssetAccess.dataFromFile("flexatar/frame.png")));

        frameBB = Data.floatArrayToBytebuffer(new float[]{0,0,0,1,1,1,0,0,1,1,1,0});
    }
    static GlBuffers bufferFactory(){
        VBO eyebrowVBO1 = new VBO(eyebrowBB, GLES20.GL_ARRAY_BUFFER);
        VBO[] speechBshVBO1 = new VBO[3];
        for (int i = 0; i < 3; i++) {
            speechBshVBO1[i] = new VBO(speechBshBB[i], GLES20.GL_ARRAY_BUFFER);
        }

        VBO uvVBO1 = new VBO(uvBB, GLES20.GL_ARRAY_BUFFER);
        VBO idxVBO1 = new VBO(idxBB, GLES20.GL_ELEMENT_ARRAY_BUFFER);
        TextureArray mouthLineTexture = new TextureArray();
        mouthLineTexture.addTexture(mouthLineBitmap);

        TextureArray frameTexture = new TextureArray();
        frameTexture.addTexture(blackFrame);
        VBO frameVBO1 = new VBO(frameBB, GLES20.GL_ARRAY_BUFFER);
        return new GlBuffers(eyebrowVBO1,  uvVBO1, idxVBO1, speechBshVBO1,idxBB.capacity()/2,mouthLineTexture,frameVBO1,frameTexture);

    }
    static void makeBuffers(){

        ByteBuffer eyebrowBuffer = AssetAccess.bufferFromFile("flexatar/FLX_bkg_anim_blendshapes.dat");
        eyebrowVBO = new VBO(eyebrowBuffer, GLES20.GL_ARRAY_BUFFER);

        byte[][] speechBshpData = repackSpeechBlendshape(new LengthBasedUnpack(AssetAccess.dataFromFile("flexatar/FLX_speech_bsh.dat")).bPacks.get(0));



        int keyIdx = 50;
        speechBspKeyBshp = new float[5];
        for (int i = 0; i < 3; i++) {
            ByteBuffer speechBshBuffer = Data.dataToBuffer(speechBshpData[i]);
            FloatBuffer speechBshFloat = speechBshBuffer.asFloatBuffer();
            if (i==0){
                speechBspKeyBshp[0] = speechBshFloat.get(keyIdx * 4 + 1);
                speechBspKeyBshp[1] = speechBshFloat.get(keyIdx * 4 + 3);
            }
            if (i==1){
                speechBspKeyBshp[2] = speechBshFloat.get(keyIdx * 4 + 1);
                speechBspKeyBshp[3] = speechBshFloat.get(keyIdx * 4 + 3);
            }
            if (i==3){
                speechBspKeyBshp[4] = speechBshFloat.get(keyIdx * 4 + 1);

            }

            speechBshBuffer.position(0);
            speechBshVBO[i] = new VBO(speechBshBuffer, GLES20.GL_ARRAY_BUFFER);
        }





        ByteBuffer uvBuffer = AssetAccess.bufferFromFile("flexatar/FLX_mesh_uv.dat");
        uvVBO = new VBO(uvBuffer, GLES20.GL_ARRAY_BUFFER);
        ByteBuffer idxBuffer = AssetAccess.bufferFromFile("flexatar/FLX_mesh_idx.dat");
        idxVBO = new VBO(idxBuffer, GLES20.GL_ELEMENT_ARRAY_BUFFER);
//        vtxCount = 3;
        idxCount = idxBuffer.capacity()/2;
        vtxCount = uvBuffer.capacity()/8;

//        byte[] imgData = AssetAccess.dataFromFile("FLX_mouth_line_mask.dat");
//        InputStream inputStream = new ByteArrayInputStream(imgData);
//        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
//        int[] textureId = new int[1];
//        GLES30.glGenTextures ( 1, textureId, 0 );
//        GLES30.glBindTexture ( GLES30.GL_TEXTURE_2D, textureId[0] );
//
//        GLUtils.texImage2D ( GLES30.GL_TEXTURE_2D, 0, bitmap, 0 );
//
//        GLES30.glTexParameteri ( GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR );
//        GLES30.glTexParameteri ( GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR );
//        GLES30.glTexParameteri ( GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE );
//        GLES30.glTexParameteri ( GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE );
//        mouthLineMaskId = textureId[0];

    }
}
