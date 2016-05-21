/**
 * Tern-iap Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.iap.workflow;

import com.opensymphony.module.propertyset.AbstractPropertySet;
import com.opensymphony.module.propertyset.InvalidPropertyTypeException;
import com.opensymphony.module.propertyset.PropertyException;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.util.Data;
import com.opensymphony.workflow.loader.StepDescriptor;
import com.opensymphony.workflow.spi.jdbc.JDBCWorkflowStore;
import com.opensymphony.workflow.spi.WorkflowEntry;
import com.opensymphony.workflow.StoreException;
import com.opensymphony.workflow.spi.SimpleStep;
import com.opensymphony.workflow.spi.Step;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.sql.ResultSet;
import java.sql.Timestamp;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import com.tern.dao.Model;
import com.tern.dao.Record;
import com.tern.db.InsertCommand;
import com.tern.db.SQL;
import com.tern.db.UpdateCommand;
import com.tern.db.db;
import com.tern.iap.Operator;
import com.tern.util.Convert;
import com.tern.util.Trace;
 

public class WorkflowStore extends JDBCWorkflowStore
{
	protected Connection getConnection() throws SQLException 
	{		
		return db.getConnection();
	}
	
	@Override
	protected void cleanup(Connection connection, Statement statement, ResultSet result) 
	{
        if (result != null)
        {
            try
            {
                result.close();
            }
            catch (SQLException ex)
            {
            	Trace.write(Trace.Error,ex, "Error closing resultset");                
            }
        }

        if (statement != null) 
        {
            try
            {
                statement.close();
            }
            catch (SQLException ex)
            {
            	Trace.write(Trace.Error,ex, "Error closing statement");   
            }
        }
        
        db.closeConnection(connection);
    }
	
	@Override
	public PropertySet getPropertySet(long entryId) 
	{
		return new IAPPropertySet("osff_" + entryId);
    }	
	
	protected long getNextEntrySequence(Connection c) throws SQLException
	{
		long ret = super.getNextEntrySequence(c);
		if(ret<=0) ret = 1;
		return ret;
	}	
	
	protected long getNextStepSequence(Connection c,long entryId) throws SQLException {
        
		String sql="select max(stepID) from wf_stepinfo where wfID="+entryId;

        PreparedStatement stmt = null;
        java.sql.ResultSet rset = null;

        try {
            stmt = c.prepareStatement(sql);
            rset = stmt.executeQuery();
            rset.next();

            long id = rset.getLong(1);
            if(0 == id) id = 1;
            
            id++;

            return id;
        } finally {
            cleanup(null, stmt, rset);
        }
    }
	
	private List findSteps(long entryId ,int state) throws StoreException
	{
		Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rset = null;
        PreparedStatement stmt2 = null;

        try {
            conn = getConnection();

            String sql = "SELECT " + stepId + ", " + stepStepId + ", " + stepActionId + ", " + stepOwner + ", " + stepStartDate + ", " + stepDueDate 
            		+ ", " + stepFinishDate + ", " + stepStatus + ", " + stepCaller +"," + stepPreviousId + " FROM " + currentTable 
            		+ " WHERE " + stepEntryId + " = ? AND sstate="+state;
            if(1 == state)
            {
            	sql += " ORDER BY " + stepId + " DESC";
            }
            
            String sql2 = "SELECT " + stepPreviousId + " FROM " + currentPrevTable + " WHERE "+ stepEntryId + " = ? AND " + stepId + " = ?";

            Trace.write(Trace.Information, "find current steps: %s" , sql);

            stmt = conn.prepareStatement(sql);

            stmt2 = conn.prepareStatement(sql2);
            stmt2.setLong(1, entryId);
            stmt.setLong(1, entryId);

            rset = stmt.executeQuery();

            ArrayList currentSteps = new ArrayList();

            while (rset.next()) 
            {
                long id = rset.getLong(1);
                int stepId = rset.getInt(2);
                int actionId = rset.getInt(3);
                String owner = rset.getString(4);
                Date startDate = rset.getTimestamp(5);
                Date dueDate = rset.getTimestamp(6);
                Date finishDate = rset.getTimestamp(7);
                String status = rset.getString(8);
                String caller = rset.getString(9);
                
                ArrayList prevIdsList = new ArrayList();
                
                Object obj = rset.getObject(10);
                if(obj == null || !(obj instanceof Integer))
                {
                	stmt2.setLong(2, id);

                	Trace.write(Trace.Information, "find pre steps: %s" , sql2);
                    ResultSet rs = stmt2.executeQuery();

                    while (rs.next()) 
                    {
                        long prevId = rs.getLong(1);
                        prevIdsList.add(new Long(prevId));
                    }                    
                }
                else
                {
                	long pid = 0;
                	if(obj instanceof Integer)
                	{
                		pid = ((Integer)obj).longValue();
                	}
                	else if(obj instanceof Long)
                	{
                		pid = ((Long)obj).longValue();
                	}

                	if(pid != 0 )
                	{
                	    prevIdsList.add(pid);
                	}
                }             
                
                long[] prevIds = new long[prevIdsList.size()];
                int i = 0;

                for (Iterator iterator = prevIdsList.iterator();
                        iterator.hasNext();)
                {
                    Long aLong = (Long) iterator.next();
                    prevIds[i] = aLong.longValue();
                    i++;
                }

                SimpleStep step = new SimpleStep(id, entryId, stepId, actionId, owner, startDate, dueDate, finishDate, status, prevIds, caller);
                currentSteps.add(step);
            }

            return currentSteps;
        } 
        catch (SQLException e)
        {
            throw new StoreException("Unable to locate "+ (state==0?"Current":"History")+ " steps for workflow instance #" + entryId, e);
        }
        finally
        {
            cleanup(null, stmt2, null);
            cleanup(conn, stmt, rset);
        }
	}
	
	public List findCurrentSteps(long entryId) throws StoreException
	{
        return findSteps(entryId,0);
    }

    public List findHistorySteps(long entryId) throws StoreException
    {
    	return findSteps(entryId,1);        
    }	
    
    public void moveToHistory(Step step) throws StoreException
    {
        /*Connection conn = null;
        PreparedStatement stmt = null;

        try
        {
            conn = getConnection();

            String sql = "UPDATE " + historyTable + " SET sstate = 1 WHERE "
                   + stepEntryId + "=? AND " + stepId + "=?";            
            Trace.write(Trace.Information, "moveToHistory: %s", sql);

            stmt = conn.prepareStatement(sql);
            stmt.setLong(1, step.getEntryId());
            stmt.setLong(2, step.getId());            
            stmt.executeUpdate();                       
        }
        catch (SQLException e) 
        {
            throw new StoreException("Unable to move current step to history step for #" + step.getEntryId(), e);
        }
        finally 
        {
            cleanup(conn, stmt, null);
        }*/        
    }
    
    /*@Override
	protected long createCurrentStep(Connection conn, long entryId, int wfStepId, String owner,
			Date startDate, Date dueDate, String status) throws SQLException 
	{        
    }*/
    
    public Step createCurrentStep(StepDescriptor step,long entryId, String owner, Date startDate, Date dueDate, 
    		String status, long[] previousIds) throws StoreException 
    {
        try
        {
        	long id = db.sql("select max(stepID) from wf_stepinfo where wfID=?",entryId).queryLong(); 
        	id+=1;
        	InsertCommand cmd = db.insert(currentTable)
      	      .set(stepEntryId, entryId)
      	      .set(stepId, id)
      	      .set(stepStepId, step.getId())
      	      .set(stepActionId, 0)
      	      .set(stepStartDate, new Timestamp(startDate.getTime()))
      	      //.set(stepDueDate, dueDate)
      	      .set(stepStatus, status)
      	      .set("sstate", 0)
      	      .set("stepName", step.getName());
        	
        	if(dueDate != null)
        	{
        		cmd.set(stepDueDate, new Timestamp(dueDate.getTime()));
        	}
        	
        	if(null == previousIds || previousIds.length <= 1)
        	{
        		cmd.set(stepPreviousId, previousIds.length==1?previousIds[0]:0);
        		cmd.exec();
        	}
        	else
        	{
        		cmd.exec();
        		
        		/*�������������*/
        		String sql = "INSERT INTO " + currentPrevTable + " (" + stepEntryId + "," + stepId + ", " + stepPreviousId + ") VALUES (?, ?, ?)";
        		SQL cmd2 = db.sql(sql);
        		
        		cmd2.param(0, entryId);
        		cmd2.param(1, id);
    			
    			for (int i = 0; i < previousIds.length; i++)
                {
    				cmd2.param(2, previousIds[i]);
                    cmd2.exec();
                }
        	}
            
            //addPreviousSteps(conn, entryId, id, previousIds);
        	
            return new SimpleStep(id, entryId, step.getId(), 0, owner, startDate, dueDate, 
            		null, status, previousIds, null);
        }
        catch (SQLException e) 
        {
            throw new StoreException("Unable to create current step for workflow instance #" + entryId, e);
        }       
    }
    
    /*protected void addPreviousSteps(Connection conn, long entryId, long id, long[] previousIds) throws SQLException
    {
    	if(null == previousIds || previousIds.length <= 1)
    	{    		    	
    		String sql = "UPDATE " + currentTable + " SET " + stepPreviousId + " = ? WHERE "
    				+ stepEntryId + " = ? AND " + stepId + " = ?";
    		
    		long preID;
    		if(1 == previousIds.length)
    		{
    			preID = previousIds[0];
    		}
    		else
    		{
    			preID = 0; //no pre steps
    		}
    		
    		Trace.write(Trace.Information, "addPreviousSteps(pre id=%d): %s" ,preID, sql);
    		PreparedStatement stmt = conn.prepareStatement(sql);
    		
    		stmt.setLong(1, preID);
    		stmt.setLong(2, entryId);
    		stmt.setLong(3, id);
    		
    		try
    		{
    		    stmt.executeUpdate();
    		}
    		finally
    		{
    		    cleanup(null, stmt, null);
    		}
    	}
    	else
    	{
    		String sql = "INSERT INTO " + currentPrevTable + " (" + stepEntryId + "," + stepId + ", " + stepPreviousId + ") VALUES (?, ?, ?)";
    		Trace.write(Trace.Information, "addPreviousSteps(pre id=%s): %s" ,Convert.join("", previousIds), sql);
    		
    		PreparedStatement stmt = conn.prepareStatement(sql);

    		try
    		{
    			stmt.setLong(1, entryId);
    			stmt.setLong(2, id);
    			
    			for (int i = 0; i < previousIds.length; i++)
                {
                    long previousId = previousIds[i];
                    stmt.setLong(3, previousId);
                    stmt.executeUpdate();
                }
    		}
    		finally
    		{
    		    cleanup(null, stmt, null);
    		}            
    	}    	        
    }*/
    
    public Step markFinished(Step step, int actionId, Date finishDate, String status, String caller,Map inputs) throws StoreException {
        //Connection conn = null;
        //PreparedStatement stmt = null;
    	
    	Operator op = Operator.current();

        try 
        {
        	UpdateCommand cmd = db.update(currentTable)
        	  .set("sstate", 1)
        	  .set(stepStatus, status)
        	  .set(stepActionId, actionId)
        	  .set(stepFinishDate, new Timestamp(finishDate.getTime()))
        	  .set(stepCaller, caller)
        	  .set(this.stepOwner, op.getId())
        	  .set("ownername", op.getName())
        	  .where(stepEntryId+"=? AND "+stepId+"=?" , step.getEntryId(),step.getId());
        	
        	if(inputs!=null)
        	{
        	    cmd.set("hDescription", inputs.get("suggest"));
        	}
        	
        	cmd.exec();
        	
            /*conn = getConnection();

            String sql = "UPDATE " + currentTable + " SET sstate=1," + stepStatus + " = ?, " + stepActionId + " = ?, " 
            + stepFinishDate + " = ?, " + stepCaller + " = ? WHERE " + stepEntryId + " = ? AND " + stepId + " = ?";

            Trace.write(Trace.Information, "markFinished: %s , status=%s", sql,status);

            stmt = conn.prepareStatement(sql);
            stmt.setString(1, status);
            stmt.setInt(2, actionId);
            stmt.setTimestamp(3, new Timestamp(finishDate.getTime()));
            stmt.setString(4, caller);
            
            stmt.setLong(5, step.getEntryId());
            stmt.setLong(6, step.getId());
            
            stmt.executeUpdate();*/

            SimpleStep theStep = (SimpleStep) step;
            theStep.setActionId(actionId);
            theStep.setFinishDate(finishDate);
            theStep.setStatus(status);
            theStep.setCaller(caller);

            return theStep;
        }
        catch (SQLException e) 
        {
            throw new StoreException("Unable to mark step finished for #" + step.getEntryId(), e);
        }
        /*finally 
        {
            cleanup(conn, stmt, null);
        }*/
    }
    
    public void setEntryState(long id, int state) throws StoreException
    {
        //Connection conn = null;
        //PreparedStatement ps = null;

        try
        {
        	UpdateCommand cmd = db.update(entryTable)
              	                  .set(entryState, state);
        	
            if(WorkflowEntry.COMPLETED == state || WorkflowEntry.KILLED == state)
            {            	
            	cmd.set("finishtime", new Timestamp(new Date().getTime()));            	
            	Trace.write(Trace.Information, "setEntryState: %s", (WorkflowEntry.KILLED == state)?"Killed":"Completed" );
            }
            else
            {            	        
            	Trace.write(Trace.Information, "setEntryState: %d", state);                               
            }
               
            cmd.where(entryId+"=?",id)
               .exec();
        }
        catch (SQLException e)
        {
            throw new StoreException("Unable to update state for workflow instance #" + id + " to " + state, e);
        }
        /*finally
        {
            cleanup(conn, ps, null);
        }*/
    }
    
    public WorkflowEntry createEntry(String workflowName) throws StoreException
    {
    	return createEntry(workflowName,null);
    }
    
    public WorkflowEntry createEntry(String workflowName,Map inputs) throws StoreException 
    {
    	//Connection conn = null;    	
    	Model model = Model.from("process");
    	Record process = model.create(); 
    	process.set("status", WorkflowEntry.CREATED);
    	//process.save();
    	return new IAPWorkflowEntry(process,workflowName,inputs);
    }
    
    public WorkflowEntry findEntry(long theEntryId) throws StoreException
    {
    	return findEntry(theEntryId,null);
    }
    
    public WorkflowEntry findEntry(long theEntryId,Map inputs) throws StoreException
    {
    	Model model = Model.from("process");
    	Record process = model.find(theEntryId);
    	return new IAPWorkflowEntry(process,inputs);
    }
}

