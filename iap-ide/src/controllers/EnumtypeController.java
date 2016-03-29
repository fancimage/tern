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

public class EnumtypeController extends Controller
{
	private Model model = Model.from("enumtype");
	
	public void index()
	{
		RecordSet records = model.query();
		
		request.setAttribute("model", model);
		request.setAttribute("records", records);
	}
	
	public void delete()
	{
		String ids = request.getParameter("items");
		if(ids==null || ids.length()<=0)
		{
			writeResult(1,null);
			return;
		}
		
		String[] arr = ids.split(",");
	    if(arr.length<=0)
	    {
	    	writeResult(1,null);
	    	return;
	    }
	    
	    StringBuffer buf = new StringBuffer("etype in (");
	    for(int i=0;i<arr.length;i++)
	    {
	    	if(i > 0) buf.append(",");
	    	buf.append("'").append(arr[i]).append("'");
	    }
	    buf.append(")");
	    
	    model.delete( buf.toString() );
	    writeResult(0,null);
	}
	
	public void create()
	{
		try
		{
		    Record record = html.new_record(model, request);
		    record.save();
		    
		    writeResult(0,null);
		}
		catch(com.tern.dao.ValueException e)
		{
			writeResult(2,e.getMessage());
		}
		catch(Throwable t)
		{
			Trace.write(Trace.Error, t, "create record(%s) failed.");
			writeResult(3,"服务器异常.");
		}
	}
	
	public void update(String id)
	{
		try
		{
		    Record record = html.update_record(model, request);
		    record.save();
		    
		    writeResult(0,null);
		}
		catch(com.tern.dao.ValueException e)
		{
			writeResult(2,e.getMessage());
		}
		catch(Throwable t)
		{
			Trace.write(Trace.Error, t, "save enumtype");
			writeResult(3,"服务器异常.");
		}
	}
	
	public String _new()
	{		
		Record record = model.create(); //new
		request.setAttribute("model", model);
		request.setAttribute("record", record);
		
		return "model/edit";
	}
	
	public String edit(String id)
	{
		Record record = model.query("etype=?",id).get(0);
		request.setAttribute("model", model);
		request.setAttribute("record", record);
		
		return "model/edit";
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
