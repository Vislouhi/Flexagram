package org.flexatar;

import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Debug;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;



import org.flexatar.DataOps.FlexatarData;
import org.telegram.messenger.ApplicationLoader;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.microedition.khronos.opengles.GL10;

public class FlxDrawer {
    private final String PARAMETER_SET_0 = "parSet0";
    private final String PARAMETER_SET_1 = "parSet1";
    private final String PARAMETER_SET_2 = "parSet2";
    private final String PARAMETER_SET_3 = "parSet3";
    private final String HEAD_TEXTURE_SAMPLER = "uSampler";
    private final String HEAD_TEXTURE_SAMPLER_ALT = "uSampler1";
    public Runnable onVideoFrameAvailableListener;
    private FlexatarCommon.GlBuffers commonBuffers;
    private float[] viewModelMatrix;
    private ShaderProgram headProgram;
    private ShaderProgram headProgramDual;
    private ShaderProgram mouthProgram;
    private ShaderProgram mouthProgramAlt;
    private FlexatarData flexatarData;
    private GlBuffers buffers1;
    private GlBuffers buffers2;
    private VBO mouthIdxVbo;
    public float screenRatio = 1f;
    private int effectID = 0;
    private boolean isEffectsOn = false;
    private float mixWeight = 1f;
    private FlexatarData flexatarDataAlt;
    private boolean isStaticControlBind = false;
    private int[] renderFrameBuffer;
    public int[] renderTexture;
    private float[] speechState = {0,0,0.05f,0,0};
    private ShaderProgram frameProgram;
    private ShaderProgram frameRoundedProgram;
    private int width = 400;
    private int height = 600;
    private boolean isFrame = false;
    private boolean isEffectsOnVal = false;
    private FlexatarStorageManager.FlexatarChooser flexatarChooser;
    private boolean needUpdateBuffer2;
    private ShaderProgram promoProgram;
    private boolean isPromo = false;
    private boolean isTgRoundVideo = false;
    private ShaderProgram videoProgram;
    private SurfaceTexture surfaceTexture;
    public FlexatarData flxvData;
    private int vidoeoOesTextureid;
    private Surface surface;
    private MediaPlayer mediaPlayer;
    private GlBuffers buffersVideo;
    private ShaderProgram mouthVideoProgram;
    private int flexatarType = -1;
//    public int[] viewport = {0,0,400,600};
    private Handler handler;
    public Handler getHandler(){
        return handler;
    }
    public void setHandler(Handler handler){
        this.handler=handler;
    }
    public FlxDrawer(){
        FlexatarNotificator.incDrawerCounter();

    }
    public void setPromo(){
        isPromo = true;
    }
    public void setRealtimeAnimation(boolean realtimeAnimation) {
        isRealtimeAnimation = realtimeAnimation;
    }

    private boolean isRealtimeAnimation = true;

    public void setFrame(){
        isFrame = true;
    }
    public void setSpeechState(float[] speechState){
        this.speechState=speechState;
    }
    public void setIsStaticControlBind(boolean isStaticControlBind){
        this.isStaticControlBind = isStaticControlBind;
    }

    public void setEffect(boolean isEffectsOn,int effectID){
        this.isEffectsOn=isEffectsOn;
        this.effectID=effectID;
    }
    public void setMixWeight(float mixWeight){
        this.mixWeight=mixWeight;
    }

    public void setHeadRotationAmplitude(float amplitude) {
        flexatarData.setHeadRotationAmplitude(amplitude);
    }

    public FlexatarData getFlexatarData() {
        if (flexatarData == null) return flxvData;
        return flexatarData;
    }

    public void setFlexatarChooser(FlexatarStorageManager.FlexatarChooser flexatarChooser) {
        this.flexatarChooser=flexatarChooser;
        flexatarChooser.subscribe(this);
    }

    public void setTgRoundVideo() {
        isTgRoundVideo = true;
    }


    private static class GlBuffers {
        private VBO eyelidVbo;
        private VBO[] mouthBuffers = new VBO[5];
        public VBO[] headBuffers = new VBO[5];
        public TextureArray headTexture;
        public VBO mouthUvVbo;
        public VBO mouthIdxVbo;
        public TextureArray mouthTexture;


        public GlBuffers(FlexatarData flxData) {
            //            ======HEAD PART=======
            if (flxData.flxDataType == FlexatarData.FlxDataType.PHOTO) {
                for (int i = 0; i < 5; i++) {
                    VBO bshpVbo = new VBO(flxData.headBB[i], GLES20.GL_ARRAY_BUFFER);
                    headBuffers[i] = bshpVbo;

                }
                eyelidVbo = new VBO(flxData.eyelidBlendshape, GLES20.GL_ARRAY_BUFFER);

                headTexture = new TextureArray();
                for (int i = 0; i < 5; i++) {
                    headTexture.addTexture(flxData.headBitmaps[i]);
                }
            }

//            ======MOUTH PART=======
            for (int i = 0; i < 5; i++) {
                VBO bshpVbo = new VBO(flxData.mouthBlendshapeBB[i], GLES20.GL_ARRAY_BUFFER);
                mouthBuffers[i] = bshpVbo;

            }
            mouthUvVbo = new VBO(flxData.mouthUvBB, GLES20.GL_ARRAY_BUFFER);
            mouthIdxVbo = new VBO(flxData.mouthIdxBB, GLES20.GL_ELEMENT_ARRAY_BUFFER);

            mouthTexture = new TextureArray();
            for (int i = 0; i < 5; i++) {
                mouthTexture.addTexture(flxData.mouthBitmaps[i]);
            }

        }

