/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.util;

import org.apache.log4j.AppenderSkeleton;  
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.Level;

public class TraceAppender extends AppenderSkeleton
{
	@Override
	public void close() 
	{
		/*if(Trace._sdf_ == null)  //reload时会报错
		{
			
		}*/
		
		String logmsg = "=>log4j trace-appender closed.";
		try
		{
		    Trace.write(Trace.Running, logmsg);
		}
		catch(Throwable t)
		{
			//t.printStackTrace();
			System.out.println(logmsg);
		}
	}

	@Override
	public boolean requiresLayout() 
	{	
		return false;
	}

	@Override
	protected void append(LoggingEvent arg0) 
	{
		Level level = arg0.getLevel();
		int _tl;// = Trace.Information;
		if(Level.OFF == level)
		{
			return;
		}
		if(Level.ERROR == level)
		{
			_tl = Trace.Error;
		}
		else if(Level.FATAL == level)
		{
			_tl = Trace.Fatal;
		}
		else if(Level.WARN == level)
		{
			_tl = Trace.Warning;
		}		
		else// if(Level.ALL == level)
		{
			_tl = Trace.Information;
		}		
		
		if(arg0.getThrowableInformation()==null || arg0.getThrowableInformation().getThrowable() == null)
		{
			Trace.write(_tl, "(%s) %s",arg0.getLoggerName(), arg0.getMessage());	
		}
		else
		{
			Trace.write(_tl,arg0.getThrowableInformation().getThrowable(),
					"(%s) %s",arg0.getLoggerName(), arg0.getMessage());
		}
	}

}
