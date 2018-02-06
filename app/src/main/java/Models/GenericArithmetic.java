package Models;

import android.widget.TextView;

import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;


public abstract class GenericArithmetic {

    private TextView tv_qn_feedback;

    public GenericArithmetic(TextView tv_qn_feedback) {
        this.tv_qn_feedback = tv_qn_feedback;
    }

    public void answer_question(int ans, int user_input) {

        if (user_input == ans) {
            tv_qn_feedback.setTextColor(GREEN);
            tv_qn_feedback.setText("Correct!!");
        } else {
            tv_qn_feedback.setTextColor(RED);
            tv_qn_feedback.setText("Wrong!!");
        }
    }

    abstract public int calculate_time();

}
