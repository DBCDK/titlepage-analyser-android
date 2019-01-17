package dk.dbc.titlepages;

public class CapturePictureException extends Exception {
    public CapturePictureException(String msg) {
        super(msg);
    }
    public CapturePictureException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
