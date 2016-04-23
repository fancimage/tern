/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package controllers;

import com.tern.util.Trace;
import com.tern.web.Controller;
import com.tern.web.HttpStream;
import com.tern.web.Route;

public class TestController extends Controller
{
	public void index()
    {
    	HttpStream out = getStream();
    	
    	out.append("Hello")
    	   .append(" ")
    	   .append("World!");
    }
    
    public void page1()
    {
    	int i=10;
    	i++;
    	Trace.write(Trace.Information, "page1--result:%d", i);
    	
    	session.setAttribute("val", "很高兴！");
    	request.setAttribute("val", "greate!");
    }
    
    @Route("/op1/$1?")
    public void optional1(String s)
    {
    	getStream().append("params:" + s);
    }
    
    @Route("/op2/$1?")
    public void optional2(boolean s)
    {
    	getStream().append("params:" + s);
    }
}
