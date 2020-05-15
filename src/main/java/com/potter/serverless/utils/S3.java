package com.potter.serverless.utils;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.FileNotFoundException;


public class S3 {

    private S3Client s3Client;
    private Region region;

    public S3(Region region) {
        this.region = region;
        this.s3Client = S3Client.builder().region(this.region).build();
    }

    protected CreateBucketRequest createBucketRequest(String bucketName){
        return CreateBucketRequest.builder()
                .bucket(bucketName)
                .acl(BucketCannedACL.PUBLIC_READ)
                /*.createBucketConfiguration(CreateBucketConfiguration.builder()
                        .locationConstraint(this.region.id()).build())*/
                .build();
    }

    public CreateBucketResponse createBucket(String bucketName){
        CreateBucketRequest request = this.createBucketRequest(bucketName);
        return this.s3Client.createBucket(request);
    }

    public PutObjectResponse uploadObjectToAExistentBucket(String bucketName, String objectLocation, String objKey){
        File file = new File(objectLocation);
        return this.s3Client.putObject(PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objKey)
                .build(),
                RequestBody.fromFile(file));
    }

}
