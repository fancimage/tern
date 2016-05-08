package com.tern.iap.util;

public class ActionResult 
{
    private int result = 0;
    private String message;
    
    public int getResult(){return result;}
    public String getMessage(){return message;}
    
    public void setResult(int v){result = v;}
    public void setMessage(String v){message = v;}
    
    public void setResult(int v,String m)
    {
    	result = v;
    	message = m;
    }
}
