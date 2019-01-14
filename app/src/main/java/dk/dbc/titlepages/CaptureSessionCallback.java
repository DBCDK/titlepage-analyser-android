package dk.dbc.titlepages;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.support.annotation.NonNull;

public class CaptureSessionCallback extends CameraCaptureSession.CaptureCallback {
    @Override
    public void onCaptureProgressed(@NonNull CameraCaptureSession session,
        @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {}

    @Override
    public void onCaptureCompleted(@NonNull CameraCaptureSession session,
        @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {}
}
