package org.example.lab;

import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Scanner scanner = new Scanner(System.in);
        String[] parts = scanner.nextLine().split(" ");
        double param = Double.parseDouble(parts[1]);
        MonteCarloAdjust adjust = new MonteCarloAdjust(param);
        adjust.tuneEpsilon();

    }
}