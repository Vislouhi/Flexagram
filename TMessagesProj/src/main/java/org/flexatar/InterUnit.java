package org.flexatar;

public class InterUnit {

    final float[] weights;
    final int[] idx;
    final float[] extRot;
    public InterUnit(float[] weights, int[] idx, float[] extRot){
        this.weights=weights;
        this.idx=idx;
        this.extRot=extRot;
    }
    public static float triSign(float[][] triangle){
        return (triangle[0][0] - triangle[2][0])*(triangle[1][1] - triangle[2][1]) - (triangle[1][0] - triangle[2][0])*(triangle[0][1] - triangle[2][1]);
    }
    public static boolean checkTriangle(float[] point,float[][] triangle){
        float[][] t1 = new float[3][2];
        t1[0] = point;
        t1[1] = triangle[0];
        t1[2] = triangle[1];
        float d1 = triSign(t1);

        float[][] t2 = new float[3][2];
        t2[0] = point;
        t2[1] = triangle[1];
        t2[2] = triangle[2];
        float d2 = triSign(t2);

        float[][] t3 = new float[3][2];
        t3[0] = point;
        t3[1] = triangle[2];
        t3[2] = triangle[0];
        float d3 = triSign(t3);

        boolean hasNeg = (d1<0) || (d2<0) || (d3<0);
        boolean hasPos = (d1>0) || (d2>0) || (d3>0);
        return !(hasNeg && hasPos);

    }
    public static int findTriangleContaining(float[] point,float[][][] triangles){
        for (int i = 0; i < triangles.length; i++) {
            boolean checkResult = InterUnit.checkTriangle(point, triangles[i]);
            if (checkResult){
                return i;
            }
        }
        return -1;
    }

    public static InterUnit makeInterUnit(float[] point,float[][][] triangles,int[] indices,float[][][] border){
        int triIdx = findTriangleContaining(point, triangles);
        float[] calculatedPoint = point;
        if (triIdx == -1) {
            float[] borderPoint=null;
            for (float[][] bLine:border){
                float[][] fromCenterLine = new float[2][2];
                fromCenterLine[0] = point;
                fromCenterLine[1][0] = 0.5f;
                fromCenterLine[1][1] = 0.45f;
                borderPoint = findIntersection(bLine, fromCenterLine);
                if (borderPoint!=null){

                    break;

                }
            }
            if (borderPoint == null){
                return null;
            }
            float[] centerPoint = new float[2];
            centerPoint[0] = 0.5f;
            centerPoint[1] = 0.45f;
            calculatedPoint = pointBetweenPoints(centerPoint,borderPoint,0.98f);
            triIdx = findTriangleContaining(calculatedPoint, triangles);
//            Log.d("====DEB====", " borderPoint " + calculatedPoint[0] +" "+ calculatedPoint[1]);
//            Log.d("====DEB====", " triIdx " + triIdx);
        }
        float[] weights = findWeightsForPoly(triangles[triIdx], calculatedPoint);
        int[] idx = new int[3];
        idx[0] = indices[3*triIdx];
        idx[1] = indices[3*triIdx + 1];
        idx[2] = indices[3*triIdx + 2];
        float[] rot = new float[2];
        rot[0] = point[0] - calculatedPoint[0];
        rot[1] = point[1] - calculatedPoint[1];
//        Log.d("====DEB====", " extara " + rot[0] + " "+rot[1]);
        return new InterUnit(weights,idx,rot);
    }
    private static float[] findIntersection(float[][] line1,float[][] line2){
        float slope1 = (line1[1][1] - line1[0][1]) / (line1[1][0] - line1[0][0]);
        float yIntercept1 = line1[0][1] - slope1 * line1[0][0];

        float slope2 = (line2[1][1] - line2[0][1]) / (line2[1][0] - line2[0][0]);
        float yIntercept2 = line2[0][1] - slope2 * line2[0][0];
        if (slope1 == slope2) {return null;}
        float x = (yIntercept2 - yIntercept1) / (slope1 - slope2);
        float y = slope1 * x + yIntercept1;

        float[] x1Range = new float[2];
        x1Range[0] = Math.min(line1[0][0], line1[1][0]);
        x1Range[1] = Math.max(line1[0][0], line1[1][0]);
        float[] x2Range = new float[2];
        x2Range[0] = Math.min(line2[0][0], line2[1][0]);
        x2Range[1] = Math.max(line2[0][0], line2[1][0]);

        float[] y1Range = new float[2];
        y1Range[0] = Math.min(line1[0][1], line1[1][1]);
        y1Range[1] = Math.max(line1[0][1], line1[1][1]);
        float[] y2Range = new float[2];
        y2Range[0] = Math.min(line2[0][1], line2[1][1]);
        y2Range[1] = Math.max(line2[0][1], line2[1][1]);
        if (rCont(x1Range,x) && rCont(y1Range,y) && rCont(x2Range,x) && rCont(y2Range,y)){
            float[] ret = new float[2];
            ret[0] = x;
            ret[1] = y;
            return ret;
        }else{
            return null;
        }
    }
    private static float[] pointBetweenPoints(float[] p1,float[] p2,float percent) {
        float[] ret = new float[2];
        ret[0] = p1[0]+(p2[0]-p1[0])*percent;
        ret[1] = p1[1]+(p2[1]-p1[1])*percent;
        return ret;
    }
    private static boolean rCont(float[] range,float val){
        return range[0]<=val &&  range[1]>=val;
    }

