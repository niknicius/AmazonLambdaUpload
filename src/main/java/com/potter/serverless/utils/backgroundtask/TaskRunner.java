package com.potter.serverless.utils.backgroundtask;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class TaskRunner extends Thread {

    private List<Task> taskList;

    public TaskRunner() {
        this.taskList = new LinkedList<>();
    }

    public void addTask(Task task){
        this.taskList.add(task);
    }

    @Override
    public void run(){
        for(Task task: this.taskList) {
            task.run();
        }
    }

}
