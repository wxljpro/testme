package com.xiaoyi.orderpayment.service.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.braintreegateway.PaymentMethod;
import com.braintreegateway.Plan;
import com.braintreegateway.Result;
import com.braintreegateway.Subscription;
import com.braintreegateway.SubscriptionRequest;
import com.braintreegateway.Transaction;
import com.braintreegateway.ValidationError;
import com.braintreegateway.Subscription.DurationUnit;
import com.braintreegateway.Subscription.Status;
import com.braintreegateway.exceptions.NotFoundException;
import com.xiaoyi.orderpayment.config.AppConfig;
import com.xiaoyi.orderpayment.dao.BraintreePlanInfoMapper;
import com.xiaoyi.orderpayment.model.BraintreePlanInfo;
import com.xiaoyi.orderpayment.model.BraintreePlanInfoExample;
import com.xiaoyi.orderpayment.service.IBraintreeService;
import com.xiaoyi.orderpayment.utilities.bean.SubscriptionDataResult;
import com.xiaoyi.orderpayment.utilities.constant.MessageConstants;
import com.xiaoyi.orderpayment.utilities.constant.Constants.RequestParamNames;

@Service
public class BraintreeService implements IBraintreeService{
	private static final Logger logger = LoggerFactory.getLogger(BraintreeService.class);

	private AppConfig appConfig;

	@Autowired
	public BraintreeService(AppConfig appConfig) {
		this.appConfig = appConfig;
	}
	
	@Override
	public boolean checkSubscriptionBeforeCancel(String subscriptionId){
		try{
			Subscription subscription = appConfig.braintreeGateway()
													.subscription()
													.find(subscriptionId);
			if(subscription.getStatus() == Subscription.Status.ACTIVE ||
					subscription.getStatus() == Subscription.Status.PENDING){
				 return  true;
			}
			return false;
		}catch(NotFoundException e){
			return false;
		}
	}
	
	@Override
	public boolean cancelAnSubscription(String subscriptionId){
    	try{
    		Result<Subscription> result = appConfig.braintreeGateway().subscription().cancel(subscriptionId);
    		if(result.isSuccess()){
    			return true;
    		}else{
//    	   		List<ValidationError> cancelSubErrors = result.getErrors()
//    	   				.forObject("paymentmethod")
//    	   				.forObject("options")
//    	   				.forObject("failOnDuplicatePaymentMethod")
//    	   				.getAllValidationErrors();
    			for(ValidationError error : result.getErrors().getAllDeepValidationErrors()){
    				logger.error("OrderPaymentController:cancelSubscription():"+error.getMessage());
    			}
    			return false;
    		}
    	}catch(Exception e){
    		e.printStackTrace();
    		return false;
    	}
	}
	
