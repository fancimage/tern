/**
 * Tern-iap Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.iap;

public class Operator 
{
	protected int operatorID;
	protected String loginName;
	protected String name;
	
	public Operator(int id,String lname,String name)
	{
		this.operatorID = id;
		this.loginName = lname;
		this.name = name;
	}
	
    public int getId() {return operatorID;}	
	public String getName() {return name;}
	public String getLoginName(){return loginName;}
	
}
