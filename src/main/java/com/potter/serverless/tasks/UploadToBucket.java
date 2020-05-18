package com.potter.serverless.tasks;

import com.potter.serverless.models.LambdaFunction;
import com.potter.serverless.services.TaskService;
import com.potter.serverless.utils.S3;
import com.potter.serverless.utils.StrUtils;
import com.potter.serverless.utils.backgroundtask.Task;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class UploadToBucket implements Task{

    private final TaskService taskService;

    private LambdaFunction lambdaFunction;

    public UploadToBucket(LambdaFunction lambdaFunction, TaskService taskService) {
        this.lambdaFunction = lambdaFunction;
        this.taskService = taskService;
    }

    @Override
    public Object run() throws InterruptedException, IOException {
        List<Object> returns = this.taskService.getTaskRunner().getReturns();
        S3 s3Client = new S3(Region.of(this.lambdaFunction.getRegion()));
        return s3Client.uploadObjectToAExistentBucket(returns.get(0).toString(), new File(returns.get(1).toString()), StrUtils.snakeToPascal(this.lambdaFunction.getName()) + ".zip");
    }
}

