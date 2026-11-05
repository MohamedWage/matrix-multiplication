package parallel.project;

public interface MatrixMultiplier {

    /**
     * Multiplies two matrices A and B to produce a new result matrix C.
     *
     * @param A The first matrix (m x n).
     * @param B The second matrix (n x p).
     * @return The resulting matrix C (m x p).
     * @throws IllegalArgumentException if the dimensions are incompatible for
     * multiplication.
     */
    Matrix multiply(Matrix A, Matrix B);
}
