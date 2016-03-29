/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.util;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;

public final class DateTimeUtil
{
    private DateTimeUtil()
    {}

    public static String now()
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dt = sdf.format(new Date());
        return dt;
    }

    public static String getNowYMD(){
     String str = now();
     return str.substring(0,10);
    }

    public static String now(String pattern)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        String dt = sdf.format(new Date());
        return dt;
    }

    public static String toString(Date date, String pattern)
    {
        if (date == null)
        {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }

    public static Date toDate(String param)
    {
        Date date = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try
        {
            date = sdf.parse(param);
        }
        catch (ParseException ex)
        {
        }
        return date;
    }

    public static Date toDate(String param, String pattern)
    {
    	if(param==null) return null;
        Date date = null;
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        try
        {
            date = sdf.parse(param);
        }
        catch (ParseException ex)
        {
        }
        return date;
    }
    
    public static Date getStartOfMonth(Date date)
    {
    	java.util.Calendar cal   =  java.util.Calendar.getInstance();  
    	cal.setTime(date);
    	cal.set(java.util.Calendar.DATE,1);
    	return cal.getTime();
    }
    
    public static Date getEndOfMonth(Date date)
    {
    	java.util.Calendar cal   =  java.util.Calendar.getInstance();  
    	cal.setTime(date);
    	cal.add(java.util.Calendar.MONTH,1);
    	cal.add(java.util.Calendar.DATE, -1);//����һ�²���ȥһ��
    	return cal.getTime();
    }
   
    public static int getMonthofNow()
    {
    	
    	return java.util.Calendar.MONTH;
    }
    
    public static int getYearofNow()
    {
    	
    	return java.util.Calendar.YEAR;
    }
}
