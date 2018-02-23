package model;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.zhijie.focus.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import bsh.EvalError;
import bsh.Interpreter;

import static android.content.ContentValues.TAG;
import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;
import static constants.Constants.cd_interval;

public abstract class GenericArithmetic {

    protected TextView tv_qn_feedback;
    protected TextView tv_question;

    protected int num_consecutive_correct = 0;
    protected long avg_time_taken;
    protected List<Long> userTimeTaken;

    protected ProgressBar pb_qsn_timeout;

    public CountDownTimer cdt_qsn;

    public void setCdt_qsn(CountDownTimer cdt_qsn) {
        this.cdt_qsn = cdt_qsn;
    }

    public void setAvg_time_taken(long avg_time_taken) {
        this.avg_time_taken = avg_time_taken;
    }

    public void setPb_qsn_timeout(ProgressBar pb_qsn_timeout) {
        this.pb_qsn_timeout = pb_qsn_timeout;
    }

    int answer;
    long question_start_time;

    public void setTv_question(View tv_question) {
        Log.d(TAG, "Set tv question");
        this.tv_question = (TextView) tv_question;
    }

    public void setTv_qn_feedback(View tv_qn_feedback) {
        Log.d(TAG, "Set tv question feedback");
        this.tv_qn_feedback = (TextView) tv_qn_feedback;
    }

    public long getAvg_time_taken() {
        return avg_time_taken;
    }

    public int getAnswer() {
        return answer;
    }


    public long getQuestion_start_time() {
        return question_start_time;
    }

    public GenericArithmetic() {
        userTimeTaken = new ArrayList<>();

    }

    public void answer_question(int user_input) {

        if (user_input == answer) {
            tv_qn_feedback.setTextColor(GREEN);
            tv_qn_feedback.setText("Correct!!");
        } else {
            tv_qn_feedback.setTextColor(RED);
            tv_qn_feedback.setText("Wrong!!");
        }

        recordTime();

    }

    public int generate_questions() {
        Random r = new Random();

        answer = 0; //r.nextInt(9);
        String eqn;
        int curr;

        int a = r.nextInt(100);
        String b = gen_next_num(a, false);
        curr = eval(a + b);

        String c = gen_next_num(curr, false);
        curr = eval(a + b + c);

        String d = gen_next_num(curr, true);

        eqn = a + b + c + d;
        answer = eval(a + b + c + d);
        Log.d(TAG, eqn + " = " + answer);

        question_start_time = System.currentTimeMillis(); //Set Start time after question generation
        tv_question.setText(eqn);
        return answer;
//        return eqn;
    }

    public void cdt_repeat() {

        if (cdt_qsn != null)
            cdt_qsn.cancel();

        pb_qsn_timeout.setMax((int) avg_time_taken / cd_interval);
        pb_qsn_timeout.setProgress((int) avg_time_taken);
        cdt_qsn = new CountDownTimer(avg_time_taken, cd_interval) {
            @Override
            public void onTick(long millisUntilFinished) {
                int progress = (int) (millisUntilFinished / cd_interval);
                pb_qsn_timeout.setProgress(progress);

            }

            @Override
            public void onFinish() {
                Log.d(TAG, "ENDED!!");
                tv_qn_feedback.setText(R.string.tv_feedback_timeout);
                tv_qn_feedback.setTextColor(RED);

                //TODO NEW CLASS NEED TO REFACTOR THIS
                answered_consecutive_helper(-1);
//                tv_arith_question.setText(generate_questions());

                answer = generate_questions();
                question_start_time = getQuestion_start_time();
                cdt_repeat();
            }
        };

        cdt_qsn.start();
    }

    /**
     * Generate Next number with operator e.g. "+ 1" or "- 1". Generated number will be kept between 0 - 99.
     * TODO Add multiplications into the next number generation
     *
     * @param curr      Current sum of all digits.
     * @param final_num Next number is final, end result will make equation answer range from 0 - 9
     * @return "+ 1" or "- 1". Generated number will keep the sum between 0 - 99
     */

    private String gen_next_num(int curr, Boolean final_num) {

        int bound;
        String new_num = "";
        Random r = new Random();
        int opt = r.nextInt(2);

        if (final_num) {
            bound = 10;
        } else {
            bound = 100;
        }

        if (curr < 10) {

            if (opt == 0) {
                new_num = "+" + r.nextInt(bound - curr);
            } else if (opt == 1)
                new_num = "-" + r.nextInt(1 + curr);

        } else {
            if (final_num) { //Answer must be between 0 - 10
                new_num = "-" + (r.nextInt(10) + curr - 9);
            } else {
                if (opt == 0) {
                    new_num = "+" + r.nextInt(bound - curr);
                } else if (opt == 1)
                    new_num = "-" + r.nextInt(1 + curr);
            }

        }

        return new_num;
    }

    /**
     * Evaluate the provided equation and returns int.
     *
     * @param eqn String of equation to be solved.
     * @return Result of equation
     */
    private int eval(String eqn) {
        int ans = -1;
        try {
            Interpreter itpr = new Interpreter();
            ans = (int) itpr.eval(eqn);
        } catch (EvalError e) {
            Log.d(TAG, e.getErrorText());
        }
        return ans;
    }

    private void answered_consecutive_helper(int user_input) {
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

    }

    abstract public long calculate_time();

    /**
     * Actions to take into account for time
     */
    abstract public void recordTime();

}