        public void destroy() {
            for (int i = 0; i < 5; i++) {
                if (headBuffers[i]!=null)
                    headBuffers[i].destroy();
                mouthBuffers[i].destroy();
            }
            if (eyelidVbo!=null)
                eyelidVbo.destroy();
            if (headTexture!=null)
                headTexture.release();
            mouthUvVbo.destroy();
            mouthIdxVbo.destroy();
            mouthTexture.release();
        }
    }

    private void makeViewModelMatrix() {
        float[] transMatrix = new float[16];
        float[] scaleMatrix = new float[16];
        viewModelMatrix = new float[16];
        Matrix.setIdentityM(transMatrix, 0);
        Matrix.translateM(transMatrix, 0, 0.0f, 0f, -2.5f);
        Matrix.setIdentityM(scaleMatrix, 0);
        Matrix.scaleM(scaleMatrix, 0, 1f, -1f, 1f);
        Matrix.multiplyMM(viewModelMatrix, 0, transMatrix, 0, scaleMatrix, 0);
    }

    private void initCommonBuffers() {
        if (commonBuffers == null) {
            FlexatarRenderer.speechState = new float[]{0, 0, 0.05f, 0, 0};
            commonBuffers = FlexatarCommon.bufferFactory();
            makeViewModelMatrix();
        }
    }



    private void initFrameProgram(){
        if (frameProgram != null) return;
        frameProgram = new ShaderProgram(ShaderLib.FRAME_VERTEX, ShaderLib.FRAME_FRAGMENT);
        frameProgram.attribute("uv" , commonBuffers.frameVBO, 2);
        frameProgram.textureArray("uSampler", commonBuffers.frameTexture, 0);


    }

    private void initPromoProgram(boolean isRound){
        if (promoProgram != null) return;
        promoProgram = new ShaderProgram(ShaderLib.PROMO_VERTEX, ShaderLib.PROMO_FRAGMENT);
        promoProgram.attribute("uv" , commonBuffers.frameVBO, 2);
        TextureArray frameTexture = new TextureArray();

        frameTexture.addTexture(isRound ? FlexatarCommon.promoRoundLabel:FlexatarCommon.promoLabel);
        promoProgram.textureArray("uSampler", frameTexture, 0);
        promoProgram.addUniform4f("sizePosition");
    }

    private void initFrameRoundedProgram(){
        if (commonBuffers == null) return;
        if (frameRoundedProgram != null) return;
        frameRoundedProgram = new ShaderProgram(ShaderLib.FRAME_VERTEX, ShaderLib.ROUNDED_FRAGMENT);
        frameRoundedProgram.attribute("uv" , commonBuffers.frameVBO, 2);
        commonBuffers.frameTexture.addTexture(renderTexture[0]);
        frameRoundedProgram.textureArray("uSampler", commonBuffers.frameTexture, 0);
//        frameRoundedProgram.uniform4f("sizePosition",1,1,0,0);
    }
    private void initHeadProgram() {
        if (headProgram != null) return;

        headProgram = new ShaderProgram(ShaderLib.HEAD_SINGLE_VERTEX, ShaderLib.HEAD_SINGLE_FRAGMENT);

        headProgram.addUniform4f(PARAMETER_SET_0);
        headProgram.addUniform4f(PARAMETER_SET_1);
        headProgram.addUniform4f(PARAMETER_SET_2);
        headProgram.addUniform4f(PARAMETER_SET_3);

        headProgram.textureArray("mLine", commonBuffers.mouthLineTexture, 10);
        for (int i = 0; i < 3; i++) {
            headProgram.attribute("speechBuff" + i, commonBuffers.speechBshVBO[i], 4);
        }
        headProgram.attribute("uvCoordinates", commonBuffers.uvVBO, 2);
        headProgram.attribute("eyebrowBshp", commonBuffers.eyebrowVBO, 2);


        headProgram.addUniform1f("opFactor");
        headProgram.addUniformMatrix4fv("vmMatrix");
        headProgram.addUniformMatrix4fv("zRotMatrix");
        headProgram.addUniformMatrix4fv("extraRotMatrix");
        headProgram.use();
        headProgram.uniformMatrix4fv("vmMatrix", viewModelMatrix);
    }

