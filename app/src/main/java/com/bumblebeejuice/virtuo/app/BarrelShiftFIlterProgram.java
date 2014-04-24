package com.bumblebeejuice.virtuo.app;

import android.opengl.GLES20;

/**
 * Created by robcavin on 4/22/14.
 */
public class BarrelShiftFilterProgram extends LTFilterProgram {

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

    public BarrelShiftFilterProgram(float[] lensCenter, float[] screenCenter, float[] scale, float[] scaleIn, float[] hmdWarp) {
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

        GLES20.glUniform2fv(muLensCenterHandle, 1, LensCenter, 0);
        GLES20.glUniform2fv(muScreenCenterHandle, 1, ScreenCenter, 0);
        GLES20.glUniform2fv(muScaleHandle, 1, Scale, 0);
        GLES20.glUniform2fv(muScaleInHandle, 1, ScaleIn, 0);
        GLES20.glUniform4fv(muHmdWarpHandle, 1, HmdWarp, 0);
    }
}
