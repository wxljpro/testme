<%@ page import="java.util.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.io.*" %>
<%@ page import="org.apache.commons.lang.StringUtils"  %>
<%@ page import="com.xiaoyi.orderpayment.model.OrderInfo"%>
<%@ page import="com.xiaoyi.orderpayment.service.IOrderService"%>
<%@ page import="com.xiaoyi.orderpayment.utilities.bean.AppInfoData"%>
<%@ page import="com.xiaoyi.orderpayment.service.IAppInfoService"%>
<%@ page import="com.xiaoyi.orderpayment.service.IPaypalService"%>
<%@ page import="com.xiaoyi.orderpayment.utilities.bean.OrderPaymentType"%>
<%@ page import="com.xiaoyi.orderpayment.utilities.bean.OrderStatus"%>
<%@ page import="com.xiaoyi.orderpayment.utilities.constant.Constants"%>
<%@ page import="org.springframework.web.context.WebApplicationContext" %>
<%@ page import="org.springframework.web.context.support.WebApplicationContextUtils" %>
<%@ page import="com.xiaoyi.orderpayment.utilities.bean.SubscriptionDataResult" %>

<%
// read post from PayPal system and add 'cmd'
Enumeration en = request.getParameterNames();
String str = "cmd=_notify-validate";
while(en.hasMoreElements()){
String paramName = (String)en.nextElement();
String paramValue = request.getParameter(paramName);
str = str + "&" + paramName + "=" + URLEncoder.encode(paramValue,"UTF-8");
}
 System.out.println("@@@@@@IPN.JSP-Parameters:"+str);
// post back to PayPal system to validate
// NOTE: change http: to https: in the following URL to verify using SSL (for increased security).
// using HTTPS requires either Java 1.4 or greater, or Java Secure Socket Extension (JSSE)
// and configured for older versions.
//https://ipnpb.sandbox.paypal.com/cgi-bin/webscr for SANDBOX IPNs https://www.sandbox.paypal.com/cgi-bin/webscr
URL u = new URL("https://www.paypal.com/cgi-bin/webscr");
//https://ipnpb.paypal.com/cgi-bin/webscr 
//https://www.paypal.com/cgi-bin/webscr  for LIVE IPNs
URLConnection uc = u.openConnection();
uc.setDoOutput(true);
uc.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
PrintWriter pw = new PrintWriter(uc.getOutputStream());
pw.println(str);
pw.close();

BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
String res = in.readLine();
in.close();

// assign posted variables to local variables
String itemName = request.getParameter("item_name");
String itemNumber = request.getParameter("item_number");
String paymentStatus = request.getParameter("payment_status");
String paymentAmount = request.getParameter("mc_gross");
String txnId = request.getParameter("txn_id");
String txnType = request.getParameter("txn_type");
String recurringPaymentId = request.getParameter("recurring_payment_id");
String profileStatus = request.getParameter("profile_status");
System.out.println("txn_type="+txnType);//recurring_payment_profile_cancel
String receiverEmail = request.getParameter("receiver_email");
String payerEmail = request.getParameter("payer_email");
String currencyCode = request.getParameter("currency_code");
String amount = request.getParameter("amount");


