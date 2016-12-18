/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.db;

import java.util.List;
import java.util.Map;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

import javax.sql.DataSource;

import com.tern.util.Convert;
import com.tern.util.Trace;

public class db 
{
	/*public static class list extends java.util.ArrayList<row>
	{}
	
	public static class row extends java.util.HashMap<String, Object>
	{
		row(int c)
		{
			super(c);
		}
	}*/

	public static SQL sql()
	{
		return Database.db.sql(null);
	}

	public static SQL sql(String sqlstr)
	{	
		return Database.db.sql(sqlstr);
	}

	public static SQL sql(String sqlstr, Object[][] params)
	{
		return Database.db.sql(sqlstr, params);
	}
	
	public static SQL sql(String sqlstr, Object... params)
	{
		return Database.db.sql(sqlstr, params);
	}

	public static SQL sql(String sqlstr, Map<String, Object> params)
	{
		return Database.db.sql(sqlstr, params);
	}

	public static Query table(String tableName)
	{
		return Database.db.table(tableName);
	}

	public static Query table(String tableName, String aliasName)
	{
		return Database.db.table(tableName, aliasName);
	}

	public static UpdateCommand update(String tableName)
	{
		return Database.db.update(tableName);
	}
	
	public static InsertCommand insert(String tableName)
	{
		return Database.db.insert(tableName);
	}

	public static DeleteCommand delete(String tableName) 
	{
		return Database.db.delete(tableName);
	}
	
	public static DBType getDbType()
	{
		return Database.db.getDbType();
	}
	
	public static Connection getConnection() throws SQLException
	{
		return Database.db.getConnection();
	}
	
	public static void closeConnection(Connection conn)
	{
		Database.db.closeConnection(conn);
	}
	
	public static Transaction transaction() throws SQLException
	{
		return Database.db.transaction();
	}
	
	public static void commit()
	{
		Database.db.commit();
	}
	
	public static void rollback()
	{
		Database.db.rollback();
	}
	
	public static boolean inTransaction()
	{
		return Database.db.inTransaction();
	}
	
	public static Database establish(String[][] props) throws SQLException
	{
		if(props == null) return null;
		
		Map<String,Object> newprops = new java.util.HashMap<String,Object>(props.length);
		for(String[] arr : props)
		{
			if(arr.length!=2) continue;
			newprops.put(arr[0], arr[1]);
		}
		
		return Database.createDB( newprops );
	}
	
	public static Database establish(Map<String,Object> props) throws SQLException
    {		
		return Database.createDB( props );
    }
	
	public static Database establish(DataSource ds,String name) throws SQLException
	{
		if(name == null || name.length()<=0)
		{
			name = "default";
		}
		if(Database.dbs.containsKey(name))
		{
			throw new SQLException("Has duplicate database name:"+name);
		}
		
		Connection con = null;
		String driverName = null;
		try
		{
			con = ds.getConnection();
	        java.sql.DatabaseMetaData meta = con.getMetaData();
	        driverName = meta.getDriverName();
		}
		catch(SQLException e)
		{
			Trace.write(Trace.Error, e, "Create database from DataSource");
		}
		finally
		{
			if (con != null)
            {
                try
                {
                    con.close();
                }
                catch (java.sql.SQLException ex1)
                {
                }
            }
		}
		
		if(driverName == null)
		{
			return null;
		}
		
		Database d = null;
		
		if(driverName.indexOf("Oracle") >=0)
		{
			d = new OracleDB();
		} 
		else if(driverName.indexOf("mysql") >= 0)
		{
			d = new MySqlDB();
		}
		else
		{
			throw new SQLException("Unknown database driver:"+driverName);
		}
		
		d.ds = ds;				
		
		if(Database.db == null)
		{
			Database.db = d;
		}
		
		Database.dbs.put(name, d);
		Trace.write(Trace.Running, "database added,name = %s,driver=%s" , name ,driverName);
		
		return d;
	}
	
	public static void closeAll()
	{
		for(Database d:Database.dbs.values())
		{
			if(d.ds != null)
			{
				try
				{
					java.lang.reflect.Method method = d.ds.getClass()
					        .getDeclaredMethod("close", (Class[])null);
					method.invoke(d.ds, (Object[])null);
				}
				catch (Throwable e)
				{
					Trace.write(Trace.Error, e, "close datasource");
				}
			}
		}
		
		Database.db = null;
		Database.dbs.clear();
	}

	/*public static List<Map<String, String>> columns(String tableName)
	{
		return Database.db.columns(tableName);
	}*/
	
	static class DBContext
	{
		public Connection con;
		public List<Transaction> trans;
	}
	
	/*public static class BLOB
	{
		byte[] value;
		public BLOB(byte[] data)
		{
			this.value = data;
		}
	}
	
	public static class CLOB
	{
		String value;
		
		public CLOB(String str)
		{
			this.value = str;
		}
	}*/
	
	public static class Transaction
	{
		DBContext ctx;
		int transCount;
		Database database;
		Savepoint svp;
		
		Transaction(DBContext contex,Database db) throws SQLException
		{
			this.ctx = contex;
			transCount = ctx.trans==null?0:ctx.trans.size();
			this.database = db;
			
			if(0 == transCount)
			{
				ctx.con.setAutoCommit(false);
			}
			else
			{
				//nested transactions
				/*if(database.supportNestedTrans)
				{
					database.sql("SAVEPOINT tern_sp_"+transCount).exec();
				}*/
				svp = ctx.con.setSavepoint("tern_sp_"+transCount);
			}
			
			Trace.write(Trace.Information, "[con][%d] begin trans(%d)." , ctx.con.hashCode() , transCount );
			
			if(ctx.trans == null)
			{
				ctx.trans = new java.util.ArrayList<db.Transaction>();
			}
			ctx.trans.add(this);
		}
		
