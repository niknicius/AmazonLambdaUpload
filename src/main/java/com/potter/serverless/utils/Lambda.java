package com.potter.serverless.utils;

import org.apache.tomcat.util.codec.binary.Base64;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

public class Lambda {

    public ListFunctionsResponse getFunctionList(){

        ListFunctionsResponse functionResult = null ;

        try {
            Region region = Region.US_EAST_1;
            LambdaClient awsLambda = LambdaClient.builder().region(region).build();


            //Get a list of all functions
            functionResult = awsLambda.listFunctions();

            List<FunctionConfiguration> list = functionResult.functions();

            for (Iterator iter = list.iterator(); iter.hasNext(); ) {
                FunctionConfiguration config = (FunctionConfiguration)iter.next();
                System.out.println("The function name is "+config.functionName());
            }
        } catch(ServiceException e) {
            e.getStackTrace();
        }

        return functionResult;
    }

    public String createFunction(){
        Region region = Region.US_EAST_1;
        LambdaClient awsLambda = LambdaClient.builder().region(region).build();
        CreateFunctionRequest functionRequest = null;
        try {
            functionRequest = CreateFunctionRequest.builder()
                    .functionName("lambda_test")
                    .code(generateFunctionCode("C:\\Users\\nikni\\OneDrive\\Área de Trabalho\\AmazonUpload\\src\\main\\resources\\zip\\express.zip"))
                    .handler("dist/handler.handler")
                    .publish(true)
                    .role("arn:aws:iam::122943367152:role/express-dev-us-east-1-lambdaRole")
                    .runtime("nodejs12.x").build();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return awsLambda.createFunction(functionRequest).toString();

    }

    private FunctionCode generateFunctionCode(String zipLocation) throws FileNotFoundException {
        FunctionCode.Builder functionCode = FunctionCode.builder();
        FileInputStream fileInputStream = new FileInputStream(zipLocation);
        SdkBytes bytes = SdkBytes.fromInputStream(fileInputStream);
        functionCode.zipFile(bytes);
        return functionCode.build();

    }

    private String encodeFile(String zipLocation){
        File originalFile = new File(zipLocation);
        String fileBase64 = null;
        try{
            FileInputStream fileInputStreamReader = new FileInputStream(originalFile);
            byte[] bytes = new byte[(int)originalFile.length()];
            fileInputStreamReader.read(bytes);
            fileBase64 = new String(Base64.encodeBase64(bytes));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileBase64;
    }

}
