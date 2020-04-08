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
       this.functionService.getAll();
       return null;
    }

    @PostMapping("functions")
    public ResponseEntity<String> createFunction(@RequestBody LambdaFunction payload) throws Exception {
        long tempoInicial = System.currentTimeMillis();
        this.functionService.create(payload);
        long tempoFinal = System.currentTimeMillis();
        System.out.println(tempoFinal - tempoInicial);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
