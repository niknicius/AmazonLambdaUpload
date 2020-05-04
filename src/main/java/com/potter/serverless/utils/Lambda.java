package com.potter.serverless.utils;

import com.potter.serverless.models.LambdaFunction;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.apigatewayv2.model.CreateApiResponse;
import software.amazon.awssdk.services.apigatewayv2.model.CreateIntegrationResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.StackResource;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class Lambda {

    private LambdaFunction lambdaFunction;
    private LambdaClient lambdaClient;
    private S3 s3Client;
    private Region region;

    public Lambda(Region region, LambdaFunction lambdaFunction) {
        this.region = region;
        this.lambdaClient = LambdaClient.builder().region(region).build();
        this.s3Client = new S3(region);
        this.lambdaFunction = lambdaFunction;
    }

    public ListFunctionsResponse getFunctionList(){
        try {
            ListFunctionsResponse functionResult = this.lambdaClient.listFunctions();
            List<FunctionConfiguration> list = functionResult.functions();
            return functionResult;
        } catch(ServiceException e) {
            e.getStackTrace();
        }

        return null;
    }

    public String createFunction() throws IOException, InterruptedException {
        CloudFormation cloudFormation = new CloudFormation(this.region, this.lambdaFunction);
        DescribeStackResourcesResponse a = cloudFormation.createStack(this.lambdaFunction.getName().replace("_", ""), "C:\\Users\\nikni\\OneDrive\\Área de Trabalho\\AmazonUpload\\src\\main\\resources\\create.json");
        for(StackResource stackResource: a.stackResources()){
           if(stackResource.logicalResourceId().equalsIgnoreCase("ServerlessDeploymentBucket")){
               this.s3Client.uploadObjectToAExistentBucket(stackResource.physicalResourceId(), this.lambdaFunction.getCodeLocation(), "code.zip");
           }
       }
        cloudFormation.updateStack(this.lambdaFunction.getName().replace("_", ""), "C:\\Users\\nikni\\OneDrive\\Área de Trabalho\\AmazonUpload\\src\\main\\resources\\update.json");
        return cloudFormation.toString();
    }

    private FunctionCode generateFunctionCode() throws FileNotFoundException {
        FunctionCode.Builder functionCode = FunctionCode.builder();
        FileInputStream fileInputStream = new FileInputStream(this.lambdaFunction.getCodeLocation());
        SdkBytes bytes = SdkBytes.fromInputStream(fileInputStream);
        functionCode.zipFile(bytes);
        return functionCode.build();
    }

}
