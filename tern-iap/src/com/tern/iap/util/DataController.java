/**
 * Tern-iap Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.iap.util;

import com.tern.dao.Model;
import com.tern.dao.Record;
import com.tern.dao.RecordSet;
import com.tern.util.Convert;
import com.tern.util.Trace;
import com.tern.util.config;
import com.tern.util.html;
import com.tern.web.Controller;
import com.tern.web.HttpStream;

public class DataController extends Controller
{
	protected String modelName;
    protected Model model;
    
    protected Model getModel()
    {
    	return Model.from(modelName);
    }
    
    public String index()
	{
    	model = getModel();
    	
		RecordSet records = model.query();
		
		request.setAttribute("model", model);
		request.setAttribute("records", records);
		
		return modelName+"/index";
	}
	
	public void delete()
	{
		ActionResult r = new ActionResult();
		this.setViewObject(r);
		
		String ids = request.getParameter("items");
		if(ids==null || ids.length()<=0)
		{
			r.setResult(1);
			return;
		}
		
		String[] arr = ids.split(",");
	    if(arr.length<=0)
	    {
	    	r.setResult(1);
	    	return;
	    }
	    
	    model = getModel();
	    model.delete(arr);
	}
	
	public void create()
	{
		ActionResult r = new ActionResult();
		this.setViewObject(r);
		
		model = getModel();
		try
		{
		    Record record = html.new_record(model, request);
		    record.save();
		}
		catch(com.tern.dao.ValueException e)
		{			
			r.setResult(2,e.getMessage());
		}
		catch(Throwable t)
		{
			Trace.write(Trace.Error, t, "create record(%s) failed.",modelName);
			r.setResult(3,"服务器异常.");			
		}
	}
	
	public void update(int id)
	{
		ActionResult r = new ActionResult();
		this.setViewObject(r);
		
		model = getModel();
		try
		{
		    Record record = html.update_record(model, request);
		    record.save();		    		   
		}
		catch(com.tern.dao.ValueException e)
		{
			r.setResult(2,e.getMessage());			
		}
		catch(Throwable t)
		{
			r.setResult(3,"服务器异常.");
			Trace.write(Trace.Error, t, "update record(%s) failed.",modelName);			
		}
	}
	
	public String _new()
	{
		model = getModel();
		
		Record record = model.create(); //new		
		request.setAttribute("model", model);
		request.setAttribute("record", record);
		
		return modelName+"/edit";
	}
	
	public String edit(int id)
	{
		model = getModel();
		
		Record record = model.find(id); //retrive		
		request.setAttribute("model", model);
		request.setAttribute("record", record);
		
		return modelName+"/edit";
	}
	
	/*protected void writeResult(int result,String err)
    {
        this.setContentType("text/javascript");
        this.response.setCharacterEncoding(config.getEncoding());
        
        HttpStream out = this.getStream();
        out.append("{\"code\":").append(String.valueOf(result));
        
        if(0 != result && err != null)
        {
        	if(err.indexOf('\n') >= 0)
        	{
        		err = Convert.replaceAll(err, "\n", "\\n");
        	}
            out.append(",\"message\":\"").append(err).append("\"");
        }
        
        out.append("}");
    }*/
}
