package com.tern.iap.controllers;

import com.tern.dao.Model;
import com.tern.dao.RecordSet;

import com.tern.web.Controller;
import com.tern.web.Route;

@Route("/enum/$type/*")
public class EnumController extends Controller
{
	private String type;
	
	public String index()
	{
		Model model = Model.from("tn_enums");
		
		RecordSet records = model.query("etype=?", type);
		request.setAttribute("model", model);
		request.setAttribute("records", records);
		
		return "/layout/enum";
	}
}
