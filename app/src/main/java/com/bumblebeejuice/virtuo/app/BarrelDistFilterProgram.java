package com.bumblebeejuice.virtuo.app;

import android.opengl.GLES20;
import android.opengl.Matrix;

/**
 * Created by robcavin on 4/22/14.
 */
public class BarrelDistFilterProgram extends LTFilterProgram {

    private int muLensCenterHandle;
    private int muScreenCenterHandle;
    private int muScaleHandle;
    private int muScaleInHandle;
    private int muHmdWarpHandle;

    private float[] LensCenter;
    private float[] ScreenCenter;
    private float[] Scale;
    private float[] ScaleIn;
    private float[] HmdWarp;

    private int width;
    private int height;

    private static enum State {LEFT, RIGHT};
    private State state;

    public static final float scale = 1.25f;

    public BarrelDistFilterProgram(float[] lensCenter, float[] screenCenter, float[] scale, float[] scaleIn, float[] hmdWarp) {
        LensCenter = lensCenter;
        ScreenCenter = screenCenter;
        Scale = scale;
        ScaleIn = scaleIn;
        HmdWarp = hmdWarp;
    }

    @Override
    public String getShaderBasename() {
        return "barrel";
    }


    @Override
    protected void getUniformLocations() {
        super.getUniformLocations();

        muLensCenterHandle = GLES20.glGetUniformLocation(mProgram, "LensCenter");
        checkGlError("glGetUniformLocation LensCenter");
        if (muLensCenterHandle == -1) {
            throw new RuntimeException("Could not get attrib location for LensCenter");
        }

        muScreenCenterHandle = GLES20.glGetUniformLocation(mProgram, "ScreenCenter");
        checkGlError("glGetUniformLocation ScreenCenter");
        if (muScreenCenterHandle == -1) {
            throw new RuntimeException("Could not get attrib location for ScreenCenter");
        }

        muScaleHandle = GLES20.glGetUniformLocation(mProgram, "Scale");
        checkGlError("glGetUniformLocation Scale");
        if (muScaleHandle == -1) {
            throw new RuntimeException("Could not get attrib location for Scale");
        }

        muScaleInHandle = GLES20.glGetUniformLocation(mProgram, "ScaleIn");
        checkGlError("glGetUniformLocation ScaleIn");
        if (muScaleInHandle == -1) {
            throw new RuntimeException("Could not get attrib location for ScaleIn");
        }

        muHmdWarpHandle = GLES20.glGetUniformLocation(mProgram, "HmdWarpParam");
        checkGlError("glGetUniformLocation HmdWarp");
        if (muHmdWarpHandle == -1) {
            throw new RuntimeException("Could not get attrib location for HmdWarp");
        }
    }

    @Override
    protected void updateUniforms(float[] mMVPMatrix, float[] mSTMatrix) {
        super.updateUniforms(mMVPMatrix, mSTMatrix);

        int offset = state == State.LEFT ? 0 : 2;

        GLES20.glUniform2fv(muLensCenterHandle, 1, LensCenter, offset);
        GLES20.glUniform2fv(muScreenCenterHandle, 1, ScreenCenter, offset);
        GLES20.glUniform2fv(muScaleHandle, 1, Scale, 0);
        GLES20.glUniform2fv(muScaleInHandle, 1, ScaleIn, 0);
        GLES20.glUniform4fv(muHmdWarpHandle, 1, HmdWarp, 0);
    }

    @Override
    protected int getPrimitiveType() {
        return GLES20.GL_TRIANGLES;
    }

    @Override
    protected void clearRenderTargetBuffers() {
        super.clearRenderTargetBuffers();
    }

    @Override
    public void setViewport(int width, int height) {
        this.width = width;
        this.height = height;
    }

    // MAIN DRAW LOOP
    public void draw(float[] mMVPMatrix, float[] mSTMatrix) {

        float[] modifiedMatrix = mMVPMatrix.clone();
        Matrix.scaleM(modifiedMatrix,0, scale, scale, 1.0f);

        GLES20.glViewport(0,0,width/2,height);
        state = State.LEFT;
        super.draw(modifiedMatrix,mSTMatrix);

        //Matrix.translateM(modifiedMatrix, 0, 2.0f, 0, 0);
        GLES20.glViewport(width/2,0,width/2,height);
        state = State.RIGHT;
        super.draw(modifiedMatrix,mSTMatrix);
    }
}
