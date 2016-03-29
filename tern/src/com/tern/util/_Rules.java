/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.util;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.regex.Pattern;

class _WordRule
{
	public static Set<String> Uncountable = new HashSet<String>(){{
	    add("information");
	    add("rice");
	    add("money");
	    add("species");
	    add("series");
	    add("fish");
	    add("sheep");
	    add("jeans");
	}}; 
	
	public static String[] Singulars = {"person","man","child",   "sex",  "move", "cow", "zombie"};
	public static String[] Plurals =   {"people","men","children","sexes","moves","kine","zombies"};
	
	public static List<_WordRule> PLURAL_RULE = new AList(){{
		add("(quiz)$","$1zes");
		add("^(oxen)$", "$1");
		add("^(ox)$","$1en");
		add("([m|l])ice$","$1ice");
		add("([m|l])ouse$","$1ice");
		add("(matr|vert|ind)(?:ix|ex)$","$1ices");
		add("(x|ch|ss|sh)$","$1es");
		add("([^aeiouy]|qu)y$","$1ies");
		add("(hive)$","$1s");
		add("(?:([^f])fe|([lr])f)$","$1$2ves");
		add("sis$","ses");
		add("([ti])a$","$1a");
		add("([ti])um$","$1a");
		add("(buffal|tomat)o$","$1oes");
		add("(bu)s$","$1ses");
		add("(alias|status)$","$1es");
		add("(octop|vir)i$","$1i");
		add("(octop|vir)us$","$1i");
		add("(ax|test)is$","$1es");
		add("s$","s");
		add("$","s");
	}};
	
	public static List<_WordRule> SINGULAR_RULES = new AList(){{
		add("(database)s$","$1");
		add("(quiz)zes$","$1");
		add("(matr)ices$","$1ix");
		add("(vert|ind)ices$","$1ex");
		add("^(ox)en","$1");
		add("(alias|status)es$","$1");
		add("(octop|vir)i$","$1us");
		add("(cris|ax|test)es$","$1is");
		add("(shoe)s$","$1");
		add("(o)es$","$1");
		add("(bus)es$","$1");
		add("([m|l])ice$","$1ouse");
		add("(x|ch|ss|sh)es$","$1");
		add("(m)ovies$","$1ovie");
		add("(s)eries$","$1eries");
		add("([^aeiouy]|qu)ies$","$1y");
		add("([lr])ves$","$1f");
		add("(tive)s$","$1");
		add("(hive)s$","$1");
		add("([^f])ves$","$1fe");
		add("(^analy)ses$","$1sis");
		add("((a)naly|(b)a|(d)iagno|(p)arenthe|(p)rogno|(s)ynop|(t)he)ses$","$1$2sis");
		add("([ti])a$","$1um");
		add("(n)ews$","$1ews");
		add("(class)$","$1");
		add("s$","");
	}};
	
	
	public Pattern pattern;
	public String  replaced;
	
	public _WordRule(String pattern,String replaced)
	{
		this.pattern = Pattern.compile(pattern);
		this.replaced = replaced;
	}
	
	static class AList extends ArrayList<_WordRule>
	{
		public void add(String s1,String s2)
		{
			super.add(new _WordRule(s1,s2));
		}
	}
}

