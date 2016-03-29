/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.web;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;

import com.tern.util.Trace;

/**
 * <p>Title: 支持MultiPart类型的Request</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2010</p>
 * @author Fancimage
 * @version 1.0
 */
public class MultiPartEnabledRequest extends HttpServletRequestWrapper
{
	private static int MaxFileSize; //一个上传文件的最大大小
	
	private boolean multipart;

	private Map fileItems = new HashMap();
	private Map httpParams = new HashMap();
	
	static
    {
        int size = com.tern.util.config.getInt("server.maxUploadSize", 3*1024); //默认3M＝3*1024        
        MaxFileSize = 1024 * size;
    }
	
	public MultiPartEnabledRequest(HttpServletRequest req) 
	{
	    super(req);
	    this.multipart = FileUpload.isMultipartContent(req);
	    if (multipart) 
	    {
	      try 
	      {
	        readHttpParams(req);
	      }
	      catch (FileUploadException e) 
	      {
	    	  Trace.write(Trace.Error,e, "MultiPartEnabledRequest");
	          e.printStackTrace();
	      }
	    }
	  }

	  private void readHttpParams(HttpServletRequest req) throws FileUploadException 
	  {
	    List all = uploadFiles(req);

	    // read form fields
	    for (Iterator it = all.iterator(); it.hasNext();) {
	      FileItem item = (FileItem) it.next();

	      if (item.isFormField()) {
	        List valList = valueList(httpParams, item.getFieldName());
	        if (req.getCharacterEncoding() != null) {
	          try
	          {
	            valList.add(item.getString(req.getCharacterEncoding()));
	          }
	          catch (UnsupportedEncodingException e)
	          {
	            Trace.write(Trace.Error, e,"");
	            valList.add(item.getString(/*encoding?*/));
	          }
	        }
	        else
	          valList.add(item.getString(/*encoding?*/));
	      } else {
	        List valList = valueList(fileItems, item.getFieldName());
	        valList.add(item);
	      }
	    }

	    // convert lists of values to arrays
	    for (Iterator it = httpParams.keySet().iterator(); it.hasNext();) {
	      String name = (String) it.next();
	      List valList = (List) httpParams.get(name);
	      httpParams.put(name, toStringArray(valList));
	    }

	    for (Iterator it = fileItems.keySet().iterator(); it.hasNext();) {
	      String name = (String) it.next();
	      List valList = (List) fileItems.get(name);
	      fileItems.put(name, toFileItemArray(valList));
	    }
	  }

	  private List valueList(Map params, String name) 
	  {
	    List valList = (List) params.get(name);
	    if (valList == null) {
	      valList = new ArrayList();
	      params.put(name, valList);
	    }
	    return valList;
	  }

	  private String[] toStringArray(List valList) 
	  {
	    String[] vals = new String[valList.size()];
	    for (int i = 0; i < vals.length; i++)
	      vals[i] = (String) valList.get(i);
	    return vals;
	  }

	  private FileItem[] toFileItemArray(List valList) 
	  {
	    FileItem[] vals = new FileItem[valList.size()];
	    for (int i = 0; i < vals.length; i++)
	      vals[i] = (FileItem) valList.get(i);
	    return vals;
	  }

	  private List uploadFiles(HttpServletRequest req) throws FileUploadException 
	  {
	    DiskFileUpload upload = new DiskFileUpload();

	    /*try 
	    {
	      upload.setSizeThreshold(res.getInteger("file.upload.size.threshold"));
	    } 
	    catch (MissingResourceException e)
	    {
	      // use defaults
	    }*/

	    try 
	    {
	      upload.setSizeMax(MaxFileSize);
	    } 
	    catch (MissingResourceException e) 
	    {
	      // use defaults
	    }

	    /*try 
	    {
	      upload.setRepositoryPath(res.getString("file.upload.repository"));
	    } 
	    catch (MissingResourceException e) 
	    {
	      // use defaults
	    }*/

	    List all = new DiskFileUpload().parseRequest(req);
	    return all;
	  }

	  public boolean isMultipart() 
	  {
	    return multipart;
	  }

	  public String getParameter(String name) 
	  {
	    if (!isMultipart())
	      return super.getParameter(name);

	    String[] vals = (String[]) httpParams.get(name);
	    if (vals == null)
	    {	    	
	    	return super.getParameter(name);
	    }

	    return vals[0];
	  }

	  public FileData getFileParameter(String name) 
	  {
	    FileItem[] vals = (FileItem[]) fileItems.get(name);
	    if (vals == null)
	      return null;

	    return new FileData(vals[0]);
	  }

	  public Map getParameterMap() 
	  {
	    if (!isMultipart())
	    {
	      return super.getParameterMap();
	    }
	    
	    return new HashMap(httpParams);
	  }

	  public Map getFileParameterMap() 
	  {
	    return new HashMap(fileItems);
	  }

	  public Enumeration getParameterNames() 
	  {
	    if (!isMultipart())
	    {
	      return super.getParameterNames();
	    }
	    
	    return new Vector(httpParams.keySet()).elements();
	  }

	  public Enumeration getFileParameterNames() 
	  {
	    return new Vector(fileItems.keySet()).elements();
	  }

	  public String[] getParameterValues(String name) 
	  {
	    if (!isMultipart())
	    {
	      return super.getParameterValues(name);
	    }
	    
	    return (String[]) httpParams.get(name);
	  }

	  public FileItem[] getFileParameterValues(String name) 
	  {
	    return (FileItem[]) fileItems.get(name);
	  }
}
