package model;

import android.util.Log;


public class ArithmeticTraining extends GenericArithmetic {

    private int currentCorrect = 0;
    String TAG = "ARITHMETIC_TRAINING_SESSION";

    public ArithmeticTraining() {
    }

    @Override
    public long calculate_time() {
        //Calculate time taken for each question for the Test later.

        long total_time_taken = 0;
        for (int i = 0; i < userTimeTaken.size(); i++) {
            total_time_taken += userTimeTaken.get(i);
        }

        if (userTimeTaken.size() == 0) {
            avg_time_taken = 8000;
        } else {
            avg_time_taken = total_time_taken / userTimeTaken.size();
        }
        Log.d(TAG, "Avg time: " + avg_time_taken);

        return avg_time_taken;

    }

    @Override
    public void recordTime() {

        // Record user time taken in milliseconds.
        userTimeTaken.add(System.currentTimeMillis() - question_start_time);
        Log.d(TAG, "Time: " + (System.currentTimeMillis() - question_start_time));

    }

}
