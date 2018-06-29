package com.xiaoyi.orderpayment.controller;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import javax.net.ssl.SSLContext;
import javax.net.ssl.HttpsURLConnection;   
import javax.net.ssl.TrustManager;                                                                        
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.security.cert.CertificateException;                                                           
import java.security.cert.X509Certificate;

import org.apache.catalina.connector.Request;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.xiaoyi.orderpayment.config.AppConfig;
import com.xiaoyi.orderpayment.service.IAppInfoService;
import com.xiaoyi.orderpayment.utilities.bean.ResponseData;
import com.xiaoyi.orderpayment.utilities.constant.Constants;
import com.xiaoyi.orderpayment.utilities.constant.Constants.InternalOrderPaymentService;
import com.xiaoyi.orderpayment.utilities.constant.Constants.RequestParamNames;
import com.xiaoyi.orderpayment.utilities.controller.BaseController;

@RestController
public class AppleIAPIPNController  extends BaseController{
	private static final Logger logger = LoggerFactory.getLogger(AppleIAPIPNController.class);
	
	private AppConfig appConfig;
	private IAppInfoService appInfoService;
	
	@Autowired
	AppleIAPIPNController(AppConfig appConfig,IAppInfoService appInfoService){
		this.appConfig = appConfig;
		this.appInfoService = appInfoService;
	}
	
	@RequestMapping(value =InternalOrderPaymentService.AppleIAPSubscriptionNotification, method=RequestMethod.POST)
	@ResponseBody
	 public ResponseData receiveNotification(
		@RequestParam(value = RequestParamNames.AppId, required = false) String appId,
		HttpServletResponse response,
		HttpServletRequest request){
//		@RequestParam(value = RequestParamNames.UserId, required = true) String userId,
//		@RequestParam(value = RequestParamNames.ApplyPayIAPRECEIPT,required = true) String receipt){
	    
		String req = requestInfo(request);
		System.out.println("@@@@"+req);
		logger.error("@@@@@"+req);
		logger.info("@@@@@"+req);
		return successResponse(req);
	}
}
