package com.xiaoyi.orderpayment.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface IPaypalService {
	public HashMap<String,String> callSetExpressCheckout(Map<String,String> checkoutDetails, String returnURL, String cancelURL);
	public HashMap<String,String> GetExpressCheckoutDetails(String token);
	public HashMap<String,String> DoExpressCheckoutPayment(Map<String,String>checkoutDetails, String serverName);
	public HashMap<String,String> cancelAnSusbscription(Map<String,String> cancelAnSubscription);
	public HashMap<String,String> CreateRecurringPaymentsProfile(Map<String,String>checkoutDetails);
	public HashMap<String,String> getAnSusbscriptionDetails(Map<String,String> getAnSubscriptionDetails);
	public List<HashMap> getSusbscriptionDetailsList(String[] subscriptionIDs);
	public HashMap<String,String> getTransactionDetailsWithProfileID(Map<String,String> getTransactionDetails);
}
