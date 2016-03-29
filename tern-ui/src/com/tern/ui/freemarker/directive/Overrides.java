/**
 * Tern-ui Library.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.ui.freemarker.directive;

import java.io.IOException;
import java.util.Map;

import com.tern.ui.freemarker.Directives;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

public class Overrides implements TemplateDirectiveModel
{
	static final String BLOCK_NAME_PRE = ".tern.block.";
	
	public void execute(Environment env, Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException 
	{
		String name = Directives.getStringParam(env, params, "name");
		
		name = BLOCK_NAME_PRE+name;
		
		//java.io.Writer out = new java.io.StringWriter();
		//body.render(out);
		
		env.getCurrentNamespace().put(name, body);
		//env.getCurrentNamespace().put(name, out.toString());
		//env.setLocalVariable(name, out.toString());		
	}
}
