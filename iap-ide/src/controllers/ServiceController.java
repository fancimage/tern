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
import com.tern.web.Route;

@Route("/service/$appName/*")
public class ServiceController extends DataResourceController
{
	public ServiceController()
	{
		this.modelName = "service";
	}
	
	public String show(int id)
	{
		model = getModel();
		
		Record record = model.find(id); //retrive
		request.setAttribute("model", model);
		request.setAttribute("record", record);
		
		return "service/design";
	}
}
