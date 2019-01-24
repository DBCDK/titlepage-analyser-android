package dk.dbc.titlepages;

import com.annimon.stream.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ImageUploaderTest {
    @Rule
    public MockWebServer mockWebServer = new MockWebServer();

    @Test
    public void test_upload() throws ImageUploader.UploadError, InterruptedException {
        final MockResponse mockResponse = new MockResponse()
            .setBody("Mr. Flying Dutchman")
            .setResponseCode(200);
        mockWebServer.enqueue(mockResponse);

        final ImageUploader imageUploader = new ImageUploader();
        final InputStream inputStream = new ByteArrayInputStream(
            "Sally Dutchman".getBytes());
        final Optional<String> result = imageUploader.upload(mockWebServer
            .url("/bikini-bottom").toString(), inputStream);

        assertThat("result is present", result.isPresent(), is(true));
        assertThat("result", result.get(), is("Mr. Flying Dutchman"));

        final RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat("path", recordedRequest.getPath(), is("/bikini-bottom"));
        assertThat("body", recordedRequest.getBody().readString(
            Charset.defaultCharset()), is("Sally Dutchman"));
    }

    @Test
    public void test_upload_error() {
        final MockResponse mockResponse = new MockResponse()
            .setResponseCode(500);
        mockWebServer.enqueue(mockResponse);

        final ImageUploader imageUploader = new ImageUploader();
        final InputStream inputStream = new ByteArrayInputStream(
            "Sally Dutchman".getBytes());
        try {
            final Optional<String> result = imageUploader.upload(mockWebServer
                .url("/bikini-bottom").toString(), inputStream);
            fail("expected UploadError to be thrown");
        } catch (ImageUploader.UploadError e) {}
    }

    @Test
    public void test_upload_invalidUrl() {
        final ImageUploader imageUploader = new ImageUploader();
        try {
            final InputStream inputStream = new ByteArrayInputStream(
                "Sally Dutchman".getBytes());
            imageUploader.upload("no-protocol.com", inputStream);
            fail("Expected UploadError");
        } catch (ImageUploader.UploadError e) {}
    }
}
