/**
 * Tern-iap Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.iap.workflow;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;

import com.tern.dao.Record;
import com.tern.db.*;
import com.tern.iap.AppContext;
import com.tern.iap.Operator;
 
import com.tern.util.Convert;
import com.tern.util.Trace;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.basic.BasicWorkflowContext;
import com.opensymphony.workflow.*;
import com.opensymphony.workflow.config.Configuration;
import com.opensymphony.workflow.loader.WorkflowDescriptor;
import com.opensymphony.workflow.spi.Step;
import com.opensymphony.workflow.spi.WorkflowEntry;
import com.opensymphony.workflow.spi.WorkflowStore;
import com.opensymphony.workflow.util.VariableResolver;
import com.opensymphony.workflow.loader.ConditionalResultDescriptor;
import com.opensymphony.workflow.loader.ConditionsDescriptor;
import com.opensymphony.workflow.loader.ResultDescriptor;
import com.opensymphony.workflow.loader.StepDescriptor;
import com.opensymphony.workflow.loader.WorkflowFactory;
import com.opensymphony.workflow.loader.URLWorkflowFactory;
import com.opensymphony.workflow.util.DefaultVariableResolver;
import com.opensymphony.workflow.loader.ActionDescriptor;
import com.opensymphony.workflow.loader.RestrictionDescriptor;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

public class Workflow extends AbstractWorkflow
{
	//private Operator _user;
	private long curr_wfid=0;
	private static final Workflow instance = new Workflow();
	
	public static Workflow getInstance()
	{
		return instance;
	}
	
	private static class MyContext extends BasicWorkflowContext
	{
		public MyContext(String caller)
		{
	        super(caller);
	    }
		
		public void setRollbackOnly()
		{
			if(db.inTransaction())
			{
				db.rollback();
			}
		}
	}
	
	private Workflow()
	{
		super.context = new MyContext(null);
	}
	
    //public Workflow(Operator user)
    //{    	
    	//super(String.valueOf(user.getOperatorID()));
    //	super.context = new MyContext(String.valueOf(user.getId()));
    //	_user=user;
    //}
    
    Map getInputs(Map<String,Object> inputs,long wfid)
    {
    	if(inputs == null)
    	{
    		inputs = new java.util.HashMap<String,Object>();
    	}
    	 
        Operator op = AppContext.getCurrentOperator();    	
    	inputs.put("caller", op.getId());
    	inputs.put("user", op);
    	
    	return inputs;
    }
    
    public long createInstance(Service service,Record entity)
    {    	
    	int initialAction=0;//固定为0?    	
    	//Service service = Service.getService(serviceType);
    	//if(service == null)
    	//{
    	//	Trace.write(Trace.Error, "Can not get service:id="+serviceType);
    	//	return 0;
    	//}    	    
    	
    	Operator op = AppContext.getCurrentOperator();
    	//Process data  = new Process(entity.getId(),serviceType,op.getId());
    	//data.setData(entity);
    	
    	java.util.HashMap<String,Object> inputs=new java.util.HashMap<String,Object>();
    	inputs.put("wfName", service.getName());
    	inputs.put("user", op);
    	inputs.put("data", entity);
    	inputs.put("service", service);
    	
    	boolean inTrans = db.inTransaction();
    	
    	try
    	{    		
    		if(!inTrans) db.transaction();
    		
    		//create osworkflow
    		long wfid = initialize(service.getName(),initialAction,inputs);    		
    		if(wfid<=0)
    		{
    			if(!inTrans) this.context.setRollbackOnly();
    			Trace.write(Trace.Error,"wfID = %d after initialize!" , wfid);    			
    			return 0;
    		}
    		
    		//get caption
    		/*PropertySet ps = this.getPropertySet(wfid);
    		String wfcaption = null;
    		if(ps != null)
    		{
    			wfcaption = ps.getString("caption");
    		}
    		
    		if(wfcaption==null || wfcaption.trim().length()<=0)
    		{
    			wfcaption = entity.toString();
    		}    		   
    		
    		db.sql("update wf_process set tid=?,creator=?,taskName=?,createtime=? where wfID=? and status=?",
    				service.getId(),op.getId(),wfcaption,
    				new Date(),//com.tern.util.DateTimeUtil.now("yyyy-MM-dd HH:mm:ss"),
    				wfid,
    				WorkflowEntry.ACTIVATED)
    		   .exec();*/
    		
    		if(!inTrans)  db.commit();
    		
    		Trace.write(Trace.Running, "Create workflow:service=%s,serviceID=%d,workflow id=%d",
    				service.getName(),entity.getId(),wfid);
    		
    		return wfid;
    	}
    	catch(Throwable e)
    	{
    		Trace.write(Trace.Error, e,"Create workflow faile:service=%s",service.getName());
    		this.context.setRollbackOnly();    		
    		return 0;
    	}   	    
    }    
    
    protected void populateTransientMap(WorkflowEntry entry, Map transientVars, List registers, Integer actionId, 
    		java.util.Collection currentSteps, PropertySet ps) throws WorkflowException
    {    	
    	//entry = new IAPWorkflowEntry(entry,transientVars);
    	((IAPWorkflowEntry)entry).assign(transientVars);
    	super.populateTransientMap(entry,transientVars,registers,actionId,currentSteps,ps);
    }
    
    /*public IAPWorkflowEntry findEntry(long id) throws WorkflowException
    {
    	WorkflowEntry entry = getPersistence().findEntry(id);
    	//Process data =  new Process(id);    	
    	return new IAPWorkflowEntry(entry,null,AppContext.getCurrentOperator());
    }*/
    
    public long initialize(String workflowName, int initialAction, Map inputs) 
                     throws InvalidRoleException, InvalidInputException, WorkflowException 
    {
    	boolean inTrans = db.inTransaction();
    	
    	if(!inTrans)
    	{
    		try
        	{
        		db.transaction();
        	}
        	catch(java.sql.SQLException e)
        	{
        		throw new WorkflowException(e);
        	}	
    	}   
    	
    	long wfid;
    	try
    	{
    		wfid = super.initialize(workflowName,initialAction,getInputs(inputs,0));     
    	}
    	catch(Throwable t)
    	{
    		this.context.setRollbackOnly();
    		Trace.write(Trace.Error, t , "initialize:");
    		throw new WorkflowException(t);
    	}       
        
        //if(wfid>0)
        //{
        	//写任务表
        	//context.setRollbackOnly();
        //}
        
        if(!inTrans)
    	{
    		db.commit();
    	}
        
        return wfid;
    }
    
    public void doAction(long id, int actionId, Map inputs) throws WorkflowException
    {
    	boolean inTrans = db.inTransaction();
    	
    	if(!inTrans)
    	{
    		try
        	{
        		db.transaction();
        	}
        	catch(java.sql.SQLException e)
        	{
        		throw new WorkflowException(e);
        	}	
    	}    	
    	
    	try
    	{
    		super.doAction(id, actionId, getInputs(inputs,id));
    	}
    	catch(Throwable t)
    	{
    		this.context.setRollbackOnly();
    		Trace.write(Trace.Error, t , "doAction:");
    		throw new WorkflowException(t);
    	} 
    	
    	
    	if(!inTrans)
    	{
    		db.commit();
    	}
    }
    
    public void changeEntryState(long id, int newState) throws WorkflowException
    {
    	boolean inTrans = db.inTransaction();
    	
    	if(!inTrans)
    	{
    		try
        	{
        		db.transaction();
        	}
        	catch(java.sql.SQLException e)
        	{
        		throw new WorkflowException(e);
        	}	
    	}    

    	try
    	{
    		super.changeEntryState(id, newState);
    	}
    	catch(Throwable t)
    	{
    		this.context.setRollbackOnly();
    		Trace.write(Trace.Error, t , "changeEntryState:");
    		throw new WorkflowException(t);
    	} 
        

        if(!inTrans)
    	{
    		db.commit();
    	}
    }
    
    public int[] getAvailableActions(long id, Map inputs)
    {
    	return super.getAvailableActions(id,getInputs(inputs,id));
    }
    
    public List getSecurityPermissions(long id, Map inputs)
    {
    	return super.getSecurityPermissions(id,getInputs(inputs,id));
    }
    
    public StepDescriptor getNextStep(long id,int actionId,Map inputs)
    {    	    	
    	try
    	{
    		db.transaction();
    		
    		WorkflowStore store = getPersistence();
            WorkflowEntry entry = store.findEntry(id);

            if (entry.getState() != WorkflowEntry.ACTIVATED) 
            {
                return null;
            }

            WorkflowDescriptor wf = getConfiguration().getWorkflow(entry.getWorkflowName());

            List currentSteps = store.findCurrentSteps(id);
            ActionDescriptor action = null;

            PropertySet ps = store.getPropertySet(id);
            Map transientVars = new HashMap();

            if (inputs != null)
            {
                transientVars.putAll(inputs);
            }

            populateTransientMap(entry, transientVars, wf.getRegisters(), new Integer(actionId), currentSteps, ps);

            //boolean validAction = false;
            Step step = null;
            for (Iterator iter = currentSteps.iterator();iter.hasNext();)
            {
            	Step _step = (Step) iter.next();
                StepDescriptor s = wf.getStep( _step.getStepId());
                ActionDescriptor actionDesc = s.getAction(actionId);
                if(actionDesc != null)
                {
                	action = actionDesc;
                	step = _step;
                	break;
                }                
            }

            if(null == action)
            {
            	//check global actions
                for (Iterator gIter = wf.getGlobalActions().iterator();gIter.hasNext();) 
                {
                    ActionDescriptor actionDesc = (ActionDescriptor) gIter.next();

                    if (actionDesc.getId() == actionId) 
                    {
                        action = actionDesc;

                        //if (isActionAvailable(action, transientVars, ps, 0))
                        //{
                        //    validAction = true;
                        //}
                        break;
                    }
                }
            }                       

            if (null == action) 
            {
                throw new InvalidActionException("Action " + actionId + " does not exists!");
            }
            
            if (action.isFinish())
            {
            	return null;
            }
            
            //post、pre-functions??---do not execute those functions.
            //check each conditional result
            List conditionalResults = action.getConditionalResults();
            ResultDescriptor[] theResults = new ResultDescriptor[1];
            for (Iterator iterator = conditionalResults.iterator();iterator.hasNext();) 
            {
                ConditionalResultDescriptor conditionalResult = (ConditionalResultDescriptor) iterator.next();

                if (passesConditions(null, conditionalResult.getConditions(),
                		Collections.unmodifiableMap(transientVars), 
                		ps, (step != null) ? step.getStepId() : (-1))) 
                {
                    //if (evaluateExpression(conditionalResult.getCondition(), entry, wf.getRegisters(), null, transientVars)) {
                    theResults[0] = conditionalResult;

                    //if (conditionalResult.getValidators().size() > 0) 
                    //{
                    //    verifyInputs(entry, conditionalResult.getValidators(), Collections.unmodifiableMap(transientVars), ps);
                    //}

                    break;
                }
            }

            // use unconditional-result if a condition hasn't been met
            if (theResults[0] == null)
            {
                theResults[0] = action.getUnconditionalResult();
                //verifyInputs(entry, theResults[0].getValidators(), Collections.unmodifiableMap(transientVars), ps);
            }
                       
            //split,join??
            if (theResults[0].getSplit() != 0)
            {
            	return null;
            }
            else if (theResults[0].getJoin() != 0)
            {
            	return null;
            }
            else
            {
            	return wf.getStep( theResults[0].getStep() );
            }
    	}
    	catch(Exception e)
    	{
    		return null;
    	}
    	finally
    	{
    		db.rollback();
    	}    	    	
    }
    
    public boolean canInitialize(String workflowName, int initialAction, Map inputs)
    {
    	return super.canInitialize(workflowName,initialAction,getInputs(inputs,0));
    }
    
    
    
    @Override
    public Configuration getConfiguration()
    {
    	Configuration config = IAPWorkflowConfig.INSTANCE;

        if (!config.isInitialized()) 
        {        	
            try 
            {            	
                config.load(null);
            }
            catch (Exception e)
            {
            	Trace.write(Trace.Error, e,"Error initialising configuration");
                return null;
            }            
        }

        return config;
    }
    
    static class IAPWorkflowConfig implements Configuration, java.io.Serializable
    {
		private static final long serialVersionUID = 7119975552346901506L;

		public static IAPWorkflowConfig INSTANCE = new IAPWorkflowConfig(); 
    	
		private Map<String,String> persistenceArgs = new java.util.HashMap<String,String>();
	    private String persistenceClass;
	    private WorkflowFactory factory;// = new URLWorkflowFactory();
	    private transient WorkflowStore store = null;
	    private VariableResolver variableResolver = new DefaultVariableResolver();
    	private boolean initialized;
    	
    	private boolean loadConfig(java.net.URL url)//  throws FactoryException
    	{
    		if(url == null)
    		{
    			String path = com.tern.util.config.getConfigurationPath()+"/process/config.xml";
    			try
    			{
    			    url = new java.net.URL("file:///"+ Convert.replaceAll(path,"\\","/") );
    			}
    			catch(Exception e)
    			{
    				return false;
    			}
    		}
    		
    		java.io.InputStream is = null;
    		try
    		{
				is = url.openStream();
			}
    		catch (IOException e)
    		{
			    return false;
			}
    		
    		if(is == null) return false;
    		
    		SAXReader reader = new SAXReader();
    		
    		try
    		{
				Document doc = reader.read(is);
				Element root = (Element)doc.selectSingleNode("osworkflow");
				if(root == null)
				{
					Trace.write(Trace.Error,"osworkflow config: no root node.");
					return false;
				}
				
				Element p = (Element)root.selectSingleNode("persistence");
				if(p != null)
				{
					persistenceClass = p.attributeValue("class");
				}
				
				Element resolver = (Element)root.selectSingleNode("resolver");
				if (resolver != null) 
				{
	                String resolverClass = resolver.attributeValue("class");

	                if (resolverClass != null)
	                {
	                    variableResolver = (VariableResolver) com.opensymphony.workflow.loader.ClassLoaderUtil.loadClass(resolverClass, getClass()).newInstance();
	                }
	            }
				
				for (Iterator i = root.elementIterator("property"); i.hasNext();) 
    	    	{
    	    		Element element = (Element) i.next();
    	    		persistenceArgs.put(element.attributeValue("key"),
    	    				element.attributeValue("value"));
    	    	}
				
				Element factoryElement = (Element)root.selectSingleNode("factory");
				if (factoryElement != null)
				{
	                String clazz = null;

	                try
	                {
	                    clazz = factoryElement.attributeValue("class");

	                    if (clazz == null) 
	                    {
	                        throw new FactoryException("factory does not specify a class attribute");
	                    }

	                    factory = (WorkflowFactory) com.opensymphony.workflow.loader.ClassLoaderUtil.loadClass(clazz, getClass()).newInstance();

	                    java.util.Properties properties = new java.util.Properties();
	                    for (Iterator i = factoryElement.elementIterator("property"); i.hasNext();) 
	                    {
	                    	Element element = (Element) i.next();
	                    	properties.setProperty(element.attributeValue("key"), 
	                    			element.attributeValue("value"));
	                    }

	                    factory.init(properties);
	                    factory.initDone();
	                } 
	                catch (FactoryException ex) 
	                {
	                    throw ex;
	                }
	                catch (Exception ex)
	                {
	                    throw new FactoryException("Error creating workflow factory " + clazz, ex);
	                }
	            }
	            	           
	            return true;
			}
    		catch (Exception e)
    		{			
				Trace.write(Trace.Error,e, "error configuration file for osworkflow.");
				//return false;
			}
    		
    		return false;
    	}
    	
    	@Override
    	public void load(java.net.URL url) throws FactoryException
    	{
    		if(!loadConfig(url))
    		{
    			//default    			
    		}
    		
    		if(persistenceClass==null)
    		{
    			persistenceClass = "com.tern.iap.workflow.WorkflowStore";
    		}
    		
    		if(!persistenceArgs.containsKey("datasource"))
    		{
    			String ds = com.tern.util.config.getString("workflow.datasource");
    			if(ds != null && ds.trim().length()>0)
    			{
    				//ds = "jdbc/default";
    				persistenceArgs.put("datasource", "jdbc/"+ds);
    			}    			
    		}
    		
    		setDefaultArg("entry.sequence","select max(wfID) + 1 from WF_PROCESS");
    		setDefaultArg("entry.table","WF_PROCESS");
    		setDefaultArg("entry.id","wfID");
    		setDefaultArg("entry.name","taskName");
    		setDefaultArg("entry.state","status");
    		setDefaultArg("step.sequence","select sum(c1) + 1 from (select 1 as tb, count(*) as c1 from os_currentstep union select 2 as tb, count(*) as c1 from os_historystep) as TabelaFinal");
    		setDefaultArg("history.table","wf_stepinfo");
    		setDefaultArg("current.table","wf_stepinfo");
    		setDefaultArg("historyPrev.table","wf_prestep");
    		setDefaultArg("currentPrev.table","wf_prestep");
    		setDefaultArg("step.id","stepID");
    		setDefaultArg("step.entryId","wfID");
    		setDefaultArg("step.stepId","wfstep");
    		setDefaultArg("step.actionId","actionid");
    		setDefaultArg("step.owner","OWNER");
    		
    		setDefaultArg("step.caller","CALLER");
    		setDefaultArg("step.startDate","sDate");
    		setDefaultArg("step.finishDate","hDate");
    		
    		setDefaultArg("step.dueDate","due_date");
    		setDefaultArg("step.status","astatus");
    		setDefaultArg("step.previousId","preStep");
    		
    		if(null == factory)
    		{
    			factory = new com.tern.iap.workflow.WorkflowFactory();
    			factory.initDone();
    		}
    		
    		initialized = true;
    	}
    	
    	private void setDefaultArg(String key,String defval)
    	{
    		if(!persistenceArgs.containsKey(key))
    		{
    			persistenceArgs.put(key, defval);
    		}
    	}
    	
    	public void setPersistence(String persistence) 
    	{
            persistenceClass = persistence;
        }
    	
    	@Override
    	public String getPersistence()
    	{
            return persistenceClass;
        }

		@Override
		public boolean isInitialized() 
		{			
			return initialized;
		}

		@Override
		public boolean isModifiable(String name)
		{
			return factory.isModifiable(name);
		}

		@Override
		public Map<?,?> getPersistenceArgs()
		{
			return persistenceArgs;
		}

		@Override
		public VariableResolver getVariableResolver()
		{
			return variableResolver;
		}

		@Override
		public WorkflowDescriptor getWorkflow(String name)
				throws FactoryException 
	    {			
			 WorkflowDescriptor workflow = factory.getWorkflow(name);

		     if (workflow == null)
		     {
		         throw new FactoryException("Unknown workflow name");
		     }

		     return workflow;
		}

		@Override
		public String[] getWorkflowNames() throws FactoryException
		{		
			return factory.getWorkflowNames();
		}

		@Override
		public WorkflowStore getWorkflowStore() throws StoreException
		{		
			if (store == null) 
			{
	            String clazz = getPersistence();

	            try {
	                store = (WorkflowStore) Class.forName(clazz).newInstance();
	            } catch (Exception ex) {
	                throw new StoreException("Error creating store", ex);
	            }

	            store.init(getPersistenceArgs());
	        }

	        return store;
		}

		@Override
		public boolean removeWorkflow(String workflow) throws FactoryException
		{		
			return factory.removeWorkflow(workflow);
		}

		@Override
		public boolean saveWorkflow(String name, WorkflowDescriptor descriptor,
				boolean replace) throws FactoryException 
		{		
			return factory.saveWorkflow(name, descriptor, replace);
		}
    }
}
