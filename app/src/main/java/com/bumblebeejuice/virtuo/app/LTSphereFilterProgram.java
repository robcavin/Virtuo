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

    private int width;
    private int height;

    private static enum State {LEFT, RIGHT};
    private State state;

    private float[] projLeft = new float[16];
    private float[] projRight = new float[16];

    @Override
    public void init(int inputSurfaceTextureId, boolean inputSurfaceIsExternal) {
        super.init(inputSurfaceTextureId, inputSurfaceIsExternal);
        Matrix.setIdentityM(rotationMatrix,0);

        // Oculus defaults
        //float fov = (float) (2.0 * Math.atan2(0.09356/2, 0.041));
        //float aspectRatio = 640.0f / 800.0f;

        float viewCenter = 0.14976f * 0.25f;
        float eyeProjectionShift = viewCenter - 0.0635f*0.5f;
        float projectionCenterOffset = 4.0f * eyeProjectionShift / 0.14976f;

        //float[] perspectiveMatrix = new float[16];
        //Matrix.perspectiveM(perspectiveMatrix,0,fov,aspectRatio,0.0001f,1000.0f);
        //Matrix.translateM(projLeft,0,perspectiveMatrix,0,projectionCenterOffset,0,0);
        //Matrix.translateM(projRight,0,perspectiveMatrix,0,projectionCenterOffset,0,0);
        float[] identity = new float[16];
        Matrix.setIdentityM(identity,0);
        Matrix.translateM(projLeft,0,identity,0,projectionCenterOffset,0,0);
        Matrix.translateM(projRight,0,identity,0,-projectionCenterOffset,0,0);
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


                x = (Math.sin((a) / 180 * Math.PI) * Math.sin((b) / 180 * Math.PI));
                y = (Math.cos((a) / 180 * Math.PI) * Math.sin((b) / 180 * Math.PI));
                z = (Math.cos((b) / 180 * Math.PI));
                vertices.put((float) (R*x - H));
                vertices.put((float) (R*y + K));
                vertices.put((float) (R*z - Z));

                u = (Math.atan2(x,-z + epsilon) / (Math.PI / 2));
                u = (u + 1) / 4;
                v = (y + 1) / 2;
                vertices.put((float) u);
                vertices.put((float) v);

                x = (Math.sin((a) / 180 * Math.PI) * Math.sin((b + space) / 180 * Math.PI));
                y = (Math.cos((a) / 180 * Math.PI) * Math.sin((b + space) / 180 * Math.PI));
                z = (Math.cos((b + space) / 180 * Math.PI));
                vertices.put((float) (R*x - H));
                vertices.put((float) (R*y + K));
                vertices.put((float) (R*z - Z));

                u = (Math.atan2(x,-z + epsilon) / (Math.PI / 2));
                u = (u + 1) / 4;
                v = (y + 1) / 2;
                vertices.put((float) u);
                vertices.put((float) v);

                x = (float) (Math.sin((a + space) / 180 * Math.PI) * Math.sin((b) / 180 * Math.PI));
                y = (float) (Math.cos((a + space) / 180 * Math.PI) * Math.sin((b) / 180 * Math.PI));
                z = (float) (Math.cos((b) / 180 * Math.PI));
                vertices.put((float) (R*x - H));
                vertices.put((float) (R*y + K));
                vertices.put((float) (R*z - Z));

                u = (Math.atan2(x,-z + epsilon) / (Math.PI / 2));
                u = (u + 1) / 4;
                v = (y + 1) / 2;
                vertices.put((float) u);
                vertices.put((float) v);

                x = (Math.sin((a + space) / 180 * Math.PI) * Math.sin((b + space) / 180 * Math.PI));
                y = (Math.cos((a + space) / 180 * Math.PI) * Math.sin((b + space) / 180 * Math.PI));
                z = (Math.cos((b + space) / 180 * Math.PI));
                vertices.put((float) (R*x - H));
                vertices.put((float) (R*y + K));
                vertices.put((float) (R*z - Z));

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

                x = (Math.sin((a) / 180 * Math.PI) * Math.sin((b) / 180 * Math.PI));
                y = (Math.cos((a) / 180 * Math.PI) * Math.sin((b) / 180 * Math.PI));
                z = (Math.cos((b) / 180 * Math.PI));
                vertices.put((float) (R*x - H));
                vertices.put((float) (R*y + K));
                vertices.put((float) (R*z - Z));

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

                x = (Math.sin((a) / 180 * Math.PI) * Math.sin((b - space) / 180 * Math.PI));
                y = (Math.cos((a) / 180 * Math.PI) * Math.sin((b - space) / 180 * Math.PI));
                z = (Math.cos((b - space) / 180 * Math.PI));
                vertices.put((float) (R*x - H));
                vertices.put((float) (R*y + K));
                vertices.put((float) (R*z - Z));

                u = (float) -(Math.atan2(x,z + epsilon) / (Math.PI / 2));
                u = (u + 1) / 4 + 0.5f;
                v = (y + 1) / 2;
                vertices.put((float) u);
                vertices.put((float) v);

                x = (Math.sin((a + space) / 180 * Math.PI) * Math.sin((b) / 180 * Math.PI));
                y = (Math.cos((a + space) / 180 * Math.PI) * Math.sin((b) / 180 * Math.PI));
                z = (Math.cos((b) / 180 * Math.PI));
                vertices.put((float) (R*x - H));
                vertices.put((float) (R*y + K));
                vertices.put((float) (R*z - Z));

                u = (float) -(Math.atan2(x,z + epsilon) / (Math.PI / 2));
                u = (u + 1) / 4 + 0.5f;
                v = (y + 1) / 2;
                vertices.put((float) u);
                vertices.put((float) v);

                x = (Math.sin((a + space) / 180 * Math.PI) * Math.sin((b - space) / 180 * Math.PI));
                y = (Math.cos((a + space) / 180 * Math.PI) * Math.sin((b - space) / 180 * Math.PI));
                z = (Math.cos((b - space) / 180 * Math.PI));
                vertices.put((float) (R*x - H));
                vertices.put((float) (R*y + K));
                vertices.put((float) (R*z - Z));

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

        Matrix.multiplyMM(rotatedMatrix, 0, mMVPMatrix, 0, rotationMatrix, 0);

        super.updateUniforms(rotatedMatrix, mSTMatrix);
    }

    @Override
    protected int getPrimitiveType() {
        return GLES20.GL_TRIANGLE_STRIP;
    }

    @Override
    public void setViewport(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void draw(float[] mMVPMatrix, float[] mSTMatrix) {

        float[] tmpMatrix = new float[16];
        Matrix.multiplyMM(tmpMatrix,0,projLeft,0,mMVPMatrix,0);
        GLES20.glViewport(0,0,width/2,height);
        state = State.LEFT;
        super.draw(tmpMatrix,mSTMatrix);

        //Matrix.translateM(modifiedMatrix, 0, 2.0f, 0, 0);
        Matrix.multiplyMM(tmpMatrix,0,projRight,0,mMVPMatrix,0);
        GLES20.glViewport(width/2,0,width/2,height);
        state = State.RIGHT;
        super.draw(tmpMatrix,mSTMatrix);
    }


}
