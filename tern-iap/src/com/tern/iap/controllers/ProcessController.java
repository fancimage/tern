/**
 * Tern-iap Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.iap.controllers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opensymphony.util.Data;
import com.opensymphony.workflow.loader.ActionDescriptor;
import com.opensymphony.workflow.loader.StepDescriptor;
import com.opensymphony.workflow.loader.WorkflowDescriptor;
import com.opensymphony.workflow.spi.Step;
import com.tern.dao.Model;
import com.tern.dao.NamedValue;
import com.tern.dao.Record;
import com.tern.dao.RecordSet;
import com.tern.dao.RecordState;
import com.tern.db.DataTable;
import com.tern.db.RowMapper;
import com.tern.db.db;
import com.tern.iap.Operator;
import com.tern.iap.util.ActionResult;
import com.tern.iap.workflow.Service;
import com.tern.iap.workflow.Workflow;
import com.tern.util.Convert;
import com.tern.util.Trace;
import com.tern.util.config;
import com.tern.util.html;
import com.tern.web.ActionException;
import com.tern.web.Controller;
import com.tern.web.ControllerException;
import com.tern.web.HttpStream;
import com.tern.web.Route;

@Route("/task/$scode/%pid/*")
public class ProcessController extends Controller
{	
	private String scode;
	private int pid;
	
	/*
	 * Get all process(task) for service $sid
	 * */
	public String index()
	{		
		Service service = Service.getService(pid,scode);
		if(null == service)
		{
			throw new ControllerException(this, String.format("Service(pid,%d) does not exists.", pid,scode));
		}
		
		Operator op = Operator.current();
		
		Model model = Model.from(service.getDataTableName());
        RecordSet records = model.query()
                                 .join("process",true,new String[]{"id","serid"})
                                 .where("B.tid=? and A.pid=?",service.getId(),pid);
		
		request.setAttribute("model", model);
		request.setAttribute("records", records);
		request.setAttribute("service",service);
		
		return String.format("service/%s/index", service.getName() );
	}
	
	public String _new()
	{
		Service service = Service.getService(pid,scode);
		if(null == service)
		{
			throw new ActionException( String.format("Service(%d,%s) does not exists.", pid,scode));
		}
		
		Model model = Model.from(service.getDataTableName());
		Record record = model.create(); //new
		
		request.setAttribute("model",  model);
		request.setAttribute("record", record);
		request.setAttribute("service",service);
		
		return String.format("service/%s/new", service.getName() );
	}
	
	private static List<Map<String,Object>> getHistorySteps(long pid)
	{
		try 
		{
			return db.sql("select stepID,wfstep,actionid,stepName,owner,ownername,sDate,hDate,sstate,hDescription from wf_stepinfo where wfID=?"
				,pid).query(new RowMapper<Map<String,Object>>(){

					@Override
					public Map<String, Object> map(ResultSet rs, int rowNum)
							throws SQLException 
					{
						Map<String,Object> row = new HashMap<String,Object>();
						row.put("stepID", rs.getInt("stepID"));
						row.put("stepName", rs.getString("stepName"));
						row.put("ownername", rs.getString("ownername"));
						row.put("sstate", rs.getInt("sstate"));
						
						row.put("sDate", rs.getTimestamp("sDate"));
						row.put("hDate", rs.getTimestamp("hDate"));
						
						row.put("actionid", rs.getInt("actionid"));
						row.put("hDescription", rs.getString("hDescription"));
						
						return row;
					}
					
				});
		} 
		catch (SQLException e)
		{
			Trace.write(Trace.Error, e, "query process history-steps.");
			throw new ActionException( String.format("query process history-steps failed."));
		}
	}

	public void operators()
	{
		long wfid = Convert.parseLong(request.getParameter("wfid"));
		int actionid = Convert.parseInt(request.getParameter("actionid"));

		if(actionid <= 0)
		{
			this.setViewObject(new DataTable());
			return;
		}

		DataTable dt = Workflow.getInstance().getNextStepOperators(wfid,actionid,null);
		if(dt == null) dt = new DataTable();
		this.setViewObject(dt);
	}
	
	public String edit(long eid)
	{			
		Service service = Service.getService(pid,scode);
		if(null == service)
		{
			throw new ActionException( String.format("Service(%d,%s) does not exists.", pid,scode));
		}
		
		WorkflowDescriptor wd = Workflow.getInstance().getWorkflowDescriptor(service.getName());
		
		//get current step
		/*StepDescriptor step = null;		
		List tmp = Workflow.getInstance().getCurrentSteps(pid);
		if(tmp != null && tmp.size() > 0 )
		{
			step = wd.getStep(Convert.parseInt( ((Step)tmp.get(0)).getStepId() ));
		}
		
		if(null == step)
		{
			throw new ActionException( String.format("Process(%d,service=%s) has been completed!", pid ,service.getName() ));
		}*/
		
		Model model = Model.from(service.getDataTableName());
		Record record = model.find(eid);
		
		Map<String,Object> inputs = new HashMap<String,Object>();
	    inputs.put("wfName", service.getName());
    	inputs.put("data", record);
    	inputs.put("service", service);
    	
    	long wfid = record.getLong("wfid");
				
		//get available actions
    	Map<Integer,StepInfo> steps = new HashMap<Integer,StepInfo>();
    	//List<ActionDescriptor> actions = new ArrayList<ActionDescriptor>();
		int[] arr = Workflow.getInstance().getAvailableActions(wfid,inputs);
		for(int a : arr)
		{
			ActionDescriptor ad = wd.getAction(a);
			if(ad != null)
			{
				StepDescriptor s = (StepDescriptor)ad.getParent();
				StepInfo info = null;
				if(steps.containsKey(s.getId()))
				{
					info = steps.get(s.getId());
				}
				else
				{
					info = new StepInfo();
					info._step = s;
					steps.put(s.getId(), info);
				}
				info._actions.add(ad);
			}
		}
		
		//get system actions
		/*List<NamedValue> sysActions = new ArrayList<NamedValue>();
		NamedValue a = new NamedValue();
		a.name = "退回";
		a.value="back";
		sysActions.add(a);
		a = new NamedValue();
		a.name = "撤回";
		a.value="restart";
		sysActions.add(a);
		a = new NamedValue();
		a.name = "否决";
		a.value="reject";
		sysActions.add(a);*/
		
		//history steps
		//DataTable history = null;
		List<Map<String,Object>> history = getHistorySteps(wfid);

		// Get the preview chmoney
		float prev_chmoney = 0;
		try {
			DataTable wf_ct_change = db.table("wf_ct_change").where("wfID=?", wfid).order("stepID").query();
			if (wf_ct_change.size() != 0) {
				prev_chmoney = wf_ct_change.get(0).getFloat("chmoney");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		request.setAttribute("model",  model);
		request.setAttribute("record", record);
		request.setAttribute("wfid", wfid);
		request.setAttribute("service",service);
		request.setAttribute("steps",steps.values());
		//request.setAttribute("actions",actions);
		//request.setAttribute("sysactions",sysActions);
		request.setAttribute("history",history);
		request.setAttribute("prev_chmoney", prev_chmoney);
		
		return String.format("service/%s/edit", service.getName() );
	}
	
	@Route("%1/detail")
	public String detail(long eid)
	{
		Service service = Service.getService(pid,scode);
		if(null == service)
		{
			throw new ActionException( String.format("Service(%d,%s) does not exists.", pid,scode));
		}
		
		Model model = Model.from(service.getDataTableName());
		Record record = model.find(eid);
		List<Map<String,Object>> history = getHistorySteps(pid);
		
		request.setAttribute("model",  model);
		request.setAttribute("record", record);
		request.setAttribute("service",service);
		request.setAttribute("history",history);
		
		return String.format("service/%s/edit", service.getName() );
	}
	
	public void create()
	{
		ActionResult r = new ActionResult();
		this.setViewObject(r);
		
		//create new process
		//1. get service info
		Service service = Service.getService(pid,scode);
		if(null == service)
		{
			//throw new ActionException( String.format("Service(%d) does not exists.", sid));
			r.setResult(101,"流程不存在.");
			return;
		}
		
		Model model = Model.from(service.getDataTableName());
		
		try
		{
		    //2. create entity-data
		    Record record = html.new_record(model, request);
		    if(record.getState() == RecordState.Error)
		    {
		    	r.setResult(2, record.getErrorMessage());
		    	return;
		    }
		    
		    db.transaction();
		
		    //3. create process
		    long pid = Workflow.getInstance().createInstance(service, record);
		    if(pid <= 0)
		    {
		        db.rollback();
		        r.setResult(103,"内部错误：建立流程失败.");
		    }
		    else
		    {
		    	record.save();
		    	db.commit();
		    	r.setResult(0,null);
		    }		    
		}
		catch(com.tern.dao.ValueException e)
		{
			db.rollback();
			r.setResult(2,e.getMessage());
		}
		catch(Exception e)
		{
			db.rollback();
			Trace.write(Trace.Error, e, "create process failed");
			//throw new ActionException( String.format("Create process for Service(%d) failed.", sid) , e);
			r.setResult(102,"内部错误：建立流程失败.");
		}
	}
	
	public void update(long eid)
	{
		ActionResult r = new ActionResult();
		this.setViewObject(r);
		
		//action id
		String tmp = request.getParameter("actionID");
		if(tmp == null)
		{
			r.setResult(3,"参数错误:no action id.");
			return;
		}
		
		int actionID = 0;
		if(tmp.equals("back"))
		{
			//go back to the previous step
		}
		else if(tmp.equals("restart"))
		{
			//re-start the process
		}
		else
		{
			actionID = Convert.parseInt(tmp);
			if(actionID <=0 )
			{
				r.setResult(3,"参数错误:wrong action id.");
				return;
			}
		}
		
		Service service =Service.getService(pid,scode);
		if(null == service)
		{
			r.setResult(101,"流程不存在.");
			return;
		}
		
		Model model = Model.from(service.getDataTableName());			
        
		try
		{
			Record record = html.update_record(model, request);
			
			db.transaction();
		    record.save();
		    
		    record = model.find(eid);
		    
		    Map<String,Object> inputs = new HashMap<String,Object>();
		    inputs.put("wfName", service.getName());
	    	inputs.put("data", record);
	    	inputs.put("service", service);
	    	inputs.put("suggest", request.getParameter("actionSuggest"));
			inputs.put("nextOperator", request.getParameter("operator"));
		    
	    	long wfid = record.getLong("wfid");
		    // fetch process
		    Workflow.getInstance().doAction(wfid, actionID ,inputs);

			DataTable stepinfo = db.table("wf_stepinfo").where("wfID= ?", wfid).query();
			for (int i = 0; i < stepinfo.size(); i++) {
				Trace.write(Trace.Running,stepinfo.get(i).getInt("stepID") + " " + stepinfo.get(i).getDateTime("sDate") + " " + " " + stepinfo.get(i).getDateTime("hDate") + " " + stepinfo.get(i).getInt("sstate") + " " + i);
			}
			Trace.write(Trace.Running, stepinfo.size() + " stepinfo.size() ");
			int stepID = stepinfo.get(stepinfo.size() - 2).getInt("stepID");
			Trace.write(Trace.Running, "stepID:" + stepID);
			Trace.write(Trace.Running, "chmoney:" + request.getParameter("chmoney"));
			if (scode.compareTo("ct_change") == 0) {
				DataTable ct_change = db.table("ct_change").where("wfid = ?", wfid).query();
				Trace.write(Trace.Running, ct_change.size() + " ct_change.size() ");
				int chid = ct_change.get(0).getInt("chid");
				float chmoney = Float.parseFloat(request.getParameter("chmoney"));

				Map<String, Object> value = new HashMap<String, Object>();
				value.put("wfID", wfid);
				value.put("stepID", stepID);
				value.put("chid", chid);
				value.put("chmoney", chmoney);
				db.insert("wf_ct_change").values(value).exec();
			}

		    // actionSuggest
		    db.commit();
		}
		catch(com.tern.dao.ValueException e)
		{
			db.rollback();
			r.setResult(2,e.getMessage());
		}
		catch(Throwable t)
		{
			db.rollback();
			Trace.write(Trace.Error, t, "fetch process failed");
			r.setResult(3,"服务器异常.");
		}
	}
	
	/*protected void writeResult(int result,String err)
    {
        this.setContentType("text/javascript");
        this.response.setCharacterEncoding(config.getEncoding());
        
        HttpStream out = this.getStream();
        out.append("{\"code\":").append(String.valueOf(result));
        
        if(0 != result && err != null)
        {
        	if(err.indexOf('\n') >= 0)
        	{
        		err = Convert.replaceAll(err, "\n", "\\n");
        	}
            out.append(",\"message\":\"").append(err).append("\"");
        }
        
        out.append("}");
    }*/
	
	public static class StepInfo
	{
		StepDescriptor _step;
		List<ActionDescriptor> _actions=new ArrayList<ActionDescriptor>();
		List<NamedValue> _sysActions=new ArrayList<NamedValue>();;
		
		public StepDescriptor getStep(){return _step;}
		public List<ActionDescriptor> getActions(){return _actions;}
		public List<NamedValue> getSysActions(){return _sysActions;}
	}
}


