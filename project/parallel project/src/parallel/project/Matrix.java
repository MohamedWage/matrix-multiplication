/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package parallel.project;

/**
 *
 * @author tech
 */
import java.util.Arrays;
import java.util.Random;

public class Matrix {

    private final double[][] data;
    private final int rows;
    private final int cols;

    public Matrix(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Number of rows and columns must be positive.");
        }
        this.rows = rows;
        this.cols = cols;
        this.data = new double[rows][cols];
    }

    public static Matrix createRandom(int rows, int cols) {
        Matrix matrix = new Matrix(rows, cols);
        Random rand = new Random();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix.data[i][j] = rand.nextDouble() * 10.0;
            }
        }
        return matrix;
    }

    public void set(int row, int col, double value) {
        if (row < 0 || row >= this.rows || col < 0 || col >= this.cols) {
            throw new IndexOutOfBoundsException("Row or column index out of bounds.");
        }
        this.data[row][col] = value;
    }

    public double get(int row, int col) {
        return this.data[row][col];
    }

    public int getRowCount() {
        return this.rows;
    }

    public int getColCount() {
        return this.cols;
    }

    public boolean equals(Matrix other) {
        double tolerance = 1e-9;

        if (other == null || this.rows != other.rows || this.cols != other.cols) {
            return false;
        }
        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < this.cols; j++) {
                if (Math.abs(this.data[i][j] - other.data[i][j]) > tolerance) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int row = 5;
        int col = 5;
        if (this.rows < 5) {
            row = this.rows;
        }
        if (this.cols < 5) {
            col = this.cols;
        }
        for (int i = 0; i < row; i++) {
            sb.append("[");
            for (int j = 0; j < col; j++) {
                sb.append(String.format("%.2f ", data[i][j]));
                if (j != col - 1) {
                    sb.append(", ");
                }
            }
            sb.append("] \n");
        }
        return sb.toString();
    }
}
