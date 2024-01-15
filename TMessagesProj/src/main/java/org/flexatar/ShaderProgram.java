package org.flexatar;

import android.opengl.GLES20;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class ShaderProgram {

    private class VtxDescriptor{
        public final int location;
        public final VBO buffer;
        public final int size;

        public VtxDescriptor(int location, VBO buffer, int size) {
            this.location = location;
            this.buffer = buffer;
            this.size = size;
        }
        public void bind(){
            buffer.bind();

            GLES20.glVertexAttribPointer(location, size, GLES20.GL_FLOAT, false, 0, 0);
            GLES20.glEnableVertexAttribArray(location);
        }
        public void unbind(){
            buffer.unbind();
        }
    }
    public class TextureArrayDescriptor{
        public final TextureArray textureArray;
        public final int location;
        public final int textureUnit;

        public TextureArrayDescriptor(TextureArray textureArray, int location, int textureUnit) {
            this.textureArray = textureArray;
            this.location = location;
            this.textureUnit = textureUnit;
        }
        public void bind(){
            int[] uniform = new int[textureArray.textureIds.size()];

            for (int i = 0; i < textureArray.textureIds.size(); i++) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + textureUnit+ i);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureArray.textureIds.get(i));
                uniform[i] = textureUnit + i;
            }
            GLES20.glUniform1iv(location,uniform.length,uniform,0);
        }

    }
    private final int id;
    private Map<String,VtxDescriptor> attributes = new HashMap<>();
    private Map<String,Integer> uniforms4f = new HashMap<>();
    private Map<String,Integer> uniforms1i = new HashMap<>();
    private Map<String,Integer> uniforms1f = new HashMap<>();
    private Map<String,Integer> uniformsMatrix4fv = new HashMap<>();
    public Map<String,TextureArrayDescriptor> textureArrays = new HashMap<>();

    public ShaderProgram(String vCode, String fCode){
        int vertexShader = loadShader ( GLES20.GL_VERTEX_SHADER, vCode );
        int fragmentShader = loadShader ( GLES20.GL_FRAGMENT_SHADER, fCode );
        id = GLES20.glCreateProgram();
        GLES20.glAttachShader ( id, vertexShader );
        GLES20.glAttachShader ( id, fragmentShader );
        GLES20.glLinkProgram ( id );

        int[] linked = new int[1];
        GLES20.glGetProgramiv ( id, GLES20.GL_LINK_STATUS, linked, 0 );

        if ( linked[0] == 0 )
        {
            Log.e ( "ESShader", "Error linking program:" );
            Log.e ( "ESShader", GLES20.glGetProgramInfoLog ( id ) );
            GLES20.glDeleteProgram ( id );

        }
        GLES20.glDeleteShader ( vertexShader );
        GLES20.glDeleteShader ( fragmentShader );
    }
    public void release(){
        GLES20.glDeleteProgram ( id );
    }

    public void use(){
        GLES20.glUseProgram(id);
    }
    public void attribute(String name,VBO buffer,int size){
        attributes.put(name,new VtxDescriptor(GLES20.glGetAttribLocation(id,name),buffer,size));
    }
    public void textureArray(String name,TextureArray textureArray,int textureUnit){
        textureArrays.put(name,
                new TextureArrayDescriptor(textureArray,GLES20.glGetUniformLocation(id,name),textureUnit)
        ) ;
    }
    public void addUniform4f(String name){
        uniforms4f.put(name,GLES20.glGetUniformLocation(id,name));
    }

    public void uniform4f(String name,float f0,float f1,float f2,float f3){
        GLES20.glUniform4f(uniforms4f.get(name),f0,f1,f2,f3);
    }
    public void addUniform1i(String name){
        uniforms1i.put(name,GLES20.glGetUniformLocation(id,name));
    }
    public void uniform1f(String name,float i0){
        GLES20.glUniform1f(uniforms1f.get(name),i0);
    }
    public void addUniform1f(String name){
        uniforms1f.put(name,GLES20.glGetUniformLocation(id,name));
    }
    public void uniform1i(String name,int i0){
        GLES20.glUniform1i(uniforms1i.get(name),i0);
    }
    public void addUniformMatrix4fv(String name){
        uniformsMatrix4fv.put(name,GLES20.glGetUniformLocation(id,name));
    }
    public void uniformMatrix4fv(String name,float[] f){
        GLES20.glUniformMatrix4fv(uniformsMatrix4fv.get(name),1, false, f, 0);
    }
    public void bind(){
        for (VtxDescriptor vtxDexcriptor : attributes.values()) {
            vtxDexcriptor.bind();
        }
        for (TextureArrayDescriptor textureArrayDescriptor : textureArrays.values()) {
            textureArrayDescriptor.bind();
        }
    }
    public void unbind(){
        for (VtxDescriptor vtxDexcriptor : attributes.values()) {
            vtxDexcriptor.unbind();
        }
//        for (TextureArrayDescriptor textureArrayDescriptor : textureArrays.values()) {
//            textureArrayDescriptor.bind();
//        }
    }


    public static int loadShader ( int type, String shaderSrc )
    {
        int shader;
        int[] compiled = new int[1];

        // Create the shader object
        shader = GLES20.glCreateShader ( type );

        if ( shader == 0 )
        {
            return 0;
        }

        // Load the shader source
        GLES20.glShaderSource ( shader, shaderSrc );

        // Compile the shader
        GLES20.glCompileShader ( shader );

        // Check the compile status
        GLES20.glGetShaderiv ( shader, GLES20.GL_COMPILE_STATUS, compiled, 0 );

        if ( compiled[0] == 0 )
        {
            Log.e ( "ESShader", GLES20.glGetShaderInfoLog ( shader ) );
            GLES20.glDeleteShader ( shader );
            return 0;
        }

        return shader;
    }
}
