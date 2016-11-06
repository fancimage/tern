/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.util;

import java.util.Enumeration;
import java.util.Map;

import com.tern.dao.Column;
import com.tern.dao.Model;
import com.tern.dao.Record;

public class html 
{
	public static Map<String, Object> getValuesFromRequest(Model m,javax.servlet.ServletRequest request,boolean isNew)
    {
    	Map<String, Object> vars = new java.util.HashMap<String, Object>();
    	//javax.servlet.ServletRequest request = page.pageContext.getRequest();
    	//Map ps = request.getParameterMap();
    	Map<String,Column> cols = m.getColumns();
    	
    	int count = 0,total = m.getColumnList().length;
    	Column id = null;
    	
    	try
    	{
    	    id = m.column("id");
    	}
    	catch(Exception e){}  //no id column?
    	
    	if(isNew && id != null) total--; //ignore id
    	
    	Enumeration<String> it = request.getParameterNames();
    	while(it.hasMoreElements())
    	{
    		String pname = it.nextElement();
    		Column col = cols.get(pname.toLowerCase());
    		//if(col==null && !isNew && id != null && id.getName().equalsIgnoreCase(pname)) col=id;
    		
    		if(col != null)
    		{
    			if(vars.containsKey(col.getName())) continue;
    			
    			count++;
    			vars.put(col.getName(), request.getParameter(pname));
    			if(count >= total) break;
    		}
    	}
    	
    	if(count < total) //retrive request attributes
    	{
    		it = request.getAttributeNames();
    		while(it.hasMoreElements())
        	{
        		String pname = it.nextElement();
        		Column col = cols.get(pname.toLowerCase());
        		//if(col==null && !isNew && id != null && id.getName().equalsIgnoreCase(pname)) col=id;
        		
        		if(col != null)
        		{
        			if(vars.containsKey(col.getName())) continue;
        			
        			count++;
        			vars.put(col.getName(), request.getAttribute(pname));
        			if(count >= total) break;
        		}
        	}
    	}
    	
    	/*for(com.tern.dao.Column col:m.getColumnList())
    	{
    		if(col.isID()) continue;
    		
    		if(ps.containsKey(col.getName()))
    		{
    			vars.put(col.getName(), request.getParameter(col.getName()));
    		}
    		else
    		{
    			Object v = request.getAttribute(col.getName());
    			if(v!=null)
    			{
    				vars.put(col.getName(), v);
    			}
    		}
    	}*/
    	
    	return vars;
    }
    
    public static Record new_record(Model m,javax.servlet.ServletRequest request)
    {
    	return m.create(getValuesFromRequest(m,request,true));
    }
    
    public static Record update_record(Model m,javax.servlet.ServletRequest request)
    {
        /*java.util.Map<String, Object> vars = new java.util.HashMap<String, Object>();
    	
    	javax.servlet.ServletRequest request = page.pageContext.getRequest();
    	java.util.Map ps = request.getParameterMap();
    	
    	for(com.tern.dao.Column col:m.getColumnList())
    	{    	
    		if(ps.containsKey(col.getName()))
    		{
    			vars.put(col.getName(), request.getParameter(col.getName()));
    		}
    		else
    		{
    			Object v = request.getAttribute(col.getName());
    			if(v!=null)
    			{
    				vars.put(col.getName(), v);
    			}
    			else if(col.isID())
    			{
    				if(ps.containsKey("id"))
    				{
    					vars.put("id", request.getParameter("id"));
    				}
    				else
    				{
    					v = request.getAttribute("id");
    					if(v!=null)
    	    			{
    	    				vars.put("id", v);
    	    			}
    				}
    			}
    		}
    	}
    	
    	return m.update(vars);*/
    	
    	Map<String, Object> vars = getValuesFromRequest(m,request,false);
    	Map<String, Object> keys = null;//new java.util.HashMap<String, Object>();
    	if(m.getId() == null && m.getKeys() != null)
    	{
    		keys = new java.util.HashMap<String, Object>();
    		for(Column c:m.getKeys())
    		{
    			Object obj = request.getParameter(c.getName()+"_ori");
    			if(obj == null)
    			{
    				obj = request.getAttribute(c.getName()+"_ori");
    			}
    			
    			if(obj != null)
    			{
    			    keys.put(c.getName(), vars.get(c.getName()));
    			    vars.put(c.getName(), obj);
    			}    			
    		}
    	}
    	
    	Record r = m.update(vars);
    	
    	if(keys != null && keys.size()>0)
    	{
    		for(String k : keys.keySet())
    		{
    			r.set(k, keys.get(k));
    		}
    	}
    	
    	return r;
    }
}
