package Models;

import android.widget.TextView;

public class ArithmeticTraining extends GenericArithmetic {

    private int currentCorrect = 0;

    public ArithmeticTraining(TextView tv_qn_feedback) {
        super(tv_qn_feedback);
    }

    @Override
    public int calculate_time() {
        int a = 0;

        return a;

    }

    public void incrementCorrect() {
        if (currentCorrect == 0) {
            currentCorrect++;
        } else {
            currentCorrect = 1;
        }

    }
}
