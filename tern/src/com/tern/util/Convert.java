/**
 * Tern Framework.
 * 
 * @author fancimage
 * @Copyright 2010 qiao_xf@163.com Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tern.util;

//import java.security.spec.*;
import javax.crypto.*;
//import java.security.*;

import java.math.BigDecimal;
import java.util.regex.Matcher;

/**
 * <p>Title: 数据类型转换、格式化实用类</p>
 * <p>Description: 提供常用的数据类型转化，数据格式化的功能。</p>
 * <p>Copyright: Copyright iEAS(c) 2008</p>
 * @author iEAS Fancimage
 * @version 1.0
 */
public final class Convert
{
	private static Cipher    desDecoder=null;
    private static Cipher    desEncoder=null;
    private static Object locked=new Object();
    
    static
    {
    	initCipher();
    }
    
    private static void initCipher()
    {
    	//初始化密钥
    	byte[] arrB = new byte[8];
    	String arrStr="20081214";
    	byte[] arrA = arrStr.getBytes();
    	
    	for (int i = 0; i < arrA.length && i < arrB.length; i++)
    	{
    		arrB[i] = arrA[i];
    	}    	    
    	
    	byte[] ivBytes=new byte[]{0,0,0,0,0,0,0,0};
    	javax.crypto.spec.IvParameterSpec iv = new javax.crypto.spec.IvParameterSpec(ivBytes);
    	
    	try
    	{
    		javax.crypto.spec.DESKeySpec keySpec = new javax.crypto.spec.DESKeySpec(arrB);
        	SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        	SecretKey desKey = keyFactory.generateSecret(keySpec);
        	
    		desDecoder = Cipher.getInstance("DES/CBC/PKCS5Padding");
    		desDecoder.init(Cipher.DECRYPT_MODE, desKey,iv);
    		
    		desEncoder = Cipher.getInstance("DES/CBC/PKCS5Padding");
    		desEncoder.init(Cipher.ENCRYPT_MODE, desKey,iv);
    	}
    	catch(Exception e)
    	{
    		Trace.write(Trace.Error, e,"Init des failed:");
    		desDecoder=null;
    	}
    }
    
    private Convert()
    {}

    /**
     * <p><b>功能：</b>表示空字符串</p>
     */
    public static final String EmptyString = "";

    public static final String True = "1";
    public static final String False = "0";

    private static char[] chinesNum = new char[]
                                      {
                                      '零', '壹', '贰', '叁', '肆', '伍', '陆', '柒',
                                      '捌', '玖',
    };

    private static char[] unit = new char[]
                                 {
                                 '拾', '佰', '仟'};
    private static char[] cir = new char[]
                                {
                                '万', '亿'};

    private static void insertCir(char ch, StringBuffer buf)
    {
        if (buf.length() >= 1)
        {
            int index = -1;
            int index1 = -1;
            char ch1 = buf.charAt(0);
            for (int i = 0; i < cir.length; i++)
            {
                if (ch1 == cir[i])
                {
                    index1 = i;
                }
                if (ch == cir[i])
                {
                    index = i;
                }
            }
            if (index == -1 || index1 == -1)
            {
                buf.insert(0, ch);
                return;
            }
            if (index > index1)
            {
                buf.setCharAt(0, ch);
            }
            else
            {
                buf.insert(0, ch);
            }
        }
        else
        {
            buf.insert(0, ch);
        }
    }

