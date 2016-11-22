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
import com.tern.iap.AppContext;
import com.tern.iap.util.DataController;
import com.tern.web.ControllerException;
import com.tern.web.Route;

@Route("/data/$appName/$modelName/*")
public class DataResourceController extends DataController
{	
	protected String appName;
	
	@Override
	protected Model getModel()
	{
		AppContext ctx = AppContext.getAppContext(appName);
		if(ctx == null || ctx.getMetaDB() == null)
		{
			throw new ControllerException(this,"app:"+appName+"不存在或未配置元数据库。");
		}
		
		this.request.setAttribute("appName", appName);
		return Model.from(modelName , ctx.getMetaDB());		
	}
}
