package parallel.project;

import java.util.concurrent.ForkJoinPool;


public class MatrixTestRunner {

    
    private static final MatrixMultiplier seqMultiplier = new SequentialMatrixMultiplier();
    private static final ForkJoinMatrixMultiplier parMultiplier = new ForkJoinMatrixMultiplier(1);

    public static void main(String[] args) {
        System.out.println("--- Minimal Matrix Multiplication Correctness Test (2x3 * 3x2) ---");
        

        Matrix A = new Matrix(2, 3);
        double [][] dataA = {
            {1, 2, 3},
            {4, 5, 6}
        };
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                A.set(i, j, dataA[i][j]);
            }
        }
        
        Matrix B = new Matrix(5, 5);
        B = Matrix.createRandom(5, 5);
        
        System.out.println("\nInput A (2x3):\n" + A.toString());
        System.out.println("\nInput B (3x2):\n" + B.toString());

        Matrix C_seq = null;
        Matrix C_par = null;

        try {
            C_seq = seqMultiplier.multiply(A, B);
            System.out.println("\n--- Sequential Result (2x2) ---");
            System.out.println(C_seq);
        } catch (Exception e) {
            System.err.println("Sequential multiplication failed: " + e.getMessage());
        }

        try (ForkJoinPool dedicatedPool = new ForkJoinPool()) {
            C_par = parMultiplier.multiply(A, B, dedicatedPool,null);
            System.out.println("\n--- Parallel Result (Fork/Join) (2x2) ---");
            System.out.println(C_par);
        } catch (Exception e) {
            System.err.println("Parallel multiplication failed: " + e.getMessage());
        }

        System.out.println("\n--- Verification ---");
        if (C_seq != null && C_par != null) {
            if (C_seq.equals(C_par)) {
                System.out.println("STATUS: SUCCESS - Sequential and Parallel results match.");
            } else {
                System.err.println("STATUS: FAILURE - Sequential and Parallel results DO NOT match.");
            }
        } else {
             System.err.println("STATUS: FAILURE - One or both results are null due to previous errors.");
        }
    }
}