    public static String toChinesNumber(String input)
    {
        if (input == null || input.length() <= 0)
        {
            return "";
        }
        int eIndex = input.lastIndexOf("E");
        if (eIndex < 0)
        {
            eIndex = input.lastIndexOf("e");
        }
        if (eIndex >= 0)
        {
            //如果采用了科学计数法，则相应地处理
            java.math.BigDecimal bg = new BigDecimal(input);
            input = bg.divide(oneNumeric, java.math.BigDecimal.ROUND_HALF_UP).toString();
        }
        StringBuffer buf = new StringBuffer();
        int dot = input.lastIndexOf('.');
        if (dot < 0)
        {
            dot = input.length();
        }
        boolean fullZero = true;
        StringBuffer fraction = new StringBuffer();
        for (int i = dot + 1; i < input.length(); i++)
        {
            String ch = input.substring(i, i + 1);
            int index = Integer.parseInt(ch);
            if (index > 0)
            {
                fullZero = false;
            }
            fraction.append(chinesNum[index]);
        }
        if (fullZero)
        {
            buf.append("圆整");
        }
        else
        {
            buf.append("点").append(fraction).append("圆");
        }
        int i1 = -1;
        int i2 = -1;
        boolean zero = true;
        boolean needCir = false;
        for (int i = dot - 1; i >= 0; i--)
        {
            String ch = input.substring(i, i + 1);
            int index = Integer.parseInt(ch);
            if (index == 0)
            {
                if (!zero)
                {
                    buf.insert(0, chinesNum[index]);
                    zero = true;
                }
                i1++;
                if (i1 >= unit.length)
                {
                    i1 = -1;
                    if (needCir)
                    {
                        insertCir(cir[i2], buf);
                        //buf.insert(0, cir[i2]);
                    }
                    i2++;
                    //增加了4个数量级
                    if (i2 >= cir.length)
                    {
                        i2 = 0;
                    }
                    needCir = true;
                }
            }
            else
            {
                zero = false;
                if (i1 >= unit.length)
                {
                    i1 = -1;
                    if (needCir)
                    {
                        //buf.insert(0, cir[i2]);
                        insertCir(cir[i2], buf);
                    }
                    i2++;
                    //增加了4个数量级
                    if (i2 >= cir.length)
                    {
                        i2 = 0;
                    }
                    needCir = true;
                }
                if (needCir)
                {
                    //buf.insert(0, cir[i2]);
                    insertCir(cir[i2], buf);
                    needCir = false;
                }
                if (i1 >= 0)
                {
                    //增加了一个数量级
                    buf.insert(0, unit[i1]);
                }
                i1++;
                buf.insert(0, chinesNum[index]);
            }
        }
        if (zero && buf.length() < 3)
        {
            buf.insert(0, chinesNum[0]);
        }
        return buf.toString();
    }

    /**
     * <p><b>功能：</b>如果字符串为null，则返回null；否则返回字符串的trim后的值</p>
     * <p>此函数保证在trim字符串的同时，不会因字符串为null而引发异常（此时会返回null）。</p>
     * @param str：要求trim的字符串
     * @return String:返回的结果
     */
    public static String trim(String str)
    {
        return str == null ? null : str.trim();
    }

    public static String toNum(Object o)
    {
        try
        {
            java.text.NumberFormat numFmt = java.text.NumberFormat.getInstance();
            numFmt.setMaximumFractionDigits(0);
            numFmt.setMinimumFractionDigits(0);
            return numFmt.format(new Double(o.toString()));
        }
        catch (Exception ex)
        {
            return toString(o);
        }
    }

    /**
     * <p><b>功能：如果对象为空，返回字符串空，否则返回对象字符串值</b></p>
     * @param o 传入的要转化为String的数据
     * @return String:返回的字符串
     */
    public static String toString(Object o)
    {
        return o == null ? EmptyString : o.toString().trim();
    }
    
    public static String toStringIgnoreEmpty(Object o,String defval)
    {
    	if(null == o) return defval;
    	else
    	{
    		String str = o.toString().trim();
    		if(str.length()<=0) return defval;
    		else return str;
    	}    	
    }
    
    public static String capitalize(String str)
    {
    	if(str == null || str.length()<=0) return str;
    	return str.substring(0,1).toUpperCase() + str.substring(1);
    }

