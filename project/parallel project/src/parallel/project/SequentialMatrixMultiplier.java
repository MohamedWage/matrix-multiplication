package parallel.project;

public class SequentialMatrixMultiplier implements MatrixMultiplier {

    @Override
    public Matrix multiply(Matrix A, Matrix B) {
        int m = A.getRowCount();
        int n = A.getColCount();
        int p = B.getColCount();

        if (n != B.getRowCount()) {
            throw new IllegalArgumentException(
                    String.format("Incompatible matrix dimensions for multiplication: A (%d x %d), B (%d x %d).",
                            m, n, B.getRowCount(), p));
        }

        Matrix C = new Matrix(m, p);
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < p; j++) {
                double sum = 0;
                for (int k = 0; k < n; k++) {
                    sum += A.get(i, k) * B.get(k, j);
                }
                C.set(i, j, sum);
            }
        }

        return C;
    }
}
