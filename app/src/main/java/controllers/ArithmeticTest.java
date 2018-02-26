package controllers;

import android.util.Log;

public class ArithmeticTest extends GenericArithmetic {

    String TAG = "ARITHMETIC_TEST_SESSION";
    private float numCorrect = 0;
    private float numAnswered = 0;

    @Override
    public void updateAverageTime(int user_input) {

        if (user_input == answer) { // User answer question correctly

            if (num_consecutive_correct < 0) //If previous answered wrongly
                num_consecutive_correct = 1;
            else
                num_consecutive_correct++; // Previously correct, increment

        } else { //User answered the question wrongly
            if (num_consecutive_correct > 0) { //If previously answered correctly
                Log.d(TAG, "num:" + num_consecutive_correct);
                num_consecutive_correct = -1;
            } else {
                Log.d(TAG, "num:" + num_consecutive_correct);
                num_consecutive_correct--;
            }
        }
        // Increase or decrease the time taken with 3 consecutively correct or wrong.
        long timeChange = avg_time_taken / 10;
        if (num_consecutive_correct <= -3) {
            avg_time_taken += timeChange;
            Log.d(TAG, "Time increased to " + avg_time_taken);

        } else if (num_consecutive_correct >= 3) {
            avg_time_taken -= timeChange;
            Log.d(TAG, "Time decreased to " + avg_time_taken);
        }
        cdt_repeat();
        updateScore(user_input == answer);
    }

    public void updateScore(boolean correct) {
        if (correct)
            numCorrect++;
        numAnswered++;
        int score = (int) ((numCorrect / numAnswered) * 100);
        tv_test_score.setText(score + "%");
    }

}
