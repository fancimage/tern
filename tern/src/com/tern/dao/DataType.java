/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.dao;

public enum DataType 
{
    ID,
    Numeric,
    String,
    /*Date,
    Time,*/
    Datetime,
    Bool,
    Text,
    Enum,
    Binary,
    Image,
    Blob,
    Belongs, //从属关系
    Having   //子表
    ;
    
    /*public static final java.util.Map<String, DataType> types = new java.util.HashMap<String, DataType>(){{
    	put("id",ID);
    	put("numeric",Numeric);
    }};*/
     
}
