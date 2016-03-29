/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.util;

public class ExprValue 
{
	//支持的数据类型
    public static final int INT=1;
    public static final int FLOAT=2;
    public static final int STRING=3;
    
    private _VALUE value;
    
    public ExprValue(int v)
    {
    	value = new _INTValue(v);
    }
    
    public ExprValue(double v)
    {
    	value = new _FLOATValue(v);
    }
    
    public ExprValue(float v)
    {
    	value = new _FLOATValue(v);
    }
    
    public ExprValue(String v)
    {
    	value = new _STRINGValue(v);
    }
    
    public void setValue(int v)
    {
    	if(value instanceof _INTValue)
    	{
    		((_INTValue) value).v = v;
    		return;
    	}
    	
    	value = new _INTValue(v);
    }
    
    public void setValue(double v)
    {
    	if(value instanceof _FLOATValue)
    	{
    		((_FLOATValue) value).v = v;
    		return;
    	}
    	
    	value = new _FLOATValue(v);
    }
    
    public void setValue(float v)
    {
    	setValue((double)v);
    }
    
    public void setValue(String v)
    {
    	if(value instanceof _STRINGValue)
    	{
    		((_STRINGValue) value).v = v;
    		return;
    	}
    	
    	value = new _STRINGValue(v);
    }
    
    public int getInt()
    {
    	if(value instanceof _INTValue)
    	{
    		return ((_INTValue)value).v;
    	}
    	
    	return 0;
    }
    
    public double getDouble()
    {
    	if(value instanceof _FLOATValue)
    	{
    		return ((_FLOATValue)value).v;
    	}
    	else if(value instanceof _INTValue)
    	{
    		return ((_INTValue)value).v;
    	}
    	
    	return 0;
    }
    
    public String getString()
    {
    	if(value instanceof _STRINGValue)
    	{
    		return ((_STRINGValue)value).v;
    	}
    	else if(value!=null) return value.toString();
    	
    	return null;
    }
    
    public String toString()
    {
    	return getString();
    }
    
    public final int getType()
    {
    	return value.getType();
    }
    
    private static abstract class _VALUE
    {
    	public abstract int getType();
    }
    
    private static class _INTValue extends _VALUE
    {
    	int v;
    	public _INTValue(int v)
    	{
    		this.v=v;
    	}
    	
    	public int getType()
    	{
    		return INT;
    	}
    	
    	public String toString(){return String.valueOf(v);}
    }
    
    private static class _FLOATValue extends _VALUE
    {
    	double v;
    	public _FLOATValue(double v)
    	{
    		this.v=v;
    	}
    	
    	public int getType()
    	{
    		return FLOAT;
    	}
    	
    	public String toString(){return String.valueOf(v);}
    }
    
    private static class _STRINGValue extends _VALUE
    {
    	String v;
    	public _STRINGValue(String v)
    	{
    		this.v=v;
    	}
    	
    	public int getType()
    	{
    		return STRING;
    	}
    	
    	public String toString(){return v;}
    }
    
}
