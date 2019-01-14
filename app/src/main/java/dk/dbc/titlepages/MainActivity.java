package dk.dbc.titlepages;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity implements View.OnClickListener {
    private final static String TAG = Constants.TAG;

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
        if(id == R.id.titlepage_picture_btn) {
            final Intent intent = new Intent(this, PictureTakerActivity.class);
            startActivity(intent);
        } else if(id == R.id.colophon_picture_btn) {
            final Intent intent = new Intent(this, PictureTakerActivity.class);
            startActivity(intent);
        } else {
            Log.w(TAG, String.format("Clicked unknown view with id %s",
                id));
        }
    }
}
