package parallel.project;

public interface TaskLogger {

    void logStart(String threadName, int startRow, int endRow);

    void logFinish(String threadName, int startRow, int endRow);
}
