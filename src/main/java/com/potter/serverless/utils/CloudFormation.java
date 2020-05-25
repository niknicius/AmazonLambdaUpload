package com.potter.serverless.utils;

import com.potter.serverless.models.LambdaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationAsyncClient;
import software.amazon.awssdk.services.cloudformation.model.*;

import java.util.concurrent.CompletableFuture;

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


    public CompletableFuture<CreateStackResponse> createStack(String stackName, String json) throws InterruptedException {
        logger.info("Stack creation started");
        return this.cloudFormationClient.createStack(CreateStackRequest.builder()
                .stackName(stackName)
                .templateBody(json)
                .build());
    }

    private boolean checkStackCreationFinished(DescribeStackEventsResponse response){
       boolean finished = false;
        for(StackEvent stackEvent: response.stackEvents()){
            if(stackEvent.logicalResourceId().equalsIgnoreCase(StrUtils.snakeToPascal(this.lambdaFunction.getName())) && stackEvent.resourceStatus().equals(ResourceStatus.CREATE_COMPLETE)){
                logger.info("Stack creation finished");
                finished = true;
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
