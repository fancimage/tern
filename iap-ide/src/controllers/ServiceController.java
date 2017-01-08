/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package controllers;

import com.opensymphony.workflow.loader.DTDEntityResolver;
import com.tern.dao.Record;
import com.tern.iap.AppContext;
import com.tern.iap.util.ActionResult;
import com.tern.util.Trace;
import com.tern.util.config;
import com.tern.web.ControllerException;
import com.tern.web.HttpStream;
import com.tern.web.Route;
import com.tern.web.routes.HttpMethod;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;

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
			path = config.getConfigurationPath()+"/models/process.template.xml";
			xmlFile = new File(path);
		}
		if(!xmlFile.exists())
		{
			//返回空文档
			stream.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
			stream.append("<!DOCTYPE workflow PUBLIC \"-//OpenSymphony Group//DTD OSWorkflow 2.6//EN\" \"http://www.opensymphony.com/osworkflow/workflow_2_8.dtd\"[]>\n");
			stream.append("<workflow>\n<initial-actions>\n<action id=\"0\" name=\"开始\">\n<results><unconditional-result old-status=\"Finished\" status=\"doing\" step=\"1\" /></results>\n</action>\n</initial-actions>\n");
			stream.append("<steps>\n<step id=\"1\" name=\"结束\">\n</step>\n</steps>\n");
			stream.append("</workflow>");
		}
		else
		{
			flushFile(xmlFile);
		}
	}

	private boolean writeBodyToFile(String filename)
	{
		String xmlFile = filename+".xml";
		String jsonFile = filename+".shape.json";
		try
		{
			BufferedReader br = request.getReader();

			boolean hasJson = false;
			StringBuffer buf = new StringBuffer();
			//Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(xmlFile),"utf-8"));

			String inputLine;
			while ((inputLine = br.readLine()) != null)
			{
				if(inputLine.equals("===boundary==="))
				{
					/*其后是shapes的定义*/
					//writer.close();
					//writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(jsonFile),"utf-8"));

					SAXReader reader = new SAXReader();
					reader.setEntityResolver(new DTDEntityResolver());
					Document document = reader.read(new StringReader(buf.toString()));
					OutputFormat format = new OutputFormat(" ", true);
					XMLWriter writer = new XMLWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(xmlFile),"utf-8")), format);
					writer.write(document);
					writer.flush();
					writer.close();

					hasJson = true;
					buf = new StringBuffer();
					continue;
				}

				buf.append(inputLine).append("\n");

				//writer.write(inputLine);
				//writer.write("\n");
			}
			//writer.close();
			br.close();

			if(!hasJson)
			{
				Trace.write(Trace.Error,"write osworkflow: no json-shape define!");
				return false;
			}

			String jsonString = buf.toString();
			jsonString = formatJsonStrings(jsonString);
			if(jsonString==null)
			{
				Trace.write(Trace.Error,"write osworkflow[json]: "+buf.toString());
				return false;
			}

			Writer jsonWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(jsonFile),"utf-8"));
			jsonWriter.write(jsonString);
			jsonWriter.close();

			return true;
		}
		catch(DocumentException de)
		{
			Trace.write(Trace.Error,de,"write osworkflow[xml].");
		}
		catch (IOException e)
		{
			Trace.write(Trace.Error,e,"write osworkflow.");
		}

		return false;
	}

	@Route(value="/%1/define/update",method= HttpMethod.POST)
	public void defineSave(int id)
	{
		model = getModel();
		Record record = model.find(id); //retrive
		if(record == null)
		{
			throw new ControllerException(this,"工作流不存在");
		}

		ActionResult r = new ActionResult();
		this.setViewObject(r);

		AppContext ctx = AppContext.getAppContext(appName);
		String path = ctx.getResourcePath() + "/models/process/"+record.getString("tname");

		if(!writeBodyToFile(path))
		{
			r.setResult(2,"save failed");
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

	private static String formatJsonStrings(String json)
	{
		StringBuffer result = new StringBuffer();

		int length = json.length();
		int number = 0;
		char key = 0;
		boolean inString = false;

		//遍历输入字符串。
		for (int i = 0; i < length; i++)
		{
			//1、获取当前字符。
			key = json.charAt(i);

			if(key == '"')
			{
				inString = !inString;
				result.append(key);
				continue;
			}

			if(inString)
			{
				result.append(key);
				continue;
			}

			//2、如果当前字符是前方括号、前花括号做如下处理：
			if((key == '[') || (key == '{') )
			{
				//（1）如果前面还有字符，并且字符为“：”，打印：换行和缩进字符字符串。
				if((i - 1 > 0) && (json.charAt(i - 1) == ':'))
				{
					result.append('\n');
					result.append(indent(number));
				}

				//（2）打印：当前字符。
				result.append(key);

				//（3）前方括号、前花括号，的后面必须换行。打印：换行。
				result.append('\n');

				//（4）每出现一次前方括号、前花括号；缩进次数增加一次。打印：新行缩进。
				number++;
				result.append(indent(number));

				//（5）进行下一次循环。
				continue;
			}

			//3、如果当前字符是后方括号、后花括号做如下处理：
			if((key == ']') || (key == '}') )
			{
				//（1）后方括号、后花括号，的前面必须换行。打印：换行。
				result.append('\n');

				//（2）每出现一次后方括号、后花括号；缩进次数减少一次。打印：缩进。
				number--;
				result.append(indent(number));

				//（3）打印：当前字符。
				result.append(key);

				//（4）如果当前字符后面还有字符，并且字符不为“，”，打印：换行。
				if(((i + 1) < length) && (json.charAt(i + 1) != ','))
				{
					result.append('\n');
				}

				//（5）继续下一次循环。
				continue;
			}

			//4、如果当前字符是逗号。逗号后面换行，并缩进，不改变缩进次数。
			if((key == ','))
			{
				result.append(key);
				result.append('\n');
				result.append(indent(number));
				continue;
			}

			//5、打印：当前字符。
			result.append(key);
		}

		if(inString)
		{
			return null;
		}

		return result.toString();
	}


	private static String indent(int number)
	{
		StringBuffer result = new StringBuffer();
		for(int i = 0; i < number; i++)
		{
			result.append("   ");
		}
		return result.toString();
	}
}
