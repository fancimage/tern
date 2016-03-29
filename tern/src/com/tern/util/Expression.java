/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.util;

import java.util.*;

public class Expression 
{	 	 
    private ArrayList right;// 存储右序表达式
    private ExpressionException paserErr=null;
    private Map<String,Object> variables; 
    
    public Expression(String expr)
    {
    	toRight(expr);
    }
    
 // 将中序表达式转换为右序表达式   
    private void toRight(String expr) 
    {
    	ArrayList expression = new ArrayList();// 存储中序表达式   
    	StringTokenizer st = new StringTokenizer(expr, "+-*/()", true);   
        while (st.hasMoreElements()) 
        {   
            expression.add(st.nextToken());
        }         
    	
        Stack aStack = new Stack(); //运算符入栈
        right = new ArrayList();
        
        String operator = null;   
        int position = 0;   
        
        while (true) 
        {
        	String op = (String) expression.get(position);
            if (Calculate.isOperator(op))
            {
                if (aStack.empty() || op.equals("("))
                {   
                    aStack.push(op);
                }
                else 
                {
                    if (op.equals(")"))  //处理括号内的运算
                    {
                    	if(aStack.empty())
                    	{
                    		paserErr = new ExpressionException("miss match ).");
                    		return;
                    	}
                    	
                    	boolean isMatch=false;
                    	operator = (String) aStack.pop();
                        do
                        {
                        	if(operator.equals(")"))
                        	{
                        		break;
                        	}
                        	else if(operator.equals("("))
                        	{
                        		isMatch=true;//正确匹配
                        		break;
                        	}
                        	
                            right.add(operator);
                            
                            if(aStack.empty()) break;                            
                            operator = (String) aStack.pop();
                        }while (true);
                        
                        if(!isMatch)
                        {
                        	paserErr = new ExpressionException("miss match ).");
                    		return;
                        }
                    }
                    else 
                    {   
                        if (!aStack.empty() 
                        		&& Calculate.priority(op) <= Calculate.priority((String) aStack.peek())) 
                        {   
                            operator = (String) aStack.peek();   
                            if (!operator.equals("("))
                            {
                                right.add(operator);
                                aStack.pop();
                            }
                        }   
                        aStack.push(op);   
                    }   
                }   
            }
            else
            {
            	Object  v = null;
            	//判断op的类型
            	if( op.startsWith("\"") )
            	{
            		if(!op.endsWith("\""))
            		{
            			paserErr = new ExpressionException("wrong const string-"+op);
                		return;
            		}            		
            		
            		v = new ExprValue(op.substring(1,op.length()-1));
            	}
            	else if(op.startsWith("'"))
            	{
            		if(!op.endsWith("'"))
            		{
            			paserErr = new ExpressionException("wrong const string-"+op);
                		return;
            		}
            		
            		v = new ExprValue(op.substring(1,op.length()-1));
            	}
            	else
            	{
            		//是不是数字?
            		try
            		{
            			double d = Double.parseDouble(op);
            			//是不是整数？
            			if(op.indexOf('.')<0)
            			{
            				int i = Integer.parseInt(op);
            				v = new ExprValue(i);
            			}
            			else
            			{
            				v = new ExprValue(d);
            			}
            		}
            		catch(Exception e)
            		{
            			//不是数字，那便认为是变量(去掉可能有的${})
            			if(op.endsWith("}") && op.startsWith("${") )
            			{
            			    op = op.substring(2, op.length()-1);
            			}
            			
            			v = op;
            			if(variables==null)
            			{
            				variables=new HashMap<String,Object>();
            			}
            			
            			variables.put(op, null);
            		}
            	}
                right.add(v);
            }
            
            position++;   
            if (position >= expression.size())  
            {
                break;
            }
        }
        
        while (!aStack.empty()) 
        {   
            operator = (String) aStack.pop();   
            right.add(operator);
        }   
    }  
    
    public Map<String,Object> getVariables()
    {
    	return this.variables;
    }
    
    public void setVariableValue(String name,int v)
    {
    	if(variables!=null && variables.containsKey(name))
    	{
    		variables.put(name, new ExprValue(v));
    	}
    }
    
    public void setVariableValue(String name,double v)
    {
    	if(variables!=null && variables.containsKey(name))
    	{
    		variables.put(name, new ExprValue(v));
    	}
    }
    
    public void setVariableValue(String name,float v)
    {
    	if(variables!=null && variables.containsKey(name))
    	{
    		variables.put(name, new ExprValue(v));
    	}
    }
    
    public void setVariableValue(String name,String v)
    {
    	if(variables!=null && variables.containsKey(name))
    	{
    		variables.put(name, new ExprValue(v));
    	}
    }
    
    public void setVariableValue(String name,Object v)
    {
    	if(variables!=null && variables.containsKey(name))
    	{
    		variables.put(name, new ExprValue(v==null?"":v.toString()));
    	}
    }
    
