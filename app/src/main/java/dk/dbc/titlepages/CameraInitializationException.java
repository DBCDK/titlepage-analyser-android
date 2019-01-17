package dk.dbc.titlepages;

public class CameraInitializationException extends Exception {
    public CameraInitializationException(String msg) {
        super(msg);
    }
    public CameraInitializationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
