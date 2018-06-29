package com.xiaoyi.orderpayment.controller;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.braintreegateway.Subscription;
import com.braintreegateway.WebhookNotification;
import com.braintreegateway.WebhookNotification.Kind;
import com.xiaoyi.orderpayment.config.AppConfig;
import com.xiaoyi.orderpayment.service.IAppInfoService;
import com.xiaoyi.orderpayment.service.IBraintreeService;
import com.xiaoyi.orderpayment.service.IOrderService;
import com.xiaoyi.orderpayment.utilities.bean.OrderStatus;
import com.xiaoyi.orderpayment.utilities.bean.ResponseData;
import com.xiaoyi.orderpayment.utilities.constant.Constants.InternalOrderPaymentService;
import com.xiaoyi.orderpayment.utilities.controller.BaseController;
import com.xiaoyi.orderpayment.util.BraintreeUtil;

@RestController
public class BraintreeIPNController extends BaseController{
	
	private static final Logger logger = LoggerFactory.getLogger(BraintreeIPNController.class);
	
	private AppConfig appConfig;
	private IAppInfoService appInfoService;
	private IOrderService orderService;
	private IBraintreeService braintreeService;
	
	@Autowired
	public BraintreeIPNController(AppConfig appConfig,IAppInfoService appInfoService,IOrderService orderService,
			IBraintreeService braintreeService) {
		this.appConfig = appConfig;
		this.appInfoService = appInfoService;
		this.orderService = orderService;
		this.braintreeService = braintreeService;
	}
	
    @RequestMapping(value =InternalOrderPaymentService.BraintreeServerIPNPath,method=RequestMethod.POST)
    @ResponseBody
    public ResponseData webhook(
    		@RequestParam("bt_signature") String signature,
    		@RequestParam("bt_payload") String payload,HttpServletResponse response) {
    	
    	WebhookNotification webhookNotification = appConfig.braintreeGateway().webhookNotification().parse(signature, payload);
    	Kind kind=webhookNotification.getKind();
    	
    	if(kind == Kind.SUBSCRIPTION_CANCELED){
	    	Subscription subscription = webhookNotification.getSubscription();
    		BraintreeUtil.updateSubscriptionInfo(subscription,braintreeService,appInfoService);
    		BraintreeUtil.updateOrderInfo(subscription,orderService,OrderStatus.invalid.value());
    		
    		logger.info("BraintreeIPNController:webhook():Subscription -{} is canceled "
    				+ "and the info are sycned",subscription.getId());
    	}else if(kind == Kind.SUBSCRIPTION_WENT_PAST_DUE){
    		Subscription subscription = webhookNotification.getSubscription();
    		BraintreeUtil.updateSubscriptionInfo(subscription,braintreeService,appInfoService);
    		
    		logger.info("BraintreeIPNController:webhook():Subscription -{} went to PAST_DUE "
    				+ "and the info are sycned",subscription.getId());
    	}else if(kind == Kind.SUBSCRIPTION_CHARGED_SUCCESSFULLY){
    		Subscription subscription = webhookNotification.getSubscription();
    		BraintreeUtil.updateSubscriptionInfo(subscription,braintreeService,appInfoService);
    		BraintreeUtil.updateOrderInfo(subscription,orderService,OrderStatus.paid.value());
    		
    		logger.info("BraintreeIPNController:webhook():Subscription -{} charged successfully "
    				+ "and the info are sycned",subscription.getId());
    	}else if(kind == Kind.SUBSCRIPTION_CHARGED_UNSUCCESSFULLY){
    		Subscription subscription = webhookNotification.getSubscription();
    		BraintreeUtil.updateSubscriptionInfo(subscription,braintreeService,appInfoService);
    		
    		logger.info("BraintreeIPNController:webhook():Subscription -{} charged unsuccessfully "
    				+ "and the info are sycned",subscription.getId());
    	}else if(kind == Kind.SUBSCRIPTION_TRIAL_ENDED){
    		Subscription subscription = webhookNotification.getSubscription();
    		BraintreeUtil.updateSubscriptionInfo(subscription,braintreeService,appInfoService);
    		
    		logger.info("BraintreeIPNController:webhook():Subscription -{}'s trail ended "
    				+ "and the info are sync-ed",subscription.getId());
    	}else if(kind == Kind.SUBSCRIPTION_WENT_ACTIVE){
    		Subscription subscription = webhookNotification.getSubscription();
    		BraintreeUtil.updateSubscriptionInfo(subscription,braintreeService,appInfoService);
    		
    		logger.info("BraintreeIPNController:webhook():Subscription -{} went active "
    				+ "and the info are sync-ed",subscription.getId());
    	}else if(kind == Kind.SUBSCRIPTION_EXPIRED){
    		Subscription subscription = webhookNotification.getSubscription();
    		BraintreeUtil.updateSubscriptionInfo(subscription,braintreeService,appInfoService);
    		
    		logger.info("BraintreeIPNController:webhook():Subscription -{} expired "
    				+ "and the info are sync-ed",subscription.getId());
    	}
    	
    	response.setStatus(200);
        return successResponse();
    } 
}
