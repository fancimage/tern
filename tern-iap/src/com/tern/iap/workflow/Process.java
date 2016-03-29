/**
 * Tern-iap Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.iap.workflow;

import com.tern.dao.Record;
import com.tern.db.DataRow;
import com.tern.db.db;
import com.tern.util.Trace;

public class Process 
{
	private long wfID;
    private int typeid=0;
    private int id=0;
    private int creatorID=0;
    private String taskname;
    
    private boolean flag1=false;
    private boolean flag2=false;
    
    private Record data;
    
    Process(Record data)
    {
    	wfID=id;
    }
    
    Process(Record data,int type,int creatorID) //when create process
    {
    	wfID=0;
    	this.data = data;
    	typeid=type; 
    	this.creatorID = creatorID;
    	
    	flag1=true;
    }
    
    public final long getWorkflowID(){return wfID;}
    
    public final int getId()
    {
    	return id;
    }
    
    public final String getName()
    {        
    	Service service = Service.getService(typeid);
    	return service.getName();
    }
    
    public final String getCaption()
    {        
    	Service service = Service.getService(typeid);
    	return service.getCaption();
    }
    
    public final String getTaskName()
    {    	
    	return taskname;
    }
    
    public int getTypeID()
    {    	
    	return typeid;
    }
    
    public final Service getService()
    {        
    	return Service.getService(typeid);
    }
    
    public final int getCreator()
    {
    	return this.creatorID;
    }              
    
    public Record getData()
    {
    	return data;
    }
}
