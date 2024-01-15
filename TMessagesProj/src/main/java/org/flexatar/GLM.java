package org.flexatar;

public class GLM {

    public static float[] cross(float[] a,float[] b){
        return new float[]{
            a[1] * b[2] - a[2]*b[1],
            a[2] * b[0] - a[0]*b[2],
            a[0] * b[1] - a[1]*b[0],
        };
    }
    public static float[] add(float[] v1,float[] v2){

        return new float[]{v1[0]+v2[0],v1[1]+v2[1],v1[2]+v2[2],v1[3]+v2[3]};

    }
    public static float[] mulS(float[] v1,float v2){

        return new float[]{v1[0]*v2,v1[1]*v2,v1[2]*v2,v1[3]*v2};

    }
    public static float[] mul(float[] v1, float[] v2){

        return new float[]{v1[0]*v2[0],v1[1]*v2[1],v1[2]*v2[2],v1[3]*v2[3]};

    }
    public static float dotv3(float[] v1,float[] v2){

        return v1[0]*v2[0]+v1[1]*v2[1]+v1[2]*v2[2];

    }

    public static float[] addv2(float[] v1,float[] v2){

        return new float[]{v1[0]+v2[0],v1[1]+v2[1]};

    }
    public static float[] mulSv2(float[] v1,float v2){

        return new float[]{v1[0]*v2,v1[1]*v2};

    }
}