    private static float vLength(float[] v){
        return (float)Math.sqrt(v[0] * v[0] + v[1] * v[1]);
    }
    private static float vLength2(float[] v){
        return v[0] * v[0] + v[1] * v[1];
    }
    private static float dot(float[] v1,float[] v2){
        return v1[0] * v2[0] + v1[1] * v2[1];
    }

    private static float[] vSub(float[] v1,float[] v2){
        float[] ret = new float[2];
        ret[0] = v1[0]-v2[0];
        ret[1] = v1[1]-v2[1];
        return ret;

    }

    private static float[] distAndWeight(float[] v0,float[] v1){
        float v1Len = vLength(v1) + 0.0001f;
        float proj = dot(v0,v1)/v1Len;
        float dist = (float)Math.sqrt(vLength2(v0) - proj * proj);
        float weight = proj/v1Len;
        float[] ret = new float[2];
        ret[0] = dist;
        ret[1] = weight;
        return ret;
    }

    public static float[] vNorm(float[] v){
        float sum = 0;
        for (float f:v){sum+=f;}
        float[] ret = new float[v.length];
        for (int i = 0; i < v.length; i++) {
            ret[i] = v[i]/sum;
        }
        return ret;
    }
    public static float[] findWeightsForPoly(float[][] poly,float[] point){
        float[] dists = new float[3];
        float[] weights = new float[3];

        float[] v1 = vSub(poly[1],poly[0]);
        float[] v0 = vSub(point,poly[0]);
        float[] dw = distAndWeight(v0, v1);
        dists[0] = 1.0f/(dw[0]+0.001f);
        weights[0] = dw[1];

        v1 = vSub(poly[2],poly[1]);
        v0 = vSub(point,poly[1]);
        dw = distAndWeight(v0, v1);
        dists[1] = 1.0f/(dw[0]+0.001f);
        weights[1] = dw[1];

        v1 = vSub(poly[0],poly[2]);
        v0 = vSub(point,poly[2]);
        dw = distAndWeight(v0, v1);
        dists[2] = 1.0f/(dw[0]+0.001f);
        weights[2] = dw[1];
        dists = vNorm(dists);
        float[] pointWeights = new float[3];
        pointWeights[0] = 0f;
        pointWeights[1] = 0f;
        pointWeights[2] = 0f;

        pointWeights[0] += (1f -weights[0])*dists[0];
        pointWeights[1] += weights[0]*dists[0];

        pointWeights[1] += (1f -weights[1])*dists[1];
        pointWeights[2] += weights[1]*dists[1];

        pointWeights[2] += (1f -weights[2])*dists[2];
        pointWeights[0] += weights[2]*dists[2];
        return vNorm(pointWeights);
    }
}