    /**
     * <p><b>功能： 将字符串source中的pattern子串全部替换为newStr</b></p>
     * @param  source：源字符串
     * @param  pattern：将被替换的子串
     * @param  newStr：新的子串
     * @return StringBuffer：返回经过处理了的字符串
     */
    public static StringBuffer replaceAll(StringBuffer source, String pattern,
                                          String newStr)
    {
        if (source == null || pattern == null || pattern.equals(newStr))
        {
            return source;
        }
        int index = source.indexOf(pattern);
        int len = pattern.length();
        int newLen = newStr.length();
        while (index >= 0)
        {
            source.replace(index, index + len, newStr);
            index = source.indexOf(pattern, index + newLen);
        }
        return source;
    }

    /**
     * <p><b>功能： 将字符串source中的pattern子串全部替换为newStr</b></p>
     * @param  source：源字符串
     * @param  pattern：将被替换的子串
     * @param  newStr：新的子串
     * @return String：返回经过处理了的字符串
     */
    public static String replaceAll(String source, String pattern,
                                    String newStr)
    {
    	if(source==null || source.indexOf(pattern)<0)
    	{
    		return source;
    	}
        StringBuffer buf = new StringBuffer(source);
        replaceAll(buf, pattern, newStr);
        return buf == null ? EmptyString : buf.toString();
    }
    
    

    /**
     * <p><b>功能： 将字符串source中的character字符变为连续的两个同样字符</b></p>
     * @param  source：源字符串
     * @param  character：将被扩张为两个同样字符的字符
     * @return StringBuffer：返回经过处理了的字符串
     */
    public static StringBuffer doubleCharacter(StringBuffer source,
                                               char character)
    {
        if (source == null)
        {
            return source;
        }
        StringBuffer tmp = new StringBuffer();
        tmp.append(character);
        String pattern = tmp.toString();
        tmp.append(character);
        String newStr = tmp.toString();
        return replaceAll(source, pattern, newStr);
    }

    /**
     * <p><b>功能： 将字符串source中的character字符变为连续的两个同样字符</b></p>
     * @param  source：源字符串
     * @param  character：将被扩张为两个同样字符的字符
     * @return String：返回经过处理了的字符串
     */
    public static String doubleCharacter(String source, char character)
    {
        StringBuffer buf = new StringBuffer(source);
        doubleCharacter(buf, character);
        return buf == null ? EmptyString : buf.toString();
    }

    /**
     * <p><b>功能： 将数据转换为bool值</b></p>
     * @param  param：要转换的数据
     * @return boolean：返回的结果
     */
    public static boolean toBoolean(Object param,boolean def)
    {
        if (param == null)
        {
            return def;
        }
        String tmp = param.toString();
        if (tmp.length() <= 0)
        {
            return def;
        }
        
        if(def)
        {
        	switch (tmp.charAt(0))
            {
                case '0':
                case 'n':
                case 'N':
                case 'F':
                case 'f':
                    return false;
            }
        }
        else
        {
        	switch (tmp.charAt(0))
            {
                case '1':
                case 'y':
                case 'Y':
                case 't':
                case 'T':
                    return true;
            }
        }
        
        return def;
    }
    
    public static boolean toBoolean(Object param)
    {
    	return toBoolean(param,false);
    }

    /**
     * <p><b>功能： 将数据转换为float值</b></p>
     * @param  param：要转换的数据
     * @return float：返回的结果
     */
    public static float parseFloat(Object param){return parseFloat(param,0);}
    public static float parseFloat(Object param,float def)
    {
        if (param == null)
        {
            return def;
        }

        try
        {
            return Float.parseFloat(param.toString());
        }
        catch (Exception e)
        {
        	return def;
        }        
    }

    /**
     * <p><b>功能： 将数据转换为double值</b></p>
     * @param  param：要转换的数据
     * @return double：返回的结果
     */
    public static double parseDouble(Object param)
    {
        return parseDouble(param,0);
    }
    
    public static double parseDouble(Object param,double def)
    {
        if (param == null)
        {
            return def;
        }

        try
        {
            return Double.parseDouble(param.toString());
        }
        catch (Exception e)
        {
            return def;
        }        
    }

