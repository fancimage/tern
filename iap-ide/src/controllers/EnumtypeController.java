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
import com.tern.iap.util.ActionResult;
import com.tern.web.Route;

@Route("/enumtype/$appName/*")
public class EnumtypeController extends DataResourceController
{
	public EnumtypeController()
	{
		this.modelName = "enumtype";
	}
	
	public void delete()
	{
		ActionResult ar = new ActionResult();
		this.setViewObject(ar);
		
		String ids = request.getParameter("items");
		if(ids==null || ids.length()<=0)
		{
			ar.setResult(1,null);
			return;
		}
		
		String[] arr = ids.split(",");
	    if(arr.length<=0)
	    {
	    	ar.setResult(1,null);
	    	return;
	    }
	    
	    StringBuffer buf = new StringBuffer("etype in (");
	    for(int i=0;i<arr.length;i++)
	    {
	    	if(i > 0) buf.append(",");
	    	buf.append("'").append(arr[i]).append("'");
	    }
	    buf.append(")");
	    
	    model = this.getModel();
	    model.delete( buf.toString() );
	}
	
	public void update(String id)
	{
		super.update(0);
	}
	
	public String edit(String id)
	{
		model = this.getModel();
		
		Record record = model.query("etype=?",id).get(0);
		request.setAttribute("model", model);
		request.setAttribute("record", record);
		
		return "enumtype/edit";
	}
}
