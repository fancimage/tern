/**
 * Tern-iap Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.iap.controllers;

import com.tern.web.Controller;
import com.tern.web.Route;

public class ServiceController extends Controller
{
	public void index()
	{
		//all my processes
	}
	
	@Route("[doing|active|complete|suspend]1")
	public String searchByState(String state)
	{
		return "process/index";
	}
}
