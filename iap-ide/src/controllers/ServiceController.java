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
import com.tern.iap.AppContext;
import com.tern.util.Trace;
import com.tern.web.ControllerException;
import com.tern.web.HttpStream;
import com.tern.web.Route;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

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

	private void flushFile(File file)
	{
		HttpStream stream = this.getStream();

		try
		{
			InputStream inStream = new FileInputStream(file);
			byte[] buf = new byte[4096];
			int readLength;
			while (((readLength = inStream.read(buf)) != -1))
			{
				stream.append(buf,0,readLength);
			}
			inStream.close();
		}
		catch(Exception e)
		{
			Trace.write(Trace.Error,e,"read osworkflow definition.");
		}
	}

	@Route("/%1/define")
	public void define(int id)
	{
		model = getModel();
		Record record = model.find(id); //retrive
		if(record == null)
		{
			throw new ControllerException(this,"工作流不存在");
		}

		response.setContentType("text/xml");
		response.setCharacterEncoding("utf-8");
		response.reset();

		HttpStream stream = this.getStream();

        /*获取工作流的XML定义*/
		AppContext ctx = AppContext.getAppContext(appName);
		String path = ctx.getResourcePath() + "/models/process/"+record.getString("tname")+".xml";
		File xmlFile = new File(path);
		if(!xmlFile.exists())
		{
			stream.append("<?xml version=\"1.0\" encoding=\"utf-8\"?><workflow></workflow>"); //返回空文档
		}
		else
		{
			flushFile(xmlFile);
		}
	}

	@Route("/%1/shape")
	public void shape(int id)
	{
		/*下载工作流的图元定义*/
		model = getModel();
		Record record = model.find(id); //retrive
		if(record == null)
		{
			throw new ControllerException(this,"工作流不存在");
		}

		response.setContentType("application/json");
		response.setCharacterEncoding("utf-8");
		response.reset();

		HttpStream stream = this.getStream();

        /*获取工作流的XML定义*/
		AppContext ctx = AppContext.getAppContext(appName);
		String path = ctx.getResourcePath() + "/models/process/"+record.getString("tname")+".shape.json";
		File file = new File(path);
		if(!file.exists())
		{
			stream.append("{}"); //返回空文档
		}
		else
		{
			flushFile(file);
		}
	}
}
