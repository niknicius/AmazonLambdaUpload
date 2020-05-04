package com.potter.serverless.services;

import com.potter.serverless.utils.backgroundtask.TaskRunner;
import com.potter.serverless.utils.backgroundtask.Task;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

    private TaskRunner taskRunner;

    public TaskService(){
        taskRunner = new TaskRunner();
    }

    public void addTask(Task task){
        taskRunner.addTask(task);
    }

    public void run(){
        this.taskRunner.start();
    }

}
