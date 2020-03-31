package com.potter.serverless.utils;

import com.potter.serverless.models.LambdaFunction;
import org.apache.tomcat.util.codec.binary.Base64;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.*;

import java.io.File;
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

    public String createFunction(){
        try {
            CreateFunctionRequest functionRequest = CreateFunctionRequest.builder()
                    .functionName(this.lambdaFunction.getName())
                    .code(generateFunctionCode(this.lambdaFunction.getCodeLocation()))
                    .handler(this.lambdaFunction.getHandler())
                    .publish(true)
                    .role("arn:aws:iam::122943367152:role/express-dev-us-east-1-lambdaRole")
                    .runtime(this.lambdaFunction.getRuntime()).build();
            return this.lambdaClient.createFunction(functionRequest).toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return null;

    }

    private FunctionCode generateFunctionCode(String zipLocation) throws FileNotFoundException {
        FunctionCode.Builder functionCode = FunctionCode.builder();
        FileInputStream fileInputStream = new FileInputStream(zipLocation);
        SdkBytes bytes = SdkBytes.fromInputStream(fileInputStream);
        functionCode.zipFile(bytes);
        return functionCode.build();

    }

}