class IAPPropertySet extends AbstractPropertySet 
{
	String globalKey;
	
	public IAPPropertySet(String key)
	{
		this.globalKey = key;
	}

	@Override
	public boolean exists(String key) throws PropertyException 
	{
		return getType(key) != 0;
	}

	@Override
	public Collection getKeys(String prefix, int type) throws PropertyException 
	{
		try 
		{
			return db.table("OS_PROPERTYENTRY").select("ITEM_KEY")
			  .where("GLOBAL_KEY=? AND ITEM_KEY LIKE ? AND (?=0 or ITEM_TYPE=?)" , globalKey , prefix+"%",type,type)
			  .query(new com.tern.db.RowMapper<String>(){

				@Override
				public String map(ResultSet rs, int rowNum) throws SQLException 
				{
					return rs.getString(1);
				}
				  
			  });
		}
		catch (SQLException e)
		{
			throw new PropertyException(e.getMessage());
			//return null;
		}
	}

	@Override
	public int getType(String key) throws PropertyException 
	{
		try
		{
			return db.table("OS_PROPERTYENTRY").select("ITEM_TYPE")
					 .where("GLOBAL_KEY=? AND ITEM_KEY=?",globalKey,key)
					 .queryInt();
		}
		catch (SQLException e)
		{
			throw new PropertyException(e.getMessage());		
		}
	}

