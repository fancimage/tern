package com.tern.iap.workflow;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.module.propertyset.PropertySetManager;

import com.opensymphony.workflow.Condition;
import com.opensymphony.workflow.FactoryException;
import com.opensymphony.workflow.FunctionProvider;
import com.opensymphony.workflow.InvalidActionException;
import com.opensymphony.workflow.InvalidEntryStateException;
import com.opensymphony.workflow.InvalidInputException;
import com.opensymphony.workflow.InvalidRoleException;
import com.opensymphony.workflow.JoinNodes;
import com.opensymphony.workflow.Register;
import com.opensymphony.workflow.StoreException;
import com.opensymphony.workflow.TypeResolver;
import com.opensymphony.workflow.Validator;
import com.opensymphony.workflow.WorkflowContext;
import com.opensymphony.workflow.WorkflowException;
import com.opensymphony.workflow.config.Configuration;
import com.opensymphony.workflow.loader.*;
import com.opensymphony.workflow.query.WorkflowExpressionQuery;
import com.opensymphony.workflow.query.WorkflowQuery;
import com.opensymphony.workflow.spi.*;
import com.opensymphony.workflow.util.DefaultVariableResolver;
import com.opensymphony.workflow.util.VariableResolver;
import com.tern.dao.Record;
import com.tern.db.db;
import com.tern.iap.AppContext;
import com.tern.iap.Operator;
import com.tern.util.Convert;
import com.tern.util.Trace;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class Workflow implements com.opensymphony.workflow.Workflow
{
    private static final Workflow instance = new Workflow();	
    private static final WorkflowPermission wfPermission = new DefaultWorkflowPermission();
    
	public static Workflow getInstance()
	{
		return instance;
	}
	
	private static class MyContext implements WorkflowContext
	{
	    public MyContext() 
	    {
	    }

	    public String getCaller() 
	    {
	    	Operator op = Operator.current();
	        return op==null?null:String.valueOf(op.getId());
	    }
		
		public void setRollbackOnly()
		{
			if(db.inTransaction())
			{
				db.rollback();
			}
		}
	}
	
    protected WorkflowContext context;
    private Configuration configuration;
    private ThreadLocal stateCache = new ThreadLocal();
    private TypeResolver typeResolver;

    public Workflow() 
    {
        stateCache.set(new HashMap());
        context = new MyContext();
    }

    public int[] getAvailableActions(long id) 
    {
        return getAvailableActions(id, new HashMap());
    }

    public int[] getAvailableActions(long id, Map inputs) 
    {
    	inputs = getInputs(inputs);
    	
        try 
        {
            WorkflowStore store = getPersistence();
            IAPWorkflowEntry entry = (IAPWorkflowEntry)store.findEntry(id,inputs);

            if (entry == null) 
            {
                throw new IllegalArgumentException("No such workflow id " + id);
            }

            if (entry.getState() != WorkflowEntry.ACTIVATED) 
            {
                return new int[0];
            }

            WorkflowDescriptor wf = getConfiguration().getWorkflow(entry.getWorkflowName());

            if (wf == null) 
            {
                throw new IllegalArgumentException("No such workflow " + entry.getWorkflowName());
            }

            List l = new ArrayList();
            PropertySet ps = store.getPropertySet(id);
            Map transientVars = (inputs == null) ? new HashMap() : new HashMap(inputs);
            Collection currentSteps = store.findCurrentSteps(id);

            populateTransientMap(entry, transientVars, wf.getRegisters(), new Integer(0), currentSteps, ps);

            // get global actions
            List globalActions = wf.getGlobalActions();

            for (Iterator iterator = globalActions.iterator();
                    iterator.hasNext();) 
            {
                ActionDescriptor action = (ActionDescriptor) iterator.next();
                RestrictionDescriptor restriction = action.getRestriction();
                ConditionsDescriptor conditions = null;

                transientVars.put("actionId", new Integer(action.getId()));

                if (restriction != null) {
                    conditions = restriction.getConditionsDescriptor();
                }

                //todo verify that 0 is the right currentStepId
                if (passesConditions(wf.getGlobalConditions(), transientVars, ps, 0) && passesConditions(conditions, transientVars, ps, 0)) 
                {
                    l.add(new Integer(action.getId()));
                }
            }

            // get normal actions
            for (Iterator iterator = currentSteps.iterator();
                    iterator.hasNext();) 
            {
                Step step = (Step) iterator.next();
                if(getPermissionHandler(wf).hasRight(entry, wf.getStep(step.getStepId()).getMetaAttributes() ))
                {
                	l.addAll(getAvailableActionsForStep(wf, step, transientVars, ps));
                }                
            }

            int[] actions = new int[l.size()];

            for (int i = 0; i < actions.length; i++) {
                actions[i] = ((Integer) l.get(i)).intValue();
            }

            return actions;
        } 
        catch (Exception e) 
        {
            Trace.write(Trace.Error, e, "Error checking available actions");
            return new int[0];
        }
    }
    
    protected WorkflowPermission getPermissionHandler(WorkflowDescriptor wf)
    {
    	String cls = Convert.toString(wf.getMetaAttributes().get("permission.class"));
    	if(cls!=null && cls.length()>0)
    	{
    		/*从缓存中读取*/
    		
    		Class<?> clz = null;
			try 
			{
				clz = AppContext.current().loadClass("workflow."+cls);
			} 
			catch (ClassNotFoundException e1) 
			{
				Trace.write(Trace.Error, e1,"Load Permission Class[%s]!", cls);
				return null;
			}
			
    		if(!WorkflowPermission.class.isAssignableFrom(clz))
    		{
    			Trace.write(Trace.Error, "Permission Class[%s] does not exists!", cls);
    			return null;
    		}
    		
    		try 
    		{
				return (WorkflowPermission)clz.newInstance();
			} 
    		catch (Exception e) 
    		{
    			Trace.write(Trace.Error, e,"Load Permission Class[%s]!", cls);
				return null;
			}
    	}
    	return wfPermission;
    }

    /**
     * @ejb.interface-method
     */
    public void setConfiguration(Configuration configuration) 
    {
        this.configuration = configuration;
    }

    /**
     * @ejb.interface-method
     */
    public List getCurrentSteps(long id) 
    {
        try 
        {
            WorkflowStore store = getPersistence();
            return store.findCurrentSteps(id);
        } 
        catch (StoreException e)
        {
        	Trace.write(Trace.Error, e,"Error checking current steps for instance #" + id);
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * @ejb.interface-method
     */
    public int getEntryState(long id) 
    {
        try 
        {
            WorkflowStore store = getPersistence();
            return store.findEntry(id).getState();
        } 
        catch (StoreException e) 
        {
        	Trace.write(Trace.Error, e,"Error checking instance state for instance #" + id);
        }

        return WorkflowEntry.UNKNOWN;
    }

    /**
     * @ejb.interface-method
     */
    public List getHistorySteps(long id)
    {
        try 
        {
            WorkflowStore store = getPersistence();
            return store.findHistorySteps(id);
        } 
        catch (StoreException e) 
        {
        	Trace.write(Trace.Error, e,"Error getting history steps for instance #" + id);
        }

        return Collections.EMPTY_LIST;
    }

    /**
     * @ejb.interface-method
     */
    public Properties getPersistenceProperties() 
    {
        Properties p = new Properties();
        Iterator iter = getConfiguration().getPersistenceArgs().entrySet().iterator();

        while (iter.hasNext()) 
        {
            Map.Entry entry = (Map.Entry) iter.next();
            p.setProperty((String) entry.getKey(), (String) entry.getValue());
        }

        return p;
    }

    /**
     * Get the PropertySet for the specified workflow ID
     * @ejb.interface-method
     * @param id The workflow ID
     */
    public PropertySet getPropertySet(long id) 
    {
        PropertySet ps = null;

        try 
        {
            ps = getPersistence().getPropertySet(id);
        } 
        catch (StoreException e)
        {
        	Trace.write(Trace.Error, e,"Error getting propertyset for instance #" + id);
        }

        return ps;
    }

    public void setResolver(TypeResolver resolver) 
    {
        this.typeResolver = resolver;
    }

    public TypeResolver getResolver() 
    {
        if (typeResolver == null) 
        {
            typeResolver = TypeResolver.getResolver();
        }

        return typeResolver;
    }

    /**
     * @ejb.interface-method
     */
    public List getSecurityPermissions(long id) 
    {
        return getSecurityPermissions(id, null);
    }

    /**
     * @ejb.interface-method
     */
    public List getSecurityPermissions(long id, Map inputs)
    {
    	inputs = getInputs(inputs);
    	
        try 
        {
            WorkflowStore store = getPersistence();
            WorkflowEntry entry = store.findEntry(id,inputs);
            WorkflowDescriptor wf = getConfiguration().getWorkflow(entry.getWorkflowName());

            PropertySet ps = store.getPropertySet(id);
            Map transientVars = (inputs == null) ? new HashMap() : new HashMap(inputs);
            Collection currentSteps = store.findCurrentSteps(id);
            populateTransientMap(entry, transientVars, wf.getRegisters(), null, currentSteps, ps);

            List s = new ArrayList();

            for (Iterator interator = currentSteps.iterator();
                    interator.hasNext();)
            {
                Step step = (Step) interator.next();

                int stepId = step.getStepId();

                StepDescriptor xmlStep = wf.getStep(stepId);

                List securities = xmlStep.getPermissions();

                for (Iterator iterator2 = securities.iterator();
                        iterator2.hasNext();)
                {
                    PermissionDescriptor security = (PermissionDescriptor) iterator2.next();

                    // to have the permission, the condition must be met or not specified
                    // securities can't have restrictions based on inputs, so it's null
                    if (security.getRestriction() != null) 
                    {
                        if (passesConditions(security.getRestriction().getConditionsDescriptor(), transientVars, ps, xmlStep.getId())) 
                        {
                            s.add(security.getName());
                        }
                    }
                }
            }

            return s;
        } 
        catch (Exception e) 
        {
        	Trace.write(Trace.Error, e,"Error getting security permissions for instance #" + id);
        }

        return Collections.EMPTY_LIST;
    }

    /**
     * Returns a workflow definition object associated with the given name.
     *
     * @param workflowName the name of the workflow
     * @return the object graph that represents a workflow definition
     * @ejb.interface-method
     */
    public WorkflowDescriptor getWorkflowDescriptor(String workflowName) 
    {
        try 
        {
            return getConfiguration().getWorkflow(workflowName);
        } 
        catch (FactoryException e) 
        {
        	Trace.write(Trace.Error, e,"Error loading workflow " + workflowName);
        }

        return null;
    }

    /**
     * @ejb.interface-method
     */
    public String getWorkflowName(long id) 
    {
        try 
        {
            WorkflowStore store = getPersistence();
            WorkflowEntry entry = store.findEntry(id);

            if (entry != null) 
            {
                return entry.getWorkflowName();
            }
        } 
        catch (StoreException e) 
        {
        	Trace.write(Trace.Error, e,"Error getting instance name for instance #" + id);
        }

        return null;
    }

    /**
     * Get a list of workflow names available
     * @return String[] an array of workflow names.
     * @ejb.interface-method
     */
    public String[] getWorkflowNames() 
    {
        try 
        {
            return getConfiguration().getWorkflowNames();
        }
        catch (FactoryException e)
        {
        	Trace.write(Trace.Error, e,"Error getting workflow names");
        }

        return new String[0];
    }

    /**
     * @ejb.interface-method
     */
    public boolean canInitialize(String workflowName, int initialAction) 
    {
        return canInitialize(workflowName, initialAction, null);
    }

    /**
     * @ejb.interface-method
     * @param workflowName the name of the workflow to check
     * @param initialAction The initial action to check
     * @param inputs the inputs map
     * @return true if the workflow can be initialized
     */
    public boolean canInitialize(String workflowName, int initialAction, Map inputs) 
    {
        final String mockWorkflowName = workflowName;
        WorkflowEntry mockEntry = new WorkflowEntry() 
        {
            public long getId() 
            {
                return 0;
            }

            public String getWorkflowName() 
            {
                return mockWorkflowName;
            }

            public boolean isInitialized()
            {
                return false;
            }

            public int getState() 
            {
                return WorkflowEntry.CREATED;
            }
        };

        // since no state change happens here, a memory instance is just fine
        PropertySet ps = PropertySetManager.getInstance("memory", null);
        Map transientVars = new HashMap();

        inputs = getInputs(inputs);
        if (inputs != null) 
        {
            transientVars.putAll(inputs);
        }

        try 
        {
            populateTransientMap(mockEntry, transientVars, Collections.EMPTY_LIST, new Integer(initialAction), Collections.EMPTY_LIST, ps);
            return canInitialize(workflowName, initialAction, transientVars, ps);
        } 
        catch (InvalidActionException e) 
        {
        	Trace.write(Trace.Error, e.getMessage());
            return false;
        } 
        catch (WorkflowException e) 
        {
        	Trace.write(Trace.Error, e,"Error checking canInitialize");
            return false;
        }
    }

    /**
     * @ejb.interface-method
     */
    public boolean canModifyEntryState(long id, int newState) 
    {
        try 
        {
            WorkflowStore store = getPersistence();
            WorkflowEntry entry = store.findEntry(id);
            int currentState = entry.getState();
            boolean result = false;

            switch (newState) 
            {
            case WorkflowEntry.COMPLETED:
                if (currentState == WorkflowEntry.ACTIVATED)
                {
                    result = true;
                }
                break;
            case WorkflowEntry.CREATED:
                result = false;
            case WorkflowEntry.ACTIVATED:
                if ((currentState == WorkflowEntry.CREATED) 
                		|| (currentState == WorkflowEntry.SUSPENDED))
                {
                    result = true;
                }
                break;
            case WorkflowEntry.SUSPENDED:
                if (currentState == WorkflowEntry.ACTIVATED) 
                {
                    result = true;
                }
                break;
            case WorkflowEntry.KILLED:
                if ((currentState == WorkflowEntry.CREATED) 
                		|| (currentState == WorkflowEntry.ACTIVATED) 
                		|| (currentState == WorkflowEntry.SUSPENDED)) 
                {
                    result = true;
                }
                break;
            default:
                result = false;
                break;
            }
            return result;
        } 
        catch (StoreException e)
        {
        	Trace.write(Trace.Error, e,"Error checking state modifiable for instance #" + id);
        }

        return false;
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
    		WorkflowStore store = getPersistence();
            WorkflowEntry entry = store.findEntry(id);

            if (entry.getState() == newState) 
            {
                return;
            }

            if (canModifyEntryState(id, newState))
            {
                if ((newState == WorkflowEntry.KILLED) || (newState == WorkflowEntry.COMPLETED)) 
                {
                    Collection currentSteps = getCurrentSteps(id);
                    if (currentSteps.size() > 0) 
                    {
                        completeEntry(null, id, currentSteps, newState);
                    }
                }

                store.setEntryState(id, newState);
            }
            else 
            {
                throw new InvalidEntryStateException("Can't transition workflow instance #" + id + ". Current state is " + entry.getState() + ", requested state is " + newState);
            }

            Trace.write(Trace.Information, entry.getId() + " : State is now : " + entry.getState());
    	}
    	catch(Throwable t)
    	{
    		this.context.setRollbackOnly();
    		Trace.write(Trace.Error, t , "changeEntryState:");
    		if(t instanceof WorkflowException) throw (WorkflowException)t; 
    		else throw new WorkflowException(t);
    	}
    	
    	if(!inTrans)
    	{
    		db.commit();
    	}
    }

    public void doAction(long id, int actionId, Map inputs) throws WorkflowException 
    {
    	inputs = getInputs(inputs);
    	
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
    		WorkflowStore store = getPersistence();
            IAPWorkflowEntry entry = (IAPWorkflowEntry)store.findEntry(id,inputs);

            if (entry.getState() != WorkflowEntry.ACTIVATED) 
            {
                return;
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

            boolean validAction = false;

            //check global actions
            for (Iterator gIter = wf.getGlobalActions().iterator();
                    !validAction && gIter.hasNext();) 
            {
                ActionDescriptor actionDesc = (ActionDescriptor) gIter.next();

                if (actionDesc.getId() == actionId) 
                {
                    action = actionDesc;

                    if (isActionAvailable(action, transientVars, ps, 0)) 
                    {
                        validAction = true;
                    }
                }
            }

            for (Iterator iter = currentSteps.iterator();
                    !validAction && iter.hasNext();) 
            {
                Step step = (Step) iter.next();
                StepDescriptor s = wf.getStep(step.getStepId());

                for (Iterator iterator = s.getActions().iterator();
                        !validAction && iterator.hasNext();) 
                {
                    ActionDescriptor actionDesc = (ActionDescriptor) iterator.next();

                    if (actionDesc.getId() == actionId) 
                    {
                        action = actionDesc;

                        if (isActionAvailable(action, transientVars, ps, s.getId()))
                        {
                        	/*判断对步骤是否有权限*/                        	
                            //validAction = true;
                        	validAction = getPermissionHandler(wf).hasRight(entry,s.getMetaAttributes());
                        }
                    }
                }
            }

            if (!validAction) 
            {
                throw new InvalidActionException("Action " + actionId + " is invalid");
            }

            //transition the workflow, if it wasn't explicitly finished, check for an implicit finish
            if (!transitionWorkflow(entry, currentSteps, store, wf, action, transientVars, inputs, ps)) 
            {
                checkImplicitFinish(action, id);
            }                        
    	}
    	catch(Throwable t)
    	{
    		this.context.setRollbackOnly();
    		Trace.write(Trace.Error, t , "doAction:");
    		if(t instanceof WorkflowException) throw (WorkflowException)t; 
    		else throw new WorkflowException(t);
    	} 
    	
    	if(!inTrans)
    	{
    		db.commit();
    	}
    }

    public void executeTriggerFunction(long id, int triggerId) throws WorkflowException 
    {
        WorkflowStore store = getPersistence();
        WorkflowEntry entry = store.findEntry(id);

        if (entry == null)
        {
        	Trace.write(Trace.Warning,"Cannot execute trigger #" + triggerId + " on non-existent workflow id#" + id);
            return;
        }

        WorkflowDescriptor wf = getConfiguration().getWorkflow(entry.getWorkflowName());

        PropertySet ps = store.getPropertySet(id);
        Map transientVars = new HashMap();
        populateTransientMap(entry, transientVars, wf.getRegisters(), null, store.findCurrentSteps(id), ps);
        executeFunction(wf.getTriggerFunction(triggerId), transientVars, ps);
    }
    
    protected Map getInputs(Map<String,Object> inputs)
    {
    	if(inputs == null)
    	{
    		inputs = new java.util.HashMap<String,Object>();
    	}
    	 
        Operator op = Operator.current();
    	inputs.put("caller", op.getId());
    	inputs.put("user", op);
    	
    	return inputs;
    }
    
    public long createInstance(Service service,Record entity)
    {    	
    	int initialAction=0;
    	//Operator op = Operator.current();
    	
    	Map inputs = getInputs(null);
    	inputs.put("wfName", service.getName());
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
    		
    		entity.save();
    		
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
    	
    	long entryId = 0;
    	try
    	{
    		WorkflowDescriptor wf = getConfiguration().getWorkflow(workflowName);

            WorkflowStore store = getPersistence();
            WorkflowEntry entry = store.createEntry(workflowName,inputs);

            // start with a memory property set, but clone it after we have an ID
            PropertySet ps = store.getPropertySet(entry.getId());
            Map transientVars = new HashMap();

            if (inputs != null)
            {
                transientVars.putAll(inputs);
            }

            populateTransientMap(entry, transientVars, wf.getRegisters(), new Integer(initialAction), Collections.EMPTY_LIST, ps);

            if (!canInitialize(workflowName, initialAction, transientVars, ps))
            {
                context.setRollbackOnly();
                throw new InvalidRoleException("You are restricted from initializing this workflow");
            }

            ActionDescriptor action = wf.getInitialAction(initialAction);

            transitionWorkflow(entry, Collections.EMPTY_LIST, store, wf, action, transientVars, inputs, ps);
            entryId = entry.getId();            
    	}
    	catch(Throwable t)
    	{
    		this.context.setRollbackOnly();
    		Trace.write(Trace.Error, t , "initialize:");
    		if(t instanceof WorkflowException) throw (WorkflowException)t; 
    		else throw new WorkflowException(t);
    	}
    	
    	if(!inTrans)
    	{
    		db.commit();
    	}        
    	
    	// now clone the memory PS to the real PS
        //PropertySetManager.clone(ps, store.getPropertySet(entryId));
        return entryId;
    }

    /**
     * @ejb.interface-method
     */
    public List query(WorkflowQuery query) throws StoreException
    {
        return getPersistence().query(query);
    }

    /**
     * @ejb.interface-method
     */
    public List query(WorkflowExpressionQuery query) throws WorkflowException
    {
        return getPersistence().query(query);
    }

    /**
     * @ejb.interface-method
     */
    public boolean removeWorkflowDescriptor(String workflowName) throws FactoryException 
    {
        return getConfiguration().removeWorkflow(workflowName);
    }

    /**
     * @ejb.interface-method
     */
    public boolean saveWorkflowDescriptor(String workflowName, WorkflowDescriptor descriptor, boolean replace) 
    		throws FactoryException 
    {
        boolean success = getConfiguration().saveWorkflow(workflowName, descriptor, replace);
        return success;
    }

    protected List getAvailableActionsForStep(WorkflowDescriptor wf, Step step, Map transientVars, PropertySet ps)
    		throws WorkflowException 
    {
        List l = new ArrayList();
        StepDescriptor s = wf.getStep(step.getStepId());

        if (s == null) 
        {
        	Trace.write(Trace.Warning,"getAvailableActionsForStep called for non-existent step Id #" + step.getStepId());

            return l;
        }

        List actions = s.getActions();

        if ((actions == null) || (actions.size() == 0))
        {
            return l;
        }

        for (Iterator iterator2 = actions.iterator(); iterator2.hasNext();) 
        {
            ActionDescriptor action = (ActionDescriptor) iterator2.next();
            RestrictionDescriptor restriction = action.getRestriction();
            ConditionsDescriptor conditions = null;

            transientVars.put("actionId", new Integer(action.getId()));

            if (restriction != null) 
            {
                conditions = restriction.getConditionsDescriptor();
            }

            if (passesConditions(wf.getGlobalConditions(), new HashMap(transientVars), ps,
            		s.getId()) && passesConditions(conditions, new HashMap(transientVars), ps, s.getId()))
            {
                l.add(new Integer(action.getId()));
            }
        }

        return l;
    }

    protected int[] getAvailableAutoActions(long id, Map inputs) 
    {
        try 
        {
            WorkflowStore store = getPersistence();
            WorkflowEntry entry = store.findEntry(id,inputs);

            if (entry == null) 
            {
                throw new IllegalArgumentException("No such workflow id " + id);
            }

            if (entry.getState() != WorkflowEntry.ACTIVATED) 
            {
            	Trace.write(Trace.Information,"--> state is " + entry.getState());
                return new int[0];
            }

            WorkflowDescriptor wf = getConfiguration().getWorkflow(entry.getWorkflowName());

            if (wf == null)
            {
                throw new IllegalArgumentException("No such workflow " + entry.getWorkflowName());
            }

            List l = new ArrayList();
            PropertySet ps = store.getPropertySet(id);
            Map transientVars = (inputs == null) ? new HashMap() : new HashMap(inputs);
            Collection currentSteps = store.findCurrentSteps(id);

            populateTransientMap(entry, transientVars, wf.getRegisters(), new Integer(0), currentSteps, ps);

            // get global actions
            List globalActions = wf.getGlobalActions();

            for (Iterator iterator = globalActions.iterator();
                    iterator.hasNext();) 
            {
                ActionDescriptor action = (ActionDescriptor) iterator.next();

                transientVars.put("actionId", new Integer(action.getId()));

                if (action.getAutoExecute()) 
                {
                    if (isActionAvailable(action, transientVars, ps, 0)) 
                    {
                        l.add(new Integer(action.getId()));
                    }
                }
            }

            // get normal actions
            for (Iterator iterator = currentSteps.iterator();
                    iterator.hasNext();) 
            {
                Step step = (Step) iterator.next();
                l.addAll(getAvailableAutoActionsForStep(wf, step, transientVars, ps));
            }

            int[] actions = new int[l.size()];

            for (int i = 0; i < actions.length; i++) 
            {
                actions[i] = ((Integer) l.get(i)).intValue();
            }

            return actions;
        } 
        catch (Exception e) 
        {
        	Trace.write(Trace.Error, e,"Error checking available actions");
            return new int[0];
        }
    }

    /**
     * Get just auto action availables for a step
     */
    protected List getAvailableAutoActionsForStep(WorkflowDescriptor wf, Step step, Map transientVars, PropertySet ps) 
    		throws WorkflowException 
    {
        List l = new ArrayList();
        StepDescriptor s = wf.getStep(step.getStepId());

        if (s == null)
        {
        	Trace.write(Trace.Warning,"getAvailableAutoActionsForStep called for non-existent step Id #" + step.getStepId());
            return l;
        }

        List actions = s.getActions();

        if ((actions == null) || (actions.size() == 0)) 
        {
            return l;
        }

        for (Iterator iterator2 = actions.iterator(); iterator2.hasNext();) 
        {
            ActionDescriptor action = (ActionDescriptor) iterator2.next();

            transientVars.put("actionId", new Integer(action.getId()));

            //check auto
            if (action.getAutoExecute()) 
            {
                if (isActionAvailable(action, transientVars, ps, s.getId())) 
                {
                    l.add(new Integer(action.getId()));
                }
            }
        }

        return l;
    }

    protected WorkflowStore getPersistence() throws StoreException 
    {
        return (WorkflowStore)getConfiguration().getWorkflowStore();
    }

    protected void checkImplicitFinish(ActionDescriptor action, long id) throws WorkflowException 
    {
        WorkflowStore store = getPersistence();
        WorkflowEntry entry = store.findEntry(id);

        WorkflowDescriptor wf = getConfiguration().getWorkflow(entry.getWorkflowName());

        Collection currentSteps = store.findCurrentSteps(id);

        boolean isCompleted = true;

        for (Iterator iterator = currentSteps.iterator(); iterator.hasNext();) 
        {
            Step step = (Step) iterator.next();
            StepDescriptor stepDes = wf.getStep(step.getStepId());

            // if at least on current step have an available action
            if (stepDes.getActions().size() > 0) 
            {
                isCompleted = false;
            }
        }

        if (isCompleted) 
        {
            completeEntry(action, id, currentSteps, WorkflowEntry.COMPLETED);
        }
    }

    /**
     * Mark the specified entry as completed, and move all current steps to history.
     */
    protected void completeEntry(ActionDescriptor action, long id, Collection currentSteps, int state) 
    		throws StoreException 
    {
        getPersistence().setEntryState(id, state);

        Iterator i = new ArrayList(currentSteps).iterator();

        while (i.hasNext()) 
        {
            Step step = (Step) i.next();
            String oldStatus = (action != null) ? action.getUnconditionalResult().getOldStatus() : "Finished";
            getPersistence().markFinished(step, (action != null) ? action.getId() : (-1), new Date(), oldStatus, context.getCaller(),null);
            getPersistence().moveToHistory(step);
        }
    }

    /**
     * Executes a function.
     *
     * @param function the function to execute
     * @param transientVars the transientVars given by the end-user
     * @param ps the persistence variables
     */
    protected void executeFunction(FunctionDescriptor function, Map transientVars, PropertySet ps) 
    		throws WorkflowException 
    {
        if (function != null) 
        {
            String type = function.getType();

            Map args = new HashMap(function.getArgs());

            for (Iterator iterator = args.entrySet().iterator();
                    iterator.hasNext();)
            {
                Map.Entry mapEntry = (Map.Entry) iterator.next();
                mapEntry.setValue(getConfiguration().getVariableResolver().translateVariables((String) mapEntry.getValue(), transientVars, ps));
            }

            FunctionProvider provider = getResolver().getFunction(type, args);

            if (provider == null)
            {
                String message = "Could not load FunctionProvider class";
                context.setRollbackOnly();
                throw new WorkflowException(message);
            }

            try 
            {
                provider.execute(transientVars, args, ps);
            } 
            catch (WorkflowException e)
            {
                context.setRollbackOnly();
                throw e;
            }
        }
    }

    protected boolean passesCondition(ConditionDescriptor conditionDesc, Map transientVars, PropertySet ps, int currentStepId) 
    		throws WorkflowException 
    {
        String type = conditionDesc.getType();

        Map args = new HashMap(conditionDesc.getArgs());

        for (Iterator iterator = args.entrySet().iterator();
                iterator.hasNext();) 
        {
            Map.Entry mapEntry = (Map.Entry) iterator.next();
            mapEntry.setValue(getConfiguration().getVariableResolver().translateVariables((String) mapEntry.getValue(), transientVars, ps));
        }

        if (currentStepId != -1) 
        {
            Object stepId = args.get("stepId");

            if ((stepId != null) && stepId.equals("-1"))
            {
                args.put("stepId", String.valueOf(currentStepId));
            }
        }

        Condition condition = getResolver().getCondition(type, args);

        if (condition == null)
        {
            context.setRollbackOnly();
            throw new WorkflowException("Could not load condition");
        }

        try 
        {
            boolean passed = condition.passesCondition(transientVars, args, ps);

            if (conditionDesc.isNegate()) 
            {
                passed = !passed;
            }

            return passed;
        } 
        catch (Exception e) 
        {
            context.setRollbackOnly();

            if (e instanceof WorkflowException) 
            {
                throw (WorkflowException) e;
            }

            throw new WorkflowException("Unknown exception encountered when checking condition " + condition, e);
        }
    }

    protected boolean passesConditions(String conditionType, List conditions, Map transientVars, PropertySet ps, int currentStepId) 
    		throws WorkflowException 
    {
        if ((conditions == null) || (conditions.size() == 0)) 
        {
            return true;
        }

        boolean and = "AND".equals(conditionType);
        boolean or = !and;

        for (Iterator iterator = conditions.iterator(); iterator.hasNext();) 
        {
            AbstractDescriptor descriptor = (AbstractDescriptor) iterator.next();
            boolean result;

            if (descriptor instanceof ConditionsDescriptor) 
            {
                ConditionsDescriptor conditionsDescriptor = (ConditionsDescriptor) descriptor;
                result = passesConditions(conditionsDescriptor.getType(), conditionsDescriptor.getConditions(), transientVars, ps, currentStepId);
            } 
            else 
            {
                result = passesCondition((ConditionDescriptor) descriptor, transientVars, ps, currentStepId);
            }

            if (and && !result) 
            {
                return false;
            }
            else if (or && result) 
            {
                return true;
            }
        }

        if (and)
        {
            return true;
        } 
        else if (or)
        {
            return false;
        }
        else 
        {
            return false;
        }
    }

    protected boolean passesConditions(ConditionsDescriptor descriptor, Map transientVars, PropertySet ps, int currentStepId) throws WorkflowException
    {
        if (descriptor == null)
        {
            return true;
        }

        return passesConditions(descriptor.getType(), descriptor.getConditions(), transientVars, ps, currentStepId);
    }

    protected void populateTransientMap(WorkflowEntry entry, Map transientVars, List registers, Integer actionId, Collection currentSteps, PropertySet ps)
    		throws WorkflowException
    {
        transientVars.put("context", context);
        transientVars.put("entry", entry);
        transientVars.put("store", getPersistence());
        transientVars.put("configuration", getConfiguration());
        transientVars.put("descriptor", getConfiguration().getWorkflow(entry.getWorkflowName()));
        
        transientVars.remove("data");
		transientVars.remove("user");
		transientVars.remove("service");
		//transientVars.remove("suggest");

        if (actionId != null)
        {
            transientVars.put("actionId", actionId);
        }

        transientVars.put("currentSteps", new ArrayList(currentSteps));

        // now talk to the registers for any extra objects needed in scope
        for (Iterator iterator = registers.iterator(); iterator.hasNext();)
        {
            RegisterDescriptor register = (RegisterDescriptor) iterator.next();
            Map args = register.getArgs();

            String type = register.getType();
            Register r = getResolver().getRegister(type, args);

            if (r == null) {
                String message = "Could not load register class";
                context.setRollbackOnly();
                throw new WorkflowException(message);
            }

            try 
            {
                transientVars.put(register.getVariableName(), r.registerVariable(context, entry, args, ps));
            } 
            catch (Exception e)
            {
                context.setRollbackOnly();

                if (e instanceof WorkflowException) 
                {
                    throw (WorkflowException) e;
                }

                throw new WorkflowException("An unknown exception occured while registering variable using register " + r, e);
            }
        }
    }

    /**
     * @return true if the instance has been explicitly completed is this transition, false otherwise
     * @throws WorkflowException
     */
    protected boolean transitionWorkflow(WorkflowEntry entry, List currentSteps, WorkflowStore store, WorkflowDescriptor wf, ActionDescriptor action, Map transientVars, Map inputs, PropertySet ps) 
    		throws WorkflowException 
    {
        Map cache = (Map) stateCache.get();

        if (cache != null) 
        {
            cache.clear();
        } 
        else 
        {
            stateCache.set(new HashMap());
        }

        Step step = getCurrentStep(wf, action.getId(), currentSteps, transientVars, ps);

        if (action.getValidators().size() > 0) 
        {
            verifyInputs(entry, action.getValidators(), Collections.unmodifiableMap(transientVars), ps);
        }

        //we're leaving the current step, so let's execute its post-functions
        //check if we actually have a current step
        if (step != null) 
        {
            List stepPostFunctions = wf.getStep(step.getStepId()).getPostFunctions();

            for (Iterator iterator = stepPostFunctions.iterator();
                    iterator.hasNext();) 
            {
                FunctionDescriptor function = (FunctionDescriptor) iterator.next();
                executeFunction(function, transientVars, ps);
            }
        }

        // preFunctions
        List preFunctions = action.getPreFunctions();

        for (Iterator iterator = preFunctions.iterator(); iterator.hasNext();) 
        {
            FunctionDescriptor function = (FunctionDescriptor) iterator.next();
            executeFunction(function, transientVars, ps);
        }

        // check each conditional result
        List conditionalResults = action.getConditionalResults();
        List extraPreFunctions = null;
        List extraPostFunctions = null;
        ResultDescriptor[] theResults = new ResultDescriptor[1];

        for (Iterator iterator = conditionalResults.iterator();
                iterator.hasNext();) 
        {
            ConditionalResultDescriptor conditionalResult = (ConditionalResultDescriptor) iterator.next();

            if (passesConditions(null, conditionalResult.getConditions(), Collections.unmodifiableMap(transientVars), ps, (step != null) ? step.getStepId() : (-1))) 
            {
                //if (evaluateExpression(conditionalResult.getCondition(), entry, wf.getRegisters(), null, transientVars)) {
                theResults[0] = conditionalResult;

                if (conditionalResult.getValidators().size() > 0) 
                {
                    verifyInputs(entry, conditionalResult.getValidators(), Collections.unmodifiableMap(transientVars), ps);
                }

                extraPreFunctions = conditionalResult.getPreFunctions();
                extraPostFunctions = conditionalResult.getPostFunctions();

                break;
            }
        }

        // use unconditional-result if a condition hasn't been met
        if (theResults[0] == null) 
        {
            theResults[0] = action.getUnconditionalResult();
            verifyInputs(entry, theResults[0].getValidators(), Collections.unmodifiableMap(transientVars), ps);
            extraPreFunctions = theResults[0].getPreFunctions();
            extraPostFunctions = theResults[0].getPostFunctions();
        }

        Trace.write(Trace.Information, "theResult=" + theResults[0].getStep() + ' ' + theResults[0].getStatus());        

        if ((extraPreFunctions != null) && (extraPreFunctions.size() > 0)) 
        {
            // run any extra pre-functions that haven't been run already
            for (Iterator iterator = extraPreFunctions.iterator();
                    iterator.hasNext();) 
            {
                FunctionDescriptor function = (FunctionDescriptor) iterator.next();
                executeFunction(function, transientVars, ps);
            }
        }

        // go to next step
        if (theResults[0].getSplit() != 0) 
        {
            // the result is a split request, handle it correctly
            SplitDescriptor splitDesc = wf.getSplit(theResults[0].getSplit());
            Collection results = splitDesc.getResults();
            List splitPreFunctions = new ArrayList();
            List splitPostFunctions = new ArrayList();

            //check all results in the split and verify the input against any validators specified
            //also build up all the pre and post functions that should be called.
            for (Iterator iterator = results.iterator(); iterator.hasNext();)
            {
                ResultDescriptor resultDescriptor = (ResultDescriptor) iterator.next();

                if (resultDescriptor.getValidators().size() > 0) 
                {
                    verifyInputs(entry, resultDescriptor.getValidators(), Collections.unmodifiableMap(transientVars), ps);
                }

                splitPreFunctions.addAll(resultDescriptor.getPreFunctions());
                splitPostFunctions.addAll(resultDescriptor.getPostFunctions());
            }

            // now execute the pre-functions
            for (Iterator iterator = splitPreFunctions.iterator();
                    iterator.hasNext();) 
            {
                FunctionDescriptor function = (FunctionDescriptor) iterator.next();
                executeFunction(function, transientVars, ps);
            }

            if (!action.isFinish()) 
            {
                // now make these steps...
                boolean moveFirst = true;

                theResults = new ResultDescriptor[results.size()];
                results.toArray(theResults);

                for (Iterator iterator = results.iterator();
                        iterator.hasNext();) 
                {
                    ResultDescriptor resultDescriptor = (ResultDescriptor) iterator.next();
                    Step moveToHistoryStep = null;

                    if (moveFirst) 
                    {
                        moveToHistoryStep = step;
                    }

                    long[] previousIds = null;

                    if (step != null) 
                    {
                        previousIds = new long[] {step.getId()};
                    }

                    createNewCurrentStep(resultDescriptor, entry, store, action.getId(), moveToHistoryStep, previousIds, transientVars, ps);
                    moveFirst = false;
                }
            }

            // now execute the post-functions
            for (Iterator iterator = splitPostFunctions.iterator();
                    iterator.hasNext();) 
            {
                FunctionDescriptor function = (FunctionDescriptor) iterator.next();
                executeFunction(function, transientVars, ps);
            }
        } 
        else if (theResults[0].getJoin() != 0)
        {
            // this is a join, finish this step...
            JoinDescriptor joinDesc = wf.getJoin(theResults[0].getJoin());
            step = store.markFinished(step, action.getId(), new Date(), theResults[0].getOldStatus(), context.getCaller(),inputs);
            store.moveToHistory(step);

            // ... now check to see if the expression evaluates
            // (get only current steps that have a result to this join)
            Collection joinSteps = new ArrayList();
            joinSteps.add(step);

            //currentSteps = store.findCurrentSteps(id); // shouldn't need to refresh the list
            for (Iterator iterator = currentSteps.iterator();
                    iterator.hasNext();) 
            {
                Step currentStep = (Step) iterator.next();

                if (currentStep.getId() != step.getId()) {
                    StepDescriptor stepDesc = wf.getStep(currentStep.getStepId());

                    if (stepDesc.resultsInJoin(theResults[0].getJoin())) {
                        joinSteps.add(currentStep);
                    }
                }
            }

            //we also need to check history steps that were finished before this one
            //that might be part of the join
            List historySteps = store.findHistorySteps(entry.getId());

            for (Iterator i = historySteps.iterator(); i.hasNext();)
            {
                Step historyStep = (Step) i.next();

                if (historyStep.getId() != step.getId())
                {
                    StepDescriptor stepDesc = wf.getStep(historyStep.getStepId());

                    if (stepDesc.resultsInJoin(theResults[0].getJoin())) 
                    {
                        joinSteps.add(historyStep);
                    }
                }
            }

            JoinNodes jn = new JoinNodes(joinSteps);
            transientVars.put("jn", jn);

            //todo verify that 0 is the right value for currentstep here
            if (passesConditions(null, joinDesc.getConditions(), Collections.unmodifiableMap(transientVars), ps, 0)) 
            {
                // move the rest without creating a new step ...
                ResultDescriptor joinresult = joinDesc.getResult();

                if (joinresult.getValidators().size() > 0)
                {
                    verifyInputs(entry, joinresult.getValidators(), Collections.unmodifiableMap(transientVars), ps);
                }

                // now execute the pre-functions
                for (Iterator iterator = joinresult.getPreFunctions().iterator();
                        iterator.hasNext();) 
                {
                    FunctionDescriptor function = (FunctionDescriptor) iterator.next();
                    executeFunction(function, transientVars, ps);
                }

                long[] previousIds = new long[joinSteps.size()];
                int i = 1;

                for (Iterator iterator = joinSteps.iterator();
                        iterator.hasNext();)
                {
                    Step currentStep = (Step) iterator.next();

                    if (currentStep.getId() != step.getId())
                    {
                        //if this is already a history step (eg, for all join steps completed prior to this one),
                        //we don't move it, since it's already history.
                        if (!historySteps.contains(currentStep)) 
                        {
                            store.moveToHistory(currentStep);
                        }

                        previousIds[i] = currentStep.getId();
                        i++;
                    }
                }

                if (!action.isFinish()) 
                {
                    // ... now finish this step normally
                    previousIds[0] = step.getId();
                    theResults[0] = joinDesc.getResult();

                    //we pass in null for the current step since we've already moved it to history above
                    createNewCurrentStep(joinDesc.getResult(), entry, store, action.getId(), null, previousIds, transientVars, ps);
                }

                // now execute the post-functions
                for (Iterator iterator = joinresult.getPostFunctions().iterator();
                        iterator.hasNext();) 
                {
                    FunctionDescriptor function = (FunctionDescriptor) iterator.next();
                    executeFunction(function, transientVars, ps);
                }
            }
        } 
        else 
        {
            // normal finish, no splits or joins
            long[] previousIds = null;

            if (step != null) {
                previousIds = new long[] {step.getId()};
            }

            if (!action.isFinish()) 
            {
                createNewCurrentStep(theResults[0], entry, store, action.getId(), step, previousIds, transientVars, ps);
            }
        }

        // postFunctions (BOTH)
        if (extraPostFunctions != null)
        {
            for (Iterator iterator = extraPostFunctions.iterator();
                    iterator.hasNext();) 
            {
                FunctionDescriptor function = (FunctionDescriptor) iterator.next();
                executeFunction(function, transientVars, ps);
            }
        }

        List postFunctions = action.getPostFunctions();

        for (Iterator iterator = postFunctions.iterator(); iterator.hasNext();)
        {
            FunctionDescriptor function = (FunctionDescriptor) iterator.next();
            executeFunction(function, transientVars, ps);
        }

        //if executed action was an initial action then workflow is activated
        if ((wf.getInitialAction(action.getId()) != null) && (entry.getState() != WorkflowEntry.ACTIVATED)) 
        {
            changeEntryState(entry.getId(), WorkflowEntry.ACTIVATED);
        }

        //if it's a finish action, then we halt
        if (action.isFinish())
        {
            completeEntry(action, entry.getId(), getCurrentSteps(entry.getId()), WorkflowEntry.COMPLETED);

            return true;
        }

        //get available autoexec actions
        int[] availableAutoActions = getAvailableAutoActions(entry.getId(), inputs);

        //we perform the first autoaction that applies, not all of them.
        if (availableAutoActions.length > 0) 
        {
            doAction(entry.getId(), availableAutoActions[0], inputs);
        }

        return false;
    }

    /**
     * Validates input against a list of ValidatorDescriptor objects.
     *
     * @param entry the workflow instance
     * @param validators the list of ValidatorDescriptors
     * @param transientVars the transientVars
     * @param ps the persistence variables
     * @throws InvalidInputException if the input is deemed invalid by any validator
     */
    protected void verifyInputs(WorkflowEntry entry, List validators, Map transientVars, PropertySet ps)
    		throws WorkflowException 
    {
        for (Iterator iterator = validators.iterator(); iterator.hasNext();)
        {
            ValidatorDescriptor input = (ValidatorDescriptor) iterator.next();

            if (input != null)
            {
                String type = input.getType();
                HashMap args = new HashMap(input.getArgs());

                for (Iterator iterator2 = args.entrySet().iterator();
                        iterator2.hasNext();)
                {
                    Map.Entry mapEntry = (Map.Entry) iterator2.next();
                    mapEntry.setValue(getConfiguration().getVariableResolver().translateVariables((String) mapEntry.getValue(), transientVars, ps));
                }

                Validator validator = getResolver().getValidator(type, args);

                if (validator == null) 
                {
                    String message = "Could not load validator class";
                    context.setRollbackOnly();
                    throw new WorkflowException(message);
                }

                try 
                {
                    validator.validate(transientVars, args, ps);
                }
                catch (InvalidInputException e)
                {
                    throw e;
                } 
                catch (Exception e) 
                {
                    context.setRollbackOnly();

                    if (e instanceof WorkflowException) 
                    {
                        throw (WorkflowException) e;
                    }

                    String message = "An unknown exception occured executing Validator " + validator;
                    throw new WorkflowException(message, e);
                }
            }
        }
    }

    /**
     * check if an action is available or not
     * @param action The action descriptor
     * @return true if the action is available
     */
    private boolean isActionAvailable(ActionDescriptor action, Map transientVars, PropertySet ps, int stepId) 
    		throws WorkflowException 
    {
        if (action == null) 
        {
            return false;
        }

        WorkflowDescriptor wf = getWorkflowDescriptorForAction(action);

        Map cache = (Map) stateCache.get();

        Boolean result = null;

        if (cache != null)
        {
            result = (Boolean) cache.get(action);
        } 
        else
        {
            cache = new HashMap();
            stateCache.set(cache);
        }

        if (result == null) 
        {
            RestrictionDescriptor restriction = action.getRestriction();
            ConditionsDescriptor conditions = null;

            if (restriction != null)
            {
                conditions = restriction.getConditionsDescriptor();
            }

            result = new Boolean(passesConditions(wf.getGlobalConditions(), new HashMap(transientVars), ps, stepId) && passesConditions(conditions, new HashMap(transientVars), ps, stepId));
            cache.put(action, result);
        }

        return result.booleanValue();
    }

    private Step getCurrentStep(WorkflowDescriptor wfDesc, int actionId, List currentSteps, Map transientVars, PropertySet ps)
    		throws WorkflowException 
    {
        if (currentSteps.size() == 1)
        {
            return (Step) currentSteps.get(0);
        }

        for (Iterator iterator = currentSteps.iterator(); iterator.hasNext();)
        {
            Step step = (Step) iterator.next();
            ActionDescriptor action = wfDesc.getStep(step.getStepId()).getAction(actionId);

            //$AR init
            if (isActionAvailable(action, transientVars, ps, step.getStepId())) 
            {
                return step;
            }

            //$AR end
        }

        return null;
    }

    private WorkflowDescriptor getWorkflowDescriptorForAction(ActionDescriptor action) 
    {
        AbstractDescriptor objWfd = action;

        while (!(objWfd instanceof WorkflowDescriptor)) 
        {
            objWfd = objWfd.getParent();
        }

        WorkflowDescriptor wf = (WorkflowDescriptor) objWfd;

        return wf;
    }

    private boolean canInitialize(String workflowName, int initialAction, Map transientVars, PropertySet ps) 
    		throws WorkflowException 
    {
        WorkflowDescriptor wf = getConfiguration().getWorkflow(workflowName);

        ActionDescriptor actionDescriptor = wf.getInitialAction(initialAction);

        if (actionDescriptor == null)
        {
            throw new InvalidActionException("Invalid Initial Action #" + initialAction);
        }

        RestrictionDescriptor restriction = actionDescriptor.getRestriction();
        ConditionsDescriptor conditions = null;

        if (restriction != null) 
        {
            conditions = restriction.getConditionsDescriptor();
        }
        
        /*是否有权限?*/
        IAPWorkflowEntry entry = (IAPWorkflowEntry)transientVars.get("entry");
        if(!getPermissionHandler(wf).hasRight(entry,actionDescriptor.getMetaAttributes()))
        {
        	Trace.write(Trace.Error, "workflow[%s] can not be initialize for no permission!", wf.getName());
        	return false;
        }      

        return passesConditions(conditions, new HashMap(transientVars), ps, 0);
    }

    private Step createNewCurrentStep(ResultDescriptor theResult, WorkflowEntry entry, WorkflowStore store, int actionId, Step currentStep, long[] previousIds, Map transientVars, PropertySet ps)
    		throws WorkflowException 
    {
        try 
        {
            int nextStep = theResult.getStep();

            if (nextStep == -1) 
            {
                if (currentStep != null)
                {
                    nextStep = currentStep.getStepId();
                } 
                else 
                {
                    throw new StoreException("Illegal argument: requested new current step same as current step, but current step not specified");
                }
            }

            Trace.write(Trace.Information, "Outcome: stepId=" + nextStep + ", status=" + theResult.getStatus() + ", owner=" + theResult.getOwner() + ", actionId=" + actionId + ", currentStep=" + ((currentStep != null) ? currentStep.getStepId() : 0));

            if (previousIds == null)
            {
                previousIds = new long[0];
            }

            String owner = theResult.getOwner();

            VariableResolver variableResolver = getConfiguration().getVariableResolver();

            if (owner != null) 
            {
                Object o = variableResolver.translateVariables(owner, transientVars, ps);
                owner = (o != null) ? o.toString() : null;
            }

            String oldStatus = theResult.getOldStatus();
            oldStatus = variableResolver.translateVariables(oldStatus, transientVars, ps).toString();

            String status = theResult.getStatus();
            status = variableResolver.translateVariables(status, transientVars, ps).toString();

            if (currentStep != null) 
            {
                store.markFinished(currentStep, actionId, new Date(), oldStatus, context.getCaller(),transientVars);
                store.moveToHistory(currentStep);

                //store.moveToHistory(actionId, new Date(), currentStep, oldStatus, context.getCaller());
            }

            // construct the start date and optional due date
            Date startDate = new Date();
            Date dueDate = null;

            if ((theResult.getDueDate() != null) && (theResult.getDueDate().length() > 0))
            {
                Object dueDateObject = variableResolver.translateVariables(theResult.getDueDate(), transientVars, ps);

                if (dueDateObject instanceof Date) 
                {
                    dueDate = (Date) dueDateObject;
                }
                else if (dueDateObject instanceof String) 
                {
                    long offset = 0;

                    try 
                    {
                        offset = Long.parseLong((String) dueDateObject);
                    }
                    catch (NumberFormatException e)
                    {
                    }

                    if (offset > 0) 
                    {
                        dueDate = new Date(startDate.getTime() + offset);
                    }
                } 
                else if (dueDateObject instanceof Number)
                {
                    Number num = (Number) dueDateObject;
                    long offset = num.longValue();

                    if (offset > 0) 
                    {
                        dueDate = new Date(startDate.getTime() + offset);
                    }
                }
            }    
            
            WorkflowDescriptor descriptor = (WorkflowDescriptor) transientVars.get("descriptor");
            StepDescriptor step = descriptor.getStep(nextStep);
            if (step == null) 
            {
                throw new WorkflowException("step #" + nextStep + " does not exist");
            }
            
            /*得到该步骤有权限处理的操作员*/
            getPermissionHandler(descriptor).initWorkflowOperators((IAPWorkflowEntry)entry, step.getMetaAttributes());

            Step newStep = store.createCurrentStep(step,entry.getId(), owner, startDate, dueDate, status, previousIds);
            
            transientVars.put("createdStep", newStep);

            if ((previousIds != null) && (previousIds.length == 0) && (currentStep == null))
            {
                // At this point, it must be a brand new workflow, so we'll overwrite the empty currentSteps
                // with an array of just this current step
                List currentSteps = new ArrayList();
                currentSteps.add(newStep);
                transientVars.put("currentSteps", new ArrayList(currentSteps));
            }
            
            List preFunctions = step.getPreFunctions();
            for (Iterator iterator = preFunctions.iterator();
                    iterator.hasNext();) 
            {
                FunctionDescriptor function = (FunctionDescriptor) iterator.next();
                executeFunction(function, transientVars, ps);
            }

            return newStep;
        } 
        catch (WorkflowException e) 
        {
            context.setRollbackOnly();
            throw e;
        }
    }
    
    /*
     * try to get the id of next step
     * */
    public StepDescriptor getNextStep(long id,int actionId,Map inputs)
    {    	    	
    	try
    	{
    		db.transaction();
    		
    		WorkflowStore store = getPersistence();
            WorkflowEntry entry = store.findEntry(id,inputs);

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
            
            //post銆乸re-functions??---do not execute those functions.
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
            	StepDescriptor ret = wf.getStep( theResults[0].getStep() );
            	/*if(inputs != null && ret != null)
            	{
            		List preFunctions = ret.getPreFunctions();
                    for (Iterator iterator = preFunctions.iterator();
                            iterator.hasNext();) 
                    {
                        FunctionDescriptor function = (FunctionDescriptor) iterator.next();
                        String fname = function.getName();
                        if(fname != null && fname.equalsIgnoreCase("assign")) //assign operators
                        {
                        	transientVars.put("$execflag", "query");
                        	executeFunction(function, transientVars, ps);
                        	inputs.put("$operators", transientVars.get("$operators"));
                        	break;
                        }
                    }
            	}*/
            	return ret;
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
    		setDefaultArg("entry.name","wfcaption");
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
    		
    		setDefaultArg("step.dueDate","dueDate");
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
