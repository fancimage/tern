/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.web;

import org.apache.commons.fileupload.FileItem;

/**
 * <p>Title: 文件数据表示</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2008</p>
 * @author fancimage
 * @version 1.0
 */
public class FileData
{
    String fileName = null;
    String type = null;
    byte[] data = null;
    FileItem item=null;

    FileData(String filename, String type, byte[] data)
    {
        fileName = filename;
        this.type = type;
        this.data = data;
    }
    
    FileData(FileItem item)
    {
    	this.fileName = item.getName();
    	this.type = item.getContentType();
    	this.data = item.get();
    	
    	if(fileName != null)
    	{
    		int i = fileName.lastIndexOf("/");
    		if(i>0)
    		{
    			fileName = fileName.substring(i+1);
    		}
    		else
    		{
    			i = fileName.lastIndexOf("\\");
    			if(i>0)
        		{
        			fileName = fileName.substring(i+1);
        		}
    		}
    	}
    	
    	this.item = item;
    }

    public FileData(String type, byte[] data)
    {
        this.type = type;
        this.data = data;
    }

    public String getFilename()
    {
        return fileName;
    }

    public String getContentType()
    {
        return type;
    }

    public byte[] getFileData()
    {
        return data;
    }

    public void setFilename(String filename)
    {
        this.fileName = filename;
    }

    public String toString()
    {
        return this.type;
    }
    
    public FileItem getFileItem(){return item;}

}