    private void initHeadEffectsProgram() {
        if (headProgramDual != null || flexatarDataAlt == null) return;

        headProgramDual = new ShaderProgram(ShaderLib.HEAD_DUAL_VERTEX, ShaderLib.HEAD_DUAL_FRAGMENT);

        headProgramDual.addUniform4f(PARAMETER_SET_0);
        headProgramDual.addUniform4f(PARAMETER_SET_1);
        headProgramDual.addUniform4f(PARAMETER_SET_2);
        headProgramDual.addUniform4f(PARAMETER_SET_3);
        headProgramDual.addUniform1f("opFactor");
        headProgramDual.textureArray("mLine", commonBuffers.mouthLineTexture, 10);

        for (int i = 0; i < 3; i++) {
            headProgramDual.attribute("speechBuff" + i, commonBuffers.speechBshVBO[i], 4);
        }
        headProgramDual.attribute("coordinates", commonBuffers.uvVBO, 2);
        headProgramDual.attribute("eyebrowBshp", commonBuffers.eyebrowVBO, 2);
        headProgramDual.addUniformMatrix4fv("vmMatrix");
        headProgramDual.addUniformMatrix4fv("zRotMatrix");
        headProgramDual.addUniformMatrix4fv("extraRotMatrix");
        headProgramDual.use();
        headProgramDual.uniformMatrix4fv("vmMatrix", viewModelMatrix);
    }

    private void initMouthProgram() {
        if (mouthProgram != null) return;
        mouthProgram = mouthProgramFactory();
    }
    private void initMouthVideoProgram() {
        if (mouthVideoProgram != null) return;
        mouthVideoProgram = mouthProgramFactory();
    }

    private void initMouthAltProgram() {
        if (mouthProgramAlt != null || flexatarDataAlt == null) return;
        mouthProgramAlt = mouthProgramFactory();
    }

    public void setFlexatarData(FlexatarData flexatarData) {
        this.flexatarData = flexatarData;
    }

    public void setFlexatarDataAlt(FlexatarData flexatarDataAlt) {
        this.flexatarDataAlt = flexatarDataAlt;
    }
    private Runnable onReadyListener = null;
    public void setOnReadyListener(Runnable listener){
        onReadyListener = listener;
    }
    private void initFlexatarVideoBuffers() {
        if (buffersVideo != null) return;
        Log.d("FLX_INJECT","initFlexatarVideoBuffers");
        buffersVideo = new FlxDrawer.GlBuffers(flxvData);

        if (mouthVideoProgram != null) {
            for (int i = 0; i < 5; i++) {
                mouthVideoProgram.attribute("bshp" + i, buffersVideo.mouthBuffers[i], 2);
            }
            mouthVideoProgram.attribute("coordinates", buffersVideo.mouthUvVbo, 2);
            mouthIdxVbo = buffersVideo.mouthIdxVbo;
            mouthVideoProgram.textureArray(HEAD_TEXTURE_SAMPLER, buffersVideo.mouthTexture, 0);
        }
        if (onReadyListener!=null) onReadyListener.run();
    }
    private void initFlexatarBuffers1() {
        if (buffers1 != null) return;
        Log.d("FLX_INJECT","initFlexatarBuffers1");
        buffers1 = new FlxDrawer.GlBuffers(flexatarData);
        if (headProgram != null) {
            for (int i = 0; i < 5; i++) {
                headProgram.attribute("bshp" + i, buffers1.headBuffers[i], 4);
            }
            headProgram.attribute("blinkBshp", buffers1.eyelidVbo, 2);
            headProgram.textureArray(HEAD_TEXTURE_SAMPLER, buffers1.headTexture, 0);
        }
        if (mouthProgram != null) {
            for (int i = 0; i < 5; i++) {
                mouthProgram.attribute("bshp" + i, buffers1.mouthBuffers[i], 2);
            }
            mouthProgram.attribute("coordinates", buffers1.mouthUvVbo, 2);
            mouthIdxVbo = buffers1.mouthIdxVbo;
            mouthProgram.textureArray(HEAD_TEXTURE_SAMPLER, buffers1.mouthTexture, 0);
        }
        if (onReadyListener!=null) onReadyListener.run();
    }
    private void initFlexatarBuffers2() {
        if ((flexatarDataAlt == null || buffers2 != null) && !needUpdateBuffer2) return;
        needUpdateBuffer2=false;
        if (flexatarDataAlt == null) return;
        Log.d("FLX_INJECT","initFlexatarBuffers2");
        if  (buffers2 == null) buffers2 = new FlxDrawer.GlBuffers(flexatarDataAlt);
        if (headProgramDual != null) {
            for (int i = 0; i < 5; i++) {
                headProgramDual.attribute("bshp" + i, buffers1.headBuffers[i], 4);
            }
            headProgramDual.attribute("blinkBshp", buffers1.eyelidVbo, 2);
            for (int i = 0; i < 5; i++) {
                headProgramDual.attribute("bshp" + i + "o", buffers2.headBuffers[i], 4);
            }
            headProgramDual.textureArray(HEAD_TEXTURE_SAMPLER, buffers1.headTexture, 0);
            headProgramDual.textureArray(HEAD_TEXTURE_SAMPLER_ALT, buffers2.headTexture, 5);
            headProgramDual.addUniform1i("effectId");
            headProgramDual.addUniform1f("mixWeight");
        }
        if (mouthProgramAlt != null) {
            for (int i = 0; i < 5; i++) {
                mouthProgramAlt.attribute("bshp" + i, buffers2.mouthBuffers[i], 2);
            }
            mouthProgramAlt.attribute("coordinates", buffers2.mouthUvVbo, 2);

            mouthProgramAlt.textureArray(HEAD_TEXTURE_SAMPLER, buffers2.mouthTexture, 0);
        }
    }

