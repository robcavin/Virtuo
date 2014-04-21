package com.bumblebeejuice.virtuo.app;

import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by robcavin on 4/19/14.
 */
public class LTSphereFilterProgram extends LTFilterProgram {

    private long startAnimationTime;
    private float[] rotationMatrix = new float[16];
    private float[] rotatedMatrix = new float[16];

    @Override
    public void init(int inputSurfaceTextureId, boolean inputSurfaceIsExternal) {
        super.init(inputSurfaceTextureId, inputSurfaceIsExternal);
        Matrix.setIdentityM(rotationMatrix,0);
    }

    @Override
    public FloatBuffer getTriangleVerticesData() {

        float R = 1;
        float H = 0;
        float K = 0;
        float Z = 0;

        final int space = 10;
        final int vertexCount = (90 / space) * (360 / space) * 4;

        FloatBuffer vertices =
                ByteBuffer.allocateDirect(vertexCount * 5 * 4)
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();

        float x, y, z;

        for (double b = 90; b <= 180 - space; b += space) {
            for (double a = 0; a <= 360 - space; a += space) {


                x = (float) (R * Math.sin((a) / 180 * Math.PI) * Math.sin((b) / 180 * Math.PI));
                y = (float) (R * Math.cos((a) / 180 * Math.PI) * Math.sin((b) / 180 * Math.PI));
                vertices.put(x - H);
                vertices.put(y + K);
                vertices.put((float) (R * Math.cos((b) / 180 * Math.PI) - Z));
                vertices.put((x + 1) / 4);
                vertices.put((y + 1) / 2);

                x = (float) (R * Math.sin((a) / 180 * Math.PI) * Math.sin((b + space) / 180 * Math.PI) - H);
                y = (float) (R * Math.cos((a) / 180 * Math.PI) * Math.sin((b + space) / 180 * Math.PI) + K);
                vertices.put(x);
                vertices.put(y);
                vertices.put((float) (R * Math.cos((b + space) / 180 * Math.PI) - Z));
                vertices.put((x + 1) / 4);
                vertices.put((y + 1) / 2);

                x = (float) (R * Math.sin((a + space) / 180 * Math.PI) * Math.sin((b) / 180 * Math.PI) - H);
                y = (float) (R * Math.cos((a + space) / 180 * Math.PI) * Math.sin((b) / 180 * Math.PI) + K);
                vertices.put(x);
                vertices.put(y);
                vertices.put((float) (R * Math.cos((b) / 180 * Math.PI) - Z));
                vertices.put((x + 1) / 4);
                vertices.put((y + 1) / 2);

                x = (float) (R * Math.sin((a + space) / 180 * Math.PI) * Math.sin((b + space) / 180 * Math.PI) - H);
                y = (float) (R * Math.cos((a + space) / 180 * Math.PI) * Math.sin((b + space) / 180 * Math.PI) + K);
                vertices.put(x);
                vertices.put(y);
                vertices.put((float) (R * Math.cos((b + space) / 180 * Math.PI) - Z));
                vertices.put((x + 1) / 4);
                vertices.put((y + 1) / 2);
            }
        }

        return vertices;
    }

    public void setRotationMatrix(float[] matrix) {
        rotationMatrix = matrix;
    }

    @Override
    protected void updateUniforms(float[] mMVPMatrix, float[] mSTMatrix) {

        /*float rotation = 0;
        long now = System.nanoTime();

        if (startAnimationTime != 0) {
            double delta = (now - startAnimationTime) / 1000000000.0;
            rotation = (float) Math.sin(2 * Math.PI * delta);
        } else {
            startAnimationTime = now;
        }

        Matrix.setRotateM(rotationMatrix, 0, rotation * 10, 0, 1, 0);*/

        Matrix.multiplyMM(rotatedMatrix, 0, mMVPMatrix, 0, rotationMatrix, 0);

        super.updateUniforms(rotatedMatrix, mSTMatrix);
    }
}
