package dk.dbc.titlepages;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;
import com.annimon.stream.Optional;

public class CaptureSessionStateCallback extends CameraCaptureSession.StateCallback {
    private static final String TAG = Constants.TAG;

    private final Context context;
    private final CaptureRequest.Builder captureRequestBuilder;
    private final CaptureSessionCallback captureSessionCallback;
    private final Handler handler;
    private boolean closed;

    private Optional<CameraCaptureSession> session = Optional.empty();

    public CaptureSessionStateCallback(Context context,
            CaptureRequest.Builder captureRequestBuilder,
            CaptureSessionCallback captureSessionCallback, Handler handler) {
        this.context = context;
        this.captureRequestBuilder = captureRequestBuilder;
        this.captureSessionCallback = captureSessionCallback;
        this.handler = handler;
    }

    public Optional<CameraCaptureSession> getSession() {
        return session;
    }

    @Override
    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
        final CaptureRequest captureRequest = captureRequestBuilder.build();
        try {
            if(!closed) {
                cameraCaptureSession.setRepeatingRequest(captureRequest,
                    captureSessionCallback, handler);
                session = Optional.of(cameraCaptureSession);
            }
        } catch (CameraAccessException | IllegalStateException e) {
            // Catching the IllegalStateException here is probably sign that
            // there is a bug somewhere where the session gets closed without
            // being marked as closed.
            Log.e(TAG, String.format("Error accessing camera: %s",
                e.toString()));
            Toast.makeText(context, context.getString(
                R.string.error_opening_camera), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
        cameraCaptureSession.close();
        closed = true;
        session = Optional.empty();
        final String message = "Failed to configure session";
        Log.e(TAG, message);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onClosed(@NonNull CameraCaptureSession cameraCaptureSession) {
        // Once a capture session has been closed all methods called
        // on its instance will throw IllegalStateException.
        cameraCaptureSession.close();
        closed = true;
        session = Optional.empty();
    }
}
