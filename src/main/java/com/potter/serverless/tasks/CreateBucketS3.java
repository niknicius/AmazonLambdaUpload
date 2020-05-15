package com.potter.serverless.tasks;

import com.potter.serverless.models.LambdaFunction;
import com.potter.serverless.utils.CloudFormation;
import com.potter.serverless.utils.StrUtils;
import com.potter.serverless.utils.backgroundtask.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesResponse;

import java.io.IOException;
import java.nio.file.Files;


public class CreateBucketS3 implements Task {

    private LambdaFunction lambdaFunction;

    public CreateBucketS3(LambdaFunction lambdaFunction) {
        this.lambdaFunction = lambdaFunction;
    }

    @Override
    public Object run() throws InterruptedException, IOException {
        String json = new String(Files.readAllBytes(new ClassPathResource("create.json").getFile().toPath()));
        json = json.replace("{{function_name}}", StrUtils.snakeToPascal(lambdaFunction.getName()));
        CloudFormation cloudFormation = new CloudFormation(Region.of(lambdaFunction.getRegion()), lambdaFunction);
        return cloudFormation.createStack(StrUtils.snakeToPascal(this.lambdaFunction.getName()), json);
    }

}