    /**
     * <p><b>功能： 将数据转换为int值</b></p>
     * @param  param：要转换的数据
     * @return int：返回的结果
     */
    public static int parseInt(Object param)
    {
    	return parseInt(param,0);
    }
    public static int parseInt(Object param,int def)
    {
        int i = 0;
        if (param == null)
        {
            return def;
        }
        String tmp = param.toString();
        try
        {
            i = Integer.parseInt(tmp);
        }
        catch (Exception e)
        {
            try
            {
                i = (int) parseFloat(tmp);
            }
            catch (Exception ee)
            {
                i = def;
            }
        }
        return i;
    }

    /**
     * <p><b>功能： 将数据转换为long值</b></p>
     * @param  param：要转换的数据
     * @return long：返回的结果
     */
    public static long parseLong(Object param)
    {
    	return parseLong(param,0);
    }
    
    public static long parseLong(Object param,long def)
    {
        if (param == null)
        {
            return def;
        }
        String tmp = param.toString();
        try
        {
            return Long.parseLong(tmp);
        }
        catch (Exception e)
        {
            return def;
        }
    }

    /**
     * <p><b>功能： 将数据转换为byte值</b></p>
     * @param  param：要转换的数据
     * @return long：返回的结果
     */
    public static byte parseByte(Object param)
    {
        byte val = 0;
        if (param != null)
        {
            try
            {
                val = Byte.parseByte(param.toString());
            }
            catch (Exception e)
            {
            }
        }
        return val;
    }

    /**
     * <p><b>功能： 将数据转换为short值</b></p>
     * @param  param：要转换的数据
     * @return long：返回的结果
     */
    public static short parseShort(Object param)
    {
        short val = 0;
        if (param != null)
        {
            try
            {
                val = Short.parseShort(param.toString());
            }
            catch (Exception e)
            {
            }
        }
        return val;
    }

    /**
     * <p><b>功能： 将数据转换为bool值, 调用toBoolean, 保持方法命名一致</b></p>
     * @param  param：要转换的数据
     * @return boolean：返回的结果
     */
    public static boolean parseBool(Object param)
    {
        return toBoolean(param);
    }

    /**
     * <p><b>功能： 将数据转换为BigDecimal值</b></p>
     * @param  param：要转换的数据
     * @return long：返回的结果
     */
    public static BigDecimal parseDecimal(Object param)
    {
        BigDecimal val = null;
        if (param != null)
        {
            if (param instanceof BigDecimal)
            {
                return (BigDecimal) param;
            }

            try
            {
                val = new BigDecimal(param.toString());
            }
            catch (Exception e)
            {
            }
        }
        return val == null ? new BigDecimal("0") : val;
    }

    static java.text.DateFormat dateFormat = new java.text.SimpleDateFormat();
    /**
     * <p><b>功能： 将数据转换为java.util.Date值</b></p>
     * @param  param：要转换的数据
     * @return long：返回的结果
     */
    public static java.util.Date parseDate(Object param)
    {
        java.util.Date val = null;
        if (param != null)
        {
            if (param instanceof java.util.Date)
            {
                return (java.util.Date) param;
            }

            try
            {

                val = dateFormat.parse(param.toString());
            }
            catch (Exception e)
            {
            }
        }
        return val == null ? new java.util.Date(0) : val;
    }

    /**
     * <p><b>功能： 将数据转换为java.sql.Time值</b></p>
     * @param  param：要转换的数据
     * @return long：返回的结果
     */
    public static java.sql.Time parseTime(Object param)
    {
        java.sql.Time val = null;
        if (param != null)
        {
            if (param instanceof java.sql.Time)
            {
                return (java.sql.Time) param;
            }

            try
            {
                java.util.Date dt = parseDate(param);
                val = new java.sql.Time(dt.getTime());
            }
            catch (Exception e)
            {
            }
        }
        return val == null ? new java.sql.Time(0) : val;
    }

