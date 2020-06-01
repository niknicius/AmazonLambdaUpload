package com.potter.serverless.tasks;

import com.potter.serverless.models.LambdaFunction;
import com.potter.serverless.services.DeployStatusService;
import com.potter.serverless.utils.CloudFormation;
import com.potter.serverless.utils.S3;
import com.potter.serverless.utils.StrUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.model.ResourceStatus;
import software.amazon.awssdk.services.cloudformation.model.StackEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;

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
                    .whenComplete(((bucketPhysicalId, throwable) -> {
                        if(throwable != null){
                            this.deployStatusService.putStatus(id, throwable.getMessage());
                        }else{
                            this.deployStatusService.putStatus(id, "Bucket successfully created");
                            this.zipAndUpload.run(bucketPhysicalId).whenComplete((s, throwable1) -> {
                                this.deployStatusService.putStatus(id, s);
                            });
                        }
                    }));
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

   /* @Async
    CompletableFuture<String> zipAndUploadFunction(){
        CompletableFuture<String> result = new CompletableFuture<>();
        try {
            this.checkBucketIsCreated().whenComplete((created, throwable) -> {
                if (throwable != null) {
                    this.deployStatusService.putStatus(this.id, throwable.getMessage());
                    result.completeExceptionally(throwable);
                } else {
                    this.deployStatusService.putStatus(this.id, "Bucket creation completed");
                    this.zipFolder(this.lambdaFunction.getCodeLocation()).whenComplete((path, throwable1) -> {
                        if(throwable1 != null){
                            this.deployStatusService.putStatus(this.id, throwable1.getMessage());
                            System.err.println(throwable1.getMessage());
                            result.completeExceptionally(throwable1);
                        }
                        this.uploadFunction(created, path).whenComplete((s, throwable2) -> {
                            if(throwable2 != null){
                                this.deployStatusService.putStatus(this.id, throwable2.getMessage());
                                System.err.println(throwable2.getMessage());
                                result.completeExceptionally(throwable2);
                            }else{
                                System.out.println("upload feito");
                                this.deployStatusService.putStatus(this.id, s);
                                result.complete(s);
                            }
                        });
                    });
                }
            });
        } catch (InterruptedException e) {
            this.deployStatusService.putStatus(this.id, e.getMessage());
        }

        return result;
    }*/

    @Async
    CompletableFuture<String> uploadFunction(String bucketName, Path zipLocation){
        CompletableFuture<String> result = new CompletableFuture<>();
        this.s3.uploadObjectToAExistentBucket(bucketName, zipLocation,
                StrUtils.snakeToPascal(this.lambdaFunction.getName()).concat(".zip"))
                .whenComplete((putObjectResponse, throwable) -> {
                    if(throwable != null){
                        result.completeExceptionally(throwable);
                    }else{
                        result.complete(putObjectResponse.toString());
                        System.out.println(putObjectResponse.toString());
                    }
                });
        return result;
    }

    @Async
    CompletableFuture<String> checkBucketIsCreated() throws InterruptedException {
        CompletableFuture<String> response = new CompletableFuture<>();
        AtomicBoolean created = new AtomicBoolean(false);
        while(!created.get()){
            CompletableFuture<List<StackEvent>> a = getStackEvents();
            a.whenComplete((stackEvents, throwable) -> {
                if(throwable != null){
                    this.deployStatusService.putStatus(this.id, throwable.getMessage());
                }else {
                    for (StackEvent event : stackEvents) {
                        if (event.logicalResourceId().equalsIgnoreCase(StrUtils.snakeToPascal(lambdaFunction.getName()).concat("DeploymentBucket"))
                                && event.resourceStatus().equals(ResourceStatus.CREATE_COMPLETE)) {
                            created.set(true);
                            response.complete(event.physicalResourceId());
                            break;
                        }
                    }
                }
            });
            Thread.sleep(10000);
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

}
