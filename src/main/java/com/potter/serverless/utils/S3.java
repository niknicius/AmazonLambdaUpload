package com.potter.serverless.utils;

import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;


public class S3 {

    private S3AsyncClient s3Client;
    private Region region;

    public S3(Region region) {
        this.region = region;
        this.s3Client = S3AsyncClient.builder().region(this.region).build();
    }


    public CompletableFuture<PutObjectResponse> uploadObjectToAExistentBucket(String bucketName, Path objectLocation, String objKey){
        File file = objectLocation.toFile();
        return this.s3Client.putObject(PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objKey)
                .build(),
                AsyncRequestBody.fromFile(file));
    }

}
