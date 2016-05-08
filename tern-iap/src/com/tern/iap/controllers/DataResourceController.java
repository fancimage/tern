/**
 * Tern-iap Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.iap.controllers;

import com.tern.dao.Model;
import com.tern.dao.Record;
import com.tern.dao.RecordSet;
import com.tern.iap.util.ActionResult;
import com.tern.util.Convert;
import com.tern.util.Trace;
import com.tern.util.config;
import com.tern.web.HttpStream;
import com.tern.web.Route;
import com.tern.web.Controller;
import com.tern.util.html;

@Route("/data/$modelName/*")
public class DataResourceController extends Controller
{
    private String modelName;
    protected Model model;
    
    public String index()
	{
    	model = Model.from(modelName);
    	
		RecordSet records = model.query().joinAll();
		
		request.setAttribute("model", model);
		request.setAttribute("records", records);
		
		return "model/index";
	}
	
	public void delete()
	{
		ActionResult r = new ActionResult();
		this.setViewObject(r);
		
		String ids = request.getParameter("items");
		if(ids==null || ids.length()<=0)
		{
			r.setResult(1);  //参数错误(非法的请求)
			return;
		}
		
		String[] arr = ids.split(",");
	    if(arr.length<=0)
	    {
	    	r.setResult(1);  //参数错误(非法的请求)
	    	return;
	    }
	    
	    model = Model.from(modelName);
	    model.delete(arr);
	}
	
	//新建
	public void create()
	{
		ActionResult r = new ActionResult();
		this.setViewObject(r);
		
		model = Model.from(modelName);
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
			r.setResult(3,"服务器异常.");
			Trace.write(Trace.Error, t, "create record(%s) failed.",modelName);
		}
	}
	
	//更新
	public void update(int id)
	{
		ActionResult r = new ActionResult();
		this.setViewObject(r);
		
		model = Model.from(modelName);
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
		}
		
		//redirect(String.format("%spet", config.getRoot()));  //to index page
	}
	
	public String _new()
	{
		model = Model.from(modelName);
		
		Record record = model.create(); //new		
		request.setAttribute("model", model);
		request.setAttribute("record", record);
		
		return "model/edit";
	}
	
	public String edit(int id)
	{
		model = Model.from(modelName);
		
		Record record = model.find(id); //retrive		
		request.setAttribute("model", model);
		request.setAttribute("record", record);
		
		return "model/edit";
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
