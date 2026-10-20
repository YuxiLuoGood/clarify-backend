package com.clarify.forecast;

/**
 * Ordinary least squares fit of y = slope·x + intercept over x = 1..n.
 * <pre>
 * slope     = (n·Σxy - Σx·Σy) / (n·Σx² - (Σx)²)
 * intercept = (Σy - slope·Σx) / n
 * </pre>
 * Immutable and dependency-free, so the model is unit-tested in isolation.
 */
public record LinearRegression(double slope, double intercept) {

    public static LinearRegression fit(double[] ys) {
        int n = ys.length;
        if (n < 2) {
            throw new IllegalArgumentException("Need at least 2 points to fit a line, got " + n);
        }
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            double x = i + 1;
            sumX += x;
            sumY += ys[i];
            sumXY += x * ys[i];
            sumX2 += x * x;
        }
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;
        return new LinearRegression(slope, intercept);
    }

    public double predict(double x) {
        return slope * x + intercept;
    }
}
