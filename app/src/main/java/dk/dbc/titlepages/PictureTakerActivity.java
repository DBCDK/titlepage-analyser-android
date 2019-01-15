package dk.dbc.titlepages;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;
import androidx.test.espresso.idling.CountingIdlingResource;
import com.android.camera.ShutterButton;
import com.annimon.stream.IntStream;
import com.annimon.stream.Optional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

/**
 * This activity is responsible for showing a preview for the main camera,
 * take a still picture and save it to a file.
 * It loans heavily from the Camera2Basic example application:
 * https://github.com/googlesamples/android-Camera2Basic/
 */
public class PictureTakerActivity extends Activity implements
        ImageReader.OnImageAvailableListener,
        TextureView.SurfaceTextureListener,
        ShutterButton.OnShutterButtonListener,
        ImageCaptureSequenceListener {
    private static final String TAG = Constants.TAG;
    private final int REQUEST_PERMISSIONS_CODE = 1;

    private CameraDeviceStateCallback cameraDeviceStateCallback =
        new CameraDeviceStateCallback(this);

    private CaptureSessionCallback captureSessionCallback =
        new CaptureSessionCallback();

    private CaptureSessionStateCallback captureSessionStateCallback;

    private TextureView textureView;
    private ImageReader imageReader;

    private static Handler backgroundHandler;

    CountingIdlingResource countingIdlingResource = new CountingIdlingResource(
        Constants.TAG);

    // This capture request builder is used to handle focusing the preview
    private Optional<CaptureRequest.Builder> preview = Optional.empty();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_taker);

        final boolean hasCameraPermission = checkSelfPermission(
            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        final String[] perms = {
            "android.permission.CAMERA",
        };
        requestPermissions(perms, REQUEST_PERMISSIONS_CODE);

        if(hasCameraPermission) {
            setup();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_PERMISSIONS_CODE) {
            if(IntStream.of(grantResults).allMatch(result -> result ==
                    PackageManager.PERMISSION_GRANTED)) {
                setup();
            } else {
                Log.w(TAG, "Permission not granted");
                new AlertDialog.Builder(this)
                    .setTitle(R.string.error_permission_not_granted)
                    .show();
            }
        } else {
            Log.w(TAG, String.format("Unrecognized request code %s", requestCode));
        }

    }

    // This method opens the camera which will then trigger building the ui
    // upon successfully opening one.
    private void setup() {
        countingIdlingResource.increment();
        captureSessionCallback.addListener(this);
        startThread();
        try {
            openCamera();
        } catch (CameraInitializationException e) {
            Log.e(TAG, String.format("Error opening camera: %s", e.toString()));
            Toast.makeText(this, getString(R.string.error_opening_camera),
                Toast.LENGTH_LONG).show();
        }
    }

    void buildUi() {
        textureView = findViewById(R.id.textureview);
        textureView.setSurfaceTextureListener(this);
        // onSurfaceTextureAvailable isn't called if the texture view is
        // already available since the listener will be registered too late.
        if(textureView.isAvailable()) {
            onSurfaceTextureAvailable(textureView.getSurfaceTexture(),
                textureView.getWidth(), textureView.getHeight());
        }

        final ShutterButton shutterButton = findViewById(R.id.shutter_button);
        shutterButton.setOnShutterButtonListener(this);
        countingIdlingResource.decrement();
    }

    private void startThread() {
        final HandlerThread backgroundThread = new HandlerThread(Constants.TAG);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private CaptureRequest.Builder createPreview() throws CameraInitializationException {
        if(!cameraDeviceStateCallback.getCameraDevice().isPresent()) {
            throw new CameraInitializationException("Camera not present");
        }
        final SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        if(surfaceTexture == null) {
            throw new CameraInitializationException("Error getting textureview surface");
        }
        final Surface surface = new Surface(surfaceTexture);
        try {
            final CameraDevice camera = cameraDeviceStateCallback
                .getCameraDevice().get();
            final CaptureRequest.Builder captureRequestBuilder = camera
                .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            configureCaptureSession(camera, surface, captureRequestBuilder);
            return captureRequestBuilder;
        } catch (CameraAccessException e) {
            throw new CameraInitializationException(String.format(
                "error creating capture request: %s", e.toString()), e);
        }
    }

    private void configureCaptureSession(CameraDevice camera, Surface surface,
            CaptureRequest.Builder captureRequestBuilder)
            throws CameraAccessException {
        captureSessionStateCallback = new CaptureSessionStateCallback(this,
            captureRequestBuilder, captureSessionCallback, backgroundHandler);
        camera.createCaptureSession(Arrays.asList(surface,
            imageReader.getSurface()), captureSessionStateCallback, null);
    }

    private void openCamera() throws CameraInitializationException {
        final CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if(cameraManager == null) {
            throw new CameraInitializationException("Error getting camera manager");
        }
        try {
            for(String id : cameraManager.getCameraIdList()) {
                final CameraCharacteristics characteristics = cameraManager
                    .getCameraCharacteristics(id);
                // characteristics.get returns a boxed Integer which might
                // be null. Therefore, don't unbox.
                final Integer facing = characteristics.get(
                    CameraCharacteristics.LENS_FACING);
                // ignore front-facing cameras
                if(facing != null && facing == CameraCharacteristics
                        .LENS_FACING_FRONT) {
                    continue;
                }
                final StreamConfigurationMap streamConfigurationMap =
                    characteristics.get(CameraCharacteristics
                    .SCALER_STREAM_CONFIGURATION_MAP);
                if(streamConfigurationMap == null) {
                    Log.w(TAG, String.format(
                        "stream configuration map for camera %s return null",
                        id));
                    continue;
                }
                final Size outputSize = Collections.max(Arrays.asList(
                    streamConfigurationMap.getOutputSizes(ImageFormat.JPEG)),
                    new SizeComparator());
                imageReader = ImageReader.newInstance(outputSize.getWidth(),
                    outputSize.getHeight(), ImageFormat.JPEG, 2);
                imageReader.setOnImageAvailableListener(this,
                    backgroundHandler);
                // TODO: check if image reader surface is ready. Otherwise an
                //  exception will be thrown when trying to open the camera.
                //  java.lang.IllegalArgumentException: Bad argument passed to camera service
                try {
                    cameraManager.openCamera(id, cameraDeviceStateCallback, backgroundHandler);
                    break;
                } catch (IllegalArgumentException | SecurityException e) {
                    throw new CameraInitializationException("Couldn't open camera", e);
                }
            }
        } catch (CameraAccessException e) {
            throw new CameraInitializationException("Error accessing camera", e);
        }
    }

    @Override
    public void onImageAvailable(ImageReader imageReader) {
        // TODO: save to the proper location and return it to the starting
        //  activity
        final File outputFile = new File(getFilesDir(), "titlepage-sender.jpg");
        final ImageSaverTask imageSaverTask = new ImageSaverTask(this,
            imageReader.acquireLatestImage(), outputFile);
        imageSaverTask.execute();
    }

    private static class ImageSaverTask extends AsyncTask<Void, Void, ResultHolder<Boolean>> {
        private final WeakReference<Context> contextReference;
        private final Image image;
        private final File outputFile;
        ImageSaverTask(Context context, Image image, File outputFile) {
            this.contextReference = new WeakReference<>(context);
            this.image = image;
            this.outputFile = outputFile;
        }

        @Override
        public ResultHolder<Boolean> doInBackground(Void... _void) {
            // A jpeg image should only have a single plane:
            // https://developer.android.com/reference/android/media/Image#getFormat()
            if(image.getPlanes().length != 1) {
                final IllegalStateException error = new IllegalStateException(
                    "Number of image planes doesn't correspond with image format");
                return new ResultHolder<>(error);
            }
            final ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
            final byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            try(final FileOutputStream outputStream = new FileOutputStream(
                    outputFile)) {
                outputStream.write(bytes);
                Log.d(TAG, String.format("Image written to file %s",
                    outputFile.getAbsolutePath()));
            } catch (IOException e) {
                return new ResultHolder<>(e);
            } finally {
                image.close();
            }
            return new ResultHolder<>(true);
        }

        @Override
        public void onPostExecute(ResultHolder<Boolean> resultHolder) {
            final Context context = contextReference.get();
            if(context != null) {
                resultHolder.getError().ifPresent(error ->
                    Toast.makeText(context, context.getString(
                        R.string.error_on_image_save, error.toString()),
                        Toast.LENGTH_LONG).show()
                );
                resultHolder.getObject().ifPresent(result -> {
                    if(result) {
                        Toast.makeText(context, context.getString(
                            R.string.image_saved), Toast.LENGTH_LONG).show();
                    } else {
                        // This should never happen
                        Toast.makeText(context, context.getString(
                            R.string.error_on_image_save_unknown_error),
                            Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    private static class ResultHolder<T> {
        private final Optional<T> object;
        private final Optional<Throwable> error;

        ResultHolder() {
            object = Optional.empty();
            error = Optional.empty();
        }

        ResultHolder(T object) {
            this.object = Optional.of(object);
            error = Optional.empty();
        }

        ResultHolder(Throwable error) {
            this.error = Optional.of(error);
            object = Optional.empty();
        }

        Optional<T> getObject() {
            return object;
        }

        Optional<Throwable> getError() {
            return error;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        try {
            preview = Optional.of(createPreview());
        } catch (CameraInitializationException e) {
            Log.e(TAG, String.format("Error creating camera preview: %s",
                e.toString()));
            Toast.makeText(this, getString(R.string
                .error_creating_camera_preview), Toast.LENGTH_LONG).show();
        }
    }
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {}
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return true;
    }
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {}

    @Override
    public void onShutterButtonFocus(boolean pressed) {
        // TODO: handle focus
    }
    @Override
    public void onShutterButtonClick() {
        try {
            captureStillPicture();
        } catch (CapturePictureException e) {
            Log.e(TAG, String.format("Error capturing image: %s", e.toString()));
            Toast.makeText(this, getString(R.string.error_on_image_capture),
                Toast.LENGTH_LONG).show();
        }
    }

    private void captureStillPicture() throws CapturePictureException {
        if(cameraDeviceStateCallback == null || captureSessionStateCallback == null) {
            throw new CapturePictureException("Camera not fully initiated");
        }
        if(!cameraDeviceStateCallback.getCameraDevice().isPresent()) {
            throw new CapturePictureException("Couldn't get camera device");
        }
        if(!captureSessionStateCallback.getSession().isPresent()) {
            throw new CapturePictureException("Couldn't get capture session");
        }
        final CameraCaptureSession session = captureSessionStateCallback
            .getSession().get();
        lockFocus(session);
    }

    private void lockFocus(CameraCaptureSession session) {
        if(preview.isPresent()) {
            final CaptureRequest.Builder previewBuilder = preview.get();
            previewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_START);
            captureSessionCallback.setMode(CaptureSessionCallback.Mode
                .AWAITING_LOCK);
            try {
                session.capture(previewBuilder.build(), captureSessionCallback,
                    backgroundHandler);
            } catch (CameraAccessException e) {
                Log.e(TAG, String.format("Error locking focus: %s",
                    e.toString()));
                Toast.makeText(this, getString(
                    R.string.camera_state_error_locking_focus),
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void startPrecapture() {
        final CameraCaptureSession session = captureSessionStateCallback
            .getSession().get();
        if(preview.isPresent()) {
            final CaptureRequest.Builder previewBuilder = preview.get();
            previewBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            captureSessionCallback.setMode(CaptureSessionCallback.Mode
                .AWAITING_PRETRIGGER);
            try {
                session.capture(previewBuilder.build(), captureSessionCallback,
                    backgroundHandler);
            } catch (CameraAccessException e) {
                Log.e(TAG, String.format("Error during precapture: %s",
                    e.toString()));
                // Show this as an error with focusing even though the real
                // problem is starting the auto-exposure sequence. The end user
                // will probably understand this message better when the error
                // reporting is so brief.
                Toast.makeText(this, getString(
                    R.string.camera_state_error_locking_focus),
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void startCapture() {
        final CameraDevice camera = cameraDeviceStateCallback
            .getCameraDevice().get();
        try {
            final CaptureRequest.Builder captureRequestBuilder = camera
                .createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(imageReader.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            final CameraCaptureSession session = captureSessionStateCallback
                .getSession().get();
            session.stopRepeating();
            session.abortCaptures();
            session.capture(captureRequestBuilder.build(),
                captureSessionCallback, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, String.format("Error capturing picture: %s",
                e.toString()));
            Toast.makeText(this, getString(R.string.error_on_image_capture),
                Toast.LENGTH_LONG).show();
        }
    }

    private class SizeComparator implements Comparator<Size> {
        @Override
        public int compare(Size a, Size b) {
            // signum returns -1 if the value is negative, 0 if it's zero
            // and 1 if it's positive.
            // cast to long to avoid overflows
            return Long.signum((long) a.getWidth() * a.getHeight() -
                (long) b.getWidth() * b.getHeight());
        }
    }
}
