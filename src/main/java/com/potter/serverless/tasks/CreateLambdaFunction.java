package com.potter.serverless.tasks;

import com.potter.serverless.models.LambdaFunction;
import com.potter.serverless.services.TaskService;
import com.potter.serverless.utils.CloudFormation;
import com.potter.serverless.utils.StrUtils;
import com.potter.serverless.utils.backgroundtask.Task;
import org.springframework.core.io.ClassPathResource;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.nio.file.Files;

public class CreateLambdaFunction implements Task {

    private LambdaFunction lambdaFunction;
    private TaskService taskService;

    public CreateLambdaFunction(LambdaFunction lambdaFunction, TaskService taskService) {
        this.lambdaFunction = lambdaFunction;
        this.taskService = taskService;
    }

    @Override
    public Object run() throws InterruptedException, IOException {
        String json = new String(Files.readAllBytes(new ClassPathResource("update.json").getFile().toPath()));
        json = json.replace("{{function_name}}", StrUtils.snakeToPascal(lambdaFunction.getName()));
        json = json.replace("{{function_name_snake}}", lambdaFunction.getName());
        json = json.replace("{{function_handler}}", StrUtils.snakeToPascal(lambdaFunction.getHandler()));
        CloudFormation cloudFormation = new CloudFormation(Region.of(lambdaFunction.getRegion()), lambdaFunction);
        return null;
        //return cloudFormation.updateStack(StrUtils.snakeToPascal(this.lambdaFunction.getName()), json);
    }
}
