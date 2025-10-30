//package org.example.lab4;
//
//import lombok.Getter;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Getter
//public class TaskManager {
//    private final Map<Integer, Thread> tasks = new ConcurrentHashMap<>();
//
//    public void addAndStartTask(Task task) {
//        Thread thread = new Thread(task);
//        tasks.put(task.getId(), thread);
//        thread.start();
//        ConsolePrinter.taskStarted(task.getId());
//    }
//
//    public void awaitTask(int id) {
//        try {
//            Thread thread = this.tasks.get(id);
//            if (thread != null) {
//                thread.join();
//            }
//            ConsolePrinter.taskAwaited(id);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    public void stopTask(int id) {
//        Thread thread = tasks.get(id);
//        if (thread != null) {
//            thread.interrupt();
//            ConsolePrinter.taskStopped(id);
//        }
//    }
//
//    public void exit() {
//        System.out.println("Останавливаем все задачи...");
//        for (Thread thread : tasks.values()) {
//            if (thread.isAlive()) {
//                thread.interrupt();
//            }
//        }
//    }
//}