		public void commit()
		{
			if(transCount < 0) return;
			
			if(ctx.trans!=null && ctx.trans.size() > transCount)
			{
				if(0 == transCount)
				{
					try
					{
					    ctx.con.commit();
					    Trace.write(Trace.Information, "[con][%d] commit(%d)." , ctx.con.hashCode(),transCount);					   
					}
					catch(SQLException e)
					{
						Trace.write(Trace.Error,e, "[con][%d] commit failed(%d).",ctx.con.hashCode(),transCount);
					}
					
					try
					{
						 ctx.con.setAutoCommit(true);
					}
					catch(SQLException e)
					{
						Trace.write(Trace.Error,e, "[con][%d] setAutoCommit failed" , ctx.con.hashCode());
					}
					
					try
					{						
						Trace.write(Trace.Information,"[con][%d] close connection" , ctx.con.hashCode());
						ctx.con.close();
					}
					catch(SQLException e)
					{
						Trace.write(Trace.Error,e, "[con][%d] close connection" , ctx.con.hashCode());
					}
					
					ctx.con = null;
					database.dbConnection.remove();
				}
				else
				{
					/*if(database.supportNestedTrans)
					{
						try
						{
						    database.sql("RELEASE SAVEPOINT tern_sp_"+transCount).exec();
						}
						catch(SQLException e)
						{
							Trace.write(Trace.Error,e, "[con][%d] commit failed(%d).",ctx.con.hashCode(),transCount);
						}
					}*/
					
					if(this.svp != null)
					{
						try
						{
						    ctx.con.releaseSavepoint(svp);
						}
						catch(SQLException e)
						{
							Trace.write(Trace.Error,e, "[con][%d] commit failed(%d).",ctx.con.hashCode(),transCount);
						}
						svp = null;
					}
					
					Trace.write(Trace.Information, "[con][%d] commit(%d)." , ctx.con.hashCode(),transCount);
				}
				
				for(int i=ctx.trans.size()-1;i>=transCount;i--)
				{
					ctx.trans.remove(i);
				}
				
				transCount = -1;
			}
			else
			{
				Trace.write(Trace.Information, "[con][%d]  not necessary to commit(%d)." , ctx.con.hashCode(),transCount);
			}
		}
		
		public void rollback()
		{
			if(transCount < 0) return;
			
			if(ctx.trans!=null && ctx.trans.size() > transCount)
			{
				if(0 == transCount)
				{
					try
					{
					    ctx.con.rollback();
					    Trace.write(Trace.Information, "[con][%d] rollback(%d)." , ctx.con.hashCode(),transCount);
					}
					catch(SQLException e)
					{
						Trace.write(Trace.Error,e, "[con][%d] rollback failed(%d).",ctx.con.hashCode(),transCount);
					}
					
					try
					{
						 ctx.con.setAutoCommit(true);
					}
					catch(SQLException e)
					{
						Trace.write(Trace.Error,e, "[con][%d] setAutoCommit failed" , ctx.con.hashCode());
					}
					
					try
					{						
						Trace.write(Trace.Information,"[con][%d] close connection" , ctx.con.hashCode());
						ctx.con.close();
					}
					catch(SQLException e)
					{
						Trace.write(Trace.Error,e, "[con][%d] close connection" , ctx.con.hashCode());
					}
					
					ctx.con = null;
					database.dbConnection.remove();
				}
				else
				{					
					if(this.svp != null)
					{
						try
						{
						    ctx.con.rollback(svp);
						}
						catch(SQLException e)
						{
							Trace.write(Trace.Error,e, "[con][%d] rollback failed(%d).",ctx.con.hashCode(),transCount);
						}
						svp = null;
					}
					
					Trace.write(Trace.Information, "[con][%d] rollback(%d)." , ctx.con.hashCode(),transCount);
				}
				
				for(int i=ctx.trans.size()-1;i>=transCount;i--)
				{
					ctx.trans.remove(i);
				}
				
				transCount = -1;
			}
			else
			{
				Trace.write(Trace.Information, "[con][%d]  not necessary to rollback(%d)." , ctx.con.hashCode(),transCount);
			}
		}
		
		protected void finalize()
		{
			if(transCount >= 0)
			{
				this.rollback();
			}
		}
	}
}

abstract class DataSourceLoader
{
	abstract public String[] getClasses();
	abstract public String[][] getAttribues(String clsName);
}

class DBCPLoader extends DataSourceLoader
{

	@Override
	public String[] getClasses()
	{		
		return new String[]{
				"org.apache.tomcat.dbcp.dbcp2.BasicDataSource",  //for tomcat8
				"org.apache.tomcat.dbcp.dbcp.BasicDataSource",  //for tomcat
				"org.apache.commons.dbcp2.BasicDataSource",
				"org.apache.commons.dbcp.BasicDataSource"};
	}

	@Override
	public String[][] getAttribues(String clsName)
	{
		boolean dbcp = (clsName!=null && clsName.indexOf(".dbcp2.")<0);
		String[][] ret = new String[][]{
			{"driverClassName","driver"},
			{"url"},
			{"username","user"},
			{"password","pw"},
			{"maxTotal","maxActive"},
			{"initialSize"},
			{"minIdle"},
			{"maxIdle"}
		};

		if(dbcp)
		{
			ret[4][0] = "maxActive";
		}

		return ret;
	}
}
