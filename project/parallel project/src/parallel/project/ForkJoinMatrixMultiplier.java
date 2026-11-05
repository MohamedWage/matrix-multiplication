package parallel.project;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class ForkJoinMatrixMultiplier implements MatrixMultiplier {

    private final int THRESHOLD;

    public ForkJoinMatrixMultiplier(int threshold) {
        this.THRESHOLD = threshold;
    }

    @Override
    public Matrix multiply(Matrix A, Matrix B) {
        return multiply(A, B, ForkJoinPool.commonPool(), null);
    }

    public Matrix multiply(Matrix matrix1, Matrix matrix2, ForkJoinPool pool, TaskLogger logger) {
        int m = matrix1.getRowCount();
        int n = matrix1.getColCount();
        int p = matrix2.getColCount();

        if (n != matrix2.getRowCount()) {
            throw new IllegalArgumentException(
                    String.format("Incompatible matrix dimensions for multiplication: A (%d x %d), B (%d x %d).",
                            m, n, matrix2.getRowCount(), p));
        }

        Matrix result = new Matrix(m, p);

        MultiplyTask mainTask = new MultiplyTask(matrix1, matrix2, result, 0, m, this.THRESHOLD, logger);
        pool.invoke(mainTask);

        return result;
    }

    private static class MultiplyTask extends RecursiveAction {

        private final Matrix matrix1;
        private final Matrix matrix2;
        private final Matrix result;
        private final int startRow;
        private final int endRow;
        private final int threshold;
        private final TaskLogger logger;

        public MultiplyTask(Matrix matrix1, Matrix matrix2, Matrix result, int startRow, int endRow, int threshold, TaskLogger logger) {
            this.matrix1 = matrix1;
            this.matrix2 = matrix2;
            this.result = result;
            this.startRow = startRow;
            this.endRow = endRow;
            this.threshold = threshold;
            this.logger = logger;
        }

        @Override
        protected void compute() {
            int rowCount = endRow - startRow;

            if (rowCount <= threshold) {

                String threadName = Thread.currentThread().getName();

                if (logger != null) {
                    logger.logStart(threadName, startRow, endRow);
                }

                int n = matrix1.getColCount();
                int p = matrix2.getColCount();

                for (int i = startRow; i < endRow; i++) {
                    for (int j = 0; j < p; j++) {
                        double sum = 0;
                        for (int k = 0; k < n; k++) {
                            sum += matrix1.get(i, k) * matrix2.get(k, j);
                        }
                        result.set(i, j, sum);
                    }
                }

                if (logger != null) {
                    logger.logFinish(threadName, startRow, endRow);
                }

            } else {
                int midRow = startRow + rowCount / 2;

                MultiplyTask t1 = new MultiplyTask(matrix1, matrix2, result, startRow, midRow, threshold, logger);
                MultiplyTask t2 = new MultiplyTask(matrix1, matrix2, result, midRow, endRow, threshold, logger);

                t1.fork();
                t2.compute();
                t1.join();
            }
        }
    }
}
