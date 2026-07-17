package exposed.thunder.thunderLBs.scheduler;

public interface TaskHandle {
    void cancel();

    boolean isCancelled();
}
