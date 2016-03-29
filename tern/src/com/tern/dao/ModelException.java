/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.dao;

public class ModelException extends RuntimeException
{
	private Model model;
	public ModelException(Model model)
	{
		this(model,null);
	}
	
    public ModelException(Model model,String msg)
    {    	
    	super(msg==null || msg.length()<=0?"model error":msg);
    	this.model = model;
    }
    
    public Model getModel(){return model;}
    
    public String toString()
    {
    	return String.format("(%s):%s", model.getCaption(),this.getMessage());
    }
}
