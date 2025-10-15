package org.example.lab4.commands;

import org.example.lab4.TaskManager;
import org.example.lab4.interfaces.Command;

public class AwaitCommand implements Command {
    private final TaskManager manager;
    private final int taskId;

    public AwaitCommand(TaskManager manager, int taskId) {
        this.manager = manager;
        this.taskId = taskId;
    }

    @Override
    public void execute() {
        this.manager.awaitTask(taskId);
    }
}
