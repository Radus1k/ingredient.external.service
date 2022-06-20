package failues;

public class AlwaysSlowNSeconds implements failures.PotentialDelay {
    int delayInSeconds;

    public AlwaysSlowNSeconds(int delayInSeconds) {
        this.delayInSeconds = delayInSeconds;
    }

    @Override
    public void occur() {
        try {
            System.out.println("Sleppy sleepy..");
            Thread.sleep(delayInSeconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
