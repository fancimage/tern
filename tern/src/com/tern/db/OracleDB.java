/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.db;

import com.tern.util.Convert;
import com.tern.util.Trace;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OracleDB extends Database
{
	public DBType getDbType()
	{
		return DBType.oracle;
	}
	
	protected void initDataSource(Map<String, Object> props) throws SQLException
	{
	    if (!props.containsKey("driver"))
	    {
	      props.put("driver", "oracle.jdbc.driver.OracleDriver");
	    }

	    String dbname = Convert.toString(props.get("db"));
	    if ((dbname == null) || (dbname.length() <= 0))
	    {
	      throw new SQLException("no db attribue.");
	    }

	    String url = String.format("jdbc:oracle:thin:@%s:%d:%s", new Object[] { 
	      Convert.toStringIgnoreEmpty(props.get("ip"), "127.0.0.1"), 
	      Integer.valueOf(Convert.parseInt(props.get("port"), 1521)), 
	      dbname });

	    props.put("url", url);

	    if (!props.containsKey("user"))
	    {
	      props.put("user", "system");
	    }

	    super.initDataSource(props);
	}
	
	public long lastID(String name) throws SQLException
	{
		//name--> tablename,sequence name--> SEQ_tablename_ID
		return this.sql(String.format("SELECT SEQ_%s_ID FROM DUAL", name.toUpperCase())).queryLong();
	}
	
	public List<Map<String, Object>> columns(String tableName) throws SQLException
	{		
		String tname = tableName.toUpperCase();
		//GET KEYS
		final Set<String> keys = this.sql(String.format("SELECT COL.COLUMN_NAME FROM USER_CONSTRAINTS CON, USER_CONS_COLUMNS COL WHERE CON.CONSTRAINT_NAME=COL.CONSTRAINT_NAME AND CON.CONSTRAINT_TYPE='P' AND COL.TABLE_NAME='%s'",tname))
				.querySet();
		
		//SEQUENCE Exists?
		final String seqName = this.sql(String.format("SELECT SEQUENCE_NAME FROM USER_SEQUENCES WHERE SEQUENCE_NAME='SEQ_%s_ID'", tname))
		    .queryString();
				
		String sql = String.format("SELECT COLUMN_NAME,DATA_TYPE,DATA_LENGTH,DATA_PRECISION,DATA_SCALE,DATA_DEFAULT,NULLABLE FROM USER_TAB_COLUMNS WHERE TABLE_NAME='%s'", tname);
		return this.sql(sql)
				.query(new RowMapper<java.util.Map<String,Object>>(){

			@Override
			public Map<String, Object> map(ResultSet rs, int rowNum)
					throws SQLException 
			{
				java.util.Map<String, Object> m = new java.util.HashMap<String, Object>();
				
				//name
				String name = rs.getString("COLUMN_NAME");
				m.put("name", name);
				
				//nullable
				String nullable = Convert.toStringIgnoreEmpty(rs.getString("NULLABLE"), "Y");
				if(nullable.equalsIgnoreCase("N"))
				{
					m.put("nullable", false);
				}
				else
				{
					m.put("nullable", true);
				}
				
				//default value
				m.put("default", rs.getString("DATA_DEFAULT"));
				
				//is key
				if( keys.contains(name) )
				{
					m.put("key", true);
					
					//only one key?
					if( 1 == keys.size() && seqName != null)
					{
						m.put("auto", true);
					}
				}
				
				//type
				String type = rs.getString("DATA_TYPE");
				if(type.equals("NUMBER"))
				{
					m.put("type", "numeric");
					int scale = rs.getInt("DATA_SCALE");
					int prec = rs.getInt("DATA_PRECISION");
					if(0 == scale)
					{
						//integer
						if(0 == prec)
						{
							//max?min?
						}
						else
						{
							m.put("length", prec );
						}
					}
					else
					{
						m.put("scale",  scale);
						m.put("length", prec );
					}
				}
				else if(type.equals("CHAR"))
				{
					m.put("type", "string");
					int len = rs.getInt("DATA_LENGTH");
					m.put("min", len);
					m.put("max", len);
				}
				else if(type.equals("NCHAR"))
				{
					m.put("type", "string");
					int len = rs.getInt("DATA_LENGTH")/2;
					m.put("min", len);
					m.put("max", len);
				}
				else if(type.equals("DATE"))
				{
					m.put("type", "datetime");
					m.put("format", "yyyy-MM-dd HH:mm:ss");
				}
				else if(type.equals("BLOB"))
				{
					m.put("type", "blob");
				}
				else if(type.equals("VARCHAR2") 
						|| type.equals("VARCHAR"))
				{
					m.put("type", "string");
					m.put("min", 0);
					m.put("max", rs.getInt("DATA_LENGTH"));
				}
				else if(type.equals("NVARCHAR2")
						|| type.equals("NVARCHAR"))
				{
					m.put("type", "string");
					m.put("min", 0);					
					m.put("max", rs.getInt("DATA_LENGTH")/2);
				}				
				else if(type.equals("CLOB") || type.equals("NCLOB"))
				{
					m.put("type", "text");
				}
				else
				{
					//xmltype,bfile...
					m.put("type", "string");
				}
				
				return m;
			}
	   });
	}
	
	/*protected void setBlob(java.sql.PreparedStatement st,int index,db.BLOB value) throws SQLException
	{
		st.setBlob(index, 
				new oracle.sql.BLOB( (oracle.jdbc.driver.OracleConnection)st.getConnection(), value.value) );
	}
	
	protected void setClob(java.sql.PreparedStatement st,int index,db.CLOB value) throws SQLException
	{
		st.setClob(index, 
				new oracle.sql.CLOB( (oracle.jdbc.driver.OracleConnection)st.getConnection(), value.value.getBytes()) );		
	}*/
	
}
