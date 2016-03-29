/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.web.routes;

public class HttpMethod
{
	public static final int GET = 0x1;
    public static final int POST = 0x2;
    public static final int PUT = 0x4;
    public static final int DELETE = 0x8;

    public static final int ALL = 0xff;
    public static final int DEFAULT = GET | POST;
    
    public static int parse(String methods) 
    {
        int ret = 0;
        String[] arr = methods.split("\\|");
        for (String str : arr)
        {
        	int v = 0;
        	str = str.trim();
        	if ("GET".equalsIgnoreCase(str)) 
        	{
                v = GET;
            }
        	else if ("POST".equalsIgnoreCase(str)) 
        	{
        		v = POST;
            }
        	else if ("PUT".equalsIgnoreCase(str)) 
        	{
        		v = PUT;
            }
        	else if ("DELETE".equalsIgnoreCase(str))
        	{
        		v = DELETE;
            } 
        	else if ("HEAD".equalsIgnoreCase(str)) 
        	{                
            } 

            ret |= v;
        }

        return ret;
    }
}
