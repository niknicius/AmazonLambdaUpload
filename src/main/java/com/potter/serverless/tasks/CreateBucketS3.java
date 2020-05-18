package com.potter.serverless.tasks;

import com.potter.serverless.models.LambdaFunction;
import com.potter.serverless.utils.CloudFormation;
import com.potter.serverless.utils.StrUtils;
import com.potter.serverless.utils.backgroundtask.Task;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.model.CreateStackResponse;
import software.amazon.awssdk.services.cloudformation.model.ResourceStatus;
import software.amazon.awssdk.services.cloudformation.model.StackEvent;

import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;

public class CreateBucketS3 implements Task {

    private final CloudFormation cloudFormation;
    private final LambdaFunction lambdaFunction;

    public CreateBucketS3(LambdaFunction lambdaFunction) {
        this.lambdaFunction = lambdaFunction;
        this.cloudFormation = new CloudFormation(Region.of(lambdaFunction.getRegion()), lambdaFunction);
    }

    @Async
    @Override
    public void run(){
        try {
            String json = new String(Files.readAllBytes(new ClassPathResource("create.json").getFile().toPath()));
            json = json.replace("{{function_name}}", StrUtils.snakeToPascal(lambdaFunction.getName()));
            CompletableFuture<CreateStackResponse> response = cloudFormation.createStack(StrUtils.snakeToPascal(lambdaFunction.getName()), json);
            response.whenComplete((createStackResponse, throwable) -> {
                if (throwable != null) {
                    System.err.println(throwable.getMessage());
                } else {
                    CompletableFuture<Boolean> a = null;
                    try {
                        a = checkBucketIsCreated();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    a.whenComplete((created, throwable1) -> {
                        System.out.println("Created");
                    });
                }
            });
        }catch (Exception ex){
            System.err.println(ex.getMessage());;
        }

    }

    @Async
    CompletableFuture<Boolean> checkBucketIsCreated() throws InterruptedException {
        CompletableFuture<Boolean> response = new CompletableFuture<>();
        AtomicBoolean created = new AtomicBoolean(false);
        while(!created.get()){
            CompletableFuture<List<StackEvent>> a = getStackEvents();
            a.whenComplete((stackEvents, throwable) -> {
                for(StackEvent event: stackEvents){
                    if(event.logicalResourceId().equalsIgnoreCase(StrUtils.snakeToPascal(lambdaFunction.getName()))
                            && event.resourceStatus().equals(ResourceStatus.CREATE_COMPLETE)){
                        created.set(true);
                        response.complete(true);
                        break;
                    }
                }
            });
            Thread.sleep(15000);
        }
        return response;
    }

    @Async
    CompletableFuture<List<StackEvent>> getStackEvents() {
        CompletableFuture<List<StackEvent>> response = new CompletableFuture<>();
        this.cloudFormation.getStackEvents().whenComplete((describeStackEventsResponse, throwable) -> {
            if(throwable == null){
                response.complete(describeStackEventsResponse.stackEvents());
            }else{
                System.err.println(throwable.getMessage());
            }
        });
        return response;
    }

}
