/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package controllers;

import com.tern.dao.Model;
import com.tern.dao.Record;
import com.tern.dao.RecordSet;
import com.tern.util.Convert;
import com.tern.util.Trace;
import com.tern.util.config;
import com.tern.util.html;
import com.tern.web.Controller;
import com.tern.web.HttpStream;
import com.tern.web.Route;

@Route("/service/$appName/*")
public class ServiceController extends Controller
{
	private Model model = Model.from("service");
	private String appName;  //application name
	
	public void index()
    {
		RecordSet records = model.query("appname=?",appName);
		request.setAttribute("model", model);
		request.setAttribute("records", records);
    }
	
	public String _new()
	{		
		Record record = model.create(); //new
		request.setAttribute("model", model);
		request.setAttribute("record", record);
		
		record.set("appname", appName);
		
		return "service/edit";
	}
	
	public String edit(int id)
	{		
		Record record = model.find(id); //retrive		
		request.setAttribute("model", model);
		request.setAttribute("record", record);
		
		return "service/edit";
	}
	
	public String show(int id)
	{
		Record record = model.find(id); //retrive
		request.setAttribute("model", model);
		request.setAttribute("record", record);
		
		return "service/design";
	}
	
	public void create()
	{
		try
		{
		    Record record = html.new_record(model, request);
		    record.set("appname", appName);
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
	
	//更新
	public void update(int id)
	{
		try
		{
		    Record record = html.update_record(model, request);
		    //record.set("appname", appName);
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
		
		//redirect(String.format("%spet", config.getRoot()));  //to index page
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
