package com.bitian.db.mybatis_helper;


public class PageHelper {
	private static ThreadLocal<Page> st=new ThreadLocal<>();
	
	public static void startPage(int pn,int ps){
		if(pn<1){
			pn=1;
		}
		if(ps<1){
			ps=10;
		}
		Page page=new Page();
		page.setPn(pn);
		page.setPs(ps);
		st.set(page);
	}
	
	protected static Page getCurrentPage(){
		return st.get();
	}
	
	public static Page getPage(){
		Page page=st.get();
		st.remove();
		return page;
	}
}
