package com.bumblebeejuice.virtuo.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import java.nio.ByteBuffer;


public class PlayerActivity extends Activity implements SensorEventListener, TextureView.SurfaceTextureListener {

    final private Object textureViewSurfaceAvailableSync = new Object();

    private Surface textureViewSurface;
    private Point textureViewSurfaceSize;

    private LTRenderer renderer;
    private Surface rendererSurface;

    private MediaExtractor extractor;
    private MediaCodec decoder;

    private MediaExtractor audioExtractor;
    private MediaCodec audioDecoder;
    private AudioTrack audioTrack;

    private Thread audioFeeder;
    private Thread audioPlayer;

    private Thread feeder;
    private Thread player;

    private SensorManager sensorManager;
    private Sensor sensor;

    private LTSphereFilterProgram sphereFilterProgram;

    int playCount = 0;
    long audioTimestamp = 0;

    long lastVideoFrameTimeUs = -1;

    long durationUs = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getActionBar() != null)
            getActionBar().hide();

        View rootView = findViewById(R.id.rootView);
        rootView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );


        TextureView textureView = (TextureView) findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);

        synchronized (textureViewSurfaceAvailableSync) {
            if (textureViewSurface != null) {
                playMovie();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);

        tearDown();
    }


    private void tearDown() {
        // Start winding down the renderer

        // Stop video player first as it may be
        //  waiting on the audio player for a sync
        if (feeder != null) {
            feeder.interrupt();
            try {
                feeder.join();
            } catch (InterruptedException e) {
                LTErrorHandler.handleException(e);
            }
            feeder = null;
        }

        if (player != null) {
            player.interrupt();
            try {
                player.join();
            } catch (InterruptedException e) {
                LTErrorHandler.handleException(e);
            }
            player = null;
        }

        if (audioFeeder != null) {
            audioFeeder.interrupt();
            try {
                audioFeeder.join();
            } catch (InterruptedException e) {
                LTErrorHandler.handleException(e);
            }
            audioFeeder = null;
        }

        if (audioPlayer != null) {
            audioPlayer.interrupt();
            try {
                audioPlayer.join();
            } catch (InterruptedException e) {
                LTErrorHandler.handleException(e);
            }
            audioPlayer = null;
        }

        if (decoder != null) {
            decoder.stop();
            decoder.release();
            decoder = null;
        }

        if (audioDecoder != null) {
            audioDecoder.stop();
            audioDecoder.release();
            audioDecoder = null;
        }

        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }

        if (extractor != null) {
            extractor.release();
            extractor = null;
        }

        if (audioExtractor != null) {
            audioExtractor.release();
            audioExtractor = null;
        }

        if (rendererSurface != null) {
            rendererSurface.release();
            rendererSurface = null;
        }

        if (renderer != null) {
            renderer.release();
            renderer = null;
        }

        // Released by renderer, but we also need to release reference
        sphereFilterProgram = null;

        if (textureViewSurface != null) {
            textureViewSurface.release();
            textureViewSurface = null;
        }
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {

        synchronized (textureViewSurfaceAvailableSync) {
            textureViewSurface = new Surface(surfaceTexture);
        }

        textureViewSurfaceSize = new Point(width, height);
        playMovie();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {


        return true;
    }


    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }


    protected void playMovie() {

        renderer = new LTRenderer();
        renderer.setOutputSurface(textureViewSurface);
        renderer.getInputSurfaceTexture(new LTRenderer.OnSurfaceTextureAvailableListener() {
            @Override
            public void surfaceTextureAvailable(SurfaceTexture surfaceTexture, int surfaceTextureId) {
                surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        if (renderer != null) {
                            renderer.renderOnce();
                        }
                    }
                });
                rendererSurface = new Surface(surfaceTexture);

                //sphereFilterProgram = new LTSphereFilterProgram();
                /*float[] LensCenter = {0.25f, 0.5f};
                float[] ScreenCenter = {0.25f, 0.5f};
                float[] Scale = {1.0f, 1.0f};
                float[] ScaleIn = {1.0f, 1.0f};
                float[] HmdWarp = {1.0f, 0.22f, 0.24f, 0.0f};

                sphereFilterProgram = new BarrelShiftFilterProgram(
                        LensCenter, ScreenCenter, Scale, ScaleIn, HmdWarp
                );*/
                sphereFilterProgram = new LTSphereFilterProgram();
                renderer.setFilterProgram(sphereFilterProgram);

                renderer.setIgnoreInputSurfaceTexMatrix(true);
                playMovie(renderer);
            }
        });

        renderer.setOnFrameRenderListener(new LTRenderer.OnFrameRenderListener() {
            @Override
            public boolean shouldSwapBuffers(LTRenderer renderer, long timeNs) {
                return true;
            }

            @Override
            public long adjustedTimestamp(LTRenderer renderer, long timeNs) {
                return 0;
            }

            @Override
            public void frameRendered(LTRenderer renderer, long timeNs) {

            }

            @Override
            public void texImageChanged(LTRenderer renderer) {

            }
        });
    }


    protected void playMovie(final LTRenderer renderer) {

        try {
            String movie = getIntent().getData().getPath();

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(movie);
            durationUs = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) * 1000;
            retriever.release();

            extractor = new MediaExtractor();
            extractor.setDataSource(movie);

            MediaFormat format = extractor.getTrackFormat(0);
            decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));

            renderer.setTextureScaleAndCrop(textureViewSurfaceSize, new Point(format.getInteger("width") / 2, format.getInteger("height")), 2);
            decoder.configure(format, rendererSurface, null, 0);
            decoder.start();

            audioExtractor = new MediaExtractor();
            audioExtractor.setDataSource(movie);

            MediaFormat audioFormat = audioExtractor.getTrackFormat(1);
            audioDecoder = MediaCodec.createDecoderByType(audioFormat.getString(MediaFormat.KEY_MIME));
            audioDecoder.configure(audioFormat, null, null, 0);
            audioDecoder.start();

            extractor.selectTrack(0);
            audioExtractor.selectTrack(1);

            final int sampleRate = audioFormat.getInteger("sample-rate");
            final int channelCount = audioFormat.getInteger("channel-count");
            int channelSelect = channelCount == 2 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;

            int bufferSize = android.media.AudioTrack.getMinBufferSize(sampleRate, channelSelect, AudioFormat.ENCODING_PCM_16BIT);
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelSelect,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
            audioTrack.play();

            final ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
            final ByteBuffer[] audioDecoderInputBuffers = audioDecoder.getInputBuffers();

            // Audio thread
            audioFeeder = new Thread() {
                @Override
                public void run() {
                    boolean inputDone = false;
                    while (!inputDone && !isInterrupted()) {
                        int inputBufIndex = audioDecoder.dequeueInputBuffer(10000);
                        if (inputBufIndex >= 0) {

                            ByteBuffer inputBuf = audioDecoderInputBuffers[inputBufIndex];

                            int chunkSize = audioExtractor.readSampleData(inputBuf, 0);
                            long presentationTimeUs = audioExtractor.getSampleTime();
                            boolean syncFrame = (audioExtractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;

                            if (chunkSize > 0) {
                                int flags = syncFrame ? MediaCodec.BUFFER_FLAG_SYNC_FRAME : 0;

                                audioDecoder.queueInputBuffer(
                                        inputBufIndex,
                                        0,
                                        chunkSize,
                                        presentationTimeUs,
                                        flags);

                                audioExtractor.advance();
                            } else {
                                // To play once...
                                // End of stream -- send empty frame with EOS flag set.
                                //fDecoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                                //        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                //inputDone = true;

                                // To repeat...
                                audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                            }
                        }
                    }
                }
            };
            audioFeeder.start();


            audioPlayer = new Thread() {
                @Override
                public void run() {
                    String TAG = "VIRTUO";
                    boolean VERBOSE = false;
                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                    ByteBuffer[] audioDecoderOutputBuffers = audioDecoder.getOutputBuffers();
                    byte tmpBuffer[] = null;

                    boolean outputDone = false;
                    while (!outputDone && !isInterrupted()) {

                        // Timeout is 0 so that we quickly fall through.
                        int decoderStatus = audioDecoder.dequeueOutputBuffer(info, 10000);
                        if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            // no output available yet
                            if (VERBOSE) Log.d(TAG, "no output from decoder available");

                        } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                            audioDecoderOutputBuffers = audioDecoder.getOutputBuffers();
                            tmpBuffer = new byte[audioDecoderOutputBuffers[0].capacity()];

                            if (VERBOSE) Log.d(TAG, "decoder output buffers changed");
                        } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            MediaFormat newFormat = audioDecoder.getOutputFormat();
                            if (VERBOSE)
                                Log.d(TAG, "decoder output format changed: " + newFormat);
                        } else if (decoderStatus < 0) {
                            LTErrorHandler.handleException(new Exception("unexpected result from decoder.dequeueOutputBuffer: " + decoderStatus));
                        } else { // decoderStatus >= 0
                            if (VERBOSE)
                                Log.d(TAG, "textureViewSurface decoder given buffer " + decoderStatus +
                                        " (size=" + info.size + ")");
                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                if (VERBOSE) Log.d(TAG, "output EOS");
                            }

                            if (info.size != 0) {

                                ByteBuffer buffer = audioDecoderOutputBuffers[decoderStatus];
                                buffer.position(info.offset);
                                buffer.limit(info.offset + info.size);
                                buffer.get(tmpBuffer, 0, info.size);
                                audioTrack.write(tmpBuffer, 0, info.size);

                                audioTimestamp += (long) (1000000.0 * info.size / (sampleRate * channelCount * 2));

                                audioDecoder.releaseOutputBuffer(decoderStatus, false);
                            }

                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                // output from decoder is finished
                                outputDone = true;
                            }
                        }
                    }
                }
            };
            audioPlayer.start();


            // Feeder thread
            feeder = new Thread() {

                @Override
                public void run() {

                    boolean inputDone = false;
                    long lastPresentationTimeUs = 0;
                    while (!inputDone && !isInterrupted()) {

                        int inputBufIndex = decoder.dequeueInputBuffer(10000);
                        if (inputBufIndex >= 0) {
                            ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];

                            int chunkSize = extractor.readSampleData(inputBuf, 0);
                            long presentationTimeUs = extractor.getSampleTime();
                            boolean syncFrame = (extractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;

                            if (chunkSize > 0) {
                                int flags = syncFrame ? MediaCodec.BUFFER_FLAG_SYNC_FRAME : 0;

                                decoder.queueInputBuffer(
                                        inputBufIndex,
                                        0,
                                        chunkSize,
                                        presentationTimeUs,
                                        flags);

                                lastPresentationTimeUs = presentationTimeUs;
                                extractor.advance();
                            } else {
                                // To play once...
                                // End of stream -- send empty frame with EOS flag set.
                                //fDecoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                                //        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                //inputDone = true;

                                // To repeat...
                                lastVideoFrameTimeUs = lastPresentationTimeUs;
                                extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                            }
                        }
                    }
                }
            };
            feeder.start();


            player = new Thread() {
                @Override
                public void run() {

                    String TAG = "VIRTUO";
                    boolean VERBOSE = false;
                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

                    boolean outputDone = false;
                    while (!outputDone && !isInterrupted()) {

                        // Timeout is 0 so that we quickly fall through.
                        int decoderStatus = decoder.dequeueOutputBuffer(info, 10000);
                        if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            // no output available yet
                            if (VERBOSE) Log.d(TAG, "no output from decoder available");

                        } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                            // not important for us, since we're using Surface
                            if (VERBOSE) Log.d(TAG, "decoder output buffers changed");
                        } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            MediaFormat newFormat = decoder.getOutputFormat();
                            if (VERBOSE)
                                Log.d(TAG, "decoder output format changed: " + newFormat);
                        } else if (decoderStatus < 0) {
                            LTErrorHandler.handleException(new Exception("unexpected result from decoder.dequeueOutputBuffer: " + decoderStatus));
                        } else { // decoderStatus >= 0
                            if (VERBOSE)
                                Log.d(TAG, "textureViewSurface decoder given buffer " + decoderStatus +
                                        " (size=" + info.size + ")");
                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                if (VERBOSE) Log.d(TAG, "output EOS");
                            }

                            if (info.size != 0) {
                                while (info.presentationTimeUs + durationUs * playCount > audioTimestamp &&
                                        !isInterrupted()) {
                                }
                                decoder.releaseOutputBuffer(decoderStatus, true);
                            }

                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                // output from decoder is finished
                                outputDone = true;
                            }

                            if (info.presentationTimeUs == lastVideoFrameTimeUs) {
                                playCount++;
                            }
                        }
                    }
                }
            };

            player.start();

        } catch (Exception e) {
            LTErrorHandler.handleException(e);

            if (decoder != null) {
                decoder.stop();
                decoder.release();
            }

            if (extractor != null) {
                extractor.release();
            }

        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (sphereFilterProgram != null) {
            //Log.d("VIRTUO SENSOR", String.format("%f %f %f %f %f", event.values[0], event.values[1], event.values[2], event.values[3], event.values[4]));
            float[] rotationMatrix = new float[16];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.remapCoordinateSystem(rotationMatrix,
                    SensorManager.AXIS_Z, SensorManager.AXIS_MINUS_X,
                    rotationMatrix);

            SensorManager.remapCoordinateSystem(rotationMatrix,
                    SensorManager.AXIS_X, SensorManager.AXIS_MINUS_Z,
                    rotationMatrix);

            Matrix.rotateM(rotationMatrix, 0, 90, 1, 0, 0);
            sphereFilterProgram.setRotationMatrix(rotationMatrix);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


}
