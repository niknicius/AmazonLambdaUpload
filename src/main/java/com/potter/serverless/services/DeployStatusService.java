package com.potter.serverless.services;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class
DeployStatusService {

    private Map<Integer, String> status;

    public DeployStatusService() {
        this.status = new HashMap<>();
    }

    public void putStatus(Integer id, String status){
        if(this.status.get(id) != null){
            replaceStatus(id, status);
        }else {
            this.status.put(id, status);
        }
    }

    private void replaceStatus(Integer id, String status){
        this.status.replace(id, status);
    }

    public String getStatus(Integer id){
        return this.status.get(id);
    }
}
