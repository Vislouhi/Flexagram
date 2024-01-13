package org.flexatar;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.google.android.exoplayer2.util.Log;

import org.flexatar.DataOps.FlexatarData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

public class FlxDrawer {
    private final String PARAMETER_SET_0 = "parSet0";
    private final String PARAMETER_SET_1 = "parSet1";
    private final String PARAMETER_SET_2 = "parSet2";
    private final String PARAMETER_SET_3 = "parSet3";
    private final String HEAD_TEXTURE_SAMPLER = "uSampler";

    public ShaderProgram headProgram;
//    private FlexatarAnimator animator;
    private float[] viewModelMatrix;

    public float screenRatio = 1;
    public float[] speechState = {0,0,-1,0,0};
    public final List<FlexatarData> headQueue = new ArrayList<>();
    private int[] renderFrameBuffer;
    public int[] renderTexture;
    public VBO[] headBuffers = new VBO[5];
    private FlexatarCommon.GlBuffers commonBuffers;
    private TextureArray headTexture;
    private FlexatarData flexatarData;

    public FlxDrawer(){
        Log.d("FLX_INJECT","flx drawer init");
    }
    private void makeViewModelMatrix(){
        float[] transMatrix = new float[16];
        float[] scaleMatrix = new float[16];
        viewModelMatrix = new float[16];
        Matrix.setIdentityM(transMatrix, 0);
        Matrix.translateM(transMatrix, 0, 0.0f, 0f, -2.5f);
        Matrix.setIdentityM(scaleMatrix, 0);
        Matrix.scaleM(scaleMatrix,0,1f,-1f,1f);
        Matrix.multiplyMM(viewModelMatrix, 0, transMatrix, 0, scaleMatrix, 0);
    }
    private void initProgramIfNot(){
        if (headProgram == null) {
            String vCode =
                    "attribute vec4 bshp0;\n" +
                            "attribute vec4 bshp1;\n" +
                            "attribute vec4 bshp2;\n" +
                            "attribute vec4 bshp3;\n" +
                            "attribute vec4 bshp4;\n" +
                            "attribute vec4 speechBuff0;" +
                            "attribute vec4 speechBuff1;" +
                            "attribute vec4 speechBuff2;" +
                            "attribute vec2 uvCoordinates;" +

                            "uniform vec4 parSet0;" +
                            "uniform vec4 parSet1;" +
                            "uniform vec4 parSet2;" +
                            "uniform vec4 parSet3;" +

                            "uniform mat4 vmMatrix;" +
                            "uniform mat4 zRotMatrix;" +

                            "varying highp vec2 uv;" +

                            "void main(void) {\n" +
                            "uv = (uvCoordinates*vec2(1.0,-1.0) + vec2(1.0,1.0))*vec2(0.5,0.5);" +
                            " vec2 speechBshp[5];" +
                            " speechBshp[0] = speechBuff0.xy;" +
                            " speechBshp[1] = speechBuff0.zw;" +
                            " speechBshp[2] = speechBuff1.xy;" +
                            " speechBshp[3] = speechBuff1.zw;" +
                            " speechBshp[4] = speechBuff2.xy;" +

                            "vec4 bshp[5];" +
                            "bshp[0] = bshp0;" +
                            "bshp[1] = bshp1;" +
                            "bshp[2] = bshp2;" +
                            "bshp[3] = bshp3;" +
                            "bshp[4] = bshp4;" +

                            "float weights[5];" +
                            "weights[0] = parSet0.x;" +
                            "weights[1] = parSet0.y;" +
                            "weights[2] = parSet0.z;" +
                            "weights[3] = parSet0.w;" +
                            "weights[4] = parSet1.x;" +

                            "float screenRatio = parSet1.y;" +
                            "float xPos = parSet1.z;" +
                            "float yPos = parSet1.w;" +
                            "float scale = parSet2.x;" +

                            " float speechWeights[5];" +
                            " speechWeights[0] = parSet2.y;" +
                            " speechWeights[1] = parSet2.z;" +
                            " speechWeights[2] = parSet2.w;" +
                            " speechWeights[3] = parSet3.x;" +
                            " speechWeights[4] = parSet3.y;" +

                            "vec4 result = vec4(0);" +
                            "for (int i = 0; i < 5; i++) {" +
                            "    result += weights[i]*bshp[i];" +
                            "}" +

                            "for (int i = 0; i < 5; i++) {" +
                            "    result.xy += speechWeights[i]*speechBshp[i];" +
                            "}" +
                            "result = vmMatrix*result;" +
                            "result.x = atan(result.x/result.z)*5.0;" +
                            "result.y = atan(result.y/result.z)*5.0;" +
                            "result.z = (1.0 - result.z)*0.01 +0.21;" +

                            "result.y -= 4.0;" +
                            "result = zRotMatrix*result;" +
                            "result.y += 4.0;" +

                            "result.x += xPos;" +
                            "result.y -= yPos;" +
                            "result.xy *= 0.8+scale;" +
                            "result.y *= -screenRatio;" +
//                    " result.xy *=0.1;"+
                            "gl_Position = result;" +
                            "}\n";
            String fCode =

                    "varying highp vec2 uv;" +
                            "uniform sampler2D uSampler[5];" +
                            "uniform highp vec4 parSet0;" +
                            "uniform highp vec4 parSet1;" +
                            "void main(void) {" +
                            " highp float weights[5];" +
                            " weights[0] = parSet0.x;" +
                            " weights[1] = parSet0.y;" +
                            " weights[2] = parSet0.z;" +
                            " weights[3] = parSet0.w;" +
                            " weights[4] = parSet1.x;" +
                            " highp vec4 result = vec4(0);" +
                            " for (int i = 0; i < 5; i++) {" +
                            "     result += weights[i]*texture2D(uSampler[i], uv).bgra;" +
                            " }" +
                            " gl_FragColor = result;" +
                            "}";

            headProgram = new ShaderProgram(vCode, fCode);

            headProgram.addUniform4f(PARAMETER_SET_0);
            headProgram.addUniform4f(PARAMETER_SET_1);
            headProgram.addUniform4f(PARAMETER_SET_2);
            headProgram.addUniform4f(PARAMETER_SET_3);

            commonBuffers = FlexatarCommon.bufferFactory();

            for (int i = 0; i < 3; i++) {
                headProgram.attribute("speechBuff" + i, commonBuffers.speechBshVBO[i], 4);
            }
            headProgram.attribute("uvCoordinates", commonBuffers.uvVBO, 2);

            makeViewModelMatrix();
            headProgram.addUniformMatrix4fv("vmMatrix");
            headProgram.addUniformMatrix4fv("zRotMatrix");
            headProgram.use();
            headProgram.uniformMatrix4fv("vmMatrix", viewModelMatrix);
        }
        if (flexatarData != FlexatarRenderer.currentFlxData) {
            releaseFlexatar();
            flexatarData = FlexatarRenderer.currentFlxData;
            FlexatarData flxData = flexatarData;
//            FlexatarData flx = headQueue.get(0);
            for (int i = 0; i < 5; i++) {
                VBO bshpVbo = new VBO(flxData.headBB[i], GLES20.GL_ARRAY_BUFFER);
                headBuffers[i] = bshpVbo;
                headProgram.attribute("bshp" + i, bshpVbo, 4);
            }
            headTexture = new TextureArray();
            for (int i = 0; i < 5; i++) {
//                byte[] imgData = flxData.flxData.get("exp0").get("mandalaTextureBlurBkg").get(i);
                headTexture.addTexture(flxData.headBitmaps[i]);
            }
            headProgram.textureArray(HEAD_TEXTURE_SAMPLER, headTexture, 0);
//            animator = new FlexatarAnimator(flxData);
//            headQueue.clear();

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

//            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, renderFrameBuffer[0]);
//            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, renderTexture[0], 0);

            int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
            if (status == GLES20.GL_FRAMEBUFFER_COMPLETE ){
                Log.d("FlxDrawer","GL_FRAMEBUFFER_COMPLETE");
            }else{
                Log.d("FlxDrawer","GL_FRAMEBUFFER_FAIL");
            }
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);


        }
    }
    public void addHead(FlexatarData flx){
        headQueue.add(flx);
            }

    public int drawToFrameBuffer(){
        FlexatarRenderer.startVoiceProcessingIfNotRunning();
        initProgramIfNot();

        if (renderFrameBuffer!=null && flexatarData!=null) {

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, renderFrameBuffer[0]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, renderTexture[0], 0);

            GLES20.glViewport(0, 0, 512, 512);
//            int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
//            if (status == GLES20.GL_FRAMEBUFFER_COMPLETE ){
//                Log.d("FlxDrawer","GL_FRAMEBUFFER_COMPLETE");
//            }else{
//                Log.d("FlxDrawer","GL_FRAMEBUFFER_FAIL");
//            }
//            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            draw();
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        }
        return renderTexture[0];
    }
    public void draw(){
        initProgramIfNot();
        FlexatarAnimator animator = FlexatarRenderer.animator;
//        if (!animator.isActive){
            animator.start();
//        }
        InterUnit interUnit = animator.getInterUnit(flexatarData);
//        Log.d("FlxDrawer","inter ready");

        if (animator !=null && interUnit != null && animator.animUnit != null) {
//            Log.d("FlxDrawer","draw");
            float[] zRotMatrix = new float[16];
            Matrix.setIdentityM(zRotMatrix, 0);
            Matrix.rotateM(zRotMatrix, 0, -animator.animUnit.rz / 3.14f * 180f, 0f, 0f, 1f);

            headProgram.use();
            headProgram.bind();
            float[] hw = interUnit.weights;
            int[] hi = interUnit.idx;

            float[] hw5 = {0,0,0,0,0};
            for (int i = 0; i < 3; i++) {
                hw5[hi[i]] = hw[i];
            }
            speechState = FlexatarRenderer.speechState;
            headProgram.uniform4f(PARAMETER_SET_0, hw5[0], hw5[1], hw5[2], hw5[3]);
            headProgram.uniform4f(PARAMETER_SET_1, hw5[4], screenRatio, animator.animUnit.tx, animator.animUnit.ty);
            headProgram.uniform4f(PARAMETER_SET_2, animator.animUnit.scale, speechState[0], speechState[1], speechState[2]);
            headProgram.uniform4f(PARAMETER_SET_3, speechState[3], speechState[4], 0, 0);
            headProgram.uniformMatrix4fv("zRotMatrix",zRotMatrix);
            commonBuffers.idxVBO.bind();
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, commonBuffers.idxCount, GLES20.GL_UNSIGNED_SHORT, 0);
            headProgram.unbind();
            commonBuffers.idxVBO.unbind();
        }


    }
    public void releaseFlexatar(){
        if (renderFrameBuffer!=null) {
            GLES20.glDeleteFramebuffers(1, renderFrameBuffer, 0);
        }
        if (renderTexture!=null)
            GLES20.glDeleteTextures(1,renderTexture,0);
        if (headTexture!=null)
            headTexture.release();
        for (int i = 0; i < 5; i++) {
            if ( headBuffers[i]!=null)
                headBuffers[i].destroy();
        }

    }
    public void release(){
        Log.d("FLX_INJECT","flx drawer release");
        if (renderFrameBuffer!=null) {
            System.out.println("framebuffers: " + Arrays.toString(renderFrameBuffer));
            GLES20.glDeleteFramebuffers(1, renderFrameBuffer, 0);
        }
        if (renderTexture!=null)
            GLES20.glDeleteTextures(1,renderTexture,0);
        if (headProgram!=null)
            headProgram.release();
        if (commonBuffers!=null)
            commonBuffers.release();
        if (headTexture!=null)
            headTexture.release();
        for (int i = 0; i < 5; i++) {
            if ( headBuffers[i]!=null)
                headBuffers[i].destroy();
        }

    }
}
