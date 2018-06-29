package com.xiaoyi.orderpayment.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.braintreegateway.Subscription;
import com.braintreegateway.Subscription.Status;
import com.xiaoyi.orderpayment.model.OrderInfo;
import com.xiaoyi.orderpayment.service.IAppInfoService;
import com.xiaoyi.orderpayment.service.IBraintreeService;
import com.xiaoyi.orderpayment.service.IOrderService;
import com.xiaoyi.orderpayment.service.IScheduledTasks;
import com.xiaoyi.orderpayment.utilities.bean.OrderStatus;
import com.xiaoyi.orderpayment.util.BraintreeUtil;

@Component
public class ScheduledTasks implements IScheduledTasks {
	private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
	private final int sleepmillis = 1000*60*10;
	
	private IAppInfoService appInfoService;
	private IOrderService orderService;
	private IBraintreeService braintreeService;
	
	@Autowired
	public ScheduledTasks(IAppInfoService appInfoService,IOrderService orderService,
			IBraintreeService braintreeService){
		this.appInfoService = appInfoService;
		this.orderService = orderService;
		this.braintreeService = braintreeService;
	}
	
//	@Scheduled(cron = "0 0 8,10,12 * * ? ")// switch to cron on production ENV, fix the rule later
	@Scheduled(fixedRate = 1000*60*60)// debugging purpose
	public void syncSubscriptionInfoFromBraintreeToAppServer(){
		int orderCodeUnit=0;
		OrderInfo orderInfo = new OrderInfo();
		while(orderCodeUnit < 10){
			orderInfo.setOrderCode(String.valueOf(orderCodeUnit));
			
			List<OrderInfo> orderList = orderService.getParticalOrdersToSyncInfoFromBTToOrderpayment(orderInfo);
			if(orderList !=null){
				for(OrderInfo order : orderList){
					Subscription subscription = braintreeService.getSubscriptionFromBraintree(order.getOrderCode());
					Status status = subscription.getStatus();
					if(status == Subscription.Status.CANCELED){
						BraintreeUtil.updateSubscriptionInfo(subscription, braintreeService, appInfoService);
						BraintreeUtil.updateOrderInfo(subscription, orderService, OrderStatus.invalid.value());
						logger.info("ScheduledTasks:syncSubscriptionInfoFromBraintreeToAppServer()-CANCELED:The info of subscription-"
								+ "{}  are sync-ed",subscription.getId());
					}else if(status == Subscription.Status.PAST_DUE){
						BraintreeUtil.updateSubscriptionInfo(subscription,braintreeService,appInfoService);
						logger.info("ScheduledTasks:syncSubscriptionInfoFromBraintreeToAppServer()-PAST_DUE:The info of subscription-"
								+ "{}  are sync-ed",subscription.getId());
					}else if(status == Subscription.Status.ACTIVE){
						BraintreeUtil.updateSubscriptionInfo(subscription,braintreeService,appInfoService);
						logger.info("ScheduledTasks:syncSubscriptionInfoFromBraintreeToAppServer()-ACTIVE:The info of subscription-"
								+ "{}  are sync-ed",subscription.getId());
					}else if(status == Subscription.Status.EXPIRED){
						BraintreeUtil.updateSubscriptionInfo(subscription,braintreeService,appInfoService);
						logger.info("ScheduledTasks:syncSubscriptionInfoFromBraintreeToAppServer()-EXPIRED:The info of subscription-"
								+ "{}  are sync-ed",subscription.getId());
					}else if(status == Subscription.Status.PENDING){
						BraintreeUtil.updateSubscriptionInfo(subscription,braintreeService,appInfoService);
						logger.info("ScheduledTasks:syncSubscriptionInfoFromBraintreeToAppServer()-PENDING:The info of subscription-"
								+ "{}  are sync-ed",subscription.getId());
					}	
				}
			}
			try {
				Thread.sleep(sleepmillis);
				logger.info("ScheduledTasks:syncSubscriptionInfoFromBraintreeToAppServer(): Sleep "+sleepmillis+" seconds");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			orderCodeUnit++;
		}
	}
}
