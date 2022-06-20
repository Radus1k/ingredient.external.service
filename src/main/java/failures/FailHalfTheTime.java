package failures;

import java.util.Random;

public class FailHalfTheTime implements failures.PotentialFailure {
    Random random = new Random();
    int times;
    int failedCount;

    public FailHalfTheTime(int times) {
        this.times = times;
    }

    @Override
    public void occur() {
        if (failedCount++ < times && random.nextInt() % 2 == 0) {
            throw new RuntimeException("Operation failed");
        }
    }

    public static void main(String[] args) {
        failures.PotentialFailure failure = new FailHalfTheTime(4);
        failure.occur();
    }
}