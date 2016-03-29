/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.web.routes;

import java.lang.reflect.Method;

import com.tern.web.Controller;

public class ActionWrapper
{
	private Controller self;
    private Method method;
    private Object[] parameters;
    private String staticPath;
    
    public ActionWrapper(Controller self, Method method, Object[] parameters,String spath) 
    {
        this.self = self;
        this.method = method;
        this.parameters = parameters;
        this.staticPath = spath;
        
        if (method != null && parameters != null)
        {
            Class[] methodTypes = method.getParameterTypes();
            if (methodTypes.length != parameters.length) 
            {
                throw new RouteException("Parameters don't match for `" + self.getClass() + "#" + method.getName() + "`");
            }
            
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = Reflection.convert(parameters[i], methodTypes[i]);
            }
        }
    }
    
    public Controller getController(){return self;}
    public String getStaticPath(){return this.staticPath;}

    public Object invoke() throws Exception
    {    	
        try 
        {
            return method.invoke(self, parameters);
        } 
        catch (Exception ex)
        {
            //throw new RouteException("Exception during invocation of `" + self.getClass() + "#" + method.getName() + "`", ex);
        	throw ex;
        }
    }

    public Method getMethod()
    {
        return method;
    }

    public Object[] getParameters() 
    {
        return parameters;
    }
    
}
