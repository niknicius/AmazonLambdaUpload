package com.potter.serverless.services;

import com.potter.serverless.models.LambdaFunction;
import com.potter.serverless.utils.Lambda;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.model.ListFunctionsResponse;

@Service
public class FunctionService {

    private Lambda lambda;
    
    public LambdaFunction create(LambdaFunction lambdaFunction) throws Exception {
        if(lambdaFunction.getRegion() != null){
            if(Region.regions().toString().contains(lambdaFunction.getRegion())){
                this.lambda = new Lambda(this.getRegion(lambdaFunction.getRegion()), lambdaFunction);
                System.out.println(this.lambda.createFunction());
            }else{
                throw new Exception("Invalid region");
            }
        }
        return lambdaFunction;
    }

    protected Region getRegion(String region){
        for(Region r: Region.regions()){
            if(r.toString().equalsIgnoreCase(region)){
                return r;
            }
        }
        return null;
    }

    public void getAll() {
        ListFunctionsResponse a = this.lambda.getFunctionList();
        System.out.println(a.toString());
    }
}
