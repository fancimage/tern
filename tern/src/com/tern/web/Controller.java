/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class Controller 
{
	protected HttpServletRequest  request;
	protected HttpServletResponse response;
	protected HttpSession  session;
	
	protected int page = -1;
	protected int pageSize = -1;
	protected Object viewObject;
	
	HttpStream  stream;
	//Map<String,Object> vars; /*页面变量*/
	
    void init(HttpServletRequest request,HttpServletResponse response)
    {
    	this.request = request;
    	this.response = response;
    	this.session = request.getSession();
    }
    
    final protected void setContentType(String type)
    {
    	this.response.setContentType(type);
    }
    
    protected void setViewObject(Object obj)
    {
    	viewObject = obj;
    }
    
    public Object getViewObject(){return viewObject;}
    
    //final protected void setAttribute(String name,Object value)
    //{   
    	//if(null == vars) vars = new java.util.HashMap<String, Object>();
    	//vars.put(name, value);
    //	request.setAttribute(name, value);
    //}
    
    final protected void redirect(String url)
    {    	    	
    	throw new RedirectRequest(1,url);
    }
    
    final protected void forward(String url)
    {    	
    	throw new RedirectRequest(2,url);
    }
    
    final protected HttpStream getStream()
    {
    	if(null == stream) 
    	{
    		stream = new HttpStream(response);
    	}
    	return stream;
    }           
}

@SuppressWarnings("serial")
class RedirectRequest extends RuntimeException
{
	int action;
	String url;
	public RedirectRequest(int action,String url)
	{
		this.action = action;
		this.url = url;
	}
}
