package org.flexatar;

public class ShaderLib {
    public static final String FRAME_VERTEX =
                    "attribute vec2 uv;\n" +
                    "varying highp vec2 UV;" +
                    "void main(void) {\n" +

                        "UV = uv;" +
                        "gl_Position = vec4(uv *vec2(2.0,2.0)-vec2(1.0,1.0),0.0,1.0);" +
                    "}\n";

    public static final String TEX_ROT_VERTEX =
            "attribute vec2 uv;\n" +
                    "varying highp vec2 UV;" +
                    "uniform mat4 uvRot;" +
                    "void main(void) {\n" +

                    "vec2 tmp = uv-vec2(0.5,0.5);" +
                    "tmp = (uvRot*vec4(tmp,0.0,1.0)).xy;" +
                    "tmp += vec2(0.5,0.5);" +
                    "UV = tmp;" +
//                    "UV = (uvRot*vec4(uv,0.0,1.0)).xy;" +
                    "gl_Position = vec4(uv *vec2(2.0,2.0)-vec2(1.0,1.0),0.0,1.0);" +
                    "}\n";
    public static final String FRAME_FRAGMENT =
                    "varying highp vec2 UV;" +
                    "uniform sampler2D uSampler[1];" +


                    "void main(void) {" +
                    "highp float alpha = texture2D(uSampler[0], UV).a;"+
//                    "highp float alpha_inv = 1.0-alpha;"+
//                            "if (alpha>0.5) discard;"+
                    " gl_FragColor = vec4(0.0,0.0,0.0,alpha);" +
                    "}";
    public static final String ROUNDED_FRAGMENT =
            "varying highp vec2 UV;" +
                    "uniform sampler2D uSampler[2];" +


                    "void main(void) {" +
                    "highp float alpha = texture2D(uSampler[0], UV).a;"+
                    "highp vec4 color = texture2D(uSampler[1], UV);"+
//                    "highp float alpha_inv = 1.0-alpha;"+
//                            "if (alpha>0.5) discard;"+
//                    " gl_FragColor = vec4(color.xyz,1.0-alpha);" +
                    " gl_FragColor = color*(1.0-alpha);" +
                    "}";
    public static final String PROMO_VERTEX =
            "attribute vec2 uv;\n" +
                    "varying highp vec2 UV;" +
                    "uniform vec4 sizePosition;" +
                    "void main(void) {\n" +

                    "UV = uv;" +
                    "gl_Position = vec4(vec2(1.0,-1.0)*((uv*sizePosition.xy+sizePosition.zw) *vec2(2.0,2.0)-vec2(1.0,1.0)),0.0,1.0);" +
                    "}\n";
    public static final String PROMO_FRAGMENT =
            "varying highp vec2 UV;" +
                    "uniform sampler2D uSampler[1];" +


                    "void main(void) {" +
                    "highp vec4 color = texture2D(uSampler[0], UV);"+
//                    "highp float alpha_inv = 1.0-alpha;"+
//                            "if (alpha>0.5) discard;"+
//                    " gl_FragColor = vec4(color.xyz,1.0-alpha);" +
                    " gl_FragColor = color;" +
                    "}";

