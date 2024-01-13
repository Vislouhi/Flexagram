package org.flexatar;

public class AnimationUnit {
    public float tx = 0f;
    public float ty = 0f;
    public float scale = 1f;
    public float rz = 1f;
    public float eyebrow = 0f;
    public float blink = 0f;

    public AnimationUnit(float tx,float ty,float scale, float rz,float eyebrow,float blink){
        this.tx=tx;
        this.ty=ty;
        this.scale=scale;
        this.rz=rz;
        this.eyebrow=eyebrow;
        this.blink=blink;
    }
}
