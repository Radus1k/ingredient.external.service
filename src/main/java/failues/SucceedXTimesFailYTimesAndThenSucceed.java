package failures;

public class SucceedXTimesFailYTimesAndThenSucceed implements failures.PotentialFailure {
  int successHowMany;
  int failHowMany;
  int successCount, failCount;

  public SucceedXTimesFailYTimesAndThenSucceed(int successHowMany, int failHowMany) {
    this.successHowMany = successHowMany;
    this.failHowMany = failHowMany;
  }

  @Override
  public void occur() {
    if (successCount < successHowMany) {
      successCount++;
      return;
    }
    if (failCount < failHowMany) {
      failCount++;
      throw new RuntimeException("Getting ingredients failed");
    }
    return;
  }
}