    public static String encodeStringInXML(Object string)
    {
        if (string == null)
        {
            return "";
        }
        else
        {
            return encodeStringInXML(string.toString());
        }
    }

    //对xml中的数据作格式化
    public static String encodeStringInXML(String string)
    {
        if (string == null)
        {
            return "";
        }
        int count = string.length();
        StringBuffer ret = new StringBuffer();
        for (int i = 0; i < count; i++)
        {
            char ch = string.charAt(i);
            if (ch == '&')
            {
                ret.append("&amp;");
            }
            else if (ch == '<')
            {
                ret.append("&#60;");
            }
            else if (ch == '>')
            {
                ret.append("&gt;");
            }
            else if (ch == '"')
            {
                ret.append("&quot;");
            }
            else if (ch == '\'')
            {
                ret.append("&apos;");
            }
            else if (ch == ' ')
            {
                ret.append("&#160;");
            }
            else
            {
                ret.append(ch);
            }
        }
        return ret.toString();
    }

    public static String encodeStringInURL(String string)
    {
        if (string == null)
        {
            return "";
        }
        int count = string.length();
        StringBuffer ret = new StringBuffer();
        //%2B  +
        //+    空格
        for (int i = 0; i < count; i++)
        {
            char ch = string.charAt(i);
            if (ch == '&')
            {
                ret.append("%26");
            }
            else if (ch == '%')
            {
                ret.append("%25");
            }
            else if (ch == ' ')
            {
                ret.append("+");
            }
            else if (ch == '?')
            {
                ret.append("%3F");
            }
            else if (ch == '+')
            {
                ret.append("%2B");
            }
            else if (ch == '\'')
            {
                ret.append("%27");
            }
            else
            {
                ret.append(ch);
            }
        }
        return ret.toString();
    }

    public static String decodeStringInURL(Object string)
    {
        if (string == null)
        {
            return "";
        }
        return decodeStringInURL(string.toString());
    }

    public static String decodeStringInURL(String string)
    {
        if (string == null)
        {
            return "";
        }
        string = string.trim();
        if (string.length() <= 0)
        {
            return "";
        }

        int index = 0;
        StringBuffer source = new StringBuffer(string);
        index=source.indexOf("+",index);
        while(index>=0)
        {
            source.replace(index,index+1," ");
            index=source.indexOf("+",index);
        }
        while (true)
        {
            index = source.indexOf("%", index);
            if (index < 0 || index > source.length() - 3)
            {
                break;
            }
            String pre = source.substring(index + 1, index + 3);
            if (pre.equals("25"))
            {
                source.replace(index, index + 3, "%");
            }
            else if (pre.equals("26"))
            {
                source.replace(index, index + 3, "&");
            }
            else if (pre.equals("27"))
            {
                source.replace(index, index + 3, "'");
            }
            else if (pre.equals("3F"))
            {
                source.replace(index, index + 3, "?");
            }
            else if (pre.equals("2B"))
            {
                source.replace(index, index + 3, "+");
            }
            index++;
        }
        return source.toString();
    }

    private static java.math.BigDecimal oneNumeric = new java.math.BigDecimal(
        "1");
    public static String round(String val, int scale)
    {
        try
        {
            java.math.BigDecimal bd = new java.math.BigDecimal(val);
            return (bd.divide(oneNumeric, scale, java.math.BigDecimal.ROUND_HALF_UP)).toString();
        }
        catch (Exception ex)
        {
            return "";
        }
    }
    
    public static double round(double val,int scale)
    {
    	try
        {
            java.math.BigDecimal bd = new java.math.BigDecimal(val);
            return bd.divide(oneNumeric, scale, java.math.BigDecimal.ROUND_HALF_UP).doubleValue();
        }
        catch (Exception ex)
        {
            return 0;
        }
    }

    public static boolean isBlankStr(String str)
    {
        return str == null || str.length() == 0;
    }
    
