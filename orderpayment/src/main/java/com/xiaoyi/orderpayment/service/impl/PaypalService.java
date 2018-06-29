package com.xiaoyi.orderpayment.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xiaoyi.orderpayment.config.AppConfig;
import com.xiaoyi.orderpayment.service.IPaypalService;
import com.xiaoyi.orderpayment.util.PayPalUtil;

@Service
public class PaypalService implements IPaypalService {
	
	private static final Logger logger = LoggerFactory.getLogger(PaypalService.class);
	
	private AppConfig appConfig;	
	private PayPalUtil payPalUtil;
	
	@Autowired
	public PaypalService(AppConfig appConfig){
		this.appConfig = appConfig;
		payPalUtil = new PayPalUtil(appConfig);
	}
		
	@Override
	public HashMap<String, String> callSetExpressCheckout(Map<String,String> checkoutDetails, String returnURL, String cancelURL){
		StringBuilder nvpstr = new StringBuilder("");
		
		if(payPalUtil.isSet(checkoutDetails.get("PAYMENTREQUEST_0_AMT") )){
			nvpstr.append( "&PAYMENTREQUEST_0_AMT=").append(checkoutDetails.get("PAYMENTREQUEST_0_AMT"));
		}
		
		if(payPalUtil.isSet(returnURL))
			nvpstr.append("&RETURNURL=").append(returnURL);

		if(payPalUtil.isSet(cancelURL))
			nvpstr.append( "&CANCELURL=").append(cancelURL);
		
		if(payPalUtil.isSet(checkoutDetails.get("LOCALECODE")))
			nvpstr.append( "&LOCALECODE=").append(checkoutDetails.get("LOCALECODE"));

		if(payPalUtil.isSet(checkoutDetails.get("currencyCodeType") ))
			nvpstr.append( "&PAYMENTREQUEST_0_CURRENCYCODE=").append(checkoutDetails.get("currencyCodeType"));
		
		if(payPalUtil.isSet(checkoutDetails.get("L_BILLINGTYPE0") ))
			nvpstr.append( "&L_BILLINGTYPE0=").append(checkoutDetails.get("L_BILLINGTYPE0"));
		
		if(payPalUtil.isSet(checkoutDetails.get("L_BILLINGAGREEMENTDESCRIPTION0") ))
			nvpstr.append( "&L_BILLINGAGREEMENTDESCRIPTION0=").append(checkoutDetails.get("L_BILLINGAGREEMENTDESCRIPTION0"));

		if(payPalUtil.isSet(checkoutDetails.get("noshipping") ))
			nvpstr.append( "&noshipping=").append(checkoutDetails.get("noshipping"));

//		System.out.println("Request str :" + nvpstr.toString());
		return payPalUtil.httpcall("SetExpressCheckout", nvpstr.toString());    
	}
	
	@Override
	public HashMap GetExpressCheckoutDetails(String token)
	{
	    String nvpstr= "&TOKEN=" + token;

	    return payPalUtil.httpcall("GetExpressCheckoutDetails", nvpstr);
	     
	}
	