    public void initFrameBuffer(){
        if (renderFrameBuffer != null) return;
        renderFrameBuffer = new int[1];
        renderTexture = new int[1];
        GLES20.glGenFramebuffers(1, renderFrameBuffer, 0);
        GLES20.glGenTextures(1, renderTexture, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, renderTexture[0]);
        GLES20.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 400, 600, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status == GLES20.GL_FRAMEBUFFER_COMPLETE) {
            android.util.Log.d("FlxDrawer", "GL_FRAMEBUFFER_COMPLETE");
        } else {
            android.util.Log.d("FlxDrawer", "GL_FRAMEBUFFER_FAIL");
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private ShaderProgram mouthProgramFactory() {
        org.flexatar.ShaderProgram mouthProgramAlt = new ShaderProgram(ShaderLib.MOUTH_SINGLE_VERTEX, ShaderLib.MOUTH_SINGLE_FRAGMENT);

        mouthProgramAlt.addUniform4f(PARAMETER_SET_0);
        mouthProgramAlt.addUniform4f(PARAMETER_SET_1);
        mouthProgramAlt.addUniform4f(PARAMETER_SET_2);
        mouthProgramAlt.addUniform4f(PARAMETER_SET_3);
        mouthProgramAlt.addUniformMatrix4fv("zRotMatrix");
        mouthProgramAlt.addUniform1i("isTop");
        mouthProgramAlt.addUniform1f("alpha");
        return mouthProgramAlt;
    }
    public  void drawRounded(){
        drawToFrameBuffer();
        initFrameRoundedProgram();
        if (frameRoundedProgram == null) return;
        GLES20.glViewport(0, 0, width, height);
        frameRoundedProgram.use();
        frameRoundedProgram.bind();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        frameRoundedProgram.unbind();
    }
    public void setSize(int width,int height){
        this.width=width;
        this.height=height;
    }
    public void prepareVideoTextures(){
        initCommonBuffers();
        initVideoTexture();

        if (videoToTextureArray != null) {

            videoToTextureArray.draw();
        }
//        checkMemoryAvailable();
    }
    public int drawToFrameBuffer(){
//        drawVideo();

        initCommonBuffers();
        if (flexatarType == 0) {
            initVideoTexture();
            if (videoToTextureArray == null) return 0;
            videoToTextureArray.draw();

        }
        initFrameBuffer();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, renderFrameBuffer[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, renderTexture[0], 0);



        GLES20.glViewport(0, 0, 400, 600);
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        draw(true);
//        GLES20.glFinish();
//        drawVideo();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);


        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            Log.e("FLX_INJECT", "Error openg: " + error);
        }
        return renderTexture[0];
    }

    private void checkMemoryAvailable(){
        IntBuffer textureLimit = IntBuffer.allocate(1);
        GLES20.glGetIntegerv(GL10.GL_MAX_TEXTURE_SIZE, textureLimit);
        int mTextureLimit = textureLimit.get(0);
        Log.d("FLX_INJECT","mTextureLimit "+mTextureLimit);
    }
    private static final Object loadBufferMutex = new Object();

    private FlexatarData changeFlexatar = null;
    public void changeFlexatar(FlexatarData flxData){
        changeFlexatar = flxData;
    }

    private int effectIdVal = 0;
    private float mixWeightVal = 1f;
    public void setEffectIdVal(int effectID){
        effectIdVal = effectID;
    }
    public void setMixWeightVal(float val){
        mixWeightVal = val;
    }
    public void setisEffectOnVal(boolean val){
        isEffectsOnVal = val;
    }
    FlexatarAnimator builtinAnimator = new FlexatarAnimator();
    public static class RenderParams{
        public FlexatarData flexatarDataVideo;
        public FlexatarData flexatarData;
        public FlexatarData flexatarDataAlt;
        public float mixWeight;
        public int effectID;
        public boolean isEffectsOn;
        public int flexatarType = -1;
    }
    public static class GroupMorphState{
        public final int changeDelta = 25*4;
        public final int morphDelta = 25;
        public boolean morphStage = false;
        public int counter = 0;
        public int morphCounter = 0;
        public int flexatarCounter = 0;
        public float mixWeight = 1;
        public int effectID = 0;
        public boolean isEffectsOn = false;
        public FlexatarData flexatarData;
        public FlexatarData flexatarDataAlt;

    }
    public interface OnFrameStart{
        RenderParams onFrameStart();
    }
    public AtomicReference<OnFrameStart> onFrameStartListener = new AtomicReference<>();
    public void draw(){
        draw(false);
    }
    public void draw(boolean isFromFb) {

//        GLES31.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        if (isStaticControlBind) {
            if (!FlexatarRenderer.isFlexatarCamera) return;

            speechState = FlexatarRenderer.speechState;
        }