 // 将 s 进行 BASE64 编码 
    public static String encodeBase64(String s) 
    { 
        if (s == null) return null; 
        return (new sun.misc.BASE64Encoder()).encode( s.getBytes() ); 
    } 

    // 将 BASE64 编码的字符串 s 进行解码 
    public static String decodeBase64(String s)
    { 
        if (s == null) return null; 
        sun.misc.BASE64Decoder decoder = new sun.misc.BASE64Decoder(); 
        
        try 
        {
        	byte[] b = decoder.decodeBuffer(s);
        	return new String(b);
        } 
        catch (Exception e) 
        { 
        	return null;
        } 
    }   
    
    public static String byteArr2HexStr(byte[] arrB) throws Exception
    {
    	int iLen = arrB.length;  
    	// 每个byte用两个字符才能表示，所以字符串的长度是数组长度的两倍  
    	StringBuffer sb = new StringBuffer(iLen * 2);  
    	for (int i = 0; i < iLen; i++) 
    	{
    		int intTmp = arrB[i];  
    	    // 把负数转换为正数  
    	    while (intTmp < 0) 
    	    {  
    	     intTmp = intTmp + 256;  
    	    }  
    	    // 小于0F的数需要在前面补0  
    	    if (intTmp < 16) 
    	    {  
    	     sb.append("0");  
    	    }  
    	    sb.append(Integer.toString(intTmp, 16));  
    	}  
    	return sb.toString();  
    }
    
    public static byte[] hexStr2ByteArr(String strIn) throws Exception 
    {
    	byte[] arrB = strIn.trim().getBytes();  
    	int iLen = arrB.length;  
    	//System.out.print(strIn);
    	   
    	// 两个字符表示一个字节，所以字节数组长度是字符串长度除以2  
    	byte[] arrOut = new byte[iLen / 2];  
    	for (int i = 0; i < iLen; i = i + 2)
    	{  
    	    String strTmp = new String(arrB, i, 2);
    	    try
    	    {
    	        arrOut[i / 2] = (byte) Integer.parseInt(strTmp, 16);  
    	    }catch(Exception e)
    	    {
    	    	System.out.print("Wrong:"+strTmp);
    	    }
    	}  
    	return arrOut;  
    }    
    
    public static String join(String str,int[] list)
    {
    	if(list==null || list.length<=0) return Convert.EmptyString;
    	
    	StringBuffer buf = new StringBuffer();
    	
    	for(int s:list)
    	{
    		if(buf.length()>0) buf.append(str);
    		buf.append(s);
    	}
    	
    	return buf.toString();
    }
    
    public static String join(String str,long[] list)
    {
    	if(list==null || list.length<=0) return Convert.EmptyString;
    	
    	StringBuffer buf = new StringBuffer();
    	
    	for(long s:list)
    	{
    		if(buf.length()>0) buf.append(str);
    		buf.append(s);
    	}
    	
    	return buf.toString();
    }
    
    public static <T> String join(String str,T[] list)
    {
    	if(list==null || list.length<=0) return Convert.EmptyString;
    	
    	StringBuffer buf = new StringBuffer();
    	
    	for(T s:list)
    	{
    		if(buf.length()>0) buf.append(str);
    		if(s!=null) buf.append(s);
    	}
    	
    	return buf.toString();
    }
    
  //对密码字段进行加密、解密
    public static String encodeString(String str)
    {
    	if(str==null || desEncoder==null) return null;
    	//desKey,desAlg
    	try
    	{
    	    byte[] ori_data = str.getBytes("utf-8");
    	    byte[] now_data = desEncoder.doFinal(ori_data);
    	    return Convert.byteArr2HexStr(now_data);
    	}
    	catch(Exception e)
    	{
    		//e.printStackTrace();
    		Trace.write(Trace.Warning,e, "decodeString");
    		synchronized(locked)
    		{
    			initCipher();
    		}
    	}
    	
    	return null;
    }
    
