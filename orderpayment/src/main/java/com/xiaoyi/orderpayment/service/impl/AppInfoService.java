package com.xiaoyi.orderpayment.service.impl;

import com.xiaoyi.orderpayment.service.IAppInfoService;
import com.xiaoyi.orderpayment.utilities.filter.IAuthInfoService;
import com.xiaoyi.orderpayment.config.AppConfig;
import com.xiaoyi.orderpayment.dao.AppInfoMapper;
import com.xiaoyi.orderpayment.utilities.bean.AppInfoData;
import com.xiaoyi.orderpayment.utilities.bean.OrderPaymentType;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xiaoyi.orderpayment.model.AppInfo;
import com.xiaoyi.orderpayment.model.AppInfoExample;
import com.xiaoyi.orderpayment.utilities.constant.Constants;
import com.xiaoyi.orderpayment.utilities.constant.MessageConstants;
import com.xiaoyi.orderpayment.utilities.security.Authentication;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AppInfoService implements IAppInfoService, IAuthInfoService {
	private static final Logger logger = LoggerFactory.getLogger(AppInfoService.class);

	private AppConfig appConfig;

	@Autowired
	public AppInfoService(AppConfig appConfig) {
		this.appConfig = appConfig;
	}
	
	@Override
	public boolean syncOrderStatusFromThirdPart(AppInfoData appInfoData,String paymentMethod){
		SqlSession session = appConfig.sqlSessionFactory().openSession();
		try{
			AppInfoMapper mapper = session.getMapper(AppInfoMapper.class);
			List<AppInfo> appInfoList = mapper.selectByExample(new AppInfoExample());
			for(AppInfo appInfo : appInfoList) {
				switch(paymentMethod){
					case Constants.OrderPaymentType.FreeCharge:
							
						break;
					case Constants.OrderPaymentType.Alipay:
						appServiceAlipay(appInfoData,appInfo);
						break;
					case Constants.OrderPaymentType.PayPal:
						appServicePayPal(appInfoData,appInfo);
						break;
					case Constants.OrderPaymentType.Braintree:
						appServiceBraintree(appInfoData,appInfo);
						break;
					case Constants.OrderPaymentType.DHPay:
						appServiceDHPay(appInfoData ,appInfo);
						break;
					case Constants.OrderPaymentType.ChargeCard:
						
						break;
					default:
						
						break;
				}
			}
		}catch(Exception e){
			logger.warn("AppInfoService:sycnOrderStatusFromThirdPart():request the order status failed");
			e.printStackTrace();
			return false;
		}finally{
				session.close();
		}
		return false;
	}
	
	@Override
	public boolean syncOrderStatusFromThirdPart(AppInfoData appInfoData) {
		SqlSession session = appConfig.sqlSessionFactory().openSession();
		AppInfo appInfo = null;
		int type 	   = appInfoData.getPaymentType();
		String payName = (OrderPaymentType.getByValue(type)).payName();
		try{
			AppInfoMapper mapper = session.getMapper(AppInfoMapper.class);
			List<AppInfo> appInfoList = mapper.selectByExample(new AppInfoExample());
			appInfo = getAppInfoByPayName(appInfoList,payName);
			if(appInfo == null){
				return false;
			}
			switch(OrderPaymentType.getByValue(appInfoData.getPaymentType())){
				case FreeCharge:
					
					break;
				case Alipay:
					appServiceAlipay(appInfoData,appInfo);
					break;
				case PayPal:
					appServicePayPal(appInfoData,appInfo);
					break;
				case DHPay:
					appServiceDHPay(appInfoData,appInfo);
					break;
				case Braintree:
					appServiceBraintree(appInfoData,appInfo);
					break;
				case ChargeCard:
					
					break;
				default:
					
					break;
			}
		}catch(Exception e){
			logger.warn("AppInfoService:sycnOrderStatusFromThirdPart():request the order status failed");
			e.printStackTrace();
			return false;
		}finally{
				session.close();
		}
		return false;
	}

	@Override
	public String getAppKey(String appId){
		SqlSession session = appConfig.sqlSessionFactory().openSession();
		try{
			AppInfoMapper mapper = session.getMapper(AppInfoMapper.class);
			AppInfoExample example = new AppInfoExample();
			example.createCriteria().andAppIdEqualTo(appId);
			List<AppInfo> result = mapper.selectByExample(example);
			if (result.size() != 1) {
				logger.warn("found {} appInfo for app id={}", result.size(), appId);
				return null;
			}
			return result.get(0).getAppKey();
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		return null;
	}
	
	@Override
	public AppInfo getCallBackUrlByAppId(String appId){
		SqlSession session = appConfig.sqlSessionFactory().openSession();
		try{
			AppInfoMapper mapper = session.getMapper(AppInfoMapper.class);
			AppInfoExample example = new AppInfoExample();
			example.createCriteria().andAppIdEqualTo(appId);
			List<AppInfo> result = mapper.selectByExample(example);
			if (result.size() != 1) {
				logger.warn("found {} appInfo for app id={}", result.size(), appId);
				return null;
			}
			return result.get(0);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		return null;
	}
	
	private boolean appServiceDHPay(AppInfoData appInfoData,AppInfo appInfo){
		String url = appInfo.getCallbackUrl();
		logger.info("send notify to {} of app {}", url, appInfo.getAppId());
		HashMap<String,String> params = new HashMap<String,String>();
		params.put(Constants.RequestParamNames.EventType, appInfoData.getEventType());
		params.put(Constants.RequestParamNames.OrderNo, appInfoData.getOrderCode());
		params.put(Constants.RequestParamNames.OrderStatus, String.valueOf(appInfoData.getOrderStatus()));
		params.put(Constants.RequestParamNames.PaymentType, String.valueOf(appInfoData.getPaymentType()));
		params.put(Constants.RequestParamNames.Operation, String.valueOf(appInfoData.getOperation()));
		
		String response = Authentication.sendRequestWithSignature(
				appConfig.getHttpClient(), RequestMethod.POST, url, appInfo.getAppId(),
				appInfo.getAppKey(), params);
		if (response == null) {
			logger.warn("AppInfoService-DHPay:sycnOrderStatusFromThirdPart():invalid url: {}", url);
		}
		
		try {
			ObjectMapper om = new ObjectMapper();
			Map<String, Object> map = om.readValue(response,new TypeReference<Map<String, Object>>() {});
			if ((Integer) map.get(Constants.RequestParamNames.Code) == MessageConstants.V5_SUCCESS_CODE) {
				logger.warn("AppInfoService-DHPay:sycnOrderStatusFromThirdPart(): The synchronization succeeded");
			}
			return true;
		} catch (IOException ex) {
			logger.warn("AppInfoService-DHPay:sycnOrderStatusFromThirdPart():{}: response is not ResponseData instance",
					ex.getMessage());
			return false;
		}
	}
	
	private boolean appServicePayPal(AppInfoData appInfoData,AppInfo appInfo){
		String url = appInfo.getCallbackUrl();
		logger.info("send notify to {} of app {}", url, appInfo.getAppId());
		HashMap<String,String> params = new HashMap<String,String>();
		params.put(Constants.RequestParamNames.EventType, appInfoData.getEventType());
		params.put(Constants.RequestParamNames.OrderNo, appInfoData.getOrderCode());
		params.put(Constants.RequestParamNames.OrderStatus, String.valueOf(appInfoData.getOrderStatus()));
		params.put(Constants.RequestParamNames.PaymentType, String.valueOf(appInfoData.getPaymentType()));
		params.put(Constants.RequestParamNames.Operation, String.valueOf(appInfoData.getOperation()));
		
		String response = Authentication.sendRequestWithSignature(
				appConfig.getHttpClient(), RequestMethod.POST, url, appInfo.getAppId(),
				appInfo.getAppKey(), params);
		if (response == null) {
			logger.warn("AppInfoService-PayPal:sycnOrderStatusFromThirdPart():invalid url: {}", url);
		}
		
		try {
			ObjectMapper om = new ObjectMapper();
			Map<String, Object> map = om.readValue(response,new TypeReference<Map<String, Object>>() {});
			if ((Integer) map.get(Constants.RequestParamNames.Code) == MessageConstants.V5_SUCCESS_CODE) {
				logger.warn("AppInfoService-PayPal:sycnOrderStatusFromThirdPart(): The synchronization succeeded");
			}
			return true;
		} catch (IOException ex) {
			logger.warn("AppInfoService-PayPal:sycnOrderStatusFromThirdPart():{}: response is not ResponseData instance",
					ex.getMessage());
			return false;
		}
	}
	
	private boolean appServiceAlipay(AppInfoData appInfoData,AppInfo appInfo){
		String url = appInfo.getCallbackUrl();
		logger.info("send notify to {} of app {}", url, appInfo.getAppId());
		HashMap<String,String> params = new HashMap<String,String>();
		params.put(Constants.RequestParamNames.EventType, appInfoData.getEventType());
		params.put(Constants.RequestParamNames.OrderNo, appInfoData.getOrderCode());
		params.put(Constants.RequestParamNames.OrderStatus, String.valueOf(appInfoData.getOrderStatus()));
		params.put(Constants.RequestParamNames.PaymentType, String.valueOf(appInfoData.getPaymentType()));
		
		String response = Authentication.sendRequestWithSignature(
				appConfig.getHttpClient(), RequestMethod.POST, url, appInfo.getAppId(),
				appInfo.getAppKey(), params);
		if (response == null) {
			logger.warn("AppInfoService-Alipay:sycnOrderStatusFromThirdPart():invalid url: {}", url);
		}
		
		try {
			ObjectMapper om = new ObjectMapper();
			Map<String, Object> map = om.readValue(response,new TypeReference<Map<String, Object>>() {});
			if ((Integer) map.get(Constants.RequestParamNames.Code) == MessageConstants.V5_SUCCESS_CODE) {
				logger.warn("AppInfoService-Alipay:sycnOrderStatusFromThirdPart(): The synchronization succeeded");
			}
			return true;
		} catch (IOException ex) {
			logger.warn("AppInfoService-Alipay:sycnOrderStatusFromThirdPart():{}: response is not ResponseData instance",
					ex.getMessage());
			return false;
		}
	}
	
	private boolean appServiceBraintree(AppInfoData appInfoData,AppInfo appInfo){
		String url = appInfo.getCallbackUrl();
		logger.info("send notify to {} of app {}", url, appInfo.getAppId());
		HashMap<String,String> params = new HashMap<String,String>();
		params.put(Constants.RequestParamNames.EventType, appInfoData.getEventType());
		params.put(Constants.RequestParamNames.PaymentType, String.valueOf(appInfoData.getPaymentType()));
		params.put(Constants.RequestParamNames.BTSubscriptionId, appInfoData.getSubscription().getSubscriptionId());
		params.put(Constants.RequestParamNames.BTPaymentMethodToken, appInfoData.getSubscription().getPaymentMethodToken());
		params.put(Constants.RequestParamNames.BTSubscriptionNextBillingDate, String.valueOf(appInfoData.getSubscription().getNextBillingDate().getTime()));
		params.put(Constants.RequestParamNames.BTSubscriptionFirstBillingDate, String.valueOf(appInfoData.getSubscription().getFirstBillingDate().getTime()));
		params.put(Constants.RequestParamNames.BTSubscriptionCreatedAtDate, String.valueOf(appInfoData.getSubscription().getCreatedAtDate().getTime()));
		params.put(Constants.RequestParamNames.BTSubscriptionstatus, String.valueOf(appInfoData.getSubscription().getStatus()));
		params.put(Constants.RequestParamNames.BTSubscriptiontrialduration, String.valueOf(appInfoData.getSubscription().getTrialDuration()));
		params.put(Constants.RequestParamNames.BTSubscriptionNextBillingAmount,appInfoData.getSubscription().getNextBillingAmount().toString());
		params.put(Constants.RequestParamNames.BTSubscriptionPrice,appInfoData.getSubscription().getPrice().toString());
		params.put(Constants.RequestParamNames.BTCustomerID,appInfoData.getSubscription().getCustomerId());
		if(appInfoData.getSubscription().getBillingEndDate() != null){
			params.put(Constants.RequestParamNames.BTSubscriptionBillingEndDate, String.valueOf(appInfoData.getSubscription().getBillingEndDate().getTime()));
		}
		if(appInfoData.getSubscription().getBillingStartDate() != null){
			params.put(Constants.RequestParamNames.BTSubscriptionBillingStartDate, String.valueOf(appInfoData.getSubscription().getBillingStartDate().getTime()));
		}
		if(appInfoData.getSubscription().getPaidThroughDate() != null){
			params.put(Constants.RequestParamNames.BTSubscriptionPaidThroughDate, String.valueOf(appInfoData.getSubscription().getPaidThroughDate().getTime()));
		}
		
		String response = Authentication.sendRequestWithSignature(
				appConfig.getHttpClient(), RequestMethod.POST, url, appInfo.getAppId(),
				appInfo.getAppKey(), params);
		if (response == null) {
			logger.warn("AppInfoService-Braintree:sycnOrderStatusFromThirdPart():invalid url: {}", url);
//			continue;
		}
		
		try {
			ObjectMapper om = new ObjectMapper();
			Map<String, Object> map = om.readValue(response,new TypeReference<Map<String, Object>>() {});
			if ((Integer) map.get(Constants.RequestParamNames.Code) == MessageConstants.V5_SUCCESS_CODE) {
//				continue;
				logger.warn("AppInfoService-Braintree:sycnOrderStatusFromThirdPart(): The synchronization succeeded");
			}
			return true;
		} catch (IOException ex) {
			logger.warn("AppInfoService-Braintree:sycnOrderStatusFromThirdPart():{}: response is not ResponseData instance",
					ex.getMessage());
			return false;
		}
	}
	
	private AppInfo getAppInfoByPayName(List<AppInfo> appInfoList,String payName){
		AppInfo appInfo = null;
		if(appInfoList != null){
			for(AppInfo app : appInfoList) {
				if(app.getAppId().equals(payName)){
					appInfo = new AppInfo();
					appInfo.setId(app.getId());
					appInfo.setAppId(app.getAppId());
					appInfo.setAppKey(app.getAppKey());
					appInfo.setCallbackUrl(app.getCallbackUrl());
					return appInfo;
				}
			}
		}
		return appInfo;
	}
}
