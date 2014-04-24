package com.bumblebeejuice.virtuo.app;

import android.content.res.AssetManager;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by robcavin on 2/6/14.
 */
public class LTFilterProgram {

    protected int mProgram;
    private int mVertexShader;
    private int mFragmentShader;

    private int muMVPMatrixHandle;
    private int muSTMatrixHandle;
    private int maPositionHandle;
    private int maTextureCoordHandle;

    private int inputSurfaceTextureId = 0;
    private boolean inputSurfaceIsExternal;

    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;

    private String staticInputTextureAssetPath = null;

    public void setStaticInputTextureAssetPath(String staticInputTextureAssetPath) {
        this.staticInputTextureAssetPath = staticInputTextureAssetPath;
    }

    public String getStaticInputTextureAssetPath() {
        return staticInputTextureAssetPath;
    }

    private final float[] mTriangleVerticesData = {
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0.0f, 0.0f, 0.0f, //0
            1.0f, -1.0f, 0.0f, 1.0f, 0.0f,  //1
            -1.0f, 1.0f, 0.0f, 0.0f, 1.0f,  //2 // triangle 1

            -1.0f, 1.0f, 0.0f, 0.0f, 1.0f,  //2
            1.0f, -1.0f, 0.0f, 1.0f, 0.0f,  //1
            1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
    };

    private FloatBuffer mTriangleVertices;

    public FloatBuffer getTriangleVerticesData() {
        FloatBuffer triangleVertices = ByteBuffer.allocateDirect(
                mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        triangleVertices.put(mTriangleVerticesData).position(0);
        return triangleVertices;
    }

    public String getShaderBasename() {
        return "basic";
    }

    public void init(int inputSurfaceTextureId, boolean inputSurfaceIsExternal) {
        this.inputSurfaceTextureId = inputSurfaceTextureId;
        this.inputSurfaceIsExternal = inputSurfaceIsExternal;

        createExtraTextures();

        mTriangleVertices = getTriangleVerticesData();

        mProgram = createProgram(getShader("vertex"), getShader("fragment"));
        if (mProgram == 0) {
            return;
        }

        getAttributeLocations();
        getUniformLocations();
    }

    protected void createExtraTextures() {
    }

    protected void getAttributeLocations() {

        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        checkGlError("glGetAttribLocation aPosition");
        if (maPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        maTextureCoordHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        checkGlError("glGetAttribLocation aTextureCoord");
        if (maTextureCoordHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }
    }

    protected void getUniformLocations() {

        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        checkGlError("glGetUniformLocation uMVPMatrix");
        if (muMVPMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }

        muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
        checkGlError("glGetUniformLocation uSTMatrix");
        if (muSTMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uSTMatrix");
        }
    }


    private String getShader(String type) {
        AssetManager am = Virtuo.context().getAssets();
        StringBuilder builder = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(am.open("shaders/" + getShaderBasename() + "_" + type + ".glsl")));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append("\n");
            }
        } catch (Exception e) {
            LTErrorHandler.handleException(e);
        }

        String shaderString = builder.toString();
        if (!inputSurfaceIsExternal)
            shaderString = shaderString.replace("samplerExternalOES", "sampler2D");

        return shaderString;
    }


    private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e("LIGHTT FILTER", "Could not compile shader " + shaderType + ":");
                Log.e("LIGHTT FILTER", GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }


    private int createProgram(String vertexSource, String fragmentSource) {
        mVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (mVertexShader == 0) {
            return 0;
        }
        mFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (mFragmentShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, mVertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(program, mFragmentShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e("LIGHTT FILTER", "Could not link program: ");
                Log.e("LIGHTT FILTER", GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    protected void checkGlError(String op) {

        int error;
        if ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("LIGHTT FILTER", op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }


    protected void bindTextures() {

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        if (inputSurfaceIsExternal) {
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, inputSurfaceTextureId);
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputSurfaceTextureId);
        }
    }

    protected void updateUniforms(float[] mMVPMatrix, float[] mSTMatrix) {
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);
    }


    public void setViewport(int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    protected void clearRenderTargetBuffers() {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
    }

    protected int getPrimitiveType() {
        return GLES20.GL_TRIANGLES;
    }


    // MAIN DRAW LOOP
    public void draw(float[] mMVPMatrix, float[] mSTMatrix) {

        clearRenderTargetBuffers();

        GLES20.glUseProgram(mProgram);
        checkGlError("glUseProgram");

        bindTextures();

        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
        checkGlError("glVertexAttribPointer maPosition");
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        checkGlError("glEnableVertexAttribArray maPositionHandle");

        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(maTextureCoordHandle, 2, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
        checkGlError("glVertexAttribPointer maTextureCoordHandle");
        GLES20.glEnableVertexAttribArray(maTextureCoordHandle);
        checkGlError("glEnableVertexAttribArray maTextureCoordHandle");

        updateUniforms(mMVPMatrix, mSTMatrix);

        GLES20.glEnable(GLES20.GL_CULL_FACE);

        GLES20.glDrawArrays(getPrimitiveType(), 0, mTriangleVertices.capacity() / 5);
        checkGlError("glDrawArrays");
    }


    public void release() {
        if (mVertexShader != 0) {
            GLES20.glDeleteShader(mVertexShader);
            mVertexShader = 0;
        }

        if (mFragmentShader != 0) {
            GLES20.glDeleteShader(mFragmentShader);
            mFragmentShader = 0;
        }

        if (mProgram != 0) {
            GLES20.glDeleteProgram(mProgram);
            mProgram = 0;
        }
        checkGlError("release");
    }
}
