package org.flexatar;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TextureArray {
    public List<Integer> textureIds = new ArrayList<>();

    public void addTexture(Bitmap bitmap){
//        InputStream inputStream = new ByteArrayInputStream(imgData);
//        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

        int[] textureId = new int[1];
        GLES30.glGenTextures ( 1, textureId, 0 );
        GLES30.glBindTexture ( GLES30.GL_TEXTURE_2D, textureId[0] );

        GLUtils.texImage2D ( GLES30.GL_TEXTURE_2D, 0, bitmap, 0 );

        GLES30.glTexParameteri ( GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR );
        GLES30.glTexParameteri ( GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR );
        GLES30.glTexParameteri ( GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE );
        GLES30.glTexParameteri ( GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE );
        GLES30.glBindTexture ( GLES30.GL_TEXTURE_2D, 0 );
        textureIds.add(textureId[0]);

//        bitmap.recycle();
    }
    public void release(){
        for (int id : textureIds) {
            int[] ids = {id};
            GLES20.glDeleteTextures(1,ids,0);
        }


    }

    public void addTexture(int i) {
        textureIds.add(i);
    }
}
