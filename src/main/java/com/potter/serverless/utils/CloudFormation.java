package com.potter.serverless.utils;

import com.potter.serverless.models.LambdaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationAsyncClient;
import software.amazon.awssdk.services.cloudformation.model.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class CloudFormation {

    private LambdaFunction lambdaFunction;
    private CloudFormationAsyncClient cloudFormationClient;
    private Region region;
    private Logger logger;

    public CloudFormation(Region region, LambdaFunction lambdaFunction) {
        this.region = region;
        this.lambdaFunction = lambdaFunction;
        this.cloudFormationClient = CloudFormationAsyncClient.builder().region(this.region).build();
        this.logger = LoggerFactory.getLogger(CloudFormation.class);
    }


    public CompletableFuture<String> createStack(String stackName, String json) throws InterruptedException {
        CompletableFuture<String> result = new CompletableFuture<>();
        this.cloudFormationClient.createStack(CreateStackRequest.builder()
                .stackName(stackName)
                .templateBody(json)
                .build()).whenComplete((createStackResponse, throwable) -> {
                    if(throwable != null){
                        result.completeExceptionally(throwable);
                    }else{
                        try {
                            result.complete(waitBucketDeploy());
                        } catch (Exception e) {
                            result.completeExceptionally(e);
                        }
                    }
        });

        return result;
    }

    private String waitBucketDeploy() throws Exception {
        AtomicReference<String> bucketPhysicalId = new AtomicReference<>();
        AtomicBoolean error = new AtomicBoolean(false);
        AtomicBoolean success = new AtomicBoolean(false);
        while(!error.get() && !success.get()){
            this.getStackEvents().whenComplete((describeStackEventsResponse, throwable) -> {
                try {
                    if(this.checkStackCreationFinished(describeStackEventsResponse)){
                        success.set(true);
                        bucketPhysicalId.set(getBucketName(describeStackEventsResponse));
                    }
                } catch (Exception e) {
                   error.set(true);
                }
            });
            Thread.sleep(10000);
        }
        if(error.get()){
            throw new Exception("Stack creation failed");
        }

        return bucketPhysicalId.get();
    }

    private String getBucketName(DescribeStackEventsResponse describeStackEventsResponse){
        for(StackEvent stackEvent: describeStackEventsResponse.stackEvents()){
            if(stackEvent.logicalResourceId().equalsIgnoreCase(StrUtils.snakeToPascal(this.lambdaFunction.getName()).concat("DeploymentBucket"))){
              return stackEvent.physicalResourceId();
            }
        }
        return null;
    }

    private boolean checkStackCreationFinished(DescribeStackEventsResponse response) throws Exception {
       boolean finished = false;
        for(StackEvent stackEvent: response.stackEvents()){
            if(stackEvent.logicalResourceId().equalsIgnoreCase(StrUtils.snakeToPascal(this.lambdaFunction.getName())) && stackEvent.resourceStatus().equals(ResourceStatus.CREATE_COMPLETE)){
                finished = true;
            }else if(stackEvent.logicalResourceId().equalsIgnoreCase(StrUtils.snakeToPascal(this.lambdaFunction.getName())) && stackEvent.resourceStatus().equals(ResourceStatus.CREATE_FAILED)){
                throw new Exception("Stack creation failed");
            }
        }
        return finished;
    }

    public CompletableFuture<DescribeStackEventsResponse> getStackEvents(){
        return this.cloudFormationClient.describeStackEvents(DescribeStackEventsRequest.builder().stackName(StrUtils.snakeToPascal(this.lambdaFunction.getName())).build());
    }

    public CompletableFuture<UpdateStackResponse> updateStack(String stackName, String json) {
        return this.cloudFormationClient.updateStack(UpdateStackRequest.builder()
                .stackName(stackName)
                .templateBody(json)
                .capabilities(Capability.CAPABILITY_NAMED_IAM)
                .build());
    }

    public CompletableFuture<DeleteStackResponse> deleteStack(String stackName){
        return this.cloudFormationClient.deleteStack(DeleteStackRequest.builder()
                .stackName(stackName)
                .build());
    }

}
