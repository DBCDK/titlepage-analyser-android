package dk.dbc.titlepages;

import android.content.Context;
import android.hardware.camera2.CameraDevice;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;
import com.annimon.stream.Optional;

import java.lang.ref.WeakReference;

public class CameraDeviceStateCallback extends CameraDevice.StateCallback {
    private static final String TAG = Constants.TAG;
    private final WeakReference<PictureTakerActivity> pictureTakerActivityReference;
    private Optional<CameraDevice> cameraDevice = Optional.empty();

    public CameraDeviceStateCallback(PictureTakerActivity pictureTakerActivity) {
        pictureTakerActivityReference = new WeakReference<>(pictureTakerActivity);
    }

    public Optional<CameraDevice> getCameraDevice() {
        return cameraDevice;
    }

    @Override
    public void onOpened(@NonNull CameraDevice cameraDevice) {
        final PictureTakerActivity pictureTakerActivity =
            pictureTakerActivityReference.get();
        if(pictureTakerActivity != null && !pictureTakerActivity.isFinishing()) {
            this.cameraDevice = Optional.of(cameraDevice);
            // The method building ui should ideally not be called here, but
            // it has to be called after the camera device has been opened.
            pictureTakerActivity.buildUi();
        }
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice cameraDevice) {
        cameraDevice.close();
        this.cameraDevice = Optional.empty();
    }

    @Override
    public void onError(@NonNull CameraDevice cameraDevice, int error) {
        cameraDevice.close();
        this.cameraDevice = Optional.empty();
        final String reason = getCameraErrorReason(error);
        Log.e(TAG, String.format("Camera state error: %s", reason));
        final PictureTakerActivity pictureTakerActivity =
            pictureTakerActivityReference.get();
        if(pictureTakerActivity != null && !pictureTakerActivity.isFinishing()) {
            final String displayedReason = getCameraErrorReason(error,
                Optional.of(pictureTakerActivity));
            Toast.makeText(pictureTakerActivity, displayedReason,
                Toast.LENGTH_LONG).show();
        }
    }

    private String getCameraErrorReason(int error) {
        return getCameraErrorReason(error, Optional.empty());
    }

    private String getCameraErrorReason(int error,
            Optional<Context> contextOptional) {
        switch(error) {
            case ERROR_CAMERA_DEVICE:
                if(contextOptional.isPresent()) {
                    return contextOptional.get().getString(R.string.camera_state_error_fatal);
                } else {
                    return "fatal error";
                }
            case ERROR_CAMERA_DISABLED:
                if(contextOptional.isPresent()) {
                    return contextOptional.get().getString(R.string.camera_state_error_disabled);
                } else {
                    return "camera disabled";
                }
            case ERROR_CAMERA_IN_USE:
                if(contextOptional.isPresent()) {
                    return contextOptional.get().getString(R.string.camera_state_error_in_use);
                } else {
                    return "camera in use";
                }
            case ERROR_CAMERA_SERVICE:
                if(contextOptional.isPresent()) {
                    return contextOptional.get().getString(R.string.camera_state_error_service_fatal);
                } else {
                    return "fatal error in camera service. Device may need a restart.";
                }
            case ERROR_MAX_CAMERAS_IN_USE:
                if(contextOptional.isPresent()) {
                    return contextOptional.get().getString(R.string.camera_state_error_max_camera_devices);
                } else {
                    return "max number of camera devices reached";
                }
            default:
                if(contextOptional.isPresent()) {
                    return contextOptional.get().getString(R.string.camera_state_error_unknown);
                }
                return "unknown";
        }
    }
}