	@Override
	public HashMap DoExpressCheckoutPayment(Map<String,String>checkoutDetails, String serverName){
		/* Gather the information to make the final call to finalize the PayPal payment.  The variable nvpstr
	     * holds the name value pairs
		 */
		String finalPaymentAmount = payPalUtil.encode(checkoutDetails.get("PAYMENTREQUEST_0_AMT"));
		
		
		StringBuilder nvpstr = new StringBuilder("");
		//mandatory parameters in DoExpressCheckoutPayment call
		if(payPalUtil.isSet(checkoutDetails.get("TOKEN")))
		nvpstr.append("&TOKEN=").append(payPalUtil.encode(checkoutDetails.get("TOKEN")));

		if(payPalUtil.isSet(checkoutDetails.get("payer_id")))
		nvpstr.append( "&PAYERID=").append(payPalUtil.encode(checkoutDetails.get("payer_id")));
		
		nvpstr.append( "&PAYMENTREQUEST_0_SELLERPAYPALACCOUNTID=").append(payPalUtil.sellerEmail);
		
		if(payPalUtil.isSet(checkoutDetails.get("paymentType")))
		nvpstr.append( "&PAYMENTREQUEST_0_PAYMENTACTION=").append(payPalUtil.encode(checkoutDetails.get("paymentType"))); 
		
		if(payPalUtil.isSet(serverName))
		nvpstr.append( "&IPADDRESS=").append(payPalUtil.encode(serverName));

		nvpstr.append( "&PAYMENTREQUEST_0_AMT=").append(finalPaymentAmount);

		//Check for additional parameters that can be passed in DoExpressCheckoutPayment API call
		if(payPalUtil.isSet(checkoutDetails.get("currencyCodeType")))
		nvpstr.append( "&PAYMENTREQUEST_0_CURRENCYCODE=").append(payPalUtil.encode(checkoutDetails.get("currencyCodeType").toString()));
		 
		return payPalUtil.httpcall("DoExpressCheckoutPayment", nvpstr.toString());
	}
	
	@Override
	public HashMap cancelAnSusbscription(Map<String,String> cancelAnSubscription){
		StringBuilder nvpstr = new StringBuilder("");
		nvpstr.append("&ACTION=").append("Cancel");	//Cancel, Suspend, Reactivate
		if(payPalUtil.isSet(cancelAnSubscription.get("PROFILEID"))){
			nvpstr.append("&PROFILEID=").append(cancelAnSubscription.get("PROFILEID"));
		}
		return payPalUtil.httpcall("ManageRecurringPaymentsProfileStatus",nvpstr.toString());
	}

	@Override
	public HashMap CreateRecurringPaymentsProfile(Map<String,String> checkoutDetails){
		String finalPaymentAmount = payPalUtil.encode(checkoutDetails.get("AMT"));		
		
		StringBuilder nvpstr = new StringBuilder("");
		//mandatory parameters in DoExpressCheckoutPayment call
		nvpstr.append( "&AMT=").append(finalPaymentAmount);
		if(payPalUtil.isSet(checkoutDetails.get("TOKEN")))
			nvpstr.append("&TOKEN=").append(payPalUtil.encode(checkoutDetails.get("TOKEN")));
		if(payPalUtil.isSet(checkoutDetails.get("PROFILESTARTDATE")))
			nvpstr.append( "&PROFILESTARTDATE=").append(payPalUtil.encode(checkoutDetails.get("PROFILESTARTDATE")));
		if(payPalUtil.isSet(checkoutDetails.get("BILLINGPERIOD")))
			nvpstr.append( "&BILLINGPERIOD=").append(payPalUtil.encode(checkoutDetails.get("BILLINGPERIOD")));
		if(payPalUtil.isSet(checkoutDetails.get("BILLINGFREQUENCY")))
			nvpstr.append( "&BILLINGFREQUENCY=").append(payPalUtil.encode(checkoutDetails.get("BILLINGFREQUENCY")));
		if(payPalUtil.isSet(checkoutDetails.get("TOTALBILLINGCYCLES")))
			nvpstr.append( "&TOTALBILLINGCYCLES=").append(payPalUtil.encode(checkoutDetails.get("TOTALBILLINGCYCLES")));
		if(payPalUtil.isSet(checkoutDetails.get("CURRENCYCODE")))
			nvpstr.append( "&CURRENCYCODE=").append(payPalUtil.encode(checkoutDetails.get("CURRENCYCODE")));
		if(payPalUtil.isSet(checkoutDetails.get("EMAIL")))
			nvpstr.append( "&EMAIL=").append(payPalUtil.encode(checkoutDetails.get("EMAIL")));
		if(payPalUtil.isSet(checkoutDetails.get("DESC")))
			nvpstr.append( "&DESC=").append(checkoutDetails.get("DESC")); 
		if(payPalUtil.isSet(checkoutDetails.get("currencyCodeType")))
			nvpstr.append( "&PAYMENTREQUEST_0_CURRENCYCODE=").append(payPalUtil.encode(checkoutDetails.get("currencyCodeType").toString()));
		
		boolean flag = false;
		if(checkoutDetails.containsKey("IFTRIEDOUTALREADY")){
			flag = Boolean.parseBoolean(checkoutDetails.get("IFTRIEDOUTALREADY"));
		}
		if(!flag){
			//add trial period
			if(payPalUtil.isSet(checkoutDetails.get("TRIALBILLINGPERIOD")))
				nvpstr.append( "&TRIALBILLINGPERIOD=").append(payPalUtil.encode(checkoutDetails.get("TRIALBILLINGPERIOD")));
			if(payPalUtil.isSet(checkoutDetails.get("TRIALBILLINGFREQUENCY")))
				nvpstr.append( "&TRIALBILLINGFREQUENCY=").append(payPalUtil.encode(checkoutDetails.get("TRIALBILLINGFREQUENCY")));
			if(payPalUtil.isSet(checkoutDetails.get("TRIALTOTALBILLINGCYCLES")))
				nvpstr.append( "&TRIALTOTALBILLINGCYCLES=").append(payPalUtil.encode(checkoutDetails.get("TRIALTOTALBILLINGCYCLES")));
			if(payPalUtil.isSet(checkoutDetails.get("TRIALAMT")))
				nvpstr.append( "&TRIALAMT=").append(payPalUtil.encode(checkoutDetails.get("TRIALAMT")));
		}
		nvpstr.append("&MAXFAILEDPAYMENTS=").append("1");
//		nvpstr.append("&L_PAYMENTREQUEST_0_ITEMCATEGORY0=").append("Digital");
		//allow 1 time payment failure before suspend the subscription
		/*
		    Make the call to PayPal to finalize payment
		    If an error occurred, show the resulting errors
		  */
		return payPalUtil.httpcall("CreateRecurringPaymentsProfile", nvpstr.toString());
	}
	
