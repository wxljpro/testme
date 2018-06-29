<%
/* *
This jsp page is prepared to let Alipay service invoke to notify 
our backend server asynchronously about the order status once the customers 
submit the requests to Alipay to pay the bill for our services/products through our App
 * */
%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*" %>
<%@ page import="java.text.*" %>
<%@ page import="com.xiaoyi.orderpayment.model.OrderInfo"%>
<%@ page import="com.xiaoyi.orderpayment.utilities.bean.AppInfoData"%>
<%@ page import="com.xiaoyi.orderpayment.service.impl.AppInfoService"%>
<%@ page import="com.xiaoyi.orderpayment.service.IAppInfoService"%>
<%@ page import="com.xiaoyi.orderpayment.service.IOrderService"%>
<%@ page import="com.xiaoyi.orderpayment.utilities.utilities.AlipayAPI"%>
<%@ page import="com.xiaoyi.orderpayment.utilities.bean.OrderPaymentType"%>
<%@ page import="com.xiaoyi.orderpayment.utilities.bean.OrderStatus"%>
<%@ page import="com.xiaoyi.orderpayment.utilities.constant.Constants"%>
<%@ page import="org.springframework.web.context.WebApplicationContext" %>
<%@ page import="org.springframework.web.context.support.WebApplicationContextUtils" %>
<% 
Map<String,String> params = new HashMap<String,String>();
Map requestParams = request.getParameterMap();
for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext();) {
	String name = (String) iter.next();
	String[] values = (String[]) requestParams.get(name);
	String valueStr = "";
	for (int i = 0; i < values.length; i++) {
		valueStr = (i == values.length - 1) ? valueStr + values[i]
				: valueStr + values[i] + ",";
	}
	//乱码解决，这段代码在出现乱码时使用。如果mysign和sign不相等也可以使用这段代码转化
	//valueStr = new String(valueStr.getBytes("ISO-8859-1"), "gbk");
	params.put(name, valueStr);
}

String out_trade_no = new String(request.getParameter("out_trade_no").getBytes("ISO-8859-1"),"UTF-8");
String trade_status = new String(request.getParameter("trade_status").getBytes("ISO-8859-1"),"UTF-8");
String trade_no     = new String(request.getParameter("trade_no").getBytes("ISO-8859-1"),"UTF-8");

if(trade_status.equals("TRADE_FINISHED") || trade_status.equals("TRADE_SUCCESS")){
	trade_status = OrderStatus.paid.toString();
}else if(trade_status.equals("TRADE_CLOSED")){
	trade_status = OrderStatus.invalid.toString();
}else if(trade_status.equals("WAIT_BUYER_PAY")){
	trade_status = OrderStatus.unpaid.toString();
}

if(AlipayAPI.verify(params)){
	WebApplicationContext wac =  WebApplicationContextUtils.getWebApplicationContext(this.getServletContext());
	IOrderService orderService = (IOrderService)wac.getBean("orderService");
	OrderInfo orderInfo = orderService.getOrderByCode(out_trade_no);
	if(orderInfo == null){
		System.out.println("notify_url.jsp Info: The order-"+out_trade_no+" doesn't exist on orderpayment system");
		out.println("fail");
	}else{
 		orderInfo.setPaymentCode(trade_no);
		orderInfo.setOrderStatus(Integer.parseInt(trade_status));
		orderInfo.setPaymentType(OrderPaymentType.Alipay.value());  
		orderInfo.setDatePaid(new Date());
		orderInfo.setLastModified(new Date());
		orderService.updateOrderStatus(orderInfo);
		System.out.println("notify_url.jsp Info:updated the order-"+out_trade_no+" status into the orderpayment db successfully");
		
		AppInfoData appInfoData = new AppInfoData();
		appInfoData.setPaymentType(OrderPaymentType.Alipay.value());
		appInfoData.setOrderCode(out_trade_no);
		appInfoData.setOrderStatus(Integer.parseInt(trade_status));
		appInfoData.setEventType(Constants.CallbackEventType.OrdeStatusSync);
		
		IAppInfoService appInfoService = (IAppInfoService)wac.getBean("appInfoService");
		appInfoService.syncOrderStatusFromThirdPart(appInfoData);
		System.out.println("notify_url.jsp Info:appInfoService.syncOrderStatusFromThirdPart() sync the order-"+out_trade_no+" status successfully");
		
		out.println("success");
	}
}else{
	System.out.println("JSP Debug Info: verify params failed");
	out.println("fail");
}
%>