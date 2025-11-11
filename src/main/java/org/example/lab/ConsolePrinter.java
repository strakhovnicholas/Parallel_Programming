package org.example.lab;

public class ConsolePrinter {

    public static void taskStarted(int id) {
        System.out.println("Задача " + id + " запущена");
    }

    public static void taskStopped(int id) {
        System.out.println("Задача " + id + " остановлена");
    }

    public static void taskInterrupted(int id) {
        System.out.println("Задача " + id + " была прервана");
    }

    public static void taskAwaited(int id) {
        System.out.println("Задача " + id + " завершена");
    }

    public static void exitStart() {
        System.out.println("Останавливаем все задачи...");
    }

    public static void exitComplete() {
        System.out.println("Все задачи завершены. Выход.");
    }

    public static void unknownCommand(String cmd) {
        System.out.println("Неизвестная команда: " + cmd);
    }

}

