package com.potter.serverless.controllers;

import com.potter.serverless.models.LambdaFunction;
import com.potter.serverless.services.FunctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class LambdaController {

    @Autowired
    private FunctionService functionService;

    @RequestMapping("functions")
    @ResponseBody
    public String getMessage() {
       /*Lambda lambda = new Lambda();
       return lambda.getFunctionList().toString();*/
       return null;
    }

    @PostMapping("functions")
        public ResponseEntity<LambdaFunction> createFunction(@RequestBody LambdaFunction payload) throws Exception {
            return new ResponseEntity<>(this.functionService.create(payload), HttpStatus.OK);
    }
}