        if (onFrameStartListener.get()!=null){
            RenderParams renderParams = onFrameStartListener.get().onFrameStart();
            mixWeight = renderParams.mixWeight;
            effectID = renderParams.effectID;
            isEffectsOn = renderParams.isEffectsOn;
            flexatarType = renderParams.flexatarType;
            if (flexatarDataAlt!=renderParams.flexatarDataAlt){
                if (buffers2!=null) buffers2.destroy();
                if (renderParams.flexatarDataAlt == flexatarData){

                    buffers2 = buffers1;
                }else{
                    buffers2 = null;
                }
                needUpdateBuffer2 = true;

            }
            if (flexatarData!=renderParams.flexatarData){
                buffers1 = null;
            }
            flexatarData = renderParams.flexatarData;
            flexatarDataAlt = renderParams.flexatarDataAlt;

            if (flxvData != renderParams.flexatarDataVideo){
                if (videoToTextureArray!=null) videoToTextureArray.release();
                videoToTextureArray=null;
                if (buffersVideo!=null)buffersVideo.destroy();
                buffersVideo = null;
            }
            flxvData = renderParams.flexatarDataVideo;


        }else {return;}
//        if (true) return;
        if (isFromFb)
            GLES20.glViewport(0, 0, 400, 600);
        else
            GLES20.glViewport(0, 0, width, height);
        if (flexatarType == 0) {
            drawVideo();
        }else if (flexatarType == 1) {

//        Log.d("FLX_INJECT","screen ratio "+screenRatio);
            if (flexatarData == null) return;
//        Log.d("FLX_INJECT", "mixWeight "+mixWeight);
            synchronized (loadBufferMutex) {
                initCommonBuffers();
                initFrameProgram();
                initHeadProgram();
                initHeadEffectsProgram();
                initMouthProgram();
                initMouthAltProgram();
                initFlexatarBuffers1();
                initFlexatarBuffers2();
//            ExternalVideoTexture.startVideoTexture();

//            initVideoTexture();
//            initVideoProgram();
            }

            FlexatarAnimator animator = isStaticControlBind ? FlexatarRenderer.animator : builtinAnimator;
            if (isTgRoundVideo) {
                animator.headScale = -0.15f;
            } else {
                animator.headScale = 0f;
            }
            if (isRealtimeAnimation) {

                animator.start();
            } else {
                animator.next();
            }

//        Log.d("FLX_INJECT", "drawer loop");
            InterUnit interUnit = animator.getInterUnit(flexatarData);
            if (interUnit == null || buffers1 == null || animator.animUnit == null) return;

            float[] zRotMatrix = new float[16];
            Matrix.setIdentityM(zRotMatrix, 0);
            float ang = animator.animUnit.rz / 3.14f * 180f;
            Matrix.rotateM(zRotMatrix, 0, -ang, 0f, 0f, 1f);

            float[] zRotMatrixInv = new float[16];
            Matrix.setIdentityM(zRotMatrixInv, 0);
            Matrix.rotateM(zRotMatrixInv, 0, ang, 0f, 0f, 1f);
            float[] extraRotMat = interUnit.calcExtraMatrix();

            FlexatarData.MouthPivots[] mouthPivots = new FlexatarData.MouthPivots[2];
            mouthPivots[0] = flexatarData.calcMouthPivots(interUnit);

            float mouthMix = mixWeight;
            if (effectID == 1) {
                mouthMix = calcWeightByKeyUv(mixWeight);
            }


//        float opFactor = 0;
            float opFactor = 0.03f + (-speechState[2] + speechState[3]) * 0.5f;
            if (opFactor < 0) opFactor = 0;
            else if (opFactor > 1) opFactor = 1;
//        Log.d("FLX_INJECT","opFactor "+opFactor);
            List<float[]> keyVtxList;
            if (isEffectsOn) {
                List<float[]> keyVtxList1 = flexatarData.calcMouthKeyVtx(interUnit, viewModelMatrix, zRotMatrix, extraRotMat, animator.animUnit, screenRatio, speechState);
                List<float[]> keyVtxList2 = flexatarDataAlt.calcMouthKeyVtx(interUnit, viewModelMatrix, zRotMatrix, extraRotMat, animator.animUnit, screenRatio, speechState);
                keyVtxList = keyVtxWeightedSum(keyVtxList1, keyVtxList2, mouthMix);
                mouthPivots[1] = flexatarDataAlt.calcMouthPivots(interUnit);
            } else {
                keyVtxList = flexatarData.calcMouthKeyVtx(interUnit, viewModelMatrix, zRotMatrix, extraRotMat, animator.animUnit, screenRatio, speechState);
            }

            float[] hw = interUnit.weights;
            int[] hi = interUnit.idx;

            float[] hw5 = {0, 0, 0, 0, 0};
            for (int i = 0; i < 3; i++) {
                hw5[hi[i]] = hw[i];
            }

            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

            drawMouth(mouthProgram, flexatarData, mouthPivots[0], hw5, keyVtxList, zRotMatrixInv, 1f);
            if (isEffectsOn) {

                drawMouth(mouthProgramAlt, flexatarDataAlt, mouthPivots[1], hw5, keyVtxList, zRotMatrixInv,
                        1f - mouthMix);
            }
            if (isEffectsOn) {
                headProgramDual.use();
                headProgramDual.bind();
                headProgramDual.uniform4f(PARAMETER_SET_0, hw5[0], hw5[1], hw5[2], hw5[3]);
                headProgramDual.uniform4f(PARAMETER_SET_1, hw5[4], screenRatio, animator.animUnit.tx, animator.animUnit.ty);
                headProgramDual.uniform4f(PARAMETER_SET_2, animator.animUnit.scale, speechState[0], speechState[1], speechState[2]);
                headProgramDual.uniform4f(PARAMETER_SET_3, speechState[3], speechState[4], animator.animUnit.eyebrow, animator.animUnit.blink);
                headProgramDual.uniform1i("effectId", effectID);
                headProgramDual.uniform1f("mixWeight", mixWeight);
                headProgramDual.uniform1f("opFactor", opFactor);
                headProgramDual.uniformMatrix4fv("zRotMatrix", zRotMatrix);
                headProgramDual.uniformMatrix4fv("extraRotMatrix", extraRotMat);
                commonBuffers.idxVBO.bind();
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, commonBuffers.idxCount, GLES20.GL_UNSIGNED_SHORT, 0);
                headProgramDual.unbind();
                commonBuffers.idxVBO.unbind();
            } else {
                headProgram.use();
                headProgram.bind();

                headProgram.uniform4f(PARAMETER_SET_0, hw5[0], hw5[1], hw5[2], hw5[3]);
                headProgram.uniform4f(PARAMETER_SET_1, hw5[4], screenRatio, animator.animUnit.tx, animator.animUnit.ty);
                headProgram.uniform4f(PARAMETER_SET_2, animator.animUnit.scale, speechState[0], speechState[1], speechState[2]);
//            headProgram.uniform4f(PARAMETER_SET_3, speechState[3], speechState[4], 1, 1);
                headProgram.uniform4f(PARAMETER_SET_3, speechState[3], speechState[4], animator.animUnit.eyebrow, animator.animUnit.blink);
                headProgram.uniformMatrix4fv("zRotMatrix", zRotMatrix);
                headProgram.uniformMatrix4fv("extraRotMatrix", extraRotMat);
                headProgram.uniform1f("opFactor", opFactor);
                commonBuffers.idxVBO.bind();
                GLES20.glDrawElements(GLES20.GL_TRIANGLES, commonBuffers.idxCount, GLES20.GL_UNSIGNED_SHORT, 0);
                headProgram.unbind();
                commonBuffers.idxVBO.unbind();
            }
       /* drawMouth(mouthProgram, flexatarData, mouthPivots[0], hw5, keyVtxList, zRotMatrixInv, 1f);
        if (isEffectsOn) {

            drawMouth(mouthProgramAlt, flexatarDataAlt, mouthPivots[1], hw5, keyVtxList, zRotMatrixInv,
                    1f - mouthMix);
        }*/
            if (isFrame) {
                frameProgram.use();
                frameProgram.bind();
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
                frameProgram.unbind();
            }

