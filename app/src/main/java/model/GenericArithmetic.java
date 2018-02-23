package model;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.Random;

import bsh.EvalError;
import bsh.Interpreter;

import static android.content.ContentValues.TAG;
import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;

public abstract class GenericArithmetic {

    private TextView tv_qn_feedback;
    private TextView tv_question;


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


    public int getAnswer() {
        return answer;
    }


    public long getQuestion_start_time() {
        return question_start_time;
    }

    public GenericArithmetic() {
    }

    public void answer_question(int user_input) {

        if (user_input == answer) {
            tv_qn_feedback.setTextColor(GREEN);
            tv_qn_feedback.setText("Correct!!");
        } else {
            tv_qn_feedback.setTextColor(RED);
            tv_qn_feedback.setText("Wrong!!");
        }
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



    abstract public int calculate_time();

}
