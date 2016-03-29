/**
 * Tern-iap Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.iap.workflow;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.tern.dao.Model;
import com.tern.db.*;
import com.tern.util.Trace;

public class Service 
{	
    private int id;
    private String name;      
    private String caption;    
    private String serviceTable;
    
    /*private String workflowName;
    private String serviceIDName;
    private String statusName;
    private String wfIDName;*/
    
    public int getId(){return id;}
    public String getName(){return name;}
    public String getCaption(){return caption;}
    
    //public String getWorkflowName(){return workflowName;}          
    //public String getDataStatusField(){return statusName;}
    
    private Service(){}      
    
    /*public void updateServiceStatus(int serid,int status) throws Exception
    {
    	updateServiceStatus(serid,status,0);
    }
    
    public void updateServiceStatus(int serid,int status,long wfID) throws Exception
    {
    	if(serviceTable==null || serviceTable.length()<=0
    		|| serviceIDName==null || serviceIDName.length()<=0
    		|| statusName==null || statusName.length()<=0)
    	{
    		Trace.write(Trace.Running, "No Service Table configuration,not to update status,id="+id);
    		return;
    	}
    	
    	StringBuffer buf=new StringBuffer("update ");
    	buf.append(getRealTableName()).append(" set ").append(statusName);
    	buf.append("=").append(status);
    	
    	if(wfID>0 && wfIDName!=null && wfIDName.length()>0)
    	{
    		buf.append(",").append(wfIDName).append("=").append(wfID);
    	}
    	
    	buf.append(" where ").append(serviceIDName);
    	buf.append("=").append(serid);
    	
    	db.sql(buf.toString()).exec();
    }*/
    
    public String getDataTableName()
    {
    	String tname = serviceTable;
		int index = tname.lastIndexOf("/");
		if(index >= 0)
		{
			tname = tname.substring(index+1);
		}
		
		return tname;
    }   
    
    public Model getModel()
    {
    	return Model.from(this.serviceTable);
    }
    
    public com.tern.dao.Record loadData(int serviceID)
    {    
    	com.tern.dao.Model model = IAPWorkflowEntry.model(this.serviceTable);
    	return model.find(serviceID);
    }
    
    public static Service getService(int id)
    {
    	if(com.tern.util.config.isDebug())
    	{    								
    	}
    	
    	try
		{
			return db.table("wf_service").where("tid=?",id)
					 .queryOne(new ServiceMapper());
			
		}
		catch(Throwable e)
		{
			Trace.write(Trace.Error,e, "getService");
			return null;
		}
    }
    
    private static class ServiceMapper implements com.tern.db.RowMapper<Service>
    {
		@Override
		public Service map(ResultSet rs, int rowNum) throws SQLException
		{
			Service s=new Service();
			
			s.id = rs.getInt("tid");
			s.name = rs.getString("tname");						
			
			s.caption = rs.getString("tcaption");
			if(s.caption==null || s.caption.trim().length()<=0)
			{
				s.caption = s.name;
			}
			
			s.serviceTable = rs.getString("serTableName");
			
			/*s.workflowName=rs.getString("wfname");
			if(s.workflowName!=null)
			{
				s.workflowName=s.workflowName.trim();
			}
			if(s.workflowName == null || s.workflowName.length()<=0)
			{
				s.workflowName = s.name;
			}
			s.serviceIDName=rs.getString("serIDName");
			if(s.serviceIDName==null || s.serviceIDName.trim().length()<=0)
			{
				s.serviceIDName = "id";
			}			
			
			s.statusName=rs.getString("statusName");
			if(s.statusName==null || s.statusName.trim().length()<=0)
			{
				s.statusName="wfstate";
			}
			
			s.wfIDName = rs.getString("wfIDName");
			if(s.wfIDName==null || s.wfIDName.trim().length()<=0)
			{
				s.wfIDName="wfid";
			}
			
			com.tern.dao.Model model = IAPWorkflowEntry.model(s.serviceTable);			
			com.tern.dao.Column col = model.column(s.serviceIDName);
			if(s.serviceIDName.equals("id"))
			{
				s.serviceIDName = col.getName();
			}
			
			model.column(s.statusName);
			model.column(s.wfIDName);*/
			
			//IAPWorkflowEntry.model(s.serviceTable);  //test the data model
			
			return s;
		}    	
    }    
    
}
