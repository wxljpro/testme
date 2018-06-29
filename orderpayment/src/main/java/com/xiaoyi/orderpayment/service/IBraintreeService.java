package com.xiaoyi.orderpayment.service;

import java.util.Calendar;
import java.util.List;

import com.braintreegateway.Subscription.DurationUnit;
import com.braintreegateway.Subscription;
import com.braintreegateway.Transaction;
import com.xiaoyi.orderpayment.model.BraintreePlanInfo;

public interface IBraintreeService {
	BraintreePlanInfo getPlanInfoBySkuAndServiceTime(Integer sku, Integer serviceTime);
	boolean cancelAnSubscription(String subscriptionId);
	boolean checkSubscriptionBeforeCancel(String subscriptionId);
	Calendar getFirstBillingDateForCancelToNew(String previousSubscriptionId);
	boolean checkIfSingleActiveSubscription(String paymentMethodToken);
	boolean updateSubscriptionWhenCancelToNew(String subscriptionId,String previousSubscription);
	Subscription getSubscriptionFromBraintree(String subscriptionId);
	String getCustomerId(String subscriptionId);
	List<Transaction> getTranscationsUnderSubscription(String subscriptionId);
	int getTrialDuration(String planId);
	DurationUnit getTrialDurationUnit(String subscriptionId);
}