    public static final String VIDEO_VERTEX =
            "attribute vec2 uvCoordinates;" +
            "attribute vec2 uv;\n" +
                    "attribute vec4 speechBuff0;" +
                    "attribute vec4 speechBuff1;" +
                    "attribute vec4 speechBuff2;" +
                    "varying highp vec2 UV;" +
                    "varying highp vec2 UV1;" +
                    "uniform vec4 parSet0;" +
                    "uniform vec4 parSet1;" +
//                    "uniform vec4 sizePosition;" +
                    "void main(void) {\n" +
                    "UV1 = (uvCoordinates*vec2(1.0,-1.0) + vec2(1.0,1.0))*vec2(0.5,0.5);" +
                    " vec2 speechBshp[5];" +
                    " speechBshp[0] = speechBuff0.xy;" +
                    " speechBshp[1] = speechBuff0.zw;" +
                    " speechBshp[2] = speechBuff1.xy;" +
                    " speechBshp[3] = speechBuff1.zw;" +
                    " speechBshp[4] = speechBuff2.xy;" +
                    " float speechWeights[5];" +
                    " speechWeights[0] = parSet0.x;" +
                    " speechWeights[1] = parSet0.y;" +
                    " speechWeights[2] = parSet0.z;" +
                    " speechWeights[3] = parSet0.w;" +
                    " speechWeights[4] = parSet1.x;" +
                    " float ratio = parSet1.y;" +
                    " float mScale = parSet1.z;" +
                    "UV = vec2(1.0) - uv;" +
//                    "UV = 1.0-UV.y;" +
//                    "UV.x += 0.003;" +
                    "vec2 result = uv*vec2(2.0,2.0)-vec2(1.0,1.0);" +
                    "for (int i = 0; i < 5; i++) {" +
                    "   result.xy += speechWeights[i]*speechBshp[i] * mScale / 0.3;" +
                    "}" +
//                    "result -= speechBshp[2];" +
                    "result.xy *= vec2(1.0,-ratio);" +
                    "gl_Position = vec4(result,0.0,1.0);" +
                    "}\n";
    public static final String TEX_ROT_FRAGMENT =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "varying highp vec2 UV;" +
                    "uniform samplerExternalOES uSampler;" +


                    "void main(void) {" +
                    "highp vec4 color = texture2D(uSampler, UV);"+
                    " gl_FragColor = vec4(color.xyz,1.0);" +
                    "}";
    public static final String VIDEO_FRAGMENT =

            "varying highp vec2 UV;" +
            "varying highp vec2 UV1;" +
                    "uniform sampler2D uSampler;" +
                    "uniform sampler2D mLine[1];" +
                    "uniform highp float opFactor;" +
                    "void main(void) {" +
                    "highp vec4 color = texture2D(uSampler, UV);"+
                    "highp vec4 mlMaskCol = texture2D( mLine[0], UV1 );" +
                    "color.a=mlMaskCol.x*opFactor + (1.0-opFactor);" +
                    " gl_FragColor = color;" +
                    "}";
    public static final String HEAD_SINGLE_VERTEX =
            "attribute vec4 bshp0;\n" +
                    "attribute vec4 bshp1;\n" +
                    "attribute vec4 bshp2;\n" +
                    "attribute vec4 bshp3;\n" +
                    "attribute vec4 bshp4;\n" +
                    "attribute vec4 speechBuff0;" +
                    "attribute vec4 speechBuff1;" +
                    "attribute vec4 speechBuff2;" +
                    "attribute vec2 eyebrowBshp;" +
                    "attribute vec2 blinkBshp;" +
                    "attribute vec2 uvCoordinates;" +

                    "uniform vec4 parSet0;" +
                    "uniform vec4 parSet1;" +
                    "uniform vec4 parSet2;" +
                    "uniform vec4 parSet3;" +

                    "uniform mat4 vmMatrix;" +
                    "uniform mat4 zRotMatrix;" +
                    "uniform mat4 extraRotMatrix;" +

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
                    " float eyebrowWeight = parSet3.z;" +
                    " float blinkWeight = parSet3.w;" +

                    "vec4 result = vec4(0);" +
                    "for (int i = 0; i < 5; i++) {" +
                    "    result += weights[i]*bshp[i];" +
                    "}" +

                    "for (int i = 0; i < 5; i++) {" +
                    "    result.xy += speechWeights[i]*speechBshp[i];" +
                    "}" +
                    " result.xy += eyebrowBshp*eyebrowWeight;" +
                    " result.xy += blinkBshp*blinkWeight;" +
                    "result = extraRotMatrix*result;" +
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

                    "gl_Position = result;" +
                    "}\n";

