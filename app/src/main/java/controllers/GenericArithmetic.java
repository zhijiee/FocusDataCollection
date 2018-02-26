package controllers;

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
import static constants.Constants.FINAL_VALUE;
import static constants.Constants.MAX_GENERATED_NUM;
import static constants.Constants.cd_interval;

public abstract class GenericArithmetic {

    int answer;
    long question_start_time;

    protected TextView tv_qn_feedback;
    protected TextView tv_question;

    protected int num_consecutive_correct = 0;
    protected long avg_time_taken;
    protected List<Long> userTimeTaken;

    protected ProgressBar pb_qsn_timeout;

    public CountDownTimer cdt_qsn;

    public void setAvg_time_taken(long avg_time_taken) {
        this.avg_time_taken = avg_time_taken;
    }

    public void setPb_qsn_timeout(ProgressBar pb_qsn_timeout) {
        this.pb_qsn_timeout = pb_qsn_timeout;
    }

    public void setTv_question(View tv_question) {
        this.tv_question = (TextView) tv_question;
    }

    public void setTv_qn_feedback(View tv_qn_feedback) {
        this.tv_qn_feedback = (TextView) tv_qn_feedback;
    }

    public GenericArithmetic() {
        userTimeTaken = new ArrayList<>();

    }

    public void answer_question(int user_input) {

        if (user_input == answer) {
            tv_qn_feedback.setTextColor(GREEN);
            tv_qn_feedback.setText("Correct!!");
            updateAverageTime(user_input);
            generate_questions();
        } else {
            tv_qn_feedback.setTextColor(RED);
            tv_qn_feedback.setText("Wrong!!");
        }


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
//                Log.d(TAG, "ENDED!!");
                tv_qn_feedback.setText(R.string.tv_feedback_timeout);
                tv_qn_feedback.setTextColor(RED);

                updateAverageTime(-1);
//                tv_arith_question.setText(generate_questions());

//                answer = generate_questions();
                generate_questions();
//                question_start_time = getQuestion_start_time();
                cdt_repeat();
            }
        };

        cdt_qsn.start();
    }

    public void generate_questions() {
        Random r = new Random();

        answer = 0; //r.nextInt(9);
        String eqn;
        int curr;

        int a = r.nextInt(MAX_GENERATED_NUM);

        String b = gen_next_num(a, false);
        curr = eval(a + b);

        String c = gen_next_num(curr, true);
        eqn = a + b + c;
        answer = eval(a + b + c);


//        String c = gen_next_num(curr, false);
//        curr = eval(a + b + c);
//
//        String d = gen_next_num(curr, true);
//        eqn = a + b + c + d;
//        answer = eval(a + b + c + d);

        Log.d(TAG, eqn + " = " + answer);

        question_start_time = System.currentTimeMillis(); //Set Start time after question generation
        tv_question.setText(eqn);
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
            bound = FINAL_VALUE;
        } else {
            bound = MAX_GENERATED_NUM;
        }

        if (curr < 10) {

            if (opt == 0) {
                new_num = "+" + r.nextInt(bound - curr);
            } else if (opt == 1)
                new_num = "-" + r.nextInt(curr + 1);

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

    public long get_avg_time() {
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
    /**
     * Actions to take into account for time
     */

    public abstract void updateAverageTime(int user_input);


}
