package dk.dbc.titlepages;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

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
    }

    @Override
    public void onClick(View view) {
        final long id = view.getId();
        final EditText bookIdEditText = findViewById(R.id.book_id);
        final String bookId = bookIdEditText.getText().toString();
        final Intent intent = new Intent(this, PictureTakerActivity.class);
        intent.putExtra(Constants.BOOK_ID_KEY, bookId);
        if(id == R.id.titlepage_picture_btn) {
            startActivityForResult(intent,
                CAPTURE_TITLEPAGE_PICTURE_REQUEST_CODE);
        } else if(id == R.id.colophon_picture_btn) {
            startActivityForResult(intent,
                CAPTURE_COLOPHON_PICTURE_REQUEST_CODE);
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
}
