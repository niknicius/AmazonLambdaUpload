package com.potter.serverless.tasks;


import com.potter.serverless.models.LambdaFunction;
import com.potter.serverless.utils.S3;
import com.potter.serverless.utils.StrUtils;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipAndUpload {

    private LambdaFunction lambdaFunction;
    private S3 s3;


    public ZipAndUpload(LambdaFunction lambdaFunction) {
        this.lambdaFunction = lambdaFunction;
        this.s3 = new S3(Region.of(lambdaFunction.getRegion()));
    }

    public CompletableFuture<String> run(String bucketPhysicalId){
        CompletableFuture<String> response = new CompletableFuture<>();
        this.zip(this.lambdaFunction.getCodeLocation()).whenComplete((path, throwable) -> {
            if(throwable != null){
                response.completeExceptionally(throwable);
            }else{
                uploadFile(bucketPhysicalId, path).whenComplete((s, throwable1) -> {
                    if(throwable1 != null){
                        response.completeExceptionally(throwable1);
                    }else{
                        response.complete(s);
                    }
                });
            }
        });
        return response;
    }

    private void threatZipResponse(Path path, Throwable throwable){}

    private CompletableFuture<Path> zip(String folderLocation){
        CompletableFuture<Path> completableFuture = new CompletableFuture<>();
        try {
            Path destinyPath = Files.createTempDirectory("zips");
            Path filePath = destinyPath.resolve(System.nanoTime() + ".zip");
            try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(filePath))) {
                Path pp = Paths.get(folderLocation);
                Files.walk(pp)
                        .filter(path -> !Files.isDirectory(path))
                        .forEach(path -> {
                            ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());
                            try {
                                zs.putNextEntry(zipEntry);
                                Files.copy(path, zs);
                                zs.closeEntry();
                            } catch (IOException e) {
                                completableFuture.completeExceptionally(e);
                            }
                        });
                completableFuture.complete(filePath);
            }
        }catch (Exception ex){
            completableFuture.completeExceptionally(ex);
        }
        return completableFuture;
    }

    private CompletableFuture<String> uploadFile(String bucketName, Path path){
        CompletableFuture<String> response = new CompletableFuture<>();
        this.s3.uploadObjectToAExistentBucket(bucketName, path,
                StrUtils.snakeToPascal(this.lambdaFunction.getName()).concat(".zip"))
                .whenComplete((putObjectResponse, throwable) -> {
                    if(throwable != null){
                        response.completeExceptionally(throwable);
                    }else{
                        response.complete(putObjectResponse.toString());
                    }
                });
        return response;
    }

}
