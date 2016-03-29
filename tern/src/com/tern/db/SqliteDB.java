/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tern.util.Convert;

public class SqliteDB extends Database
{
	public DBType getDbType()
	{
		return DBType.sqlite;
	}
	
	protected void initDataSource(Map<String,Object> props) throws SQLException
	{
		props.put("driver", "org.sqlite.JDBC");
		
		String dbname = Convert.toString(props.get("db"));
		if(dbname==null || dbname.length()<=0)
		{
			dbname = com.tern.util.config.getConfigurationPath()+"/sqlite.db";
		}
		
		String url = String.format("jdbc:sqlite:%s" , dbname);
		props.put("url",url);
		
		super.initDataSource(props);
	}
	
	public long lastID(String name) throws SQLException
	{
		return this.sql("SELECT last_insert_rowid() FROM "+name).queryLong();
	}
	
	public List<Map<String, Object>> columns(String tableName) throws SQLException
	{
		String sql = String.format("PRAGMA table_info(%s)", tableName);
		final Pattern pat = Pattern.compile("^(\\w+)(?:\\((\\d+)(?:,(\\d+))?\\))?");
		
		return this.sql(sql)
				.query(new RowMapper<java.util.Map<String,Object>>(){

			@Override
			public Map<String, Object> map(ResultSet rs, int rowNum)
					throws SQLException 
			{
				java.util.Map<String, Object> m = new java.util.HashMap<String, Object>();
				
				//name
				m.put("name", rs.getString("name"));
				
				//nullable
				if(rs.getInt("notnull") == 0)
				{
					m.put("nullable", true);
				}
				else
				{
					m.put("nullable", false);
				}
				
				String def = rs.getString("dflt_value");
				if(def == null || def.equalsIgnoreCase("null"))
				{
				     //pass	
				}
				else
				{
					m.put("default", def);
				}
				
				if(rs.getInt("pk") == 1)
				{
					m.put("key", true);
				}
				
				Matcher matcher = pat.matcher(rs.getString("type").toLowerCase());
				matcher.find();
				String _type = matcher.group(1);
				String _len = matcher.group(2);
				String _scale = matcher.group(3);
				
				if(_type.equals("integer"))
				{
					m.put("type", "numeric");
					m.put("min",-2147483648);
					m.put("max", 2147483647);
					
					if(rs.getInt("pk") == 1)
					{
						m.put("auto", true);
					}
				}
				else if(_type.equals("int"))
				{
					m.put("type", "numeric");
					m.put("min",-2147483648);
					m.put("max", 2147483647);
				}
				else if(_type.equals("smallint"))
				{
					m.put("type", "numeric");
					m.put("min",-32768);
					m.put("max", 32767);
				}
				else if(_type.equals("char"))
				{
					m.put("type", "string");
					int l = Convert.parseInt(_len,1);
					m.put("min", l);
					m.put("max", l);
				}
				else if(_type.equals("varchar"))
				{
					m.put("type", "string");
					m.put("min", 0);
					m.put("max", Convert.parseInt(_len,1));
				}
				else if(_type.equals("float")
						|| _type.equals("double")
						|| _type.equals("decimal"))
				{
					m.put("type", "numeric");
					m.put("length", Convert.parseInt(_len,1));
					m.put("scale",  Convert.parseInt(_scale,1));
				}
				else if(_type.equals("datetime"))
				{
					m.put("type", "datetime");
					m.put("format", "yyyy-MM-dd HH:mm:ss");
				}
				else if(_type.equals("date"))
				{
					m.put("type", "datetime");
					m.put("format", "yyyy-MM-dd");
				}
				else if(_type.equals("time"))
				{
					m.put("type", "datetime");
					m.put("format", "HH:mm:ss");
				}
				else if(_type.endsWith("text"))
				{
					m.put("type", "text");
				}
				else if(_type.endsWith("blob"))
				{
					m.put("type", "blob");
				}
				else
				{
					m.put("type", "string");
				}
				
				return m;
			}
		});
	}
	
}
