package parallel.project;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

public class MatrixBenchmarking extends JFrame {

    private int warm_up = 0;
    private Matrix seqResult;
    private Matrix parResult;

    private final JTextField rowMatrix1 = new JTextField("1024", 6);
    private final JTextField colMatrix1 = new JTextField("1024", 6);
    private final JTextField rowMatrix2 = new JTextField("1024", 6);
    private final JTextField colMatrix2 = new JTextField("1024", 6);
    private final JTextField thresholdField = new JTextField("64", 6);
    private final JTextField MAX_ITERATIONS = new JTextField("2", 6);

    private final JButton runButton = new JButton("Run Benchmark");
    private final JTextArea resultsArea = new JTextArea(15, 70);

    public MatrixBenchmarking() {
        super("Parallel Matrix Multiplication Benchmark (Fork/Join)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setupUI();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void setupUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel matrix1Panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        matrix1Panel.setBorder(BorderFactory.createTitledBorder("Matrix 1 (Rows x Cols)"));
        matrix1Panel.add(new JLabel("Matrix 1 Rows:"));
        matrix1Panel.add(this.rowMatrix1);
        matrix1Panel.add(new JLabel("Matrix 1 Cols:"));
        matrix1Panel.add(this.colMatrix1);

        JPanel matrix2Panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        matrix2Panel.setBorder(BorderFactory.createTitledBorder("Matrix 2 (Rows x Cols)"));
        matrix2Panel.add(new JLabel("Matrix 2 Rows:"));
        matrix2Panel.add(this.rowMatrix2);
        matrix2Panel.add(new JLabel("Matrix 2 Cols:"));
        matrix2Panel.add(this.colMatrix2);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        controlPanel.add(new JLabel("Parallel Threshold (Rows):"));
        controlPanel.add(this.thresholdField);

        controlPanel.add(new JLabel("Max Iteration:"));
        controlPanel.add(this.MAX_ITERATIONS);

        controlPanel.add(this.runButton);

        this.resultsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        this.resultsArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(this.resultsArea);

        mainPanel.add(matrix1Panel);
        mainPanel.add(matrix2Panel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(controlPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(scrollPane);

        add(mainPanel);

        this.runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runButton.setEnabled(false);
                resultsArea.setText("Starting benchmark...\n");
                new BenchmarkRunner().execute();
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MatrixBenchmarking());
    }

    private class BenchmarkRunner extends SwingWorker<String, String> implements TaskLogger {

        private String extractThreadNumber(String threadName) {
            int workerIndex = threadName.indexOf("-worker-");
            if (workerIndex != -1) {
                return "thread " + threadName.substring(workerIndex + 8);
            }
            return threadName;
        }

        @Override
        public void logStart(String threadName, int startRow, int endRow) {
            String threadID = extractThreadNumber(threadName);
            publish(String.format("[START] %s (Rows %d-%d)\n", threadID, startRow, endRow));
        }

        @Override
        public void logFinish(String threadName, int startRow, int endRow) {
            String threadID = extractThreadNumber(threadName);
            publish(String.format("[FINISH] %s (Rows %d-%d)\n", threadID, startRow, endRow));
        }

        @Override
        protected String doInBackground() {
            int m1_rows, m1_cols, m2_rows, m2_cols, threshold, iteration;
            try {
                m1_rows = Integer.parseInt(rowMatrix1.getText());
                m1_cols = Integer.parseInt(colMatrix1.getText());
                m2_rows = Integer.parseInt(rowMatrix2.getText());
                m2_cols = Integer.parseInt(colMatrix2.getText());
                threshold = Integer.parseInt(thresholdField.getText());
                iteration = Integer.parseInt(MAX_ITERATIONS.getText());
                if (m1_rows <= 0 || m1_cols <= 0 || m2_rows <= 0 || m2_cols <= 0 || threshold <= 0 || iteration <= 0) {
                    throw new IllegalArgumentException("All dimensions, iteration, and Threshold must be positive integers.");
                }

                if (m1_cols != m2_rows) {
                    throw new IllegalArgumentException(
                            "Matrix dimensions are incompatible. Columns of Matrix 1 (%d) must equal Rows of Matrix 2 (%d)."
                                    .formatted(m1_cols, m2_rows)
                    );
                }

                return runFullBenchmark(m1_rows, m1_cols, m2_cols, threshold, iteration);

            } catch (NumberFormatException ex) {
                return "\nError: Invalid input. Please ensure all inputs are valid integers.";
            } catch (IllegalArgumentException ex) {
                return "\nError: " + ex.getMessage();
            } catch (Exception ex) {
                return "\nAn unexpected error occurred: " + ex.getMessage();
            }
        }

        @Override
        protected void process(List<String> chunks) {
            for (String message : chunks) {
                resultsArea.append(message);
                resultsArea.setCaretPosition(resultsArea.getDocument().getLength());
            }
        }

        @Override
        protected void done() {
            try {
                resultsArea.append(get());
            } catch (Exception ex) {
                resultsArea.append("\nCritical error retrieving results.");
            } finally {
                runButton.setEnabled(true);
            }
        }

        private String runFullBenchmark(int m1_rows, int n_common, int m2_cols, int threshold, int iteration) {

            StringBuilder output = new StringBuilder();

            output.append(String.format("\n--- Running Benchmark for A (%d x %d) * B (%d x %d) (Threshold: %d) ---\n",
                    m1_rows, n_common, n_common, m2_cols, threshold));

            Matrix matrix1 = Matrix.createRandom(m1_rows, n_common);
            Matrix matrix2 = Matrix.createRandom(n_common, m2_cols);

            MatrixMultiplier sequentialMultiplier = new SequentialMatrixMultiplier();
            ForkJoinMatrixMultiplier parallelMultiplier = new ForkJoinMatrixMultiplier(threshold);

            if (warm_up == 0) {
                output.append("Warming up Just-In-Time Compilation (JIT) compiler...\n");
                try (ForkJoinPool warmUpPool = new ForkJoinPool()) {
                    sequentialMultiplier.multiply(matrix1, matrix2);
                    parallelMultiplier.multiply(matrix1, matrix2, warmUpPool, null);
                }
                output.append("Warm-up complete.\n");
                warm_up = 1;
            }
            output.append(String.format("\nthe first 5 rows and cols of matrix 1 is :\n %s\n", matrix1.toString()));
            output.append(String.format("\nthe first 5 rows and cols of matrix 2 is :\n %s\n", matrix2.toString()));

            output.append(String.format("Starting Sequential tests (%d iterations)....\n", iteration));
            long sequentialTime = measureTime(sequentialMultiplier, matrix1, matrix2, "Sequential", output, null, iteration);

            output.append(String.format("Starting Parallel tests (%d iterations)....\n", iteration));
            long parallelTime = measureTime(parallelMultiplier, matrix1, matrix2, "Parallel", output, this, iteration);

            output.append(validateResults(seqResult, parResult));
            output.append(String.format("\nthe first 5 rows and cols of matrix sequential is :\n %s\n", seqResult.toString()));
            output.append(String.format("\nthe first 5 rows and cols of matrix parallel is :\n %s\n", parResult.toString()));
            output.append(printResults(sequentialTime, parallelTime));

            return output.toString();
        }

        private long measureTime(MatrixMultiplier multiplier, Matrix matrix1, Matrix matrix2, String name, StringBuilder output, TaskLogger logger, int iteration) {
            long totalTime = 0;
            boolean isForkJoin = (multiplier instanceof ForkJoinMatrixMultiplier);

            for (int i = 0; i < iteration; i++) {
                long startTime = System.nanoTime();

                if (isForkJoin) {
                    try (ForkJoinPool timedPool = new ForkJoinPool()) {
                        parResult = ((ForkJoinMatrixMultiplier) multiplier).multiply(matrix1, matrix2, timedPool, logger);
                    }
                } else {
                    seqResult = multiplier.multiply(matrix1, matrix2);
                }

                long endTime = System.nanoTime();
                totalTime += (endTime - startTime);
            }

            long averageTimeNanos = totalTime / iteration;
            output.append(String.format("Average %s Time: %.2f ms\n",
                    name, averageTimeNanos / 1_000_000.0));

            return totalTime;
        }

        private String validateResults(Matrix seqResult, Matrix parResult) {
            try {
                if (seqResult.equals(parResult)) {
                    return "\n[Validation] Sequential and Parallel results match.\n";

                } else {
                    return "\n[Validation] Sequential and Parallel results DO NOT match.\n";
                }
            } catch (Exception e) {
                return "\n[Validation] Error during validation: " + e.getMessage() + "\n";
            }
        }

        private String printResults(long sequentialTime, long parallelTime) {
            double speedup = (double) sequentialTime / parallelTime;

            return String.format("\n--- Performance Summary ---\n"
                    + "Sequential Time: %.2f ms\n"
                    + "Parallel Time:   %.2f ms\n"
                    + "Speedup (Ts/Tp): %.2f x\n"
                    + "---------------------------\n",
                    sequentialTime / 1_000_000.0,
                    parallelTime / 1_000_000.0,
                    speedup
            );
        }
    }
}
