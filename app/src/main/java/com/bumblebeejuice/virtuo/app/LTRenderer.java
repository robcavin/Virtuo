package com.bumblebeejuice.virtuo.app;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by robcavin on 3/3/14.
 */
public class LTRenderer {

    public interface OnSurfaceTextureAvailableListener {
        void surfaceTextureAvailable(SurfaceTexture surfaceTexture, int surfaceTextureId);
    }

    public interface OnFrameRenderListener {
        boolean shouldSwapBuffers(LTRenderer renderer, long timeNs);
        long adjustedTimestamp(LTRenderer renderer, long timeNs);
        void frameRendered(LTRenderer renderer, long timeNs);
        void texImageChanged(LTRenderer renderer);
    }

    public interface OnBitmapRenderListener {
        void onBitmapRender(LTRenderer renderer, Bitmap bitmap);
    }

    private OnFrameRenderListener onFrameRenderListener;
    private OnBitmapRenderListener onBitmapRenderListener;

    private static final LTRenderThread renderThread = prepareRenderThread();

    private static LTRenderThread prepareRenderThread() {
        LTRenderThread renderThread = new LTRenderThread();
        renderThread.start();
        while (renderThread.handler == null) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
        }
        return renderThread;
    }

    public void setOnFrameRenderListener(OnFrameRenderListener onFrameRenderListener) {
        this.onFrameRenderListener = onFrameRenderListener;
    }

    public void setOnBitmapRenderListener(OnBitmapRenderListener onBitmapRenderListener) {
        this.onBitmapRenderListener = onBitmapRenderListener;
    }

    private EGLSurface _mEGLSurface;

    private LTFilterProgram _filterProgram;

    private Runnable _renderPass;

    private int _outputSurfaceTextureId = 0;
    private SurfaceTexture _outputSurfaceTexture;

    private int _outputSurfaceWidth;
    private int _outputSurfaceHeight;

    private int _inputSurfaceTextureId = 0;
    private SurfaceTexture _inputSurfaceTexture;
    boolean _inputSurfaceTextureOwner = false;
    boolean _inputSurfaceTextureIsExternal = false;

    private float[] _projMatrix = new float[16];
    private float[] _texCoordMatrix = new float[16];
    private boolean _fixedTexCoordMatrix = false;

    public LTRenderer() {
        Matrix.setIdentityM(_projMatrix, 0);

        Matrix.setIdentityM(_texCoordMatrix, 0);
        Matrix.translateM(_texCoordMatrix, 0, 0, 1, 0);
        Matrix.scaleM(_texCoordMatrix, 0, 1, -1, 1);
    }

    public void createOutputSurface(final int width, final int height) {
        renderThread.handler.post(new Runnable() {
            @Override
            public void run() {
                _outputSurfaceTextureId = createTextureForSurface();
                _outputSurfaceTexture = new SurfaceTexture(_outputSurfaceTextureId);
                _outputSurfaceTexture.setDefaultBufferSize(width,height);

                _outputSurfaceWidth = width;
                _outputSurfaceHeight = height;

                if (_mEGLSurface != null) renderThread.deleteOutputSurface(_mEGLSurface);
                _mEGLSurface = renderThread.createOutputSurface(_outputSurfaceTexture);
            }
        });
    }


    public void setOutputSurface(final Object outputSurface) {
        renderThread.handler.post(new Runnable() {
            @Override
            public void run() {
                if (_mEGLSurface != null) renderThread.deleteOutputSurface(_mEGLSurface);
                _mEGLSurface = renderThread.createOutputSurface(outputSurface);
            }
        });
    }


    public void runOnGLThread(final Runnable runnable) {
        renderThread.handler.post(new Runnable() {
            @Override
            public void run() {
                renderThread.makeCurrent(_mEGLSurface);
                runnable.run();
            }
        });
    }

    public void setIgnoreInputSurfaceTexMatrix(boolean ignoreInputSurfaceTexMatrix) {
        _fixedTexCoordMatrix = true;
    }


    public void setInputSurfaceTextureAndId(final SurfaceTexture inputSurfaceTexture, final int inputSurfaceTextureId) {

        runOnGLThread(new Runnable() {
            @Override
            public void run() {
                if (_inputSurfaceTextureOwner && _inputSurfaceTexture != null) {
                    _inputSurfaceTexture.release();
                }

                if (_inputSurfaceTextureOwner && _inputSurfaceTextureId != 0) {
                    int[] textures = {_inputSurfaceTextureId};
                    GLES20.glDeleteTextures(1,textures,0);
                    _inputSurfaceTextureId = 0;
                }

                _inputSurfaceTexture = inputSurfaceTexture;
                _inputSurfaceTextureId = inputSurfaceTextureId;
                _inputSurfaceTextureIsExternal = true;
                _inputSurfaceTextureOwner = false;
            }
        });
    }


    public void getInputSurfaceTexture(final OnSurfaceTextureAvailableListener onSurfaceTextureAvailableListener) {

        if (_inputSurfaceTexture != null && _inputSurfaceTextureId != 0) {
            onSurfaceTextureAvailableListener.surfaceTextureAvailable(_inputSurfaceTexture, _inputSurfaceTextureId);

        } else {
            runOnGLThread(new Runnable() {
                @Override
                public void run() {
                    if (_inputSurfaceTextureOwner && _inputSurfaceTexture != null) {
                        _inputSurfaceTexture.release();
                    }

                    if (_inputSurfaceTextureOwner && _inputSurfaceTextureId != 0) {
                        int[] textures = {_inputSurfaceTextureId};
                        GLES20.glDeleteTextures(1,textures,0);
                        _inputSurfaceTextureId = 0;
                    }

                    _inputSurfaceTextureId = createTextureForSurface();

                    if (_inputSurfaceTextureId > 0) {
                        _inputSurfaceTexture = new SurfaceTexture(_inputSurfaceTextureId);
                        _inputSurfaceTextureOwner = true;
                        _inputSurfaceTextureIsExternal = true;

                        onSurfaceTextureAvailableListener.surfaceTextureAvailable(_inputSurfaceTexture, _inputSurfaceTextureId);
                    }
                }
            });
        }
    }

    private static int createTextureForSurface() {
        int[] textures = new int[1];

        // generate one texture pointer and bind it as an external texture.
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        return textures[0];
    }

    public static int createTextureWithAsset(String assetPath) {
        int[] textures = new int[1];

        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        try {
            AssetManager am = Virtuo.context().getAssets();
            Bitmap bitmap = BitmapFactory.decodeStream(am.open(assetPath));
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();
        } catch (Exception e) {
            LTErrorHandler.handleException(e);
        }

        return textures[0];
    }


    public void setTextureScaleAndCrop(Point outputSurfaceSize, Point inputSurfaceSize) {
        setTextureScaleAndCrop(outputSurfaceSize, inputSurfaceSize, false, false);
    }

    public void setTextureScaleAndCrop(Point outputSurfaceSize, Point inputSurfaceSize, boolean flipVertical, boolean flipHorizontal) {

        double width_scale = 1.0 * outputSurfaceSize.x / inputSurfaceSize.x;
        double height_scale = 1.0 * outputSurfaceSize.y / inputSurfaceSize.y;

        double scale = Math.max(width_scale, height_scale);
        Point normalizedInputSize = new Point((int) (inputSurfaceSize.x * scale), (int) (inputSurfaceSize.y * scale));

        float width_scaling = 1.0f * normalizedInputSize.x / outputSurfaceSize.x;
        float height_scaling = 1.0f * normalizedInputSize.y / outputSurfaceSize.y;

        if (flipVertical) height_scaling = -height_scaling;
        if (flipHorizontal) width_scaling = -width_scaling;

        Matrix.setIdentityM(_projMatrix, 0);
        Matrix.scaleM(_projMatrix, 0, width_scaling, height_scaling, 1);
    }


    public void setFilterProgram(final LTFilterProgram filterProgram) {
        runOnGLThread(new Runnable() {
            @Override
            public void run() {
                if (_filterProgram != null) _filterProgram.release();
                _filterProgram = filterProgram;

                if (_inputSurfaceTextureId == 0) {
                    String assetPath = _filterProgram.getStaticInputTextureAssetPath();
                    if (assetPath != null) {

                        if (_inputSurfaceTextureOwner && _inputSurfaceTextureId != 0) {
                            int[] textures = {_inputSurfaceTextureId};
                            GLES20.glDeleteTextures(1,textures,0);
                            _inputSurfaceTextureId = 0;
                        }

                        _inputSurfaceTextureId = createTextureWithAsset(_filterProgram.getStaticInputTextureAssetPath());
                        _inputSurfaceTextureOwner = true;
                        _inputSurfaceTextureIsExternal = false;

                    } else {
                        LTErrorHandler.handleException(new Exception("No input texture set or no static image source set before setting filter"));
                    }
                }

                _filterProgram.init(_inputSurfaceTextureId,_inputSurfaceTextureIsExternal);
            }
        });
    }


    public void setRenderPass(Runnable renderPass) {
        _renderPass = renderPass;
    }


    public void renderOnce() {

        if (_renderPass == null) {
            setRenderPass(new Runnable() {
                @Override
                public void run() {
                    GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
                    GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

                    long timeNs = -1L;
                    if (_inputSurfaceTexture != null) {

                        _inputSurfaceTexture.updateTexImage();

                        timeNs = _inputSurfaceTexture.getTimestamp();

                        if (!_fixedTexCoordMatrix)
                            _inputSurfaceTexture.getTransformMatrix(_texCoordMatrix);
                    }

                    if (onFrameRenderListener != null)
                    {
                        onFrameRenderListener.texImageChanged(LTRenderer.this);
                    }

                    _filterProgram.draw(_projMatrix, _texCoordMatrix);

                    boolean doSwap = true;
                    if (onFrameRenderListener != null)
                        doSwap = onFrameRenderListener.shouldSwapBuffers(LTRenderer.this, timeNs);

                    if (doSwap) {

                        if (onFrameRenderListener != null) {
                            timeNs = onFrameRenderListener.adjustedTimestamp(LTRenderer.this, timeNs);
                            if (timeNs != -1L) renderThread.setTimestamp(_mEGLSurface, timeNs);
                        }

                        if (onBitmapRenderListener != null) {

                            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(_outputSurfaceWidth * _outputSurfaceHeight * 4);
                            byteBuffer.order(ByteOrder.nativeOrder());
                            GLES20.glReadPixels(0, 0, _outputSurfaceWidth, _outputSurfaceHeight, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, byteBuffer);
                            Bitmap bitmap = Bitmap.createBitmap(_outputSurfaceWidth, _outputSurfaceHeight, Bitmap.Config.ARGB_8888);
                            bitmap.copyPixelsFromBuffer(byteBuffer);

                            onBitmapRenderListener.onBitmapRender(LTRenderer.this, bitmap);
                        }

                        renderThread.swapBuffers(_mEGLSurface);

                        if (onFrameRenderListener != null) {
                            onFrameRenderListener.frameRendered(LTRenderer.this, timeNs);
                        }
                    }
                }
            });
        }

        runOnGLThread(_renderPass);
    }


    public void release() {
        release(null);
    }

    public void release(final Runnable onComplete) {
        runOnGLThread(new Runnable() {
            @Override
            public void run() {

                if (_outputSurfaceTexture != null) {
                    _outputSurfaceTexture.release();
                    _outputSurfaceTexture = null;
                }

                if (_outputSurfaceTextureId != 0) {
                    int[] textures = {_outputSurfaceTextureId};
                    GLES20.glDeleteTextures(1,textures,0);
                    _outputSurfaceTextureId = 0;
                }

                if (_inputSurfaceTextureOwner && _inputSurfaceTexture != null) {
                    _inputSurfaceTexture.release();
                    _inputSurfaceTexture = null;
                }

                if (_inputSurfaceTextureOwner && _inputSurfaceTextureId != 0) {
                    int[] textures = {_inputSurfaceTextureId};
                    GLES20.glDeleteTextures(1,textures,0);
                    _inputSurfaceTextureId = 0;
                }

                if (_filterProgram != null) {
                    _filterProgram.release();
                    _filterProgram = null;
                }

                if (_mEGLSurface != null) {
                    renderThread.deleteOutputSurface(_mEGLSurface);
                    _mEGLSurface = null;
                }

                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }
}
