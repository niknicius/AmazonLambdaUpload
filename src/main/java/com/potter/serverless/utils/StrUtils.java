package com.potter.serverless.utils;

import org.apache.commons.lang3.StringUtils;

public class StrUtils {

    public static String snakeToPascal(String str){
        String[] strings = str.split("_");
        String string = "";
        for(String s: strings){
            string = string.concat(StringUtils.capitalize(s));
        }

        return string;
    }

}