    public static String decodeString(String str)
    {
    	//String tmp=encodeString("11111111");
    	//System.out.println(tmp);
    	if(str==null || desDecoder==null) return null;
    	//desKey,desAlg
    	try
    	{
    	    //byte[] ori_data = str.getBytes("utf-8");
    		byte[] ori_data = Convert.hexStr2ByteArr(str);
    	    byte[] now_data = desDecoder.doFinal(ori_data);
    	    return new String(now_data,"utf-8");
    	}
    	catch(Exception e)
    	{
    		//e.printStackTrace();
    		Trace.write(Trace.Warning,e, "decodeString");
    		synchronized(locked)
    		{
    			initCipher();  //长时间地运行后，doFinal可能会出错，因此此处重建一下
    		}
    	}
    	
    	return null;
    }
    
    public static Object getBeanProperty(Object owner, String fieldName)
    {
    	Class ownerClass = owner.getClass();                                          
        
    	try
    	{
    	    java.lang.reflect.Field field = ownerClass.getField(fieldName);
            return field.get(owner);            
    	}
    	catch(Exception e)
    	{
    	    Trace.write(Trace.Warning,e, "BeanProperty[%s]:"+owner,fieldName);
    	}
        return null;
    }
	
	public static int getStrLen(String strIn) throws Exception 
    {
    	
    	byte[] arrB = strIn.trim().getBytes();  
    	int iLen = arrB.length / 2;  
    	//System.out.print(strIn);
    	   
    	// 两个字符表示一个字节，所以字节数组长度是字符串长度除以2  
    	   
    	return iLen;  
    }
	
	public static String toExcelValue(Object obj,String Type)
    {
    	String value = (obj==null?Convert.EmptyString:obj.toString());
    	if(value.length()<=0) return Convert.EmptyString;
    	 
    	//if(Type.equals("Number") && 0==Float.parseFloat(value))
    	//{
    	//	return Convert.EmptyString;
    	//}
    	//else
    	{
    	    StringBuffer buf=new StringBuffer("<Data ss:Type=\"");
    	    buf.append(Type).append("\">");
    	    buf.append(toXMLString(obj.toString()));
    	    buf.append("</Data>");
    	    return buf.toString();
    	}
    }
    
    public static String toXMLString(String str)
    {
    	if(str==null) return str;
    	str = Convert.replaceAll(str, "\n", "&#10;");
    	return str;
    }
    
    /*
     * java.util.Map<String, Object> maps = new java.util.HashMap<String, Object>();
		maps.put("name", "乔旭峰");
		maps.put("age", "32");
		
		String src = "name=$name";
		Trace.write(Trace.Information, "%s ==> %s",src,Convert.format(src, maps) );
		
		src = "name=$name others";
		Trace.write(Trace.Information, "%s ==> %s",src,Convert.format(src, maps) );
		
		src = "name=$name and age=$age";
		Trace.write(Trace.Information, "%s ==> %s",src,Convert.format(src, maps) );
		
		src = "name=${name} and age=${age}";
		Trace.write(Trace.Information, "%s ==> %s",src,Convert.format(src, maps) );
		
		src = "name=$ name";		
		Trace.write(Trace.Information, "%s ==> %s",src,Convert.format(src, maps) );
		
		src = "name=${name";
		Trace.write(Trace.Information, "%s ==> %s",src,Convert.format(src, maps) );
		
		src = "name=$name and age=$age1 and age=${age1}";
		Trace.write(Trace.Information, "%s ==> %s",src,Convert.format(src, maps) );
     * */
    
    public static interface FormatFilter
    {
    	Object filter(String key,Object value,int index);
    }
    
    public static String format(String src,java.util.Map<String,Object> vars)
    {
    	return format(src,vars,null);
    }
    
