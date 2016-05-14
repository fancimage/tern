package com.tern.iap.controllers;

import java.sql.SQLException;

import com.tern.db.DataRow;
import com.tern.db.db;
import com.tern.iap.AppContext;
import com.tern.iap.Operator;
import com.tern.iap.SessionExpireException;
import com.tern.iap.util.ActionResult;
import com.tern.util.Convert;
import com.tern.util.Trace;
import com.tern.web.Controller;
import com.tern.web.Route;
import com.tern.web.routes.HttpMethod;

public class LoginController extends Controller
{
	@Route(value="/",method=HttpMethod.POST)
	public void index()
	{
		ActionResult r = new ActionResult();
		this.setViewObject(r);
		
		String name = request.getParameter("username");
		String pwd = request.getParameter("password");
    	
		if(name==null || pwd == null
			|| name.trim().length()<=0 || pwd.trim().length()<=0)
		{
			r.setResult(1,"参数错误!");
			return;
		}
		
		//对用户名进行初步的合法性判断
		if(name.indexOf("'")>=0 || name.indexOf(" ")>=0
			|| name.indexOf("(")>=0 || name.indexOf(")")>=0)
		{
			r.setResult(2,"用户名中有非法字符!");
			return;
		}				
		
		//查询操作员表
		try 
		{
			DataRow row = db.sql("select operatorID,uid,oname,opwd,ostatus from t_operator where loginname=?" , name)
			  .queryOne();
			
			if(null == row)
			{
				r.setResult(5,"操作员账户不存在!");
				return;
			}
			
			String dbPwd = row.getString("opwd");
			if(Convert.encodeString(pwd).equals(dbPwd))
			{
				//验证通过
				new Operator(row.getInt("operatorID"),name,row.getString("oname"));
			}
			else
			{
				r.setResult(4,"用户名与密码不匹配.");
			}
		} 
		catch (SQLException e) 
		{
			Trace.write(Trace.Error,e, "operator login");
			r.setResult(3,"内部错误，请联系管理员!");
		};
	}
	
	@Route(value="out")
	public void logout()
	{
		Operator.current(null);
		//this.redirect("/static/login.html");
		throw new SessionExpireException();
	}
}
