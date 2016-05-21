package com.tern.iap.workflow;

import java.util.Map;

import com.opensymphony.workflow.WorkflowException;

/*
 * 处理工作流的权限
 * */
public interface WorkflowPermission 
{
	/*
	 * 判断当前用户是否有对data的权限
	 * */
    boolean hasRight(IAPWorkflowEntry entry,Map attrs);
    
    /*
     * 检索对data有权限的所有操作员，将其插入到流程处理步骤的待处理人列表
     * */
    void initWorkflowOperators(IAPWorkflowEntry entry,Map attrs) throws WorkflowException;
}