    public static String format(String src,java.util.Map<String,Object> vars,FormatFilter filter)
    {
    	if(src == null || src.length()<=0 || vars == null) return src;
    	else
    	{
    		int from = 0;
    		int index = src.indexOf('$',from);
    		int len = src.length();
    		int count = 0;
    		
    		if(index >=0)
    		{
    			//search $name,or ${name}
    			StringBuffer buf = new StringBuffer();
    			
    			do
    			{
    				if(index > from)
    				{
    					buf.append(src.substring(from,index));
    				}
    				
    				from = index+1;
    				if(from >= len)
    				{
    					buf.append('$');
    					break;
    				}
    				
    				char ch = src.charAt(from);
    				if( (ch>='a' && ch <='z')
    				  ||(ch>='A' && ch <='Z')
    				  || ch == '_' || ch == '{')
    				{
    					//is variable,find the next seprator
    					boolean flag = true;
    					String _name;
    					if(ch == '{')
    					{
    						//ok,find next ‘}’
    						from = src.indexOf('}',from);
    						if(from < 0)
    						{
    							//buf.append(src.substring(index));
    							from = index;
    							break;
    						}      	
    						
    						flag = false;
    					}
    					else
    					{
    						from++;    						
    						while(from < len)
    						{
    							ch = src.charAt(from);
    							if(!( (ch>='a' && ch <='z')
    				    		    ||(ch>='A' && ch <='Z')
    				    		    ||(ch>='0' && ch <='9')
    				    			|| ch == '_' ))
        						{
        							break;
        						}
    							
    							from++;
    						}
    					}
    					
    					if(flag)
    					{
    					    _name = src.substring(index+1,from);
    					}
    					else
    					{
    						_name = src.substring(index+2,from);
    						from++;
    					}
    					
    					if(vars.containsKey(_name))
    					{    						
    						if(filter == null)
    						{
    						    Object v = vars.get(_name);
    						    buf.append(v);
    						}
    						else
    						{
    							Object v = filter.filter(_name, vars.get(_name), count);
    							buf.append(v);
    						}
    						count++;
    					}
    					else
    					{
    						buf.append(src.substring(index,from));
    					}    					    					
    				}
    				else
    				{
    					buf.append('$');
    				}
    				
    				index = src.indexOf('$',from);
    			}while(index>=0);
    			
    			if(from < len)
    			{
    				buf.append(src.substring(from));
    			}
    			
    			return buf.toString();
    		}
    		else
    		{
    			return src;
    		}    			    		   
    	}
    }
    
    private static int findarray(String[] arr,String s)
    {
    	for(int i=0;i<arr.length;i++)
    	{
    		if(arr[i].equals(s)) return i;
    	}
    	
    	return -1;
    }
    
    public static String plural(String word)
    {
    	if(word == null) return null;
    	String _lowcase = word.toLowerCase();
    	
    	if(_WordRule.Uncountable.contains(_lowcase) 
    		|| findarray(_WordRule.Plurals,_lowcase)>=0)
    	{
    		return word;
    	}
    	
    	int i = findarray(_WordRule.Singulars,_lowcase);
    	if(i>=0)
    	{
    		return _WordRule.Plurals[i];
    	}
    	
    	for(_WordRule r:_WordRule.PLURAL_RULE)
    	{
    		Matcher matcher = r.pattern.matcher(word);
    		if(matcher.find())
    		{
    			return matcher.replaceAll(r.replaced);
    		}
    	}
    	
    	return word;
    }
    
    public static String singular(String word)
    {
    	if(word == null) return null;
    	String _lowcase = word.toLowerCase();
    	
    	if(_WordRule.Uncountable.contains(_lowcase) 
    		|| findarray(_WordRule.Singulars,_lowcase)>=0)
    	{
    		return word;
    	}
    	
    	int i = findarray(_WordRule.Plurals,_lowcase);
    	if(i>=0)
    	{
    		return _WordRule.Singulars[i];
    	}
    	
    	for(_WordRule r:_WordRule.SINGULAR_RULES)
    	{
    		Matcher matcher = r.pattern.matcher(word);
    		if(matcher.find())
    		{
    			return matcher.replaceAll(r.replaced);
    		}
    	}
    	
    	return word;
    }
    
}
