package org.example.lab4.commands;

import org.example.lab4.Task;
import org.example.lab4.TaskManager;
import org.example.lab4.interfaces.Command;

public class StartCommand implements Command {
    private final TaskManager manager;
    private final double parameter;

    public StartCommand(TaskManager manager, double parameter) {
        this.manager = manager;
        this.parameter = parameter;
    }

    @Override
    public void execute() {
        Task task = new Task(parameter);
        this.manager.addAndStartTask(task);
    }
}
