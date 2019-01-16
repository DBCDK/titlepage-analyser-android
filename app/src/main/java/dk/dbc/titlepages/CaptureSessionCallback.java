package dk.dbc.titlepages;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.support.annotation.NonNull;
import com.annimon.stream.Optional;

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

    private Optional<ImageCaptureSequenceListener> sequenceListener = Optional.empty();
    private Mode mode;

    private void process(CaptureResult result) {
        if(mode == Mode.AWAITING_LOCK) {
            final Integer focusState = result.get(CaptureResult.CONTROL_AF_STATE);
            // Check if focusState is null to avoid null pointers when unboxing
            if(focusState != null && (focusState == CaptureResult
                    .CONTROL_AF_STATE_FOCUSED_LOCKED || focusState ==
                    CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED)) {
                final Integer exposureState = result.get(CaptureResult
                    .CONTROL_AE_STATE);
                if(exposureState == null || exposureState == CaptureResult
                        .CONTROL_AE_STATE_CONVERGED) {
                    mode = Mode.PICTURE_TAKEN;
                    sequenceListener.ifPresent(listener -> listener.startCapture());
                } else {
                    sequenceListener.ifPresent(listener -> listener.startPrecapture());
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

    public void setListener(ImageCaptureSequenceListener listener) {
        sequenceListener = Optional.of(listener);
    }
}
