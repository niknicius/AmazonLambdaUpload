package com.potter.serverless.utils;

import com.potter.serverless.models.LambdaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;;
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

    private String getJson(String path) throws IOException {
        InputStream is = new FileInputStream(path);
        BufferedReader buf = new BufferedReader(new InputStreamReader(is));

        String line = buf.readLine();
        StringBuilder sb = new StringBuilder();

        while(line != null){ sb.append(line).append("\n"); line = buf.readLine(); }

        return sb.toString();
    }

    public CreateStackResponse createStack(String stackName, String templateLocation) throws IOException, InterruptedException {
        logger.info("Stack creation started");
        CreateStackResponse response = this.cloudFormationClient.createStack(CreateStackRequest.builder()
                .stackName(stackName)
                .templateBody(getJson(templateLocation))
                .build());
        boolean finishedCreation = this.checkStackCreationFinished(this.getStackEvents());
        while(!finishedCreation){
            logger.warn("Not Yet");
            Thread.sleep(1000);
            finishedCreation = this.checkStackCreationFinished(this.getStackEvents());
        }

        return null;
    }

    private boolean checkStackCreationFinished(DescribeStackEventsResponse response){
       boolean finished = false;
        for(StackEvent stackEvent: response.stackEvents()){
            if(stackEvent.logicalResourceId().equalsIgnoreCase(this.lambdaFunction.getName().replace("_", "")) && stackEvent.resourceStatus().equals(ResourceStatus.CREATE_COMPLETE)){
                logger.info("Stack creation finished");
                finished = true;
            }
        }
        return finished;
    }

    private DescribeStackEventsResponse getStackEvents(){
        return this.cloudFormationClient.describeStackEvents(DescribeStackEventsRequest.builder().stackName(this.lambdaFunction.getName().replace("_", "")).build());
    }

}
