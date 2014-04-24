package com.bumblebeejuice.virtuo.app;

import android.graphics.Point;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Created by robcavin on 3/3/14.
 */
public class LTRenderThread extends Thread {

    protected static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    protected static final int EGL_OPENGL_ES2_BIT = 4;

    private EGLDisplay mEglDisplay;
    private EGLConfig mEglConfig;
    private EGLContext mEglContext;

    public Handler handler;

    @Override
    public void run() {
        setName("Lightt Render Thread");

        Looper.prepare();

        handler = new Handler();

        initGL();

        Looper.loop();
    }

    // Initialization
    //
    private void initGL() {

        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed "
                    + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }

        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
            throw new RuntimeException("eglInitialize failed " +
                    GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }

        mEglConfig = chooseEglConfig();
        if (mEglConfig == null) {
            throw new RuntimeException("eglConfig not initialized");
        }

        mEglContext = createContext(mEglDisplay, mEglConfig);
    }

    public EGLSurface createOutputSurface(Object outputSurface) {
        int[] eglSurfaceAttribList = new int[]{
                EGL14.EGL_NONE
        };

        EGLSurface eglSurface = EGL14.eglCreateWindowSurface(mEglDisplay, mEglConfig, outputSurface, eglSurfaceAttribList, 0);

        if (eglSurface == null || eglSurface == EGL14.EGL_NO_SURFACE) {
            int error = EGL14.eglGetError();
            if (error == EGL14.EGL_BAD_NATIVE_WINDOW) {
                Log.e("LIGHTT RENDER CONTEXT", "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
                return null;
            }
            throw new RuntimeException("createWindowSurface failed "
                    + GLUtils.getEGLErrorString(error));
        }

        return eglSurface;
    }

    public void deleteOutputSurface(EGLSurface surface) {
        if (mEglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT);

            EGL14.eglDestroySurface(mEglDisplay, surface);
        }
    }

    public Point makeCurrent(EGLSurface surface) {
        Point sizeUpdate = null;
        if (!mEglContext.equals(EGL14.eglGetCurrentContext()) ||
                !surface.equals(EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW))) {
            if (!EGL14.eglMakeCurrent(mEglDisplay, surface, surface, mEglContext)) {
                throw new RuntimeException("eglMakeCurrent failed "
                        + GLUtils.getEGLErrorString(EGL14.eglGetError()));
            }

            int width[] = new int[1];
            int height[] = new int[1];
            EGL14.eglQuerySurface(mEglDisplay,surface,EGL14.EGL_WIDTH,width,0);
            EGL14.eglQuerySurface(mEglDisplay,surface,EGL14.EGL_HEIGHT,height,0);
            sizeUpdate = new Point(width[0],height[0]);
        }
        return sizeUpdate;
    }

    EGLContext createContext(EGLDisplay eglDisplay, EGLConfig eglConfig) {
        int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE};
        return EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, attrib_list, 0);
    }

    private EGLConfig chooseEglConfig() {
        int[] configsCount = new int[1];
        EGLConfig[] configs = new EGLConfig[1];
        int[] configSpec = getConfig();
        if (!EGL14.eglChooseConfig(mEglDisplay, configSpec, 0, configs, 0, 1, configsCount, 0)) {
            throw new IllegalArgumentException("eglChooseConfig failed " +
                    GLUtils.getEGLErrorString(EGL14.eglGetError()));
        } else if (configsCount[0] > 0) {
            return configs[0];
        }
        return null;
    }

    private int[] getConfig() {
        final int EGL_RECORDABLE_ANDROID = 0x3142;

        return new int[]{
                EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 0,
                EGL14.EGL_STENCIL_SIZE, 0,
                EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE
        };
    }

    // Helpers
    //
    public void checkGlError(String op) {

        int error;
        if ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("LIGHTT RENDER CONTEXT", op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    public void checkEglError(String msg) {
        int error;
        if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }

    public void swapBuffers(EGLSurface surface) {
        if (!EGL14.eglSwapBuffers(mEglDisplay, surface)) {
            throw new RuntimeException("Cannot swap buffers");
        }
        checkEglError("eglSwapBuffers");
    }

    public void setTimestamp(EGLSurface surface, long timeUs) {
        EGLExt.eglPresentationTimeANDROID(mEglDisplay, surface, timeUs);
        checkEglError("eglPresentationTimeANDROID");
    }

    /**
     * Discards all resources held by this class, notably the EGL context.  Also releases the
     * Surface that was passed to our constructor.
     */
    private void releaseStuff() {

        if (mEglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT);

            EGL14.eglDestroyContext(mEglDisplay, mEglContext);

            EGL14.eglTerminate(mEglDisplay);

            // RDC - We SHOULD be calling the below to remove a small amount of state retained per thread,
            //  but this crashes the app every time for whatever reason.  There is a good chance
            //  that the state is freed when the thread terminates anyway, but could use more investigation
            //EGL14.eglReleaseThread();
        }

        mEglDisplay = EGL14.EGL_NO_DISPLAY;
        mEglContext = EGL14.EGL_NO_CONTEXT;
    }

    public void release() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                releaseStuff();
                Looper.myLooper().quitSafely();
            }
        });
    }


}