	@Override
	public void remove() throws PropertyException 
	{
		try
		{
			db.delete("OS_PROPERTYENTRY")
			  .where("GLOBAL_KEY=?",globalKey)
			  .exec();
		}
		catch (SQLException e)
		{
			throw new PropertyException(e.getMessage());		
		}
	}

	@Override
	public void remove(String key) throws PropertyException 
	{
		try
		{
			db.delete("OS_PROPERTYENTRY")
			  .where("GLOBAL_KEY=? AND ITEM_KEY=?",globalKey,key)
			  .exec();
		}
		catch (SQLException e)
		{
			throw new PropertyException(e.getMessage());		
		}
	}

	@Override
	protected Object get(final int type, String key) throws PropertyException 
	{
		try
		{
			return db.table("OS_PROPERTYENTRY")
					 .select("ITEM_TYPE,STRING_VALUE,DATE_VALUE,DATA_VALUE,FLOAT_VALUE,NUMBER_VALUE")
					 .where("GLOBAL_KEY=? AND ITEM_KEY=?",globalKey,key)
					 .queryOne(new com.tern.db.RowMapper<Object>(){

						@Override
						public Object map(ResultSet rs, int rowNum)
								throws SQLException 
						{
							int propertyType = rs.getInt("ITEM_TYPE");
							if (propertyType != type) 
							{
								throw new InvalidPropertyTypeException();
							}
							
							switch (type) 
							{
							case PropertySet.BOOLEAN:
							{
								int boolVal = rs.getInt("NUMBER_VALUE");
								return boolVal == 1;
							}
							case PropertySet.DATA:
								return rs.getBytes("DATA_VALUE");
							case PropertySet.DATE:
								return rs.getTimestamp("DATE_VALUE");
							case PropertySet.DOUBLE:
								return new Double(rs.getDouble("FLOAT_VALUE"));
							case PropertySet.INT:
								return new Integer(rs.getInt("NUMBER_VALUE"));
							case PropertySet.LONG:
								return new Long(rs.getLong("NUMBER_VALUE"));
							case PropertySet.STRING:
								return rs.getString("STRING_VALUE");
							default:
								throw new InvalidPropertyTypeException("JDBCPropertySet doesn't support this type yet.");
							}
							
						}
						 
					 });
		}
		catch (SQLException e)
		{
			throw new PropertyException(e.getMessage());		
		}
	}

