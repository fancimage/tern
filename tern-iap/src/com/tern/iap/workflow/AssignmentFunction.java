package com.tern.iap.workflow;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.FunctionProvider;
import com.opensymphony.workflow.WorkflowException;
import com.tern.db.db;
import com.tern.util.Convert;

public class AssignmentFunction implements FunctionProvider
{
	@SuppressWarnings("rawtypes")
	@Override
	public void execute(Map transientVars, Map args, PropertySet ps)
			throws WorkflowException 
	{
		//op.name
		String name = (String)args.get("op.name");
		if(name==null || name.length()<=0)
		{
			throw new WorkflowException("parameter op.name is illegal.");
		}
		
		//op.type
		boolean isRole = true;
		String type = (String)args.get("op.type");
		if(type!=null && type.equalsIgnoreCase("oper"))
		{
			isRole = false;
		}
		
		Map<Integer,String> opers = new HashMap<Integer,String>();
		
		try 
		{
			if(isRole)
			{
				//get operators
			}
			else
			{
				int opid = db.table("t_operator").select("operatorID").where("loginName=?",name).queryInt();
				if(opid > 0)
				{
					opers.put(opid, name);
				}
			}
		} 
		catch (SQLException e) 
		{
			throw new WorkflowException(e);
		}
		
		if(opers.size() <=0 )
		{
			throw new WorkflowException("No user to process this step.");
		}
		
		String flag = Convert.toString(transientVars.get("$execflag"));
		if(flag != null && flag.equals("query"))
		{
			//$operators
		}
		else
		{
			//store to database: wf_operator
		}
	}

}
