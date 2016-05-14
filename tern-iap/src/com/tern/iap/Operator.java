/**
 * Tern-iap Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.iap;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.tern.db.RowMapper;
import com.tern.db.db;
import com.tern.util.Convert;
import com.tern.util.TernContext;
import com.tern.util.Trace;

public class Operator 
{
	protected long operatorID;
	protected String loginName;
	protected String name;
	
	protected long[] roleIds;
	
	protected int[] myMenus;
	
	public Operator(int id,String lname,String name)
	{
		this.operatorID = id;
		this.loginName = lname;
		this.name = name;
		
		AppContext.setCurrentOperator(this);
	}
	
    public long getId() {return operatorID;}
	public String getName() {return name;}
	public String getLoginName(){return loginName;}
	
	public long[] getRoles(){return roleIds;}
	
	public static Operator current()
	{
		return (Operator)TernContext.current().currentOperator();
	}
	
	public static void current(Operator op)
	{
		AppContext.setCurrentOperator(op);
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
	
	public boolean hasPermission(int mid)
	{
		loadPermission();
		for(int i:myMenus)
		{
			if(i == mid) return true;
		}
		
		return false;
	}
	
	/*得到用户菜单*/
	public String getMenus()
	{		
		loadPermission();
		return parseMenu(AppContext.current().getMenus(),myMenus);
	}	
	
	private String parseMenu(Menu[] appMenus,int[] myMenus)
	{
		if(appMenus == null || myMenus == null) return Convert.EmptyString;
		
		StringBuffer buf = new StringBuffer("[");
		
		for(Menu m : appMenus)
		{
			boolean hasPri = false;
			for(int id : myMenus)
			{
				if(m.getId() == id)
				{
					hasPri = true;
					break;
				}
			}
			
			if(!hasPri) continue;
			
			if(buf.length() > 1) buf.append(",");
			buf.append(String.format("{\"id\":\"%d\",\"code\":\"%s\",\"caption\":\"%s\",\"url\":\"%s\",\"target\":\"%s\",\"icon\":\"%s\"",
			   m.getId(),m.getCode(),m.getCaption(),m.getUrl(),m.getTarget(),m.getIcon()));
			if(m.getChildItem() != null)
			{
				buf.append(",\"children\":").append( parseMenu(m.getChildItem(),myMenus) );
			}
			buf.append("}");
		}
		
		buf.append("]");
		
		return buf.toString();
	}
	
	private void loadPermission()
	{
		if(myMenus!=null && !com.tern.util.config.isDebug() )
		{
			return;
		}
		//得到用户的权限组
		try 
		{
			List<Integer> pgs = db.sql("select distinct pgid from t_userperm where operatorID=?" , this.operatorID)
			  .query(new RowMapper<Integer>(){
				  @Override
					public Integer map(ResultSet rs, int rowNum) throws SQLException {
					  return rs.getInt("pgid");
				  }
			  });
			
			if(pgs != null && pgs.size() > 0)
			{
				StringBuffer sql = new StringBuffer();
				for(Integer p : pgs)
				{
					if(sql.length() > 0) sql.append(",");
					sql.append(p);
				}
				
				sql.append(")").insert(0, "select distinct mid from t_permission where pgid in (");
				
				List<Integer> ids = TernContext.current().getMetaDB().sql(sql.toString())
						.query(new RowMapper<Integer>(){
				             @Override
					         public Integer map(ResultSet rs, int rowNum) throws SQLException {
					             return rs.getInt("mid");
				             }
			            });
				
				this.myMenus = new int[ids.size()];
				int i = 0;
				for(int m : ids)
				{
					myMenus[i] = m;
					i++;
				}
			}
		} 
		catch (SQLException e) 
		{
			Trace.write(Trace.Error, e, "load permission");
		}
	}
	
}
