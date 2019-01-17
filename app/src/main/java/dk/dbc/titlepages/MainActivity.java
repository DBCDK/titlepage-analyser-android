package dk.dbc.titlepages;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.annimon.stream.Optional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity implements View.OnClickListener {
    private final static String TAG = Constants.TAG;
    private final static int CAPTURE_TITLEPAGE_PICTURE_REQUEST_CODE = 1;
    private final static int CAPTURE_COLOPHON_PICTURE_REQUEST_CODE = 2;

    // map for storing one or two filenames of the captured images
    final Map<String, String> resultImageFilenames = new HashMap<>(2);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button titlepage_picture_button = findViewById(R.id.titlepage_picture_btn);
        titlepage_picture_button.setOnClickListener(this);

        final Button colophon_picture_button = findViewById(R.id.colophon_picture_btn);
        colophon_picture_button.setOnClickListener(this);

        final Button sendButton = findViewById(R.id.send);
        sendButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        final long id = view.getId();
        final EditText bookIdEditText = findViewById(R.id.book_id);
        final String bookId = bookIdEditText.getText().toString();
        final Intent intent = new Intent(this, PictureTakerActivity.class);
        intent.putExtra(Constants.BOOK_ID_KEY, bookId);
        if(id == R.id.titlepage_picture_btn) {
            intent.putExtra(Constants.IMAGE_TYPE_KEY,
                Constants.IMAGE_TYPE_TITLEPAGE);
            startActivityForResult(intent,
                CAPTURE_TITLEPAGE_PICTURE_REQUEST_CODE);
        } else if(id == R.id.colophon_picture_btn) {
            intent.putExtra(Constants.IMAGE_TYPE_KEY,
                Constants.IMAGE_TYPE_COLOPHON);
            startActivityForResult(intent,
                CAPTURE_COLOPHON_PICTURE_REQUEST_CODE);
        } else if(id == R.id.send) {
            final TextView resultTextview = findViewById(R.id.resultTextview);
            if(resultImageFilenames.containsKey(Constants.TITLEPAGE_FILENAME_KEY)) {
                final UploadTask titlePageUploadTask = new UploadTask(
                    ImageUploadRestPath.titlePageEndpoint(bookId),
                    resultImageFilenames.get(Constants.TITLEPAGE_FILENAME_KEY), resultTextview, this);
                titlePageUploadTask.execute();
            }
            if(resultImageFilenames.containsKey(Constants.COLOPHON_FILENAME_KEY)) {
                final UploadTask titlePageUploadTask = new UploadTask(
                    ImageUploadRestPath.colophonEndpoint(bookId),
                    resultImageFilenames.get(Constants.COLOPHON_FILENAME_KEY), resultTextview, this);
                titlePageUploadTask.execute();
            }
        } else {
            Log.w(TAG, String.format("Clicked unknown view with id %s",
                id));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK && (
                requestCode == CAPTURE_TITLEPAGE_PICTURE_REQUEST_CODE ||
                requestCode == CAPTURE_COLOPHON_PICTURE_REQUEST_CODE)) {
            if (data.hasExtra(Constants.RESULT_FILENAME_KEY)) {
                final String filename = data.getStringExtra(Constants
                    .RESULT_FILENAME_KEY);
                if (requestCode == CAPTURE_TITLEPAGE_PICTURE_REQUEST_CODE) {
                    resultImageFilenames.put(Constants.TITLEPAGE_FILENAME_KEY,
                        filename);
                } else {
                    resultImageFilenames.put(Constants.COLOPHON_FILENAME_KEY, filename);
                }
            } else {
                Log.w(TAG, String.format(
                    "Activity result for %s was successful but result data is missing filename",
                    requestCode));
            }
        }
    }

    static class UploadTask extends AsyncTask<Void, Void, ResultHolder<String>> {
        private final String endpoint;
        private final String filename;
        private final WeakReference<TextView> textViewReference;
        private final WeakReference<Context> contextReference;

        UploadTask(String endpoint, String filename, TextView textView, Context context) {
            this.endpoint = endpoint;
            this.filename = filename;
            textViewReference = new WeakReference<>(textView);
            contextReference = new WeakReference<>(context);
        }

        @Override
        public ResultHolder<String> doInBackground(Void... _void) {
            final ImageUploader imageUploader = new ImageUploader();
            final File titlePage = new File(filename);
            try(final FileInputStream fileInputStream = new FileInputStream(titlePage)) {
                final Optional<String> result = imageUploader.upload(
                    endpoint, fileInputStream);
                if(result.isPresent()) {
                    return new ResultHolder<>(result.get());
                }
            } catch (ImageUploader.UploadError | IOException e) {
                return new ResultHolder<>(e);
            }
            return new ResultHolder<>();
        }

        @Override
        public void onPostExecute(ResultHolder<String> resultHolder) {
            resultHolder.getObject().ifPresent(text -> {
                final TextView textView = textViewReference.get();
                if(textView != null) {
                    // TODO: make a better presentation of the results
                    textView.append(text);
                    textView.append("\n\n");
                }
            });
            resultHolder.getError().ifPresent(error -> {
                Log.e(TAG, String.format("Couldn't upload image: %s", error.toString()));
                final Context context = contextReference.get();
                if(context != null) {
                    Toast.makeText(context, context.getString(
                        R.string.error_on_image_upload), Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
