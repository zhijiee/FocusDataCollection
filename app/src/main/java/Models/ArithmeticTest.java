package Models;

public class ArithmeticTest extends GenericArithmetic {

    private int currentCorrect = 0;

    public ArithmeticTest() {
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