    public static final String HEAD_SINGLE_FRAGMENT =
            "varying highp vec2 uv;" +
                    "uniform sampler2D uSampler[5];" +
                    "uniform sampler2D mLine[1];" +
                    "uniform highp vec4 parSet0;" +
                    "uniform highp vec4 parSet1;" +
                    "uniform highp float opFactor;" +
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
                    "highp vec4 mlMaskCol = texture2D( mLine[0], uv );" +
                    "result.a*=mlMaskCol.x*opFactor + (1.0-opFactor);" +
                    " gl_FragColor = result;" +
                    "}";
    public static final String MOUTH_SINGLE_VERTEX =
            "attribute vec2 bshp0;" +
                    "attribute vec2 bshp1;" +
                    "attribute vec2 bshp2;" +
                    "attribute vec2 bshp3;" +
                    "attribute vec2 bshp4;" +
                    "attribute vec2 coordinates;" +

                    "varying highp vec2 uv;" +

                    "uniform vec4 parSet0;" +
                    "uniform vec4 parSet1;" +
                    "uniform vec4 parSet2;" +
                    "uniform vec4 parSet3;" +
                    "uniform mat4 zRotMatrix;" +

                    "void main(void) {" +

                    " vec2 bshp[5];" +
                    " bshp[0] = bshp0;" +
                    " bshp[1] = bshp1;" +
                    " bshp[2] = bshp2;" +
                    " bshp[3] = bshp3;" +
                    " bshp[4] = bshp4;" +
                    " float weights[5];" +
                    " weights[0] = parSet0.x;" +
                    " weights[1] = parSet0.y;" +
                    " weights[2] = parSet0.z;" +
                    " weights[3] = parSet0.w;" +
                    " weights[4] = parSet1.x;" +
                    " float screenRatio = parSet1.y;" +
                    " float px = parSet1.z;" +
                    " float py = parSet1.w;" +
                    " float topPivX = parSet2.x;" +
                    " float topPivY = parSet2.y;" +
                    " float mouthRatio = parSet3.x;" +
                    " float mouthScale = parSet3.y;" +


                    " vec2 result = vec2(0);" +
                    " for (int i = 0; i < 5; i++) {" +
                    "     result += weights[i]*bshp[i];" +
                    " }" +
                    " result.x -= topPivX;" +
                    " result.y -= topPivY;" +

                    " result *= -1.0;" +
                    " result.y *= mouthRatio;" +
                    " result *= mouthScale;" +
                    " result = (zRotMatrix*vec4(result,0.0,1.0)).xy;" +
                    " result.y *= screenRatio;" +
                    " result += vec2(px,py);" +

                    " uv = coordinates;" +
                    " uv.y = 1.0 - uv.y;" +
                    //   " uv.x = 1.0 - uv.x;" +
                    " gl_Position = vec4(result,0.0,1.0);" +
                    "}";

    public static final String MOUTH_SINGLE_FRAGMENT =
            "varying highp vec2 uv;" +
                    "uniform sampler2D uSampler[5];" +
                    "uniform highp vec4 parSet0;" +
                    "uniform highp vec4 parSet1;" +
                    "uniform highp vec4 parSet3;" +
                    "uniform highp float alpha;" +
                    "uniform int isTop;" +
                    "void main(void) {" +
                    " highp float weights[5];" +
                    " weights[0] = parSet0.x;" +
                    " weights[1] = parSet0.y;" +
                    " weights[2] = parSet0.z;" +
                    " weights[3] = parSet0.w;" +
                    " weights[4] = parSet1.x;" +
                    " highp float teethTopKeyPointY = parSet3.z;" +
                    " highp float teethBotKeyPointY = parSet3.w;" +
                    " highp vec4 result = vec4(0);" +
                    " for (int i = 0; i < 5; i++) {" +
                    "     result += weights[i]*texture2D(uSampler[i], uv).bgra;" +
                    " }" +