	@Override
	public HashMap getAnSusbscriptionDetails(Map<String,String> getAnSubscriptionDetails){
		StringBuilder nvpstr = new StringBuilder("");
		if(payPalUtil.isSet(getAnSubscriptionDetails.get("PROFILEID"))){
			nvpstr.append("&PROFILEID=").append(getAnSubscriptionDetails.get("PROFILEID"));
		}
		return payPalUtil.httpcall("GetRecurringPaymentsProfileDetails",nvpstr.toString());
	}
	
	@Override
	public List<HashMap> getSusbscriptionDetailsList(String[] subscriptionId){
		List<HashMap> subscriptionList = new ArrayList<HashMap>();
		for (String item:subscriptionId){
			HashMap<String,String> getAnSubscriptionDetails = new HashMap<String,String>();
			getAnSubscriptionDetails.put("PROFILEID", item);
			HashMap subscription = getAnSusbscriptionDetails(getAnSubscriptionDetails);
			subscriptionList.add(subscription);
		}
		
		return subscriptionList;
	}
	
	@Override
	public HashMap getTransactionDetailsWithProfileID(Map<String,String> getTransactionDetails){
		StringBuilder nvpstr = new StringBuilder("");
		if(payPalUtil.isSet(getTransactionDetails.get("start_date"))){
			nvpstr.append("&STARTDATE=").append(getTransactionDetails.get("start_date"));
		}
		if(payPalUtil.isSet(getTransactionDetails.get("end_date"))){
			nvpstr.append("&ENDDATE=").append(getTransactionDetails.get("end_date"));
		}
		if(payPalUtil.isSet(getTransactionDetails.get("emaild"))){
			nvpstr.append("&EMAIL=").append(getTransactionDetails.get("email"));
		}
		if(payPalUtil.isSet(getTransactionDetails.get("PROFILEID"))){
			nvpstr.append("&PROFILEID=").append(getTransactionDetails.get("PROFILEID"));
		}
		return payPalUtil.httpcall("TransactionSearch",nvpstr.toString());
	}
}
