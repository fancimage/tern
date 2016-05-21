/**
 * Tern-iap Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.iap.workflow;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

import com.opensymphony.workflow.spi.SimpleWorkflowEntry;
import com.opensymphony.workflow.spi.WorkflowEntry;
import com.tern.util.Trace;
import com.tern.dao.Record;
import com.tern.dao.RecordState;
import com.tern.db.db;
import com.tern.db.Database;
import com.tern.iap.AppContext;
import com.tern.iap.Operator;

@SuppressWarnings("serial")
public class IAPWorkflowEntry extends SimpleWorkflowEntry
{
	//private Process process;
	private Record record;
	private Operator user;
	private Service service;
	private Record  process;
	
	IAPWorkflowEntry(Record process,Map inputs)
	{
		super(process.getId(),null,process.getInt("status"));
		this.process = process;
		if(inputs!=null) assign(inputs);
	}
	
	IAPWorkflowEntry(Record process,String name,Map inputs)  //new
	{
		super(0,name,process.getInt("status"));
		this.process = process;
		if(inputs!=null) assign(inputs);
	}
	
	/*IAPWorkflowEntry(WorkflowEntry entry,Map<String,Object> vars) 
	{
		super(entry.getId(), entry.getWorkflowName(), entry.getState());			
	}*/
	
	void assign(Map vars)
	{
		record = (Record)vars.get("data");
		user = (Operator)vars.get("user");
		service = (Service)vars.get("service");
		
		//vars.remove("data");
		//vars.remove("user");
		//vars.remove("service");
		
		if(process.getState() == RecordState.New)
		{
			process.set("tid", service.getId());
			if(record.getModel().column("pid") != null)
			{
				process.set("pid", record.getInt("pid"));
			}
			
			process.set("creator", user.getId());
			process.set("createtime", new Timestamp(new Date().getTime()));
			process.set("wfcaption", service.getCaption()+":"+record.toString());
			process.set("serid", record.getId());
			
			process.save();
			
			record.set("wfid", process.getId() );
			record.set("wfstatus", WorkflowEntry.ACTIVATED ); //???
		}

		this.id = process.getId();
		this.workflowName = service.getName();
	}	
	
	/*IAPWorkflowEntry(WorkflowEntry entry,Record s,Operator u) 
	{
		super(entry.getId(), entry.getWorkflowName(), entry.getState());
		this.record = s;
		this.user = u;
	}*/
	
	public String getWorkflowName()
	{
		if(null == this.workflowName)
		{
			this.workflowName = this.getService().getName();
		}
		return this.workflowName;
	}
	
	public Operator getUser()
	{
		if(user == null)
		{
			user = Operator.current();//AppContext.getCurrentOperator();
		}
		return user;
	}
	
	public Record getData()
	{
		return record;
	}
	
	public Record getProcess()
	{
		return process;
	}
	
	//public int getProcessID(){return process.getTypeID();}
	public int getCreator(){return process.getInt("creator");}
	
	public String getName(){return service.getName();}
	
	public String getTitle()
	{
		return process.getString("taskName");
	}
	
	public Service getService()
	{
		if(null == service)
		{
			int sid = process.getInt("tid");
			service = Service.getService(sid);
		}
		return service;
	}	
	
	public static void execsql(String sql)
    {    	
    	try
   		{
   			db.sql(sql).exec();
   		}
   		catch(Exception e)
   		{
   			Trace.write(Trace.Error,e, "Workflow SQLCommand:");
   			throw new _ContextException(e);
   		}
    }
	
	public static Database db(){return Database.defaultDB();}
    
    public static com.tern.dao.ITable query(String sql)
    {
    	try
    	{
    	    return db.sql(sql).query();
    	}
    	catch(java.sql.SQLException e)
    	{
    		Trace.write(Trace.Error,e, "Workflow Query:");
    		throw new _ContextException(e);
    	}
    }
    
    public static com.tern.dao.Model model(String name)
    {
    	return com.tern.dao.Model.from(name);
    }
    
    @SuppressWarnings("serial")
	static class _ContextException extends RuntimeException
    {
		public _ContextException(Exception e)
    	{
    		super(e.getMessage());
    	}
    }
    
}
