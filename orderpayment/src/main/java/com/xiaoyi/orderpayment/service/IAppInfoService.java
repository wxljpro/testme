package com.xiaoyi.orderpayment.service;

import com.xiaoyi.orderpayment.model.AppInfo;
import com.xiaoyi.orderpayment.utilities.bean.AppInfoData;

public interface IAppInfoService {
	boolean syncOrderStatusFromThirdPart(AppInfoData appInfoData);
	boolean syncOrderStatusFromThirdPart(AppInfoData appInfoData,String paymentMethod);
	AppInfo getCallBackUrlByAppId(String appId);
	}