package com.potter.serverless.tasks;

import com.potter.serverless.models.LambdaFunction;
import com.potter.serverless.services.DeployStatusService;
import com.potter.serverless.utils.CloudFormation;
import com.potter.serverless.utils.S3;
import com.potter.serverless.utils.StrUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CreateBucketS3 {

    private final CloudFormation cloudFormation;
    private final S3 s3;
    private final LambdaFunction lambdaFunction;
    private final DeployStatusService deployStatusService;
    private final Integer id;
    private final ZipAndUpload zipAndUpload;

    public CreateBucketS3(LambdaFunction lambdaFunction, DeployStatusService deployStatusService, Integer id) {
        this.lambdaFunction = lambdaFunction;
        this.cloudFormation = new CloudFormation(Region.of(lambdaFunction.getRegion()), lambdaFunction);
        this.s3 = new S3(Region.of(lambdaFunction.getRegion()));
        this.deployStatusService = deployStatusService;
        this.id = id;
        this.zipAndUpload = new ZipAndUpload(lambdaFunction);
    }

    @Async
    public CompletableFuture<String> run(){
        CompletableFuture<String> result = new CompletableFuture<>();
        Map<String, String> keysToReplace = this.populateKeysMap();
        try {
            this.deployStatusService.putStatus(this.id, "Bucket creation request started");
            String jsonCreate = StrUtils.replaceJsonKey(new String(Files.readAllBytes(new ClassPathResource("create.json")
                    .getFile().toPath())), keysToReplace);
            cloudFormation.createStack(StrUtils.snakeToPascal(lambdaFunction.getName()), jsonCreate)
                    .whenComplete((this::threatCreateStackResponse));
        }catch (Exception ex){
            this.deployStatusService.putStatus(this.id, ex.getMessage());
            this.rollback().whenComplete((Boolean, throwable) -> {
                if(throwable != null){
                    System.err.println(throwable.toString());
                }else{
                    System.out.println("Deletado");
                }
            });
            System.out.println(ex.getMessage());
        }

        return result;
    }

    @Async
    CompletableFuture<Boolean> rollback(){
        CompletableFuture<Boolean> response = new CompletableFuture<>();
        this.cloudFormation.deleteStack(StrUtils.snakeToPascal(lambdaFunction.getName()))
                .whenComplete((deleteStackResponse, throwable) -> {
                    if(throwable != null){
                        response.completeExceptionally(throwable);
                    }else{

                    }
        });
        return response;
    }


    @Async
    CompletableFuture<String> updateStack() throws IOException {
        CompletableFuture<String> response = new CompletableFuture<>();
        this.deployStatusService.putStatus(this.id, "Bucket update request started");
        String json = new String(Files.readAllBytes(new ClassPathResource("update.json").getFile().toPath()));
        json = json.replace("{{function_name}}", StrUtils.snakeToPascal(lambdaFunction.getName()));
        json = json.replace("{{function_name_snake}}", lambdaFunction.getName());
        json = json.replace("{{code_key}}", StrUtils.snakeToPascal(lambdaFunction.getName()).concat(".zip"));
        json = json.replace("{{function_handler}}", lambdaFunction.getHandler());
        json = json.replace("{{function_runtime}}", lambdaFunction.getRuntime());
        this.cloudFormation.updateStack(StrUtils.snakeToPascal(lambdaFunction.getName()), json)
                .whenComplete((updateStackResponse, throwable) -> {
                    if(throwable != null){
                        response.completeExceptionally(throwable);
                    }else {
                        response.complete(updateStackResponse.toString());
                    }
                });
        return response;
    }

    private Map<String, String> populateKeysMap(){
        Map<String, String> map = new HashMap<>();
        map.put("{{function_name}}", StrUtils.snakeToPascal(lambdaFunction.getName()));
        map.put("{{function_name_snake}}", lambdaFunction.getName());
        map.put("{{code_key}}", StrUtils.snakeToPascal(lambdaFunction.getName()).concat(".zip"));
        map.put("{{function_handler}}", lambdaFunction.getHandler());
        map.put("{{function_runtime}}", lambdaFunction.getRuntime());
        return map;
    }

    private void threatCreateStackResponse(String bucketPhysicalId, Throwable throwable) {
        if (throwable != null) {
            this.deployStatusService.putStatus(id, throwable.getMessage());
        } else {
            this.deployStatusService.putStatus(id, "Bucket successfully created");
            this.zipAndUpload.run(bucketPhysicalId).whenComplete(this::threatZipAndUploadResponse);
        }
    }


    private void threatZipAndUploadResponse(String s, Throwable throwable) {
        if (throwable != null) {
            this.deployStatusService.putStatus(id, throwable.getMessage());
        } else {
            this.deployStatusService.putStatus(id, s);
            Map<String, String> keysToReplace = this.populateKeysMap();
            try {
                String jsonUpdate = StrUtils.replaceJsonKey(new String(Files.readAllBytes(new ClassPathResource("update.json")
                        .getFile().toPath())), keysToReplace);
                this.cloudFormation.updateStack(StrUtils.snakeToPascal(lambdaFunction.getName()), jsonUpdate)
                        .whenComplete((updateStackResponse, throwable1) -> {
                            if(throwable1 != null){
                                this.deployStatusService.putStatus(id, throwable1.getMessage());
                            }
                        });
            } catch (IOException e) {
                this.deployStatusService.putStatus(id, e.getMessage());
            }

        }
    }
}
