package com.bumblebeejuice.virtuo.app;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by robcavin on 4/19/14.
 */
public class LTSphereFilterProgram extends LTFilterProgram {

    @Override
    public FloatBuffer getTriangleVerticesData() {

        int R = 1;
        int H = 0;
        int K = 0;
        int Z = 0;

        final int space = 10;
        final int vertexCount = (90 / space) * (360 / space) * 4;

        FloatBuffer vertices =
                ByteBuffer.allocateDirect(vertexCount * 5 * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();

        for (double b = 0; b <= 90 - space; b += space) {
            for (double a = 0; a <= 360 - space; a += space) {

                vertices.put((float) (R * Math.sin((a) / 180 * Math.PI) * Math.sin((b) / 180 * Math.PI) - H));
                vertices.put((float) (R * Math.cos((a) / 180 * Math.PI) * Math.sin((b) / 180 * Math.PI) + K));
                vertices.put((float) (R * Math.cos((b) / 180 * Math.PI) - Z));
                vertices.put((float) ((2 * b) / 360));
                vertices.put((float) ((a) / 360));

                vertices.put((float) (R * Math.sin((a) / 180 * Math.PI) * Math.sin((b + space) / 180 * Math.PI) - H));
                vertices.put((float) (R * Math.cos((a) / 180 * Math.PI) * Math.sin((b + space) / 180 * Math.PI) + K));
                vertices.put((float) (R * Math.cos((b + space) / 180 * Math.PI) - Z));
                vertices.put((float) ((2 * (b + space)) / 360));
                vertices.put((float) ((a) / 360));

                vertices.put((float) (R * Math.sin((a + space) / 180 * Math.PI) * Math.sin((b) / 180 * Math.PI) - H));
                vertices.put((float) (R * Math.cos((a + space) / 180 * Math.PI) * Math.sin((b) / 180 * Math.PI) + K));
                vertices.put((float) (R * Math.cos((b) / 180 * Math.PI) - Z));
                vertices.put((float) ((2 * b) / 360));
                vertices.put((float) ((a + space) / 360));

                vertices.put((float) (R * Math.sin((a + space) / 180 * Math.PI) * Math.sin((b + space) / 180 * Math.PI) - H));
                vertices.put((float) (R * Math.cos((a + space) / 180 * Math.PI) * Math.sin((b + space) / 180 * Math.PI) + K));
                vertices.put((float) (R * Math.cos((b + space) / 180 * Math.PI) - Z));
                vertices.put((float) ((2 * (b + space)) / 360));
                vertices.put((float) ((a + space) / 360));
            }
        }

        return vertices;
    }
}