    private ExprValue getVariable(Object obj)
    {
    	if(this.variables==null || obj==null) return null;
    	String name=obj.toString();
    	if(variables.containsKey(name))
    	{
    	    return (ExprValue)variables.get(name);
    	}
    	
    	return null;
    }
    
    public Object calculate() throws ExpressionException
    {
    	if(paserErr!=null)
    	{
    		throw paserErr;
    	}
    	
        Stack aStack = new Stack();
        ExprValue op1, op2;
        Object is = null;
        Iterator it = right.iterator();
  
        while (it.hasNext()) 
        {   
            is = it.next();
            if ((is instanceof String) && Calculate.isOperator((String)is))
            {
            	Object v = aStack.pop();
            	if(v instanceof ExprValue)
            	{
            		op1 = (ExprValue)v;
            	}
            	else
            	{
            		//是变量
            		op1=getVariable(v);
            		if(op1==null)
            		{
            			throw new ExpressionException("variable ["+v+"] not assigned.");
            		}
            	}
            	
            	v = aStack.pop();
            	if(v instanceof ExprValue)
            	{
            		op2 = (ExprValue)v;
            	}
            	else
            	{
            		//是变量
            		op2=getVariable(v);
            		if(op2==null)
            		{
            			throw new ExpressionException("variable ["+v+"] not assigned.");
            		}
            	}
            	
                aStack.push(Calculate.twoResult((String)is, op1, op2));   
            } 
            else  
            {
                aStack.push(is);   
            }
        }   
        return aStack.pop();
    }
      
} 

class Calculate 
{   
    // 判断是否为操作符号   
    public static boolean isOperator(String operator)
    {   
        if (operator.equals("+") || operator.equals("-")   
                || operator.equals("*") || operator.equals("/")   
                || operator.equals("(") || operator.equals(")")) 
        {
            return true;
        }
        else
        {
            return false;
        }
    }   
  
    // 设置操作符号的优先级别   
    public static int priority(String operator) {   
        /*if (operator.equals("+") || operator.equals("-")   
                || operator.equals("("))   
            return 1;   
        else if (operator.equals("*") || operator.equals("/"))   
            return 2;   
        else  
            return 0; */
        
        if (operator.equals("+") || operator.equals("-"))
        {  
            return 1;  
        }     
        else if (operator.equals("*") || operator.equals("/"))
        {  
            return 2;  
        }     
        else if (operator.equals("(") || operator.equals(")"))
        {  
            return 3;  
        }  
        else 
        {  
            return 0;     
        }
    }   
  
    // 做2值之间的计算   
    public static ExprValue twoResult(String operator, ExprValue a, ExprValue b)
    {
    	int typea = a.getType();
    	int typeb = b.getType();
    	
        try
        {
            String op = operator;     

            double z = 0;   
            if (op.equals("+"))
            {            	
                if(typeb == typea)
                {
                	if(ExprValue.INT == typea)
                	{
                		return new ExprValue(b.getInt()+a.getInt());
                	}
                	else if(ExprValue.FLOAT == typea)
                	{
                		return new ExprValue(b.getDouble()+a.getDouble());
                	}
                	else if(ExprValue.STRING == typea)
                	{
                		return new ExprValue(b.getString()+a.getString());
                	}
                }
                else
                {
                	if(ExprValue.STRING == typea || ExprValue.STRING == typeb)
                	{
                		return new ExprValue(b.getString()+a.getString());
                	}
                	else
                	{
                		return new ExprValue(b.getDouble()+a.getDouble());
                	}
                }
            }
            else if (op.equals("-"))   
            {
            	if(ExprValue.STRING == typea || ExprValue.STRING == typeb)
            	{
            		return null;
            	}
            	
            	if(ExprValue.FLOAT == typea || ExprValue.FLOAT == typeb)
            	{
            		return new ExprValue(b.getDouble()-a.getDouble());
            	}
            	else
            	{
            		return new ExprValue(b.getInt()-a.getInt());
            	}
            }
            else if (op.equals("*"))   
            {
            	if(ExprValue.STRING == typea || ExprValue.STRING == typeb)
            	{
            		return null;
            	}
            	
            	if(ExprValue.FLOAT == typea || ExprValue.FLOAT == typeb)
            	{
            		return new ExprValue(b.getDouble()*a.getDouble());
            	}
            	else
            	{
            		return new ExprValue(b.getInt()*a.getInt());
            	}
            }   
            else if (op.equals("/"))   
            {
            	if(ExprValue.STRING == typea || ExprValue.STRING == typeb)
            	{
            		return null;
            	}
            	
            	if(ExprValue.FLOAT == typea || ExprValue.FLOAT == typeb)
            	{
            		double ia = a.getDouble();
            		if(ia==0) return new ExprValue(0);
            		return new ExprValue(b.getDouble()/a.getDouble());
            	}
            	else
            	{
            		int ia = a.getInt();
            		if(ia==0) return new ExprValue(0);
            		return new ExprValue(b.getInt()/ia);
            	}
            }  
        } 
        catch (Exception e) {   
            //System.out.println("input has something wrong!");   
            
        } 
        
        return null;   
    }   
}   