                    " if (isTop == 1) {" +
                    " if (uv.y<teethTopKeyPointY) {result.a = 0.0;}" +
                    " }else{" +
                    " if (uv.y>teethBotKeyPointY) {" +
                    " result.xyz *= uv.y/(teethBotKeyPointY - teethTopKeyPointY)  - teethTopKeyPointY/(teethBotKeyPointY - teethTopKeyPointY) ;" +
                    " }" +
                    " }" +
                    " highp float xDarken = pow(cos((uv.x-0.5)*3.14),3.0);" +
                    " result.xyz*=xDarken;" +
//            " gl_FragColor = vec4(vec3(1.0),0.1);" +
//            " gl_FragColor = result;" +
//                    " gl_FragColor = vec4(result.xyz,0.5);" +
                    " gl_FragColor = vec4(result.xyz,result.a*alpha);" +
                    "}";

    public static final String HEAD_DUAL_VERTEX =
            "attribute vec4 bshp0;" +
                    "attribute vec4 bshp1;" +
                    "attribute vec4 bshp2;" +
                    "attribute vec4 bshp3;" +
                    "attribute vec4 bshp4;" +

                    "attribute vec4 bshp0o;" +
                    "attribute vec4 bshp1o;" +
                    "attribute vec4 bshp2o;" +
                    "attribute vec4 bshp3o;" +
                    "attribute vec4 bshp4o;" +

                    "attribute vec4 speechBuff0;" +
                    "attribute vec4 speechBuff1;" +
                    "attribute vec4 speechBuff2;" +

                    "attribute vec2 eyebrowBshp;" +
                    "attribute vec2 blinkBshp;" +
                    "attribute vec2 coordinates;" +


                    "varying highp vec2 uv;" +
                    "varying highp float wHybrid;" +
                    "uniform vec4 parSet0;" +
                    "uniform vec4 parSet1;" +
                    "uniform vec4 parSet2;" +
                    "uniform vec4 parSet3;" +
                    "uniform mat4 vmMatrix;" +
                    "uniform mat4 zRotMatrix;" +
                    "uniform mat4 extraRotMatrix;" +
                    "uniform float mixWeight;" +
                    "uniform int effectId;" +

                    "void main(void) {" +
                    " vec2 speechBshp[5];" +
                    " speechBshp[0] = speechBuff0.xy;" +
                    " speechBshp[1] = speechBuff0.zw;" +
                    " speechBshp[2] = speechBuff1.xy;" +
                    " speechBshp[3] = speechBuff1.zw;" +
                    " speechBshp[4] = speechBuff2.xy;" +

                    " vec4 bshp[5];" +
                    " bshp[0] = bshp0;" +
                    " bshp[1] = bshp1;" +
                    " bshp[2] = bshp2;" +
                    " bshp[3] = bshp3;" +
                    " bshp[4] = bshp4;" +

                    " vec4 bshp1[5];" +
                    " bshp1[0] = bshp0o;" +
                    " bshp1[1] = bshp1o;" +
                    " bshp1[2] = bshp2o;" +
                    " bshp1[3] = bshp3o;" +
                    " bshp1[4] = bshp4o;" +


                    " float weights[5];" +
                    " weights[0] = parSet0.x;" +
                    " weights[1] = parSet0.y;" +
                    " weights[2] = parSet0.z;" +
                    " weights[3] = parSet0.w;" +
                    " weights[4] = parSet1.x;" +
                    " float screenRatio = parSet1.y;" +
                    " float xPos = parSet1.z;" +
                    " float yPos = parSet1.w;" +
                    " float scale = parSet2.x;" +

                    " float speechWeights[5];" +
                    " speechWeights[0] = parSet2.y;" +
                    " speechWeights[1] = parSet2.z;" +
                    " speechWeights[2] = parSet2.w;" +
                    " speechWeights[3] = parSet3.x;" +
                    " speechWeights[4] = parSet3.y;" +
                    " float eyebrowWeight = parSet3.z;" +
                    " float blinkWeight = parSet3.w;" +

