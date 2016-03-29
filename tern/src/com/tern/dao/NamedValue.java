/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.dao;

public class NamedValue 
{
    public String value;
    public String name;
    
    public NamedValue(){}
    
    final public String toString()
    {
    	return name;
    }
    
    final public String getValue()
    {
    	return value;
    }
    
    //final void setValue(String value)
    //{
    //	this.value = value;
    //}
    
    public final String getName()
    {
    	return name;
    }
    
    @Override
    public boolean equals(Object obj)
    {
    	String r = null;
    	if(obj instanceof NamedValue)
    	{
    		 r = ((NamedValue) obj).value;
    	}
    	else if(obj instanceof String)
    	{
    		r = (String)obj;
    	}
    	else if(null == obj && value == null)
    	{
    		return true;
    	}
    	else
    	{
    		return false;
    	}
    	if(value==r) return true;
    	else if(value == null)
    	{
    		return false;
    	}
    	else
    	{
    		return value.equals(r);
    	}
    }
}