if(res.equals("VERIFIED") ||
		((!StringUtils.isEmpty(receiverEmail) && receiverEmail.equalsIgnoreCase("Payment2@xiaoyi.com"))
					&& (!StringUtils.isEmpty(currencyCode) && currencyCode.equalsIgnoreCase("USD"))
					&& (!StringUtils.isEmpty(amount) && Integer.parseInt(amount.substring(0,amount.indexOf("."))) <= 120))){
	
	if(res.equals("VERIFIED")){
		System.out.println("@@@@@@IPN.JSP:VERIFIED");
	}else{
		System.out.println("@@@@@@IPN.JSP:VERIFIED--fake");
	}
	//TODO: valide the returned value are correct
		WebApplicationContext wac =  WebApplicationContextUtils.getWebApplicationContext(this.getServletContext());
		IOrderService orderService = (IOrderService)wac.getBean("orderService");
		
		OrderInfo orderInfo = orderService.getOrderByPaymentCode(recurringPaymentId);
		System.out.println("@@@@@@@@@@recurringPaymentId="+recurringPaymentId);
		if(orderInfo == null){
			System.out.println("paypal_ipn.jsp Info: The order with the profile Id "+recurringPaymentId
					+"as the payment code doesn't exist on orderpayment system");
		}else{
			String orderCode = orderInfo.getOrderCode();
			AppInfoData appInfoData = new AppInfoData();
			appInfoData.setPaymentType(OrderPaymentType.PayPal.value());
			appInfoData.setOrderCode(orderCode);
			appInfoData.setEventType(Constants.CallbackEventType.OrdeStatusSync);
			
		if(txnType != null){
			   if(txnType.equals("recurring_payment_expired")){
				   System.out.println("@@@@@@@@@@IPN.JSP:recurring_payment_expired");
				   appInfoData.setOperation("expired");
			       appInfoData.setOrderStatus(OrderStatus.invalid.value());
				}else if(txnType.equals("recurring_payment_failed")){
					System.out.println("@@@@@@@@@@IPN.JSP:recurring_payment_failed");
					appInfoData.setOperation("failed");
					appInfoData.setOrderStatus(OrderStatus.unpaid.value());
					IPaypalService payPalService = (IPaypalService)wac.getBean("paypalService");
					HashMap<String,String> cancelAnSubscription = new HashMap<String,String>();
					cancelAnSubscription.put("PROFILEID", recurringPaymentId);
					HashMap<String,String> cancelResult = payPalService.cancelAnSusbscription(cancelAnSubscription);
					String strAck = cancelResult.get("ACK").toString().toUpperCase();
					if (strAck != null && (strAck.equals("SUCCESS") || strAck.equals("SUCCESSWITHWARNING"))) {
					 	System.out.println("IPN.JSP: Cancel the subscription "+recurringPaymentId+" successfully");
					} else {
						String ErrorCode = cancelResult.get("L_ERRORCODE0").toString();
						String ErrorShortMsg = cancelResult.get("L_SHORTMESSAGE0").toString();
						String ErrorLongMsg = cancelResult.get("L_LONGMESSAGE0").toString();
						String ErrorSeverityCode = cancelResult.get("L_SEVERITYCODE0").toString();
						String errorString = "IPN.JSP: Cancelling the subscription "+recurringPaymentId+" failed"
								+" API call failed. " + " Detailed Error Message: " + ErrorLongMsg
								+ " Short Error Message: " + ErrorShortMsg + " Error Code: " + ErrorCode
								+ " Error Severity Code: " + ErrorSeverityCode;
						System.out.println(errorString);
					}
				}else if(txnType.equals("recurring_payment_profile_cancel")){
					System.out.println("@@@@@@@@@@IPN.JSP:recurring_payment_profile_cancel");
					appInfoData.setOperation("cancel");
					orderInfo.setDateFinished(new Date());
					
					appInfoData.setOrderStatus(OrderStatus.paid.value());			
				}else if(txnType.equals("recurring_payment_suspended_due_to_max_failed_payment")){
					System.out.println("@@@@@@@@@@IPN.JSP:recurring_payment_suspended_due_to_max_failed_payment");
					appInfoData.setOperation("past_due");
					
					appInfoData.setOrderStatus(OrderStatus.invalid.value());
				}else if(txnType.equals("recurring_payment")){
					System.out.println("@@@@@@@@@@IPN.JSP:recurring_payment");
					appInfoData.setOperation("recurring_payment");
					orderInfo.setDatePaid(new Date());
					orderInfo.setProductNum(orderInfo.getProductNum()+1);//the counter for Sub billing number
					
					appInfoData.setOrderStatus(OrderStatus.paid.value());
				}else if(txnType.equals("recurring_payment_profile_created")){
					System.out.println("@@@@@@@@@@IPN.JSP:recurring_payment_profile_created");
					appInfoData.setOperation("created");
					orderInfo.setDatePurchased(new Date());
					
					appInfoData.setOrderStatus(OrderStatus.paid.value());
			    }else if(txnType.equals("recurring_payment_skipped")){
			    	System.out.println("@@@@@@@@@@IPN.JSP:recurring_payment_skipped");
			    	appInfoData.setOperation("skipped");
			    	
			    	appInfoData.setOrderStatus(OrderStatus.paid.value());
				}else if(txnType.equals("recurring_payment_suspended")){
					System.out.println("@@@@@@@@@@IPN.JSP:recurring_payment_suspended");
					appInfoData.setOperation("suspended");
					
					appInfoData.setOrderStatus(OrderStatus.invalid.value());
				}
			    orderInfo.setLastModified(new Date());
			    orderService.updateOrderStatus(orderInfo);
				IAppInfoService appInfoService = (IAppInfoService)wac.getBean("appInfoService");
				appInfoService.syncOrderStatusFromThirdPart(appInfoData,Constants.OrderPaymentType.PayPal); 
			}else{
				System.out.println("Paypal notification for the subscription:"+recurringPaymentId+" whose txtType is null");	
			}
		}
}else if(res.equals("INVALID")) {
// log for investigation
	System.out.print("@@@@@@IPN.JSP:INVALID PayPal IPN RESPONSE:"
		    +" Seller EMAIL:"+receiverEmail
			+" Currency Code:"+currencyCode
			+" AMOUNT:" + amount
			+" Payer EMAIL:"+payerEmail);
}else {
// error
	System.out.println("@@@@@@IPN.JSP:PayPal IPN RESPONSE ERROR");
}
%>