            drawPromo();
        }
//        drawVideo();
        GLES20.glDisable(GLES20.GL_BLEND);
    }
    public void drawPromo(){
        if (isPromo) {
            initPromoProgram(isTgRoundVideo);
            promoProgram.use();
            promoProgram.bind();
            if (isTgRoundVideo) {
                float width = 0.4f;
                float height = width / FlexatarCommon.promoRoundRatio;

                promoProgram.uniform4f("sizePosition", width, height * screenRatio, (1f - width) / 2f, 0.8f);
            }else{
                float width = 0.12f;
                float height = width / FlexatarCommon.promoRatio;

                promoProgram.uniform4f("sizePosition", width, height * screenRatio, 0.02f, 0.02f);
            }
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
            promoProgram.unbind();

        }
    }


    private void drawMouth(ShaderProgram mouthProgram, FlexatarData flexatarData, FlexatarData.MouthPivots mp, float[] hw5, List<float[]> keyVtxList, float[] zRotMatrixInv, float alpha) {
        mouthProgram.use();
        mouthProgram.bind();
        mouthIdxVbo.bind();
        float mouthScale = 0.9f * (keyVtxList.get(5)[0] - keyVtxList.get(4)[0]) / mp.lipSize;
        mouthProgram.uniform4f(PARAMETER_SET_3, flexatarData.mouthRatio, mouthScale, flexatarData.teethGap[0], flexatarData.teethGap[1]);
        mouthProgram.uniform4f(PARAMETER_SET_0, hw5[0], hw5[1], hw5[2], hw5[3]);
        mouthProgram.uniform4f(PARAMETER_SET_1, hw5[4], screenRatio, keyVtxList.get(3)[0], keyVtxList.get(2)[1]);
        mouthProgram.uniformMatrix4fv("zRotMatrix", zRotMatrixInv);
        mouthProgram.uniform1f("alpha", alpha);
        mouthProgram.uniform4f(PARAMETER_SET_2, mp.botPivot[0], mp.botPivot[1], mp.botPivot[0], mp.botPivot[1]);

        mouthProgram.uniform1i("isTop", 0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, flexatarData.mouthIdxCount, GLES20.GL_UNSIGNED_SHORT, 0);

        mouthProgram.uniform4f(PARAMETER_SET_1, hw5[4], screenRatio, keyVtxList.get(1)[0], keyVtxList.get(0)[1]);
        mouthProgram.uniform4f(PARAMETER_SET_2, mp.topPivot[0], mp.topPivot[1], mp.botPivot[0], mp.botPivot[1]);
        mouthProgram.uniform1i("isTop", 1);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, flexatarData.mouthIdxCount, GLES20.GL_UNSIGNED_SHORT, 0);
        mouthProgram.unbind();
        mouthIdxVbo.unbind();
    }

    private void drawMouthV(ShaderProgram mouthProgram, FlexatarData flexatarData, FlexatarData.MouthPivots mp, float[] hw5, List<float[]> keyVtxList, float[] zRotMatrixInv, float alpha) {
        mouthProgram.use();
        mouthProgram.bind();
        mouthIdxVbo.bind();
        float mouthScale = - (keyVtxList.get(5)[0] - keyVtxList.get(4)[0]) / mp.lipSize;
        mouthProgram.uniform4f(PARAMETER_SET_3, flexatarData.mouthRatio, mouthScale, flexatarData.teethGap[0], flexatarData.teethGap[1]);
        mouthProgram.uniform4f(PARAMETER_SET_0, hw5[0], hw5[1], hw5[2], hw5[3]);
        mouthProgram.uniform4f(PARAMETER_SET_1, hw5[4], videoScreenRatio, keyVtxList.get(3)[0], keyVtxList.get(2)[1]);
        mouthProgram.uniformMatrix4fv("zRotMatrix", zRotMatrixInv);
        mouthProgram.uniform1f("alpha", alpha);
        mouthProgram.uniform4f(PARAMETER_SET_2, mp.botPivot[0], mp.botPivot[1], mp.botPivot[0], mp.botPivot[1]);

        mouthProgram.uniform1i("isTop", 0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, flexatarData.mouthIdxCount, GLES20.GL_UNSIGNED_SHORT, 0);

        mouthProgram.uniform4f(PARAMETER_SET_1, hw5[4], videoScreenRatio, keyVtxList.get(1)[0], keyVtxList.get(0)[1]);
        mouthProgram.uniform4f(PARAMETER_SET_2, mp.topPivot[0], mp.topPivot[1], mp.botPivot[0], mp.botPivot[1]);
        mouthProgram.uniform1i("isTop", 1);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, flexatarData.mouthIdxCount, GLES20.GL_UNSIGNED_SHORT, 0);
        mouthProgram.unbind();
        mouthIdxVbo.unbind();
    }

    private float calcWeightByKeyUv(float mixWeight) {
        double theta = mixWeight * 6.28;
        float[] linePoint = {(float) Math.sin(theta), (float) Math.cos(theta), 1.0f};
        float[] curPoint = FlexatarCommon.uvKeyPoint;
        float w = GLM.dotv3(GLM.cross(linePoint, curPoint), new float[]{0, 0, 1}) / 0.15f;
        if (w < -1.0) w = -1;
        if (w > 1.0) w = 1;
        w += 1.0;
        w /= 2.0;
        return w;
    }

    private List<float[]> keyVtxWeightedSum(List<float[]> kv1, List<float[]> kv2, float weight) {
        List<float[]> keyVtxList = new ArrayList<>();
        for (int i = 0; i < kv1.size(); i++) {
            keyVtxList.add(
                    GLM.add(GLM.mulS(kv1.get(i), weight), GLM.mulS(kv2.get(i), 1f - weight))
            );
        }
        return keyVtxList;

    }

    private void initVideoProgram(){
        if (videoProgram != null) return;
//        flxvData = new FlexatarData(new LengthBasedFlxUnpack(AssetAccess.dataFromFile("flexatar/test_flx.v")));
        videoProgram = new ShaderProgram(ShaderLib.VIDEO_VERTEX, ShaderLib.VIDEO_FRAGMENT);
        videoProgram.addUniform4f(PARAMETER_SET_0);
        videoProgram.addUniform4f(PARAMETER_SET_1);
        videoProgram.addUniform1f("opFactor");
        videoProgram.textureArray("mLine", commonBuffers.mouthLineTexture, 10);
    }

    public VideoToTextureArray videoToTextureArray;
    private float videoScreenRatio = 1f;
    private void initVideoTexture(){
        if (videoToTextureArray!=null || flxvData == null ) return;
//        Log.d("FLX_INJECT","initVideoTexture");
        videoToTextureArray = new VideoToTextureArray(commonBuffers,flxvData.getVideo());
        videoToTextureArray.onFrameAvailableListener = onVideoFrameAvailableListener;
        videoToTextureArray.getNextFrame();
        videoScreenRatio = screenRatio/((float)videoToTextureArray.saveWidth/videoToTextureArray.saveHeight);
//        videoScreenRatio = (400f/600f)/((float)videoToTextureArray.saveWidth/videoToTextureArray.saveHeight);
//        videoScreenRatio = (600f/400f)/((float)videoToTextureArray.saveHeight/videoToTextureArray.saveWidth);
    }
    private void drawVideo(){
        if (videoToTextureArray == null || flxvData == null) return;
        synchronized (loadBufferMutex) {
//            initFrameBuffer();

            initVideoProgram();
            initMouthVideoProgram();
            initFlexatarVideoBuffers();
            initFrameProgram();
        }


        int videoTextureId = videoToTextureArray.getVideoTexId();
//        Log.d("FLX_INJECT","videoTextureId "+videoTextureId);
//        if (videoTextureId>=0) renderTexture[0] = videoTextureId;
//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, renderFrameBuffer[0]);
//        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, renderTexture[0], 0);
//        GLES20.glViewport(0, 0, 400, 600);
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
//        Log.d("FLX_INJECT","draw video frame"+videoTextureId);
        if (videoTextureId>=0) {

            if (isStaticControlBind) {
                speechState = FlexatarRenderer.speechState;
            }
            float opFactor = 0.03f + (-speechState[2] + speechState[3]) * 0.5f;
            if (opFactor<0) opFactor =0; else if (opFactor>1) opFactor = 1;
//            if (opFactor>0.1f)
//                opFactor=1f;
            float zRot = flxvData.getHeadRotZVideo(videoToTextureArray.currentTextureIdx);

//            InterUnit interUnit = new InterUnit(new float[]{1f, 0.0f, 0.0f}, new int[]{0, 1, 2}, new float[]{0, 0});
            InterUnit interUnit = flxvData.makeVideoInterUnit(videoToTextureArray.currentTextureIdx);
            FlexatarData.MouthPivots mouthPivots = flxvData.calcMouthPivots(interUnit);
            float[] hw = interUnit.weights;
            int[] hi = interUnit.idx;

            float[] hw5 = {0, 0, 0, 0, 0};
            for (int i = 0; i < 3; i++) {
                hw5[hi[i]] = hw[i];
            }
            float[] zRotMatrixInv = new float[16];
            Matrix.setIdentityM(zRotMatrixInv, 0);
            Matrix.rotateM(zRotMatrixInv, 0, -zRot/3.14f*150f, 0f, 0f, 1f);
            List<float[]> keyVtxList = flxvData.calcMouthKeyVtxVideo(videoToTextureArray.currentTextureIdx, videoScreenRatio,speechState);

            drawMouthV(mouthVideoProgram,flxvData,mouthPivots,hw5,keyVtxList,zRotMatrixInv,1f);

    //        speechState = new float[]{0f,0f,-1f,0f,0f};
            videoProgram.use();
            videoProgram.bind();
            int positionHandle = GLES20.glGetAttribLocation(videoProgram.id, "uv");
            flxvData.videoMarkerBB.position(FlexatarCommon.videoStride*videoToTextureArray.currentTextureIdx);
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(
                    positionHandle,
                    2,
                    GLES20.GL_FLOAT,
                    false,
                    0,
                    flxvData.videoMarkerBB);
            int positionHandle2 = GLES20.glGetAttribLocation(videoProgram.id, "uvCoordinates");
            GLES20.glEnableVertexAttribArray(positionHandle2);
            GLES20.glVertexAttribPointer(
                    positionHandle2,
                    2,
                    GLES20.GL_FLOAT,
                    false,
                    0,
                    FlexatarCommon.uvBB);

            for (int i = 0; i < 3; i++) {
                int positionHandle1 = GLES20.glGetAttribLocation(videoProgram.id, "speechBuff"+i);
                GLES20.glEnableVertexAttribArray(positionHandle1);
                GLES20.glVertexAttribPointer(
                        positionHandle1,
                        4,
                        GLES20.GL_FLOAT,
                        false,
                        0,
                        FlexatarCommon.speechBshBB[i]);
            }
            float mouthScale =  -(keyVtxList.get(5)[0] - keyVtxList.get(4)[0]);

            videoProgram.uniform4f(PARAMETER_SET_0,  speechState[0], speechState[1], speechState[2], speechState[3]);
            videoProgram.uniform4f(PARAMETER_SET_1, speechState[4], videoScreenRatio, mouthScale, 0);
            videoProgram.uniform1f("opFactor",opFactor);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, videoTextureId);
            int videoTextureHandle = GLES20.glGetUniformLocation(videoProgram.id, "uSampler");
            GLES20.glUniform1i(videoTextureHandle, 0);
            commonBuffers.idxVBO.bind();
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, commonBuffers.idxCount, GLES20.GL_UNSIGNED_SHORT, 0);

            commonBuffers.idxVBO.unbind();
            videoProgram.unbind();
//            drawMouthV(mouthVideoProgram,flxvData,mouthPivots,hw5,keyVtxList,zRotMatrixInv,1f);
            if (isFrame) {
                frameProgram.use();
                frameProgram.bind();
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
                frameProgram.unbind();
            }
            drawPromo();
        }
        GLES20.glDisable(GLES20.GL_BLEND);
//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }
    public void releaseHeadBuffers(){
        if (buffers1!=null){
            buffers1.destroy();
            buffers1 = null;
        }
        if (buffers2!=null){
            buffers2.destroy();
            buffers2 = null;
        }
    }
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        FlexatarRenderer.animator.release();
        builtinAnimator.release();
        FlexatarNotificator.decDrawerCounter();
    }
}
