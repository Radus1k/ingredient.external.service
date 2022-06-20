package failures;

public class FailNTimes implements failures.PotentialFailure {
    int times;
    int failedCount;

    public FailNTimes(int times) {
        this.times = times;
    }

    @Override
    public void occur() {
        if (failedCount++ < times) {
            System.out.println("Operation failed");
            throw new RuntimeException("Operation failed");
        }
    }
}