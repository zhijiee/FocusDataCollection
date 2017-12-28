/**
 * Example of using libmuse library on android.
 * Interaxon, Inc. 2016
 */

package com.zhijie.focus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;


public class MainActivity extends Activity implements OnClickListener {

    private final String TAG = "TestLibMuseAndroid";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button recordData = (Button) findViewById(R.id.refresh);
        recordData.setOnClickListener(this);

    }


    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.refresh) {
            Intent myIntent = new Intent(MainActivity.this,
                    Activity_RecordData.class);
            startActivity(myIntent);
        }

    }
}
