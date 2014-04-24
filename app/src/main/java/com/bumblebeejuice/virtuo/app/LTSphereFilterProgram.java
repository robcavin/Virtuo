package com.bumblebeejuice.virtuo.app;

import android.opengl.GLES20;
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

        final int space = 5;
        final int vertexCount = (180 / space) * (360 / space) * 4 + 3;

        FloatBuffer vertices =
                ByteBuffer.allocateDirect(vertexCount * 5 * 4)
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();

        double x = 0, y = 0, z = 0, u = 0, v = 0;

        double epsilon = 0.0000001f;

        for (double b = 90; b <= 180 - space; b += space) {
            for (double a = 0; a <= 360 - space; a += space) {


                x = (R * Math.sin((a) / 180 * Math.PI) * Math.sin((b) / 180 * Math.PI));
                y = (R * Math.cos((a) / 180 * Math.PI) * Math.sin((b) / 180 * Math.PI));
                z = (R * Math.cos((b) / 180 * Math.PI));
                vertices.put((float) (x - H));
                vertices.put((float) (y + K));
                vertices.put((float) (z - Z));

                u = (Math.atan2(x,-z + epsilon) / (Math.PI / 2));
                u = (u + 1) / 4;
                v = (y + 1) / 2;
                vertices.put((float) u);
                vertices.put((float) v);

                x = (R * Math.sin((a) / 180 * Math.PI) * Math.sin((b + space) / 180 * Math.PI));
                y = (R * Math.cos((a) / 180 * Math.PI) * Math.sin((b + space) / 180 * Math.PI));
                z = (R * Math.cos((b + space) / 180 * Math.PI));
                vertices.put((float) (x - H));
                vertices.put((float) (y + K));
                vertices.put((float) (z - Z));

                u = (Math.atan2(x,-z + epsilon) / (Math.PI / 2));
                u = (u + 1) / 4;
                v = (y + 1) / 2;
                vertices.put((float) u);
                vertices.put((float) v);

                x = (float) (R * Math.sin((a + space) / 180 * Math.PI) * Math.sin((b) / 180 * Math.PI));
                y = (float) (R * Math.cos((a + space) / 180 * Math.PI) * Math.sin((b) / 180 * Math.PI));
                z = (float) (R * Math.cos((b) / 180 * Math.PI));
                vertices.put((float) (x - H));
                vertices.put((float) (y + K));
                vertices.put((float) (z - Z));

                u = (Math.atan2(x,-z + epsilon) / (Math.PI / 2));
                u = (u + 1) / 4;
                v = (y + 1) / 2;
                vertices.put((float) u);
                vertices.put((float) v);

                x = (R * Math.sin((a + space) / 180 * Math.PI) * Math.sin((b + space) / 180 * Math.PI));
                y = (R * Math.cos((a + space) / 180 * Math.PI) * Math.sin((b + space) / 180 * Math.PI));
                z = (R * Math.cos((b + space) / 180 * Math.PI));
                vertices.put((float) (x - H));
                vertices.put((float) (y + K));
                vertices.put((float) (z - Z));

                u = (Math.atan2(x,-z + epsilon) / (Math.PI / 2));
                u = (u + 1) / 4;
                v = (y + 1) / 2;
                vertices.put((float) u);
                vertices.put((float) v);
            }
        }


        // Degenerate connecting triangle start
        vertices.put((float) (x - H));
        vertices.put((float) (y + K));
        vertices.put((float) (z - Z));
        vertices.put((float) u);
        vertices.put((float) v);


        boolean first = true;

        // NOTE - reversed z ordering to wind in opposite direction, since we need 3 verts to
        //  generate the 0 area triangle which itself reverses ordering
        for (double b = 90; b >= space; b -= space) {
            for (double a = 0; a <= 360 - space; a += space) {

                x = (R * Math.sin((a) / 180 * Math.PI) * Math.sin((b) / 180 * Math.PI));
                y = (R * Math.cos((a) / 180 * Math.PI) * Math.sin((b) / 180 * Math.PI));
                z = (R * Math.cos((b) / 180 * Math.PI));
                vertices.put((float) (x - H));
                vertices.put((float) (y + K));
                vertices.put((float) (z - Z));

                u = -(Math.atan2(x,z + epsilon) / (Math.PI / 2));
                u = (u + 1) / 4 + 0.5f;
                v = (y + 1) / 2;
                vertices.put((float) u);
                vertices.put((float) v);

                // To close degenerate connecting triangle
                if (first) {
                    vertices.put((float) (x - H));
                    vertices.put((float) (y + K));
                    vertices.put((float) (z - Z));
                    vertices.put((float) u);
                    vertices.put((float) v);

                    vertices.put((float) (x - H));
                    vertices.put((float) (y + K));
                    vertices.put((float) (z - Z));
                    vertices.put((float) u);
                    vertices.put((float) v);
                    first = false;

                }

                x = (R * Math.sin((a) / 180 * Math.PI) * Math.sin((b - space) / 180 * Math.PI));
                y = (R * Math.cos((a) / 180 * Math.PI) * Math.sin((b - space) / 180 * Math.PI));
                z = (R * Math.cos((b - space) / 180 * Math.PI));
                vertices.put((float) (x - H));
                vertices.put((float) (y + K));
                vertices.put((float) (z - Z));

                u = (float) -(Math.atan2(x,z + epsilon) / (Math.PI / 2));
                u = (u + 1) / 4 + 0.5f;
                v = (y + 1) / 2;
                vertices.put((float) u);
                vertices.put((float) v);

                x = (R * Math.sin((a + space) / 180 * Math.PI) * Math.sin((b) / 180 * Math.PI));
                y = (R * Math.cos((a + space) / 180 * Math.PI) * Math.sin((b) / 180 * Math.PI));
                z = (R * Math.cos((b) / 180 * Math.PI));
                vertices.put((float) (x - H));
                vertices.put((float) (y + K));
                vertices.put((float) (z - Z));

                u = (float) -(Math.atan2(x,z + epsilon) / (Math.PI / 2));
                u = (u + 1) / 4 + 0.5f;
                v = (y + 1) / 2;
                vertices.put((float) u);
                vertices.put((float) v);

                x = (R * Math.sin((a + space) / 180 * Math.PI) * Math.sin((b - space) / 180 * Math.PI));
                y = (R * Math.cos((a + space) / 180 * Math.PI) * Math.sin((b - space) / 180 * Math.PI));
                z = (R * Math.cos((b - space) / 180 * Math.PI));
                vertices.put((float) (x - H));
                vertices.put((float) (y + K));
                vertices.put((float) (z - Z));

                u = (float) -(Math.atan2(x,z + epsilon) / (Math.PI / 2));
                u = (u + 1) / 4 + 0.5f;
                v = (y + 1) / 2;
                vertices.put((float) u);
                vertices.put((float) v);
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

    @Override
    protected int getPrimitiveType() {
        return GLES20.GL_TRIANGLE_STRIP;
    }
}
