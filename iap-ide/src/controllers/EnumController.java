/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package controllers;

import com.tern.dao.Record;
import com.tern.dao.RecordSet;
import com.tern.util.Trace;
import com.tern.util.html;
import com.tern.web.Route;

@Route("/enum/$appName/$etype/*")
public class EnumController extends DataResourceController
{
	private String etype;
	
	public EnumController()
	{
		this.modelName = "enum";
	}
	
	@Override
	public String index()
	{
		model = this.getModel();
		RecordSet records = model.query("A.etype=?" , etype).joinAll();
		
		request.setAttribute("model", model);
		request.setAttribute("records", records);
		
		return "enum/index";
	}
	
	public void create()
	{
		model = this.getModel();
		
		try
		{
		    Record record = html.new_record(model, request);
		    record.set("etype", etype);
		    record.save();
		    
		    writeResult(0,null);
		}
		catch(com.tern.dao.ValueException e)
		{
			writeResult(2,e.getMessage());
		}
		catch(Throwable t)
		{
			Trace.write(Trace.Error, t, "create record(enum) failed.");
			writeResult(3,"服务器异常.");
		}
	}
	
	public void update(int id)
	{
		model = this.getModel();
		
		try
		{
		    Record record = html.update_record(model, request);
		    record.set("etype", etype);
		    record.save();
		    
		    writeResult(0,null);
		}
		catch(com.tern.dao.ValueException e)
		{
			writeResult(2,e.getMessage());
		}
		catch(Throwable t)
		{
			writeResult(3,"服务器异常.");
		}
	}
	
	public String _new()
	{		
		model = this.getModel();
		
		Record record = model.create(); //new	
		record.set("etype", etype);
		
		request.setAttribute("model", model);
		request.setAttribute("record", record);
		
		return "enum/edit";
	}
}