                    " vec4 result = vec4(0);" +
                    " if (effectId == 0) {" +

                    "    for (int i = 0; i < 5; i++) {" +
                    "        result += weights[i]*bshp[i]*mixWeight + weights[i]*bshp1[i]*(1.0-mixWeight);" +
                    "    }" +
                    " }else{" +

                    "    float theta = mixWeight*6.28;"+
                    "    vec3 linePoint = vec3(sin(theta),cos(theta),1.0);"+
                    "    vec3 curPoint = vec3(coordinates+vec2(0.0,-0.15),1.0);"+
                    "    float s = dot(cross(linePoint,curPoint),vec3(0.0,0.0,1.0));"+
                    "    float w = clamp(s/0.15,-1.0,1.0);"+
                    "    w+=1.0;"+
                    "    w/=2.0;"+
                    "    float weightHybrid = w; wHybrid = w;"+

                    "    for (int i = 0; i < 5; i++) {" +
                    "        result += weights[i]*bshp[i]*weightHybrid + weights[i]*bshp1[i]*(1.0-weightHybrid);" +
                    "    }" +

                    " }" +

                    " for (int i = 0; i < 5; i++) {" +
                    "     result.xy += speechWeights[i]*speechBshp[i];" +
                    " }" +
                    " result.xy += eyebrowBshp*eyebrowWeight;" +
                    " result.xy += blinkBshp*blinkWeight;" +

                    " result = extraRotMatrix*result;" +
                    " result = vmMatrix*result;" +
                    " result.x = atan(result.x/result.z)*5.0;" +
                    " result.y = atan(result.y/result.z)*5.0;" +
                    " result.z = (1.0 - result.z)*0.01 +0.21;" +


                    " result.y -= 4.0;" +
                    " result = zRotMatrix*result;" +
                    " result.y += 4.0;" +

                    " result.x += xPos;" +
                    " result.y -= yPos;" +
                    " result.xy *= 0.8+scale;" +
                    " result.y *= -screenRatio;" +

                    " uv = (coordinates*vec2(1.0,-1.0) + vec2(1.0,1.0))*vec2(0.5,0.5);" +
                    " gl_Position = result;" +
                    "}";
    public static final String HEAD_DUAL_FRAGMENT =
            "varying highp vec2 uv;" +
                    "varying highp float wHybrid;" +
                    "uniform sampler2D uSampler[5];" +
                    "uniform sampler2D uSampler1[5];" +
                    "uniform sampler2D mLine[1];" +
                    "uniform highp vec4 parSet0;" +
                    "uniform highp vec4 parSet1;" +
                    "uniform highp float mixWeight;" +
                    "uniform highp int effectId;" +
                    "uniform highp float opFactor;" +

                    "void main(void) {" +
                    " highp float weights[5];" +
                    " weights[0] = parSet0.x;" +
                    " weights[1] = parSet0.y;" +
                    " weights[2] = parSet0.z;" +
                    " weights[3] = parSet0.w;" +
                    " weights[4] = parSet1.x;" +
                    " highp vec4 result = vec4(0);" +
                    " if (effectId == 0) {" +
                    "    for (int i = 0; i < 5; i++) {" +
                    "        result += weights[i]*texture2D(uSampler[i], uv).bgra * mixWeight + weights[i]*texture2D(uSampler1[i], uv).bgra * (1.0 - mixWeight);" +
                    "    }" +
                    " }else{" +
                    "    for (int i = 0; i < 5; i++) {" +
                    "        result += weights[i]*texture2D(uSampler[i], uv).bgra * wHybrid + weights[i]*texture2D(uSampler1[i], uv).bgra * (1.0 - wHybrid);" +
                    "    }" +
                    " }" +
                    "highp vec4 mlMaskCol = texture2D( mLine[0], uv );" +
                    "result.a*=mlMaskCol.x*opFactor + (1.0-opFactor);" +
                    " gl_FragColor = result;" +
                    "}";


}
