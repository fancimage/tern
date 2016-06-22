package com.tern.iap.workflow;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.opensymphony.workflow.WorkflowException;
import com.tern.db.InsertCommand;
import com.tern.db.RowMapper;
import com.tern.db.SQL;
import com.tern.db.db;
import com.tern.iap.Operator;
import com.tern.util.Convert;
import com.tern.util.TernContext;
import com.tern.util.Trace;

public class DefaultWorkflowPermission implements WorkflowPermission
{

	@Override
	public boolean hasRight(IAPWorkflowEntry entry, Map attrs) 
	{
		if(attrs==null || !attrs.containsKey("op.name"))
		{
			return true;
		}
		
		String opName = Convert.toStringIgnoreEmpty(attrs.get("op.name"), "");    	
    	String type = Convert.toStringIgnoreEmpty(attrs.get("op.type"), "role");
    	int pid = entry.getProcess().getInt("pid");
    	
    	Operator op = Operator.current();    	    	
    	if(type.equals("user"))
    	{
    		if( opName.equals(op.getLoginName()) )
    		{
    			/*判断当前用户对工程是否有权限?*/
    			return true;
    		}
    	}
    	else
    	{
    		return op.isRole(opName, pid);    		
    	}
    	
    	return false;
	}

	@Override
	public void initWorkflowOperators(IAPWorkflowEntry entry, Map attrs) throws WorkflowException 
	{
		if(attrs==null || !attrs.containsKey("op.name"))
		{
			return;
		}
		
		String opName = Convert.toStringIgnoreEmpty(attrs.get("op.name"), "");
		int pid = entry.getProcess().getInt("pid");
		String type = Convert.toStringIgnoreEmpty(attrs.get("op.type"), "role");
		
		List<Integer> ops = getIDByName(opName,type,pid);
		if(ops==null || ops.size() <=0)
		{
			throw new WorkflowException("no operator has permission to process workflow.");
		}
		
		/*先清空之前的待办人*/
		try 
		{
			db.delete("wf_operator").where("wfID=?",entry.getId()).exec();

			SQL sql = db.sql("insert into wf_operator(wfID,operatorID) values(?,?)").param(0,entry.getId());
			//InsertCommand cmd = db.insert("wf_operator").set("wfID", entry.getId());
			for(int op : ops)
			{
				//cmd.set("operatorID", op).exec();
				sql.param(1 , op).exec();
			}
		}
		catch (SQLException e) 
		{
			throw new WorkflowException(e);
		}				
	}
	
	public static List<Integer> getIDByName(String name,String type,int pid)
	{		
		if(name == null) return null;
		
		String[] rns = name.split(","); 		
		StringBuffer buf = new StringBuffer();
		for(String s:rns)
		{
			if(buf.length() > 0) buf.append(",");
			buf.append("'").append(s).append("'");
		}
		
		RowMapper<Integer> mapper = new RowMapper<Integer>(){
			@Override
			public Integer map(ResultSet rs, int rowNum) throws SQLException {
				return rs.getInt("operatorID");
			}
		};
		
		List<Integer> list = null;
		try
		{
			if(type.equals("user"))
			{
				buf.append(")").insert(0, "select operatorID from t_operator where loginname in (");
				list = db.sql(buf.toString()).query(mapper);				
			}
			else
			{
				final StringBuffer sqlBuf = new StringBuffer();
				
				buf.append(")").insert(0, "select rid from t_role where rname in(");
				TernContext.current().getMetaDB().sql(buf.toString()).query(new RowMapper<Object>(){

					@Override
					public Object map(ResultSet rs, int rowNum) throws SQLException {
						if(sqlBuf.length()>0) sqlBuf.append(",");
						sqlBuf.append(rs.getInt("rid"));
						return null;
					}				   
			    });
				
				if(sqlBuf.length()<=0) return null;
				sqlBuf.append(")").insert(0, "select distinct operatorID from t_userrole where pid=? and rid in (");
				
				list = db.sql(sqlBuf.toString(),pid).query(mapper);
			}
		}
		catch(SQLException e)
		{
			Trace.write(Trace.Error, e, "getIDByName");
		}
		
		return list;
	}

}