	@Override
	protected void setImpl(int type, String key, Object value)
			throws PropertyException 
	{
		try
		{
			UpdateCommand upd = db.update("OS_PROPERTYENTRY")
					              .where("GLOBAL_KEY=? AND ITEM_KEY=?",globalKey,key);
			
			String fname=null;
			Object val = value;			
			
			switch (type) 
			{
			case PropertySet.BOOLEAN:
			{
				Boolean boolVal = (Boolean) value;
				fname = "NUMBER_VALUE";
				val = boolVal?1:0;				
				break;
			}
			case PropertySet.DATA:
			{
				Data data = (Data) value;
				fname = "DATA_VALUE";
				val = data.getBytes();
				break;
			}
			case PropertySet.DATE:
			{
				fname = "DATE_VALUE";
				
				Date date = (Date) value;
				val = new Timestamp(date.getTime());
				break;
			}
			case PropertySet.DOUBLE:
			{
				fname = "FLOAT_VALUE";
				//Double d = (Double) value;
				//val = d.doubleValue();
				break;
			}
			case PropertySet.INT:
			{
				fname = "NUMBER_VALUE";
				//Integer i = (Integer) value;
				//val = i.intValue();
				break;
			}
			case PropertySet.LONG:
			{
				fname = "NUMBER_VALUE";
				//Long l = (Long) value;
				//val = l.longValue();
				break;
			}
			case PropertySet.STRING:
			{
				fname = "STRING_VALUE";
				break;
			}
			default:
				throw new PropertyException("This type isn't supported!");
			}
			
			
			int rows = upd.set(fname, val).exec();
			if (rows != 1) 
			{
				db.insert("OS_PROPERTYENTRY")
			              .set("GLOBAL_KEY", globalKey)
			              .set("ITEM_KEY", key)
			              .set(fname, val)
			              .exec();
			}
			
		}
		catch (SQLException e)
		{
			throw new PropertyException(e.getMessage());		
		}		
	}
}
