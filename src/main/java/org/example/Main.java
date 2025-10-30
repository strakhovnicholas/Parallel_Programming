package org.example;

import org.example.lab4.TaskManager;
import org.example.lab4.commands.AwaitCommand;
import org.example.lab4.commands.ExitCommand;
import org.example.lab4.commands.StartCommand;
import org.example.lab4.commands.StopCommand;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        TaskManager taskManager = new TaskManager();
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        while (running) {
            String[] parts = scanner.nextLine().split(" ");
            String cmd = parts[0];

            switch (cmd) {
                case "start" -> {
                    double param = Double.parseDouble(parts[1]);
                    StartCommand sc = new StartCommand(taskManager, param);
                    sc.execute();
                }
                case "await" -> {
                    int taskId = Integer.parseInt(parts[1]);
                    AwaitCommand ac = new AwaitCommand(taskManager, taskId);
                    ac.execute();
                }
                case "stop" -> {
                    int taskId = Integer.parseInt(parts[1]);
                    StopCommand sc = new StopCommand(taskManager, taskId);
                    sc.execute();
                }
                case "exit" -> {
                    ExitCommand ec = new ExitCommand(taskManager);
                    ec.execute();
                    running = false;
                }
                default -> System.out.println("Неизвестная команда: " + cmd);
            }
        }

    }
}