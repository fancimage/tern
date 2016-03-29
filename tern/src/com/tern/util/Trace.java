/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Trace
{
    public static final int Information = 4;
    public static final int Warning = 3;
    public static final int Running = 2;
    public static final int Error = 1;
    public static final int Fatal = 0;

    private static int nLevel = Error;
    private static boolean _isForceWriter = true;

    static final SimpleDateFormat _sdf_ = new SimpleDateFormat("yy-MM-dd HH:mm:ss:SSS");
    private final static String levelName[] =new String[]
        {
             "Fatal",
             "Error",
             "  Run",
             " Warn",
             "Infor"
        };
    
    ////日志在内存中的缓冲区
    private static StringBuffer logBuffer = new StringBuffer(10240);
    
    private static final int maxLogLen = 10240;
    private static final int maxFileLen = 5*1024*1024;  //5M
    private static String logFile=null;
    
    //日志文件所在的文件夹和文件短名称
    private static String logForder=null;
    private static String shorterFileName=null;
    
    private static int maxLogFileCount = 20;  //最大日志文件个数
    private static int currentFileIndex=1;
    
    private static java.io.ByteArrayOutputStream  bout;
    private static java.io.PrintStream exceptiobWriter;
    
    static
    {
    	bout=new java.io.ByteArrayOutputStream();
    	exceptiobWriter = new java.io.PrintStream(bout);
    	//_sdf_ = new SimpleDateFormat("yy-MM-dd HH:mm:ss:SSS");
    }

    public static int setTraceLevel(int level)
    {
    	int old = nLevel;
        nLevel = level;
        return old;
    }
    
    public static int getTraceLevel()
    {
    	return nLevel;
    }
    
    public static void flush()
    {
    	//判断文件大小（只保留5M）
    	if(logFile != null)
    	{
    		java.io.FileOutputStream outStream = null;
    		try
            {        	       
                outStream=new java.io.FileOutputStream(logFile,true);
                java.io.PrintStream writer = new java.io.PrintStream(outStream,false,config.getEncoding());
                //outStream.write(logBuffer.toString().getBytes());
                writer.print(logBuffer);
                writer.close();
            }
            catch(Exception e)
            {
            	e.printStackTrace();
            }
            finally
            {
            	if(outStream!=null) 
            	{
            		try{
            		    outStream.close();
            		}catch(Exception e){e.printStackTrace();}
            	}
            }
            
            //清空缓存
            logBuffer.delete(0, logBuffer.length());
            
    		java.io.File file = new java.io.File(logFile);
    		if(file.length() >= maxFileLen)
    		{    			    			
    			//closeLogStream();
    			//将文件另存
    			java.io.File bakfile = new java.io.File(logFile+".old"+currentFileIndex);
    			if(bakfile.exists())
    			{
    				bakfile.delete();
    			}
    			file.renameTo(bakfile);
    			currentFileIndex++;
    			if(currentFileIndex>maxLogFileCount)
    			{
    				currentFileIndex=1;
    			}
    			//reOpenLog();
    		}
    	}
    	else
    	{
    		System.out.print(logBuffer);
    		logBuffer.delete(0, logBuffer.length());
    	}
    }
    
    public static boolean isForceWriter()
    {
    	return _isForceWriter;
    }
    
    public static void setIsForceWriter(boolean v)
    {
    	_isForceWriter = v;
    }

    public static void setTraceFile(String fileName,boolean isForce)
    {
    	_isForceWriter = isForce;
    	logFile = fileName;
    	
    	//maxLogFileCount
    	try
    	{
    	    maxLogFileCount = config.getInt("log.maxFileCount", 20);
    	    if(maxLogFileCount<=0)
    	    {
    	    	maxLogFileCount=20;
    	    }
    	    
    	    if(maxLogFileCount>100)
    	    {
    	    	maxLogFileCount=100;
    	    }
    	}
    	catch(Exception e)
    	{
    		maxLogFileCount=20;
    	}
    	
    	//currentFileIndex
    	if(logFile != null)
    	{
    		int i = logFile.lastIndexOf(java.io.File.separator);
    		if(i>=0)
    		{
    			logForder = logFile.substring(0,i);
    			shorterFileName = logFile.substring(i+1);
    			
    			java.io.File dir = new java.io.File(logForder);
    			if(dir.isDirectory())
    			{
    				java.io.File[] files = dir.listFiles();
    				//找到最后一个备份文件
    				long maxTime = 0;
    				for(i=0;i<files.length;i++)
    				{
    					String name = files[i].getName();
    					if(files[i].isFile() && name.startsWith(shorterFileName)
    						&& files[i].lastModified()>maxTime)
    					{    						
    						int index = name.indexOf(".old");
    						if(index<0)
    						{
    							continue;
    						}
    						index+=4;
    						currentFileIndex = Convert.parseInt(name.substring(index));
    						maxTime = files[i].lastModified();    						
    					}
    				}
    			}
    		}
    		else
    		{
    			shorterFileName = logFile;
    			currentFileIndex=1;
    		}
    	}
    	
    	if(currentFileIndex>maxLogFileCount)
    	{
    		currentFileIndex=1;
    	}
    	
    	//reOpenLog();
    	synchronized(logBuffer)
    	{
    		/*if(exceptiobWriter==null)
    		{
        		exceptiobWriter = new java.io.PrintStream(new java.io.ByteArrayOutputStream());
    		}*/
    		
    		flush();
    	}    	
    }

    @Deprecated
    public static void out(int level,Object param)
    {
    	if (level<=nLevel && level <= Information)
        {
        	_out(level,(param==null?"":param.toString()),null);
        }
    }
    
    public static boolean needTrace(int level)
    {
    	if (level<=nLevel && level <= Information) return true;
    	else return false;
    }
    
    public static void write(int level,String format,Object...params)
    {
    	if (level<=nLevel && level <= Information)
    	{
    		if(params==null || params.length<=0)
    		{
    			_out(level,format,null);
    		}
    		else
    		{
    		    _out(level,String.format(format, params),null);
    		}    		
    	}
    }
    
    public static void write(int level,Throwable t,String format,Object...params)
    {
    	if (level<=nLevel && level <= Information)
    	{
    		if(params==null || params.length<=0)
    		{
    			_out(level,format,t);
    		}
    		else
    		{
    			_out(level,String.format(format, params),null);
    		}
    	}
    }
    
    @Deprecated
    public static void out(int level,Object param,Throwable t)
    {
        if (level<=nLevel && level <= Information)
        {
        	_out(level,(param==null?"":param.toString()),t);
        }
    }
    
    public static void wrap()
    {
    	synchronized(logBuffer)
    	{
    		logBuffer.append("\r\n");
    	}
    }
    
    private static void _out(int level,String param,Throwable t)
    {
    	StringBuffer buf=new StringBuffer();
    	//buf.append("[tern][");
    	buf.append(_sdf_.format(new Date())).append(" [");
    	//buf.append("][");
    	buf.append(levelName[level]);
    	buf.append("] ");
    	buf.append(param).append("\r\n");
    	
    	//writer.println(buf.toString());
    	synchronized(logBuffer)
    	{
    		logBuffer.append(buf); 
    		
    		if(t!=null)
            {
                t.printStackTrace(exceptiobWriter);            		
        		logBuffer.append(bout.toString());
        		bout.reset();
            } 
            
            if(_isForceWriter || maxLogLen<logBuffer.length())
            {
            	flush();
            }
    	}
    }
    
}