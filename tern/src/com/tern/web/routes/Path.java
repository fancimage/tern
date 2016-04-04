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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.tern.util.ThreadSafeMap;
import com.tern.web.Controller;

abstract class Path implements Comparable<Path> 
{
	private static final String PATTERN = "$%[{(*";
	private String key = "";
	private List<Element> elements = new ArrayList<Element>();
	protected boolean named;	
	
	protected void parseAllElement(String url) throws IOException
	{
		String[] items = url.split("/");
		boolean first = true;
		
		for (String item : items)
		{
			item = item.trim();
			if(item.length()<=0) continue;
			
			if (PATTERN.indexOf(item.charAt(0)) != -1) 
			{
				PathScanner in = new PathScanner(new PeekReader(item));
                elements.add(parseElement(in));
            }
			else
			{
                if (first) 
                {
                    key = item;
                }
                
                elements.add(new StaticElement(item));
            }
			
			first = false;
		}
	}
	
	private Element parseElement(PathScanner in) throws IOException 
	{
        in.read();
        
        if (in.isSymbol("%")) 
        {
            return parseReference(in, new IntegerElement());
        }
        else if (in.isSymbol("$"))
        {
            return parseReference(in, new StringElement());
        }
        else if (in.isSymbol("*")) 
        {
            return parseReference(in, new StarElement());
        }
        else if (in.isSymbol("[")) 
        {
            List<String> ls = new ArrayList<String>();
            for (;;) 
            {
                in.read();
                if (in.isName())
                {
                    ls.add(in.getToken());
                }
                else if (in.isSymbol("]")) 
                {
                    break;
                }
                else if (!in.isSymbol("|")) 
                {
                    in.error("Expected | or ] instead of `" + in.getToken() + "'");
                }
            }
            
            String[] values = ls.toArray(new String[ls.size()]);
            return parseReference(in, new OptionsElement(values));
        }
        
        return null;
    }
	
	private ParameterElement parseReference(PathScanner in, ParameterElement element) throws IOException
	{
        in.peek();
        
        if (in.isEnd()) 
        {
            return element; 
        }
        else if ((named && in.isName()) || in.isNumber()) 
        {
            in.read();
            String parameter = in.getToken();
            StringBuilder out = new StringBuilder();
            
            if (named) 
            {
                String s = in.getToken();
                out.append(s);
                element.setProperties(s);
                element.setParameter(0);
            }
            else 
            {
                int v = Integer.parseInt(parameter);
                element.setParameter(v - 1);
            }
            
            in.peek();
            if (in.isSymbol(".")) 
            {
                for (;;)
                {
                    in.peek();
                    if (in.isSymbol(".")) 
                    {
                        in.read();
                        if (out.length() > 0)
                        {
                            out.append(".");
                        }
                    }
                    else
                    {
                        break;
                    }
                    
                    in.read();
                    if (in.isName())
                    {
                        out.append(in.getToken());
                    } 
                    else
                    {
                        in.error("Expected NAME instead of `" + in.getToken() + "'");
                    }
                }
                
                element.setProperties(out.toString());
            }
        }
        else if (!in.isSymbol("?")) 
        {
            if (named) 
            {
                in.error("Expected NAME instead of `" + in.getToken() + "'");
            }
            else
            {
                in.error("Expected PARAMETER NUMBER instead of `" + in.getToken() + "'");
            }
        }

        in.peek();
        
        if (in.isSymbol("?")) 
        {
            in.read();
            element.setOptional(true);
        }
        
        return element;
    }
	
	public final String getKey(){return key;}
	
	public abstract Object getTarget();	
	
	public boolean match(PathReader input)
	{
        input.reset();
        //input.bind(getTarget());
                
        for (Element element : elements)
        {
            if (element == null) 
            {
                continue;
            }
            
            if (!element.match(input)) 
            {
                return false;
            }
        }
        
        return !input.hasNext();
    }
	
	public int compareTo(Path path) 
	{
        int sizeDiff = elements.size() - path.elements.size();
        
        Iterator<Element> i = elements.iterator();
        Iterator<Element> j = path.elements.iterator();
        
        for (;;)
        {
            if (!i.hasNext() || !j.hasNext()) 
            {
                return sizeDiff;
            }
            
            Element elemA = i.next();
            Element elemB = j.next();
            
            int result = elemA.priority() - elemB.priority();
            if (result != 0) 
            {
                return result;
            }
        }
    }
	
	public abstract Object resolve(PathReader input);
}

class ControllerPath extends Path
{
	private static Map<Class<Controller>,ControllerWrapper> controllers = 
		    new ThreadSafeMap<Class<Controller>,ControllerWrapper>();
	
	private ControllerWrapper wrapper;
	
	ControllerPath(Class<Controller> target,String url)  throws IOException
	{	
		this.named = true;
		
		//parse controller
   	    wrapper = controllers.get(target);
   	    if(null == wrapper)
   	    {
   		     wrapper = new ControllerWrapper(target);
   		     controllers.put(target, wrapper);
   	    }
   	    
   	    parseAllElement(url);
	}		
	
	public Class<Controller> getTarget()
	{
		return wrapper.target;
	}
	
	public ActionWrapper resolve(PathReader input)
	{	
		if(this.match(input))
		{
			PathReader newInput = new PathReader(input.getRemaining(),input.getHttpMethod());
			Object m = wrapper.actionPaths.resolve( newInput );
			if(m instanceof ActionPath)
			{
				ActionPath apath = (ActionPath)m;							
				
				Controller obj;
				try 
				{
					obj = wrapper.target.newInstance();
					
					StringBuffer defPath =new StringBuffer();
					//parse controller parameters
					input.bind(obj,defPath);
					this.match(input);
					
					//parse action parameters
					Method method = apath.getTarget();
					newInput.bind(method,defPath);
					apath.match(newInput);
					
					String _spath = newInput.getStaticPath();
					if(null != apath.extraStatic)
					{
						_spath +="/"+apath.extraStatic;
					}
					
					return new ActionWrapper(obj, method, newInput.getParameters() ,_spath);
				}
				catch (Exception e) 
				{
					throw new RouteException("create controller instance failed",e);
				}			
				
			}
		}
		
		return null;
	}
		
}

class ActionPath extends Path
{
	private Method method;
	private int httpMethods;
	
	String extraStatic;
	
	ActionPath(Method method,String url,int httpMethods)  throws IOException
	{
		this.method = method;
		this.httpMethods= httpMethods;
		
		parseAllElement(url);
	}
	
	public Method getTarget()
	{
		return method;
	}
	
	public boolean match(PathReader input)
	{
		if ((httpMethods & input.getHttpMethod()) == 0) 
		{
			//Trace.write(Trace.Information, "Path[%d]:%s,Action[%d]:%s", 
			//		input.getHttpMethod(),input.toString(),this.httpMethods,this.toString());
            return false;
        }
		
		return super.match(input);
	}
	
	public ActionPath resolve(PathReader input)
	{		
		if(this.match(input))
		{
			return this;
		}
		
		return null;
	}
}
