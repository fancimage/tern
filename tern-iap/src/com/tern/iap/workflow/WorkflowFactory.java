/**
 * Tern-iap Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.iap.workflow;

import com.opensymphony.workflow.FactoryException;

import java.io.*;

import java.net.MalformedURLException;
import java.net.URL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opensymphony.workflow.loader.AbstractWorkflowFactory;
import com.opensymphony.workflow.loader.WorkflowDescriptor;
import com.opensymphony.workflow.loader.WorkflowLoader;
import com.tern.db.RowMapper;
import com.tern.db.db;
import com.tern.util.Convert;
import com.tern.util.TernContext;

@SuppressWarnings("serial")
public class WorkflowFactory extends AbstractWorkflowFactory implements Serializable
{
	 private transient Map<String,WorkflowDescriptor> cache = new HashMap<String,WorkflowDescriptor>();

	@Override
	public void setLayout(String workflowName, Object layout) {}

	@Override
	public Object getLayout(String workflowName)
	{	
		return null;
	}

	@Override
	public boolean isModifiable(String name)
	{
		return false;
	}

	@Override
	public String getName()
    {
		return "IAP Workflow";
	}

	@Override
	public WorkflowDescriptor getWorkflow(String name, boolean validate)
			throws FactoryException
	{		
		boolean useCache = !com.tern.util.config.isDebug();//getProperties().getProperty("cache", "true").equals("true");
		
		if (useCache) 
		{
            WorkflowDescriptor descriptor = cache.get(name);

            if (descriptor != null)
            {
                return descriptor;
            }
        }
		
		String path = TernContext.current().getResourcePath()+"/models/process/"+name+".xml";
		java.io.File f = new java.io.File(path);
		if(!f.exists())
		{
			path = com.tern.util.config.getConfigurationPath()+"/models/process/"+name+".oswf.xml";
			f = new java.io.File(path);
			{
				throw new FactoryException(String.format("workflow(%s) file does not exists.",name));
			}
		}
		
		try 
		{
			URL url = new URL("file:///"+ Convert.replaceAll(path,"\\","/") );
            WorkflowDescriptor descriptor = WorkflowLoader.load(url, validate);

            if (useCache)
            {
                cache.put(name, descriptor);
            }

            return descriptor;
        } 
		catch (Exception e)
		{
            throw new FactoryException("Unable to find workflow " + name, e);
        }
	}

	@Override
	public String[] getWorkflowNames() throws FactoryException
	{
		String appName = com.tern.iap.AppContext.current().getApplicationName();
		
		try 
		{
			List<String> list = db.table("wf_service")
			  .select("tid")
			  .where("appname=?" , appName)
			  .query(new RowMapper<String>(){
				@Override
				public String map(ResultSet rs, int rowNum) throws SQLException 
				{
					return rs.getString("tid");
				}				  
			  });
			
			String[] ret = new String[list.size()];
			return list.toArray(ret);
		} 
		catch (SQLException e)
		{
		}
		return null;
	}

	@Override
	public void createWorkflow(String name) 
	{
		
	}

	@Override
	public boolean removeWorkflow(String name) throws FactoryException
	{
		return false;
	}

	@Override
	public void renameWorkflow(String oldName, String newName)
	{
		
	}

	@Override
	public void save() 
	{
		
	}

	@Override
	public boolean saveWorkflow(String name, WorkflowDescriptor descriptor,
			boolean replace) throws FactoryException
	{
		WorkflowDescriptor c = (WorkflowDescriptor) cache.get(name);
        URL url;

        String path = TernContext.current().getResourcePath()+"/models/process/"+name+".xml";
        
        try {
        	url = new URL("file:///"+ Convert.replaceAll(path,"\\","/") );            
        } catch (MalformedURLException ex) {
            throw new FactoryException("workflow '" + name + "' is an invalid url:" + ex);
        }

        boolean useCache = !com.tern.util.config.isDebug();

        if (useCache && (c != null) && !replace) {
            return false;
        }

        if (new File(url.getFile()).exists() && !replace) {
            return false;
        }

        Writer out;

        try {
            out = new OutputStreamWriter(new FileOutputStream(url.getFile() + ".new"), "utf-8");
        } catch (FileNotFoundException ex) {
            throw new FactoryException("Could not create new file to save workflow " + url.getFile());
        } catch (UnsupportedEncodingException ex) {
            throw new FactoryException("utf-8 encoding not supported, contact your JVM vendor!");
        }

        //write it out to a new file, to ensure we don't end up with a messed up file if we're interrupted halfway for some reason
        PrintWriter writer = new PrintWriter(new BufferedWriter(out));
        writer.println(WorkflowDescriptor.XML_HEADER);
        writer.println(WorkflowDescriptor.DOCTYPE_DECL);
        descriptor.writeXML(writer, 0);
        writer.flush();
        writer.close();

        //now lets rename
        File original = new File(url.getFile());
        File backup = new File(url.getFile() + ".bak");
        File updated = new File(url.getFile() + ".new");
        boolean isOK = original.renameTo(backup);

        if (!isOK) {
            throw new FactoryException("Unable to backup original workflow file " + original + " to " + backup + ", aborting save");
        }

        isOK = updated.renameTo(original);

        if (!isOK) {
            throw new FactoryException("Unable to rename new  workflow file " + updated + " to " + original + ", aborting save");
        }

        backup.delete();

        if (useCache) {
            cache.put(name, descriptor);
        }

        return true;
	}

}
