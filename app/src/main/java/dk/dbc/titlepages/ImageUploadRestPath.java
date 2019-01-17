package dk.dbc.titlepages;

/**
 * This class models the rest interface for uploading images.
 * The server-side api is not ready yet, so this class is probably only
 * temporary.
 */
public class ImageUploadRestPath {
    private static final String BASE = "id";
    private static final String url = BuildConfig.uploadHost;
    private ImageUploadRestPath() {}
    static String titlePageEndpoint(String bookdId) {
        return String.format("%s/%s/%s/titlepage", url, BASE, bookdId);
    }
    static String colophonEndpoint(String bookId) {
        return String.format("%s/%s/%s/colophon", url, BASE, bookId);
    }
}
