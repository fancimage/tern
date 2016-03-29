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
import java.util.ArrayList;
import java.util.List;

class PathReader
{
	private Object[] values; 
    private String[] items;
    private Class[] types;
    private int position;
    private boolean bind = false;
    private String remaining = "";
    private int httpMethod = 0;
    
    private StringBuffer staticPath;
    
    public PathReader(String input) 
    {
        this(input, HttpMethod.GET);
    }
    
    public PathReader(String input, int httpMethod) 
    {
        this.httpMethod = httpMethod;
        
        if (input == null) 
        {
            items = new String[0];
        }
        else
        {
            String[] _items = input.split("/");
            
            List<String> list = new ArrayList<String>();
            for (String item : _items) 
            {
                if (item != null && item.trim().length() > 0)
                {
                	list.add(item.trim());
                }
            }
            
            this.items = list.toArray(new String[list.size()]);
        }
    }
    
    void next()
    {
        position++;
    }

    boolean hasNext() 
    {
        return position < items.length;
    }

    void reset() 
    {
        position = 0;
        //types = null;
        //values = null;
    }
    
    void consume()
    {
        position = items.length;
    }

    public String current()
    {
        if (position >= 0 && position < items.length) 
        {
            return items[position];
        }
        else
        {
            return null;
        }
    }

    String remaining() 
    {
        if (position >= 0 && position < items.length) 
        {
            StringBuilder out = new StringBuilder();
            for (int i = position; i < items.length; i++) 
            {
                if (out.length() > 0) 
                {
                    out.append("/");
                }
                out.append(items[i]);
            }
            return remaining = out.toString();
        }
        else 
        {
            return null;
        }
    }

    void setParameter(int position, Object value) 
    {
        if (values!=null && position >= 0 && position < values.length) 
        {
            values[position] = value;
        }
    }
    
    Object getParameter(int position) 
    {
        if (values!= null && position >= 0 && position < values.length) 
        {
            if (values[position] == null)
            {
                try 
                {
                    values[position] = types[position].newInstance();
                }
                catch (Exception ex)
                {
                    throw new RuntimeException(ex);
                }
            }
            
            return values[position];
        } 
        else
        {
            return null;
        }
    }

    boolean isBind() 
    {
        return bind;
    	//return true;
    }
    
    void appendStatic(String segment)
    {
    	staticPath.append("/").append(segment);
    }
    
    String getStaticPath()
    {
    	return staticPath.toString();
    }

    public void bind(Object target,StringBuffer container) 
    {
    	this.staticPath = container;
        if (target instanceof Method) 
        {
            this.bind = true;
            types = ((Method)target).getParameterTypes();
            if(types.length>0)
            {
                this.values = new Object[types.length];
            }
        } 
        else 
        {
            this.bind = true;
            types = new Class[] { target.getClass() };        
            this.values = new Object[1];
            this.values[0] = target;
        }
    }

    public Object[] getParameters() 
    {
        return values;
    }
    
    public String getRemaining()
    {
        return remaining;
    }

    int getHttpMethod() 
    {
        return httpMethod;
    }
    
    public String toString()
    {
        StringBuilder out = new StringBuilder();

        if (items != null) 
        {
            for (String s : items) 
            {
                out.append(s);
                out.append("/");
            }
        }
        
        return out.toString();
    }
}
