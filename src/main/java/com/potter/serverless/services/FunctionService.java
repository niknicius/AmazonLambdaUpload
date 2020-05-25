package com.potter.serverless.services;

import com.potter.serverless.models.LambdaFunction;
import com.potter.serverless.tasks.CreateBucketS3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;


@Service
public class FunctionService {

    @Autowired
    private TaskService taskService;

    @Autowired
    private DeployStatusService deployStatusService;

    @Autowired
    ResourceLoader resourceLoader;
    
    public Integer create(LambdaFunction lambdaFunction) {
        Integer id = this.deployStatusService.getLastId();
        CreateBucketS3 task = new CreateBucketS3(lambdaFunction, this.deployStatusService, id);
        task.run();
        /*if(lambdaFunction.getRegion() != null){
            if(Region.regions().toString().contains(lambdaFunction.getRegion())){
                this.lambda = new Lambda(this.getRegion(lambdaFunction.getRegion()), lambdaFunction);
                System.out.println(this.lambda.createFunction());
            }else{
                throw new Exception("Invalid region");
            }
        }*/

        return id;
    }

    protected Region getRegion(String region){
        for(Region r: Region.regions()){
            if(r.toString().equalsIgnoreCase(region)){
                return r;
            }
        }
        return null;
    }

    public String getStatus(Integer id){
        return this.deployStatusService.getStatus(id);
    }

}
