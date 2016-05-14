/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.web.routes;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.tern.util.Trace;
import com.tern.util.config;
import com.tern.web.Controller;
import com.tern.web.Route;

public class ControllerWrapper
{
	Class<Controller> target;
	RouteSet actionPaths;
	private static Class<?> BASE_CLASS = Controller.class.getSuperclass();
	
	@SuppressWarnings("serial")
	static Map<Class<?>,String> paramTypes  = new HashMap<Class<?>,String>(){{
		this.put(int.class, "%");
		this.put(long.class, "%");
		this.put(short.class, "%");
		this.put(byte.class, "%");
		this.put(Integer.class, "%");
		this.put(Long.class, "%");
		this.put(Short.class, "%");
		this.put(Byte.class, "%");
		this.put(String.class, "$");
	}};
	
    public ControllerWrapper(Class<Controller> clazz)
    {
    	this.target = clazz; 
    	this.actionPaths = new RouteSet();
    	
    	//parse methods
    	addClass(clazz);
    	
    	actionPaths.sort();
    }
    
    public RouteSet getRouteSet(){return actionPaths;}
    
    private void addClass(Class<Controller> clazz) 
    {
        Method[] methods = clazz.getMethods();//clazz.getDeclaredMethods();        
        for (Method method : methods)
        {
        	if(method.getDeclaringClass() == BASE_CLASS) continue;
        	
            addMethod(method);
        }
        
        /*Class[] ifaces = clazz.getInterfaces();
        if (ifaces != null) 
        {
            for (Class iface : ifaces) 
            {
                addClass(iface);
            }
        }*/
    }
    
    private void addMethod(Method method)
    {
    	String[] items = null;
    	int httpMethod = HttpMethod.DEFAULT;
    	String extra_path = null;
    	
    	Route deploy = (Route)method.getAnnotation(Route.class);
        if (deploy != null) 
        {
        	items = deploy.value();
        	httpMethod = deploy.method();
        }
        
        if(items==null || items.length<=0)
        {
        	String name = method.getName();
        	String pathname = null;
        	Class<?>[] params = method.getParameterTypes();
        	
        	if(name.equals("index"))  //default action
        	{
        		pathname = "/";
        		//httpMethod = HttpMethod.GET;
        		extra_path = "index";
        	}
        	else if(name.equals("create"))
        	{
        		pathname = "/create";
        		httpMethod = HttpMethod.POST;
        		//extra_path = "index";
        	}
        	else if(name.equals("_new"))  //new
        	{
        		pathname = "/new";
        		httpMethod = HttpMethod.GET;
        	}
        	else if(name.equals("edit"))
        	{
        		if(params!= null && params.length==1)
        		{
        			httpMethod = HttpMethod.GET;
        			String t = paramTypes.get(params[0]);
        			if(t != null)
        			{
        				httpMethod = HttpMethod.GET;
        				pathname = String.format("/%s1/edit", t);
        			}
        			else
        			{
        				return;
        			}
        		}        		
        	}
        	else if(name.equals("update"))
        	{        		
        		if(params!= null && params.length==1)
        		{        			
        			String t = paramTypes.get(params[0]);
        			if(t != null)
        			{
        				httpMethod = HttpMethod.POST;
        				pathname = String.format("/%s1/update", t);
        			}
        			else
        			{
        				return;
        			}
        		}        
        	}
        	else if(name.equals("delete"))
        	{
        		httpMethod = HttpMethod.POST;
        		if(params!= null && params.length==1)
        		{        			
        			String t = paramTypes.get(params[0]);
        			if(t != null)
        			{
        				pathname = String.format("/%s1/delete", t);
        			}
        			else
        			{
        				return;
        			}
        		} 
        		/*else
        		{
        			extra_path = "index";
        		}*/
        	}
        	else if(name.equals("show"))
        	{
        		//has one int or String parameter        		
        		if(params!= null && params.length==1)
        		{
        			String t = paramTypes.get(params[0]);
        			if(t != null)
        			{
        				httpMethod = HttpMethod.GET;
        				pathname = String.format("/%s1", t);
        			}
        			else
        			{
        				return;
        			}
        		}        		
        	}
        	
        	if(pathname == null)
        	{
        		pathname = "/"+name;
        		if(params!=null && params.length>0)
        		{
        			int idx = 1;
        			for(Class<?> cls:params)
        			{
        				String t = paramTypes.get(cls);
        				if(t != null)
        				{
        					pathname += String.format("/%s%d", t,idx);
        					idx++;
        				}
        				else
        				{
        					return; //unexcept action
        				}        				
        			}
        		}
        	}
        	
        	items = new String[]{ pathname };
        }
        
        for (String path : items) 
        {
        	method.setAccessible(true);
        	try
        	{
        		ActionPath _path = new ActionPath(method,path,httpMethod);
        		_path.extraStatic = extra_path;
        		
        	    actionPaths.addPath(_path);
        	    
        	    /*if(config.isDebug())
           	    {
           	    	Trace.write(Trace.Information, "method[%s]:%s", method.toString(), _path);
           	    }*/
        	}
        	catch(IOException e)
			{
        		Trace.write(Trace.Error, e, "controller[%s] method[%s] parsed failed:%s",
        				this.target.toString(),method.toString(),path);
			}
        }
    }

    @Override
    public String toString()
    {
    	return target.getName();
    }
}
