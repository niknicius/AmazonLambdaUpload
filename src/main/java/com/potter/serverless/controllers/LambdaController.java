package com.potter.serverless.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    
    @PostMapping("functions")
    public ResponseEntity<String> createFunction(@RequestBody LambdaFunction payload) throws Exception {
        Integer id = this.functionService.create(payload);
        return new ResponseEntity<>("{\"status\": \"Lambda function upload successfully requested\", \"id\": " + id + "}", HttpStatus.OK);
    }

    @GetMapping("functions/{id}")
    public ResponseEntity<String> checkStatus(@PathVariable Integer id){
        return new ResponseEntity<String>("{\"status\":\"" + this.functionService.getStatus(id) + "\"}", HttpStatus.OK);
    }
}