	@Override
	public BraintreePlanInfo getPlanInfoBySkuAndServiceTime(Integer sku, Integer serviceTime){
		SqlSession session = appConfig.sqlSessionFactory().openSession();
		try {
			BraintreePlanInfoMapper mapper = session.getMapper(BraintreePlanInfoMapper.class);
			BraintreePlanInfoExample braintreePlanInfoExample = new BraintreePlanInfoExample();
			braintreePlanInfoExample.createCriteria().andSkuEqualTo(sku).andServicetimeEqualTo(serviceTime);
			List<BraintreePlanInfo> results = mapper.selectByExample(braintreePlanInfoExample);
			
			return results.isEmpty() ? null : results.get(0);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		return null;
	}

	@Override
	public Calendar getFirstBillingDateForCancelToNew(String previousSubscriptionId){
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());	
		try{
////			PaymentMethod paymentMethod = appConfig.braintreeGateway().paymentMethod().find(paymentMethodToken);
////			List<Subscription> subList = paymentMethod.getSubscriptions();
//			max = subList.get(0).getPaidThroughDate();
//			if(max==null){
//				max = subList.get(0).getFirstBillingDate();
//			}
//			for(int i=1;i<subList.size();i++){
//				if(subList.get(i).getStatus()==Subscription.Status.CANCELED){
//					Calendar current = subList.get(i).getPaidThroughDate();
//					if(current.compareTo(max) >= 0){
//						max = current;
//					}
//				}
//			}
			Subscription previousCanceledSubscription = getSubscriptionFromBraintree(previousSubscriptionId);
			boolean  hasTrialPeriod = previousCanceledSubscription.hasTrialPeriod();
			Status status = previousCanceledSubscription.getStatus();

			if(previousCanceledSubscription!=null){
				Calendar paidThroughDate = previousCanceledSubscription.getPaidThroughDate();
				if(paidThroughDate != null){
					if(paidThroughDate.compareTo(calendar) >= 0){
						paidThroughDate.add(Calendar.DAY_OF_MONTH, 1);
						return paidThroughDate;
					}
				}else if(hasTrialPeriod && status == Status.CANCELED){
					Calendar createdAtCalendar = previousCanceledSubscription.getCreatedAt();
					createdAtCalendar.add(Calendar.MONTH, previousCanceledSubscription.getTrialDuration());
					
					if(createdAtCalendar.compareTo(calendar) >= 0){
						createdAtCalendar.add(Calendar.DAY_OF_MONTH, 1);
						return createdAtCalendar;
					}
				}
			}
		}catch(NotFoundException e){
			e.printStackTrace();
			return null;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
		return calendar;
	}
	
	@Override
	public boolean checkIfSingleActiveSubscription(String paymentMethodToken){
		try{
	   		PaymentMethod paymentMethod = appConfig.braintreeGateway().paymentMethod().find(paymentMethodToken);	
	   		List<Subscription> subscriptionList = paymentMethod.getSubscriptions();
	   		int activeSubscription = 0;
	   		for(Subscription sub : subscriptionList){
	   			if(sub.getStatus() == Subscription.Status.ACTIVE){
	   				activeSubscription++;
	   			}
	   		}
	   		if(activeSubscription != 1){
    			return false;
	   		}
	   	}catch(NotFoundException e){
	   		e.printStackTrace();
	   	}catch(Exception e){
	   		e.printStackTrace();
	   	}
		return true;
	}
	
	@Override
	public boolean updateSubscriptionWhenCancelToNew(String subscriptionId,String previousSubscriptionId){
		SubscriptionRequest updateSubrequest = new SubscriptionRequest()
					.trialPeriod(false)
					.firstBillingDate(getFirstBillingDateForCancelToNew(previousSubscriptionId));
			Result<Subscription> updateSubResult = appConfig.braintreeGateway().subscription().update(subscriptionId, updateSubrequest);
			if(updateSubResult.isSuccess()){
				return true;	
			}else{
				for (ValidationError error : updateSubResult.getErrors().getAllDeepValidationErrors()) {
			        logger.error("OrderPaymentController:newSubscription():Creating the customer failed:"+error.getMessage());
			    }
			}
			return false;
	}
	
	@Override
	public Subscription getSubscriptionFromBraintree(String subscriptionId){
		try{
		   	Subscription subscription = appConfig.braintreeGateway().subscription().find(subscriptionId);
		   	return subscription;
	   }catch(NotFoundException e){
		   System.err.println(e.getMessage());
		   return null;
	   }catch(Exception e){
		   System.err.println(e.getMessage());
		   return null;
	   }
	}
	
	@Override
	public String getCustomerId(String subscriptionId){
		try{
			Subscription subscription = getSubscriptionFromBraintree(subscriptionId);
			if(subscription == null){
				return null;
			}
			String paymentMethodToken = subscription.getPaymentMethodToken();
			PaymentMethod paymentMethod = appConfig.braintreeGateway().paymentMethod().find(paymentMethodToken);
			return paymentMethod.getCustomerId();
		}catch(NotFoundException e){
			e.printStackTrace();
			return null;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public List<Transaction> getTranscationsUnderSubscription(String subscriptionId){
		try{
			Subscription subscription = getSubscriptionFromBraintree(subscriptionId);
			if(subscription == null){
				return null;
			}
			List<Transaction> transactions = subscription.getTransactions();
			return transactions;
		}catch(NotFoundException e){
			e.printStackTrace();
			return null;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	public int getTrialDuration(String planId){
		int trialDuration = 0;
		try{
			List<Plan> plans = appConfig.braintreeGateway().plan().all();
	    	for(Plan plan : plans){
	    		if(plan.getId().equals(planId)){
	    			trialDuration = plan.getTrialDuration();
	    		}
	    	}
	    	return trialDuration;
		}catch(NotFoundException e){
			e.printStackTrace();
			return trialDuration;
		}
	}
	
	@Override 
	public DurationUnit getTrialDurationUnit(String subscriptionId){
		DurationUnit durationUnit = null;
		try{
			Subscription subscription = getSubscriptionFromBraintree(subscriptionId);
			if(subscription == null){
				return durationUnit;
			}
	    	return subscription.getTrialDurationUnit();
		}catch(NotFoundException e){
				e.printStackTrace();
				return durationUnit;
		}
	}
}
