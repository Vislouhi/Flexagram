package org.flexatar;



import android.opengl.GLES20;

import java.nio.ByteBuffer;

public class VBO {
    public final int[] id = new int[1];
    public int buffer_type = 0;
    public VBO(ByteBuffer data, int type){
        buffer_type = type;
        GLES20.glGenBuffers ( 1, this.id, 0 );
        GLES20.glBindBuffer ( type, this.id[0] );
        GLES20.glBufferData ( type, data.capacity(),
                data, GLES20.GL_STATIC_DRAW );
        GLES20.glBindBuffer ( type, 0);
    }
    public VBO(int size, int type){
        buffer_type = type;
        GLES20.glGenBuffers ( 1, this.id, 0 );
        GLES20.glBindBuffer ( type, this.id[0] );
        GLES20.glBufferData ( type, size,
                null, GLES20.GL_STATIC_DRAW );
        GLES20.glBindBuffer ( type, 0 );
    }
    public void bind(){
        GLES20.glBindBuffer ( buffer_type, this.id[0] );
    }
    public void unbind(){
        GLES20.glBindBuffer ( buffer_type, 0 );
    }
    public void destroy(){
        int numberOfBuffersToDelete = 1; // You can delete multiple buffers by changing this number
        GLES20.glDeleteBuffers(numberOfBuffersToDelete, id, 0);
    }
}
