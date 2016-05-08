/**
 * Tern-iap Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.iap;

import java.sql.SQLException;

import com.tern.db.db;

public class Operator 
{
	protected long operatorID;
	protected String loginName;
	protected String name;
	
	protected long[] roleIds;
	
	public Operator(int id,String lname,String name)
	{
		this.operatorID = id;
		this.loginName = lname;
		this.name = name;
	}
	
    public long getId() {return operatorID;}
	public String getName() {return name;}
	public String getLoginName(){return loginName;}
	
	public long[] getRoles()
	{
		return roleIds;
	}
	
	public int getIDByName(String name,String type)
	{
		if(name.indexOf("'")>=0 || name.indexOf(" ")>=0)
		{
			return 0;
		}
		
		try
		{
			return db.sql("select roleID from t_role where roleName=?",name).queryInt();
		}
		catch(SQLException e)
		{
			return 0;
		}
	}
	
}
