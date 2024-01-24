package org.flexatar;

import android.opengl.GLES20;
import android.opengl.Matrix;

import org.flexatar.DataOps.FlexatarData;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

public class FlxDrawerNew {
    private final String PARAMETER_SET_0 = "parSet0";
    private final String PARAMETER_SET_1 = "parSet1";
    private final String PARAMETER_SET_2 = "parSet2";
    private final String PARAMETER_SET_3 = "parSet3";
    private final String HEAD_TEXTURE_SAMPLER = "uSampler";
    private final String HEAD_TEXTURE_SAMPLER_ALT = "uSampler1";
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


    private static class GlBuffers {
        private VBO[] mouthBuffers = new VBO[5];
        public VBO[] headBuffers = new VBO[5];
        public TextureArray headTexture;
        public VBO mouthUvVbo;
        public VBO mouthIdxVbo;
        public TextureArray mouthTexture;


        public GlBuffers(FlexatarData flxData) {
            //            ======HEAD PART=======
            for (int i = 0; i < 5; i++) {
                VBO bshpVbo = new VBO(flxData.headBB[i], GLES20.GL_ARRAY_BUFFER);
                headBuffers[i] = bshpVbo;

            }
            headTexture = new TextureArray();
            for (int i = 0; i < 5; i++) {
                headTexture.addTexture(flxData.headBitmaps[i]);
            }

//            ======HEAD PART=======
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
                headBuffers[i].destroy();
                mouthBuffers[i].destroy();
            }
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

    private void initFlexatarBuffers() {
        if (buffers1 != null) return;
        buffers1 = new FlxDrawerNew.GlBuffers(flexatarData);
        if (flexatarDataAlt!=null && buffers2 == null)
            buffers2 = new FlxDrawerNew.GlBuffers(flexatarDataAlt);

        if (headProgram != null) {
            for (int i = 0; i < 5; i++) {
                headProgram.attribute("bshp" + i, buffers1.headBuffers[i], 4);
            }

            headProgram.textureArray(HEAD_TEXTURE_SAMPLER, buffers1.headTexture, 0);
        }
        if (headProgramDual != null && buffers2 != null) {
            for (int i = 0; i < 5; i++) {
                headProgramDual.attribute("bshp" + i, buffers1.headBuffers[i], 4);
            }
            for (int i = 0; i < 5; i++) {
                headProgramDual.attribute("bshp" + i + "o", buffers2.headBuffers[i], 4);
            }
            headProgramDual.textureArray(HEAD_TEXTURE_SAMPLER, buffers1.headTexture, 0);
            headProgramDual.textureArray(HEAD_TEXTURE_SAMPLER_ALT, buffers2.headTexture, 5);
            headProgramDual.addUniform1i("effectId");
            headProgramDual.addUniform1f("mixWeight");
        }
        if (mouthProgram != null) {
            for (int i = 0; i < 5; i++) {
                mouthProgram.attribute("bshp" + i, buffers1.mouthBuffers[i], 2);
            }
            mouthProgram.attribute("coordinates", buffers1.mouthUvVbo, 2);
            mouthIdxVbo = buffers1.mouthIdxVbo;
            mouthProgram.textureArray(HEAD_TEXTURE_SAMPLER, buffers1.mouthTexture, 0);
        }
        if (mouthProgramAlt != null && buffers2 != null) {
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

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 512, 512, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
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
    public int drawToFrameBuffer(){
        initFrameBuffer();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, renderFrameBuffer[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, renderTexture[0], 0);

        GLES20.glViewport(0, 0, 512, 512);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        draw();
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        return renderTexture[0];
    }

    private static final Object loadBufferMutex = new Object();
    public void draw() {
        if (isStaticControlBind) {
            if (flexatarData != FlexatarRenderer.currentFlxData) {
                if (flexatarData == null) {
                    flexatarDataAlt = FlexatarRenderer.altFlxData;
                    buffers2 = new FlxDrawerNew.GlBuffers(flexatarDataAlt);
                    flexatarData = FlexatarRenderer.currentFlxData;
                } else {
                    buffers2.destroy();
                    flexatarDataAlt = flexatarData;
                    flexatarData = FlexatarRenderer.currentFlxData;
                    buffers2 = buffers1;
                    buffers1 = null;
                }
            }
            effectID = FlexatarRenderer.effectID;
            mixWeight = FlexatarRenderer.effectsMixWeight;
            isEffectsOn = FlexatarRenderer.isEffectsOn;
            speechState = FlexatarRenderer.speechState;
        }
        synchronized (loadBufferMutex) {
            initCommonBuffers();
            initHeadProgram();
            initHeadEffectsProgram();
            initMouthProgram();
            initMouthAltProgram();
            initFlexatarBuffers();
        }
        FlexatarAnimator animator = FlexatarRenderer.animator;
        animator.start();
        InterUnit interUnit = FlexatarRenderer.animator.getInterUnit(flexatarData);
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
            mouthMix = calcWeightByKeyUv(FlexatarRenderer.effectsMixWeight);
        }




        float opFactor = 0;
//        float opFactor = 0.03f + (-speechState[2] + speechState[3]) * 0.9f;

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
            headProgramDual.uniform4f(PARAMETER_SET_3, speechState[3], speechState[4], 0, 0);
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
            headProgram.uniform4f(PARAMETER_SET_3, speechState[3], speechState[4], 0, 0);
            headProgram.uniformMatrix4fv("zRotMatrix", zRotMatrix);
            headProgram.uniformMatrix4fv("extraRotMatrix", extraRotMat);
            headProgram.uniform1f("opFactor", opFactor);
            commonBuffers.idxVBO.bind();
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, commonBuffers.idxCount, GLES20.GL_UNSIGNED_SHORT, 0);
            headProgram.unbind();
            commonBuffers.idxVBO.unbind();
        }

        GLES20.glDisable(GLES20.GL_BLEND);
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
        mouthProgram.uniform4f(PARAMETER_SET_2, mp.topPivot[0], mp.botPivot[1], mp.botPivot[0], mp.botPivot[1]);

        mouthProgram.uniform1i("isTop", 0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, flexatarData.mouthIdxCount, GLES20.GL_UNSIGNED_SHORT, 0);

        mouthProgram.uniform4f(PARAMETER_SET_1, hw5[4], screenRatio, keyVtxList.get(1)[0], keyVtxList.get(0)[1]);
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
}
