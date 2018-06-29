package com.xiaoyi.orderpayment.util;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMethod;

import com.braintreegateway.Subscription;
import com.braintreegateway.WebhookNotification;
import com.braintreegateway.Subscription.DurationUnit;
import com.fasterxml.jackson.core.type.TypeReference;
import com.xiaoyi.orderpayment.model.OrderInfo;
import com.xiaoyi.orderpayment.service.IAppInfoService;
import com.xiaoyi.orderpayment.service.IBraintreeService;
import com.xiaoyi.orderpayment.service.IOrderService;
import com.xiaoyi.orderpayment.utilities.bean.AppInfoData;
import com.xiaoyi.orderpayment.utilities.bean.SubscriptionDataResult;
import com.xiaoyi.orderpayment.utilities.bean.BtraintreeSubscriptionStatus;
import com.xiaoyi.orderpayment.utilities.bean.OrderPaymentType;
import com.xiaoyi.orderpayment.utilities.bean.OrderStatus;
import com.xiaoyi.orderpayment.utilities.constant.Constants;
import com.xiaoyi.orderpayment.utilities.constant.MessageConstants;
import com.xiaoyi.orderpayment.utilities.security.Authentication;

public class BraintreeUtil {
	  private static final Logger logger = LoggerFactory.getLogger(BraintreeUtil.class);
	
	   public static void updateSubscriptionInfo(Subscription subscription,IBraintreeService braintreeService,
			   IAppInfoService appInfoService){
			SubscriptionDataResult subscriptionDataResult = new SubscriptionDataResult();
			subscriptionDataResult = BraintreeUtil.subscriptinToSubscriptionDataResult(subscription);
			String customerId = braintreeService.getCustomerId(subscriptionDataResult.getSubscriptionId());
			subscriptionDataResult.setCustomerId(customerId);
			BraintreeUtil.covertCreatedDate(subscriptionDataResult, subscription);
			
			AppInfoData appInfoData = new AppInfoData();
			appInfoData.setSubscription(subscriptionDataResult);
			appInfoData.setPaymentType(OrderPaymentType.Braintree.value());
			appInfoData.setEventType(Constants.CallbackEventType.OrdeStatusSync);
			appInfoService.syncOrderStatusFromThirdPart(appInfoData);
	    }
	   
	   public static void updateOrderInfo(Subscription subscription,IOrderService orderService,
			   int orderStatus){
			OrderInfo orderInfo = orderService.getOrderByCode(subscription.getId());
			orderInfo.setOrderStatus(orderStatus);
			orderInfo.setLastModified(new Date());
			if(orderStatus == OrderStatus.paid.value()){
				orderInfo.setDatePaid(new Date());
				if(orderInfo.getProductNum() == null){
					orderInfo.setProductNum(2);
				}else{
					orderInfo.setProductNum(orderInfo.getProductNum()+1);
				}
	    	}
			orderService.updateOrderStatus(orderInfo);
	    }
		public static SubscriptionDataResult subscriptinToSubscriptionDataResult(Subscription subscription){
			 SubscriptionDataResult  subscriptionDataResult = new SubscriptionDataResult();
			 
			 subscriptionDataResult.setSubscriptionId(subscription.getId());
			 subscriptionDataResult.setPaymentMethodToken(subscription.getPaymentMethodToken());
			 subscriptionDataResult.setCreatedAtDate(subscription.getCreatedAt().getTime());
		   	 subscriptionDataResult.setFirstBillingDate(subscription.getFirstBillingDate().getTime());
		   	 subscriptionDataResult.setNextBillingDate(subscription.getNextBillingDate().getTime());
		     subscriptionDataResult.setPrice(subscription.getPrice());
		     subscriptionDataResult.setNextBillingAmount(subscription.getNextBillAmount());
		   	if(subscription.hasTrialPeriod()){
			   	subscriptionDataResult.setTrialDuration(subscription.getTrialDuration());
			   	DurationUnit unit =subscription.getTrialDurationUnit();
			   	if(unit.ordinal() == 0){
			   		subscriptionDataResult.setTrialDurationUnit(1);	
			   	}else if(unit.ordinal() == 1){
			   		subscriptionDataResult.setTrialDurationUnit(2);
			   	}
		   	}else{
		   		subscriptionDataResult.setTrialDuration(0);
		   		subscriptionDataResult.setTrialDurationUnit(0);
		   	}
		   	
		   	if(subscription.getPaidThroughDate() !=null){
		   		subscriptionDataResult.setPaidThroughDate(subscription.getPaidThroughDate().getTime());
		   	}
		   	if(subscription.getBillingPeriodStartDate() !=null){
		   		subscriptionDataResult.setBillingStartDate(subscription.getBillingPeriodStartDate().getTime());
		   	}
		   	if(subscription.getBillingPeriodEndDate() !=null){
		   	 subscriptionDataResult.setBillingEndDate(subscription.getBillingPeriodEndDate().getTime());
		   	}
		   	
		   	if(subscription.getStatus()==Subscription.Status.ACTIVE){
		   		subscriptionDataResult.setStatus(BtraintreeSubscriptionStatus.active.value());
		   		boolean ifInTrial = false;
			   	if(subscription.hasTrialPeriod()){
			   		if(subscription.getFirstBillingDate().getTime().equals(subscription.getNextBillingDate().getTime())){
			   			ifInTrial = true;
			   		}
			   	}
			   	if(ifInTrial){
			   		subscriptionDataResult.setStatus(BtraintreeSubscriptionStatus.intrial.value());
			   	}
		   	}else if(subscription.getStatus() == Subscription.Status.PAST_DUE){
		   		subscriptionDataResult.setStatus(BtraintreeSubscriptionStatus.past_due.value());
		   	}else if(subscription.getStatus() == Subscription.Status.CANCELED){
		   		subscriptionDataResult.setStatus(BtraintreeSubscriptionStatus.cancel.value());
		   	}else if(subscription.getStatus() == Subscription.Status.PENDING){
		   		subscriptionDataResult.setStatus(BtraintreeSubscriptionStatus.pending.value());
		   	}else if(subscription.getStatus() == Subscription.Status.EXPIRED){
		   		subscriptionDataResult.setStatus(BtraintreeSubscriptionStatus.expired.value());
		   	}
			 return subscriptionDataResult;
		}
		
		public static void covertCreatedDate(SubscriptionDataResult subscriptionDataResult,Subscription subscription){
    	   	TimeZone.setDefault(TimeZone.getTimeZone("GMT-6"));
    	   	Calendar cal = subscription.getCreatedAt();
    	   	subscriptionDataResult.setCreatedAtDate(cal.getTime());
		}
}
