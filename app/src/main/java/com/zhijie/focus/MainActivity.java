package com.zhijie.focus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import constants.Constants;

import static android.graphics.Color.RED;



public class MainActivity extends Activity implements OnClickListener {

    private final String TAG = "Main_Activity!";
    private EditText et_name;
    private TextView tv_feedback;

    private EditText et_time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStart = findViewById(R.id.btn_start);
        btnStart.setOnClickListener(this);

        Button btnNoMuse = findViewById(R.id.btnNoMuse);
        btnNoMuse.setOnClickListener(this);

        et_name = findViewById(R.id.nameField);
        tv_feedback = findViewById(R.id.tv_name_feedback);

        et_time = findViewById(R.id.ET_TrainingTime);


    }


    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.btn_start) {
            String name = et_name.getText().toString();

            if ( !(name == null) && !name.equals("")){
                Log.d(TAG, "Name:" + et_name.getText());
                Constants.USE_MUSE = true;
                Constants.ARITH_TEST_TIMEOUT = Constants.ARITH_REAL_TEST_TIMEOUT;
                Constants.ARITH_TRAINING_TIMEOUT = Constants.ARITH_REAL_TRAINING_TIMEOUT;
                Constants.GUIDED_MEDITATION_TRACK = Constants.GUIDED_REAL_MEDITATION_TRACK;

                Intent myIntent = new Intent(MainActivity.this,
                    Activity_Record_Data.class);
                myIntent.putExtra("name", name);
                startActivity(myIntent);

            }else {
                tv_feedback.setText(R.string.enter_name);
                tv_feedback.setTextColor(RED);
            }

        }

        if (v.getId() == R.id.btnNoMuse) {

            String timeText = et_time.getText().toString();
            if (!(timeText == null) && !timeText.equals("")) {
                Constants.ARITH_TEST_TIMEOUT = Integer.parseInt(timeText);
                Constants.ARITH_TRAINING_TIMEOUT = Integer.parseInt(timeText);
                Constants.GUIDED_MEDITATION_TRACK = R.raw.ting;

            } else {
                Constants.ARITH_TEST_TIMEOUT = 15;
                Constants.ARITH_TRAINING_TIMEOUT = 15;
                Constants.GUIDED_MEDITATION_TRACK = R.raw.ting;
            }
            Constants.USE_MUSE = false;

            Intent myIntent = new Intent(MainActivity.this,
                    Activity_Record_Data.class);
            myIntent.putExtra("name", "NoMuseTest");
            startActivity(myIntent);

        }

    }
}
