package model;

import android.util.Log;


public class ArithmeticTraining extends GenericArithmetic {

    String TAG = "ARITHMETIC_TRAINING_SESSION";


    @Override
    public void updateAverageTime(int user_input) {

        // Record user time taken in milliseconds.
        userTimeTaken.add(System.currentTimeMillis() - question_start_time);
        Log.d(TAG, "Time: " + (System.currentTimeMillis() - question_start_time));

    }

}
