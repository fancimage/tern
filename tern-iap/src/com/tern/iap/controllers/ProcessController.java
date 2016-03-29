/**
 * Tern-iap Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.iap.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opensymphony.workflow.loader.ActionDescriptor;
import com.opensymphony.workflow.loader.StepDescriptor;
import com.opensymphony.workflow.loader.WorkflowDescriptor;
import com.tern.dao.Model;
import com.tern.dao.Record;
import com.tern.dao.RecordSet;
import com.tern.dao.RecordState;
import com.tern.db.db;
import com.tern.iap.workflow.Service;
import com.tern.iap.workflow.Workflow;
import com.tern.util.Convert;
import com.tern.util.Trace;
import com.tern.util.config;
import com.tern.util.html;
import com.tern.web.ActionException;
import com.tern.web.Controller;
import com.tern.web.HttpStream;
import com.tern.web.Route;

@Route("/service/%sid/process/*")
public class ProcessController extends Controller
{	
	private int sid;
	
	public String index()
	{
		Service service = Service.getService(sid);
		if(null == service)
		{
			throw new ActionException( String.format("Service(%d) does not exists.", sid));
		}
		
		Model model = Model.from(service.getDataTableName());
        RecordSet records = model.query()
                                 .join("process",true,new String[]{"id","id"});
		
		request.setAttribute("model", model);
		request.setAttribute("records", records);
		request.setAttribute("service",service);
		
		return String.format("service/%s/index", service.getName() );
	}
	
	public String _new()
	{
		Service service = Service.getService(sid);
		if(null == service)
		{
			throw new ActionException( String.format("Service(%d) does not exists.", sid));
		}
		
		Model model = Model.from(service.getDataTableName());
		Record record = model.create(); //new
		
		request.setAttribute("model",  model);
		request.setAttribute("record", record);
		request.setAttribute("service",service);
		
		return String.format("service/%s/new", service.getName() );
	}
	
	public String edit(long pid)
	{
		if(config.isDebug())
		{
			com.tern.iap.Operator op = new com.tern.iap.Operator(1,"乔旭峰","qiao");
			request.getSession().setAttribute("tern.operator", op);
		}
		
		Service service = Service.getService(sid);
		if(null == service)
		{
			throw new ActionException( String.format("Service(%d) does not exists.", sid));
		}
		
		//get current step
		StepDescriptor step = null;
		WorkflowDescriptor wd = Workflow.getInstance().getWorkflowDescriptor(service.getName());
		List tmp = Workflow.getInstance().getCurrentSteps(pid);
		if(tmp != null && tmp.size() > 0 )
		{
			step = wd.getStep(Convert.parseInt(tmp.get(0)));
		}
		
		Model model = Model.from(service.getDataTableName());
		Record record = model.find(pid);	
		
		Map<String,Object> inputs = new HashMap<String,Object>();
	    inputs.put("wfName", service.getName());
    	inputs.put("data", record);
    	inputs.put("service", service);
				
		//get available actions
    	List<ActionDescriptor> actions = new ArrayList<ActionDescriptor>();
		int[] arr = Workflow.getInstance().getAvailableActions(pid,inputs);
		for(int a : arr)
		{
			ActionDescriptor ad = step.getAction(a);
			if(ad != null)
			{
				actions.add(ad);
			}
		}
		
		request.setAttribute("model",  model);
		request.setAttribute("record", record);
		request.setAttribute("service",service);
		request.setAttribute("step",step);
		request.setAttribute("actions",actions);
		
		return String.format("service/%s/edit", service.getName() );
	}
	
	public void create()
	{
		if(config.isDebug())
		{
			com.tern.iap.Operator op = new com.tern.iap.Operator(1,"乔旭峰","qiao");
			request.getSession().setAttribute("tern.operator", op);
		}
		
		//create new process
		//1. get service info
		Service service = Service.getService(sid);
		if(null == service)
		{
			//throw new ActionException( String.format("Service(%d) does not exists.", sid));
			writeResult(101,"流程不存在.");
			return;
		}
		
		Model model = Model.from(service.getDataTableName());
		
		try
		{
		    //2. create entity-data
		    Record record = html.new_record(model, request);
		    if(record.getState() == RecordState.Error)
		    {
		    	writeResult(2, record.getErrorMessage());
		    	return;
		    }
		    
		    db.transaction();
		
		    //3. create process
		    long pid = Workflow.getInstance().createInstance(service, record);
		    if(pid <= 0)
		    {
		        db.rollback();
		    }
		    		    
		    record.save();
		    
		    db.commit();
		}
		catch(com.tern.dao.ValueException e)
		{
			db.rollback();
			writeResult(2,e.getMessage());
		}
		catch(Exception e)
		{
			db.rollback();
			Trace.write(Trace.Error, e, "create process failed");
			//throw new ActionException( String.format("Create process for Service(%d) failed.", sid) , e);
			writeResult(102,"内部错误：建立流程失败.");
		}
	}
	
	public void update(long pid)
	{	
		//action id
		String tmp = request.getParameter("actionID");
		if(tmp == null)
		{
			writeResult(3,"参数错误:no action id.");
			return;
		}
		
		int actionID = 0;
		if(tmp.equals("back"))
		{
			//go back to the previous step
		}
		else if(tmp.equals("restart"))
		{
			//re-start the process
		}
		else
		{
			actionID = Convert.parseInt(tmp);
			if(actionID <=0 )
			{
				writeResult(3,"参数错误:wrong action id.");
				return;
			}
		}
		
		Service service = Service.getService(sid);
		if(null == service)
		{
			writeResult(101,"流程不存在.");
			return;
		}
		
		Model model = Model.from(service.getDataTableName());			
        
		try
		{
			Record record = html.update_record(model, request);
			
			db.transaction();
		    record.save();
		    
		    Map<String,Object> inputs = new HashMap<String,Object>();
		    inputs.put("wfName", service.getName());
	    	inputs.put("data", record);
	    	inputs.put("service", service);
		    
		    //fetch process	    	
		    Workflow.getInstance().doAction(pid, actionID ,inputs);
		    db.commit();
		    
		    writeResult(0,null);
		}
		catch(com.tern.dao.ValueException e)
		{
			db.rollback();
			writeResult(2,e.getMessage());
		}
		catch(Throwable t)
		{
			db.rollback();
			Trace.write(Trace.Error, t, "fetch process failed");
			writeResult(3,"服务器异常.");
		}
	}
	
	protected void writeResult(int result,String err)
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
    }
}
