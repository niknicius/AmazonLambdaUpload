package com.potter.serverless.utils;

import com.potter.serverless.models.LambdaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.*;

import java.io.*;

public class CloudFormation {

    private LambdaFunction lambdaFunction;
    private CloudFormationClient cloudFormationClient;
    private Region region;
    private Logger logger;

    public CloudFormation(Region region, LambdaFunction lambdaFunction) {
        this.region = region;
        this.lambdaFunction = lambdaFunction;
        this.cloudFormationClient = CloudFormationClient.builder().region(this.region).build();
        this.logger = LoggerFactory.getLogger(CloudFormation.class);
    }


    public DescribeStackResourcesResponse createStack(String stackName, String json) throws InterruptedException {
        logger.info("Stack creation started");
        CreateStackResponse response = this.cloudFormationClient.createStack(CreateStackRequest.builder()
                .stackName(stackName)
                .templateBody(json)
                .build());
        boolean finishedCreation = this.checkStackCreationFinished(this.getStackEvents());
        while(!finishedCreation){
            Thread.sleep(1000);
            finishedCreation = this.checkStackCreationFinished(this.getStackEvents());
        }

        return this.cloudFormationClient.describeStackResources(DescribeStackResourcesRequest.builder().stackName(stackName).build());
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

    private DescribeStackEventsResponse getStackEvents(){
        return this.cloudFormationClient.describeStackEvents(DescribeStackEventsRequest.builder().stackName(this.lambdaFunction.getName().replace("_", "")).build());
    }

    public UpdateStackResponse updateStack(String stackName, String json) {
        return this.cloudFormationClient.updateStack(UpdateStackRequest.builder()
                .stackName(stackName)
                .templateBody(json)
                .capabilities(Capability.CAPABILITY_NAMED_IAM)
                .build());
    }

}
