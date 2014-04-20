package com.bumblebeejuice.virtuo.app;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import java.nio.ByteBuffer;


public class MainActivity extends Activity {

    TextureView textureView;
    Surface surface;
    Point surfaceSize;

    LTRenderer renderer;
    Surface rendererSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getActionBar().hide();

        View rootView = findViewById(R.id.rootView);
        rootView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);


        textureView = (TextureView) findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                surface = new Surface(surfaceTexture);
                surfaceSize = new Point(width,height);
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
        });
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
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void playMovie() {

        renderer = new LTRenderer();
        renderer.setOutputSurface(surface);
        renderer.getInputSurfaceTexture(new LTRenderer.OnSurfaceTextureAvailableListener() {
            @Override
            public void surfaceTextureAvailable(SurfaceTexture surfaceTexture, int surfaceTextureId) {
                surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        renderer.renderOnce();
                    }
                });
                rendererSurface = new Surface(surfaceTexture);
                renderer.setFilterProgram(new LTSphereFilterProgram());
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

        MediaExtractor extractor = null;
        MediaCodec decoder = null;

        try {
            extractor = new MediaExtractor();
            AssetFileDescriptor fd = getResources().openRawResourceFd(R.raw.boxing);
            extractor.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            extractor.selectTrack(0);

            MediaFormat format = extractor.getTrackFormat(0);
            decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));

            decoder.configure(format, rendererSurface, null, 0);
            decoder.start();

            final ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();

            final MediaCodec fDecoder = decoder;
            final MediaExtractor fExtractor = extractor;

            // Feeder thread
            Thread feeder = new Thread() {

                @Override
                public void run() {

                    boolean inputDone = false;
                    while (!inputDone && !isInterrupted()) {
                        int inputBufIndex = fDecoder.dequeueInputBuffer(10000);
                        if (inputBufIndex >= 0) {

                            ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                            int chunkSize = fExtractor.readSampleData(inputBuf, 0);
                            long presentationTimeUs = fExtractor.getSampleTime();
                            boolean syncFrame = (fExtractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;

                            if (chunkSize > 0) {
                                int flags = syncFrame ? MediaCodec.BUFFER_FLAG_SYNC_FRAME : 0;

                                fDecoder.queueInputBuffer(
                                        inputBufIndex,
                                        0,
                                        chunkSize,
                                        presentationTimeUs,
                                        flags);

                                fExtractor.advance();
                            } else {
                                // End of stream -- send empty frame with EOS flag set.
                                    /*fDecoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                    inputDone = true;*/
                                fExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                            }
                        }
                    }

                }
            };
            feeder.start();


            Thread player = new Thread() {
                @Override
                public void run() {

                    String TAG = "VIRTUO";
                    boolean VERBOSE = false;
                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

                    boolean outputDone = false;
                    while (!outputDone && !isInterrupted()) {

                        // Timeout is 0 so that we quickly fall through.
                        int decoderStatus = fDecoder.dequeueOutputBuffer(info, 10000);
                        if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            // no output available yet
                            if (VERBOSE) Log.d(TAG, "no output from decoder available");

                        } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                            // not important for us, since we're using Surface
                            if (VERBOSE) Log.d(TAG, "decoder output buffers changed");
                        } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            MediaFormat newFormat = fDecoder.getOutputFormat();
                            if (VERBOSE)
                                Log.d(TAG, "decoder output format changed: " + newFormat);
                        } else if (decoderStatus < 0) {
                            LTErrorHandler.handleException(new Exception("unexpected result from decoder.dequeueOutputBuffer: " + decoderStatus));
                        } else { // decoderStatus >= 0
                            if (VERBOSE)
                                Log.d(TAG, "surface decoder given buffer " + decoderStatus +
                                        " (size=" + info.size + ")");
                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                if (VERBOSE) Log.d(TAG, "output EOS");
                            }

                            if (info.size != 0) {
                                fDecoder.releaseOutputBuffer(decoderStatus, true);
                            }

                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                // output from decoder is finished
                                outputDone = true;
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
}
