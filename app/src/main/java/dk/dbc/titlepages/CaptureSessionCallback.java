package dk.dbc.titlepages;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class CaptureSessionCallback extends CameraCaptureSession.CaptureCallback {
    /*
     * Using enums on Android is a point of contention. But the places
     * discouraging their use in the official documentation seem to have
     * disappeared over time.
     * Even though using enums requires more resources than plain int
     * constants as the Android source code does, I use an enum here for the
     * added type safety.
     */
    enum Mode {
        AWAITING_LOCK,
        AWAITING_PRETRIGGER,
        PICTURE_TAKEN
    }

    private final List<ImageCaptureSequenceListener> sequenceListeners;
    private Mode mode;

    CaptureSessionCallback() {
        sequenceListeners = new ArrayList<>();
    }

    private void process(CaptureResult result) {
        if(mode == Mode.AWAITING_LOCK) {
            final Integer focusState = result.get(CaptureResult.CONTROL_AF_STATE);
            if(focusState == null) {
                for(ImageCaptureSequenceListener listener : sequenceListeners) {
                    listener.startCapture();
                }
            } else if(focusState == CaptureResult
                    .CONTROL_AF_STATE_FOCUSED_LOCKED || focusState ==
                    CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                final Integer exposureState = result.get(CaptureResult
                    .CONTROL_AE_STATE);
                if(exposureState == null || exposureState == CaptureResult
                        .CONTROL_AE_STATE_CONVERGED) {
                    mode = Mode.PICTURE_TAKEN;
                    for(ImageCaptureSequenceListener listener : sequenceListeners) {
                        listener.startCapture();
                    }
                } else {
                    for (ImageCaptureSequenceListener listener : sequenceListeners) {
                        listener.startPrecapture();
                    }
                }
            }
        }
    }

    @Override
    public void onCaptureProgressed(@NonNull CameraCaptureSession session,
            @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
        process(partialResult);
    }

    @Override
    public void onCaptureCompleted(@NonNull CameraCaptureSession session,
            @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
        process(result);
    }

    void setMode(Mode mode) {
        this.mode = mode;
    }

    public void addListener(ImageCaptureSequenceListener listener) {
        sequenceListeners.add(listener);
    }
}
