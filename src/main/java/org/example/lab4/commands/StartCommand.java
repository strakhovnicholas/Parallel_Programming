package org.example.lab4.commands;

import org.example.lab4.MonteCarloAdjust;
import org.example.lab4.MonteCarloWorker;
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
        MonteCarloAdjust monteCarloAdjust = new MonteCarloAdjust(parameter);
        this.manager.addAndStartTask(monteCarloAdjust);
    }
}
