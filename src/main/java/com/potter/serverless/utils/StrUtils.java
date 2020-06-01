package com.potter.serverless.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class StrUtils {

    public static String snakeToPascal(String str){
        String[] strings = str.split("_");
        String string = "";
        for(String s: strings){
            string = string.concat(StringUtils.capitalize(s));
        }

        return string;
    }

    public static String replaceJsonKey(String json, Map<String, String> map ){
        for(Map.Entry<String, String> entry: map.entrySet()){
            json = json.replace(entry.getKey(), entry.getValue());
        }
        return json;
    }

}
