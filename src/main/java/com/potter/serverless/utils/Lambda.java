package com.potter.serverless.utils;

import com.potter.serverless.models.LambdaFunction;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.apigatewayv2.model.CreateApiResponse;
import software.amazon.awssdk.services.apigatewayv2.model.CreateIntegrationResponse;
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
    private ApiGateway apiGatewayClient;
    private Region region;

    public Lambda(Region region, LambdaFunction lambdaFunction) {
        this.region = region;
        this.lambdaClient = LambdaClient.builder().region(region).build();
        this.s3Client = new S3(region);
        this.apiGatewayClient = new ApiGateway(region, lambdaFunction);
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
        /*CreateFunctionRequest functionRequest = CreateFunctionRequest.builder()
                .functionName(this.lambdaFunction.getName())
                .code(generateFunctionCode())
                .handler(this.lambdaFunction.getHandler())
                .publish(true)
                .role("arn:aws:iam::122943367152:role/express-dev-us-east-1-lambdaRole")
                .runtime(this.lambdaFunction.getRuntime()).build();
        CreateFunctionResponse createFunctionResponse = this.lambdaClient.createFunction(functionRequest);
        CreateApiResponse createApiResponse = this.apiGatewayClient.createApi(createFunctionResponse.functionArn());
        CreateIntegrationResponse createIntegrationResponse = this.apiGatewayClient.createIntegration(createApiResponse.apiId(), createFunctionResponse.functionArn());
        return createIntegrationResponse.toString();*/
        CloudFormation cloudFormation = new CloudFormation(this.region, this.lambdaFunction);
        return cloudFormation.createStack(this.lambdaFunction.getName().replace("_", ""),"C:\\Users\\nikni\\OneDrive\\Área de Trabalho\\AmazonUpload\\src\\main\\resources\\create.json").toString();
    }

    private FunctionCode generateFunctionCode() throws FileNotFoundException {
        FunctionCode.Builder functionCode = FunctionCode.builder();
        FileInputStream fileInputStream = new FileInputStream(this.lambdaFunction.getCodeLocation());
        SdkBytes bytes = SdkBytes.fromInputStream(fileInputStream);
        functionCode.zipFile(bytes);
        return functionCode.build();

    }

}
