/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.dao;

public class Column //extends com.tern.data.DataColumn
{
	String name;
	String code;
	String caption;
	DataType type = DataType.Numeric;
	boolean nullable;
	String default_val;
	boolean readonly;
	boolean iskey;
	boolean auto;
	
	Double min,max;
	int minLen,maxLen;
	
	Relation belongsTo;
	Object extra1; //for datetime(format),enum
	int extraInt = -1;
	
	public static final int COLUMN_SYS = 1;
	int style = 0;/*列的附加特性*/
	
	/*关联显示条件*/
	//long showById = 0;
	String showCondtion;
	
	protected Column()
	{
		
	}
	
	public String getName(){return name;}
	public String getCode(){if(code!=null && code.length()>0) return code;else return name;}
	public String getCaption(){return caption!=null && caption.length()>0? caption: name;}
	public DataType getType(){return type;}
	public boolean isNullable(){return nullable;}
	public boolean isReadonly(){return readonly;}
	public String getDefault(){return default_val;}
	public boolean isKey(){return iskey;}
	
	public Double getMax()
	{
		return type == DataType.Numeric?max:null;
	}
	
	public Double getMin()
	{
		return type == DataType.Numeric?min:null;
	}
	
	public int getLength()
	{
		return maxLen;
	}
	
	public int getMinLength()
	{
		return type == DataType.String?minLen:0;
	}
	
	public int getScale()
	{
		return type == DataType.Numeric?minLen:0;
	}
	
	//only for datetime
	public java.text.SimpleDateFormat getFormat()
	{ 
		return type == DataType.Datetime?(java.text.SimpleDateFormat)extra1:null;
	}
	
	public int getDateMode()
	{
		if(extraInt < 0 && type == DataType.Datetime)
		{
			if(extra1 == ModelReader.SDF1) extraInt = 1;   //date
			else if(extra1 == ModelReader.SDF3) extraInt = 2; //time
			else if(extra1 == ModelReader.SDF2) extraInt = 3; //date&&time
			else
			{				
				//is date?
				String _fmt = ((java.text.SimpleDateFormat)extra1).toPattern();
				boolean isDate = false,isTime = false;
				if(_fmt.indexOf('y')>=0 || _fmt.indexOf('M')>=0 || _fmt.indexOf('d')>=0
					|| _fmt.indexOf('w')>=0 || _fmt.indexOf('W')>=0 || _fmt.indexOf('D')>=0
					|| _fmt.indexOf('F')>=0 || _fmt.indexOf('E')>=0)
				{
					isDate = true;
				}
				
				if(_fmt.indexOf('H')>=0 || _fmt.indexOf('m')>=0 || _fmt.indexOf('s')>=0
						|| _fmt.indexOf('h')>=0 || _fmt.indexOf('S')>=0 || _fmt.indexOf('a')>=0
						|| _fmt.indexOf('k')>=0 || _fmt.indexOf('K')>=0)
				{
					isTime = true;
				}
				
				if(isDate && isTime) extraInt = 3;
				else if(isTime) extraInt = 2;
				else extraInt = 1;
			}
		}
		
		return extraInt;  //None
	}
	
	public String getEnum()
	{
		return type == DataType.Enum?(extra1!=null&&((String)extra1).length()>0?(String)extra1:name):null;
	}
	
	public boolean isId()
	{
		return type==DataType.ID?true:false;
	}
	
	public boolean isAuto(){return this.auto;}
	
	public String toString()
	{
		return this.getCaption();
	}
	
	public Relation getBelongsTo(){return belongsTo;}
	public String getShowCondtion(){return this.showCondtion;}
	
	public void setStyle(int s)
	{
		this.style |= s;
	}
	
	public boolean isStyle(int s)
	{
		return (this.style&s)!=0;
	}
}
