package org.example.lab;

public class ExperimentResult {
    private final int success;
    private final int total;

    public ExperimentResult(int success, int total) {
        this.success = success;
        this.total = total;
    }

    public int getSuccess() { return success; }
    public int getTotal() { return total; }
}
