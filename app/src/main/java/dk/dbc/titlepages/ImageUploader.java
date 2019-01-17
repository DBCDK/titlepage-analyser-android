package dk.dbc.titlepages;

import android.support.annotation.NonNull;
import com.annimon.stream.Optional;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

import java.io.IOException;
import java.io.InputStream;

public class ImageUploader {
    private final OkHttpClient client;

    ImageUploader() {
        client = new OkHttpClient();
    }

    public Optional<String> upload(String url, InputStream inputStream)
            throws UploadError {
        final RequestBody body = new StreamBody(inputStream);
        try {
            final Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
            try(final Response response = client.newCall(request).execute()) {
                // TODO: retry on certain conditions
                if(response.code() != 200) {
                    throw new UploadError(String.format(
                        "Server responded with error code %s", response.code()));
                }
                if(response.body() != null) {
                    return Optional.of(response.body().string());
                } else {
                    return Optional.empty();
                }
            } catch (IOException e) {
                throw new UploadError(String.format("Uploading to %s failed",
                    url), e);
            }
        } catch (IllegalArgumentException e) {
            throw new UploadError("Error building request", e);
        }
    }

    private static class StreamBody extends RequestBody {
        private final InputStream inputStream;

        StreamBody(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public MediaType contentType() {
            return MediaType.get("application/octet-stream");
        }

        @Override
        public long contentLength() {
            try {
                return inputStream.available();
            } catch (IOException e) {
                return 0;
            }
        }

        @Override
        public void writeTo(@NonNull BufferedSink sink) throws IOException {
            Source source = null;
            try {
                source = Okio.source(inputStream);
                sink.writeAll(source);
            } finally {
                Util.closeQuietly(source);
            }
        }
    }

    static class UploadError extends Exception {
        UploadError(String msg) {
            super(msg);
        }
        UploadError(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}
