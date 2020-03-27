package com.potter.serverless.controllers;

import com.potter.serverless.utils.Lambda;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LambdaController {

    @RequestMapping("functions")
    @ResponseBody
    public String getMessage() {
       Lambda lambda = new Lambda();
       return lambda.getFunctionList().toString();
    }

    @PostMapping("functions")
    @ResponseBody
    public String createFunction() {
        Lambda lambda = new Lambda();
        return lambda.createFunction();
    }
}
