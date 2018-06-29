package com.xiaoyi.orderpayment.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.braintreegateway.ClientTokenRequest;
import com.braintreegateway.Customer;
import com.braintreegateway.CustomerRequest;
import com.braintreegateway.PaymentMethod;
import com.braintreegateway.PaymentMethodRequest;
import com.braintreegateway.Result;
import com.braintreegateway.Subscription;
import com.braintreegateway.SubscriptionRequest;
import com.braintreegateway.Transaction;
import com.braintreegateway.ValidationError;
import com.braintreegateway.ValidationErrorCode;
import com.braintreegateway.exceptions.NotFoundException;
import com.xiaoyi.orderpayment.config.AppConfig;
import com.xiaoyi.orderpayment.model.BraintreePlanInfo;
import com.xiaoyi.orderpayment.model.OrderInfo;
import com.xiaoyi.orderpayment.service.IBraintreeService;
import com.xiaoyi.orderpayment.service.IOrderService;
import com.xiaoyi.orderpayment.service.IPaymentService;
import com.xiaoyi.orderpayment.service.IPaypalService;
import com.xiaoyi.orderpayment.utilities.bean.AlipayStringDataResult;
import com.xiaoyi.orderpayment.utilities.bean.SubscriptionDataResult;
import com.xiaoyi.orderpayment.utilities.bean.BraintreeTransactionDataResult;
import com.xiaoyi.orderpayment.utilities.bean.OrderInfoDataResult;
import com.xiaoyi.orderpayment.utilities.bean.OrderStatus;
import com.xiaoyi.orderpayment.utilities.bean.ResponseData;
import com.xiaoyi.orderpayment.utilities.constant.MessageConstants;
import com.xiaoyi.orderpayment.utilities.constant.Constants.InternalOrderPaymentService;
import com.xiaoyi.orderpayment.utilities.constant.Constants.RequestParamNames;
import com.xiaoyi.orderpayment.utilities.controller.BaseController;
import com.xiaoyi.orderpayment.utilities.pay.PayPal.PayPal;
import com.xiaoyi.orderpayment.util.BraintreeUtil;
import com.xiaoyi.orderpayment.util.PayPalUtil;

@RestController
public class PaymentController extends BaseController{

	private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
	
	private AppConfig appConfig;
	private IOrderService orderService;
	private IPaymentService paymentService;
	private IBraintreeService braintreeService;
	private IPaypalService	payPalService;
	
	@Autowired
	public PaymentController(AppConfig appConfig,IOrderService orderService,
			IPaymentService paymentService,IBraintreeService braintreeService,
			IPaypalService	payPalService) {
		this.appConfig = appConfig;
		this.orderService = orderService;
		this.paymentService = paymentService;
		this.braintreeService = braintreeService;
		this.payPalService = payPalService;
	}
	
	@RequestMapping(value = InternalOrderPaymentService.GetTransactionUnderAnSusbscription, method = RequestMethod.GET)
    @ResponseBody
    public ResponseData getTransactionDetailsWithProfileID(@RequestParam(value = RequestParamNames.AppId, required = true) String appId,
    		@RequestParam(value = "PROFILEID", required = true) String profileId){
		HashMap<String,String> getTransactionDetailsWithProfileID = new HashMap<String,String>();
		getTransactionDetailsWithProfileID.put("start_date", "2016-06-30T11:59:59Z");
		getTransactionDetailsWithProfileID.put("PROFILEID", profileId);
		HashMap<String,String> nvpResult = payPalService.getTransactionDetailsWithProfileID(getTransactionDetailsWithProfileID);
		
		String strAck = nvpResult.get("ACK").toString().toUpperCase();
		if (strAck != null && (strAck.equals("SUCCESS") || strAck.equals("SUCCESSWITHWARNING"))) {
			return successResponse(RequestParamNames.PayPalNVPResult,nvpResult);
		} else {
			logger.warn("PaymentController:getTransactionDetailsWithProfileID():"+PayPalUtil.showError(nvpResult));                                                                                                            
			return FailureResponse(RequestParamNames.PayPalNVPResult,nvpResult);
		}
	}
	
	@RequestMapping(value = InternalOrderPaymentService.GetAnSusbscriptionDetails, method = RequestMethod.GET)
    @ResponseBody
    public ResponseData getAnSusbscriptionDetails(@RequestParam(value = RequestParamNames.AppId, required = true) String appId,
    		@RequestParam(value = "PROFILEID", required = true) String profileId){
		HashMap<String,String> getAnSusbscriptionDetails = new HashMap<String,String>();
		getAnSusbscriptionDetails.put("PROFILEID", profileId);
		HashMap<String,String> nvpResult = payPalService.getAnSusbscriptionDetails(getAnSusbscriptionDetails);
		
		String strAck = nvpResult.get("ACK").toString().toUpperCase();
		if (strAck != null && (strAck.equals("SUCCESS") || strAck.equals("SUCCESSWITHWARNING"))) {
			
			return successResponse(RequestParamNames.PayPalNVPResult,nvpResult);
		} else {
			logger.warn("PaymentController:getAnSusbscriptionDetails():"+PayPalUtil.showError(nvpResult));                                                                                                            
			return FailureResponse(RequestParamNames.PayPalNVPResult,nvpResult);
		}
	}
	
	@RequestMapping(value = InternalOrderPaymentService.GetAnSusbscriptionlistPath, method = RequestMethod.GET)
    @ResponseBody
    public ResponseData getAnSusbscriptionList(@RequestParam(value = RequestParamNames.AppId, required = true) String appId,
    		@RequestParam(value = RequestParamNames.PayPalSubscriptionIDs, required = true) String[] subscriptionIDs){
		List<HashMap> nvpResult = payPalService.getSusbscriptionDetailsList(subscriptionIDs);
		for(HashMap nvp :nvpResult){
			String strAck = nvp.get("ACK").toString().toUpperCase();
			if (strAck != null && (strAck.equals("SUCCESS") || strAck.equals("SUCCESSWITHWARNING"))) {
				
			} else {
				logger.warn("PaymentController:getAnSusbscriptionDetails():"+PayPalUtil.showError(nvp));                                                                                                            
				nvpResult.remove(nvp);
			}
		}
		return successResponse(RequestParamNames.PayPalNVPResult,nvpResult);
	}
	
	@RequestMapping(value = InternalOrderPaymentService.CancelAnSusbscriptionPath, method = RequestMethod.PUT)
    @ResponseBody
    public ResponseData cancelAnSusbscription(@RequestParam(value = RequestParamNames.AppId, required = true) String appId,
    		@RequestParam(value = "PROFILEID", required = true) String profileId){
		HashMap<String,String> cancelAnSubscription = new HashMap<String,String>();
		cancelAnSubscription.put("PROFILEID", profileId);
		HashMap<String,String> nvpResult = payPalService.cancelAnSusbscription(cancelAnSubscription);
		
		String strAck = nvpResult.get("ACK").toString().toUpperCase();
		if (strAck != null && (strAck.equals("SUCCESS") || strAck.equals("SUCCESSWITHWARNING"))) {
			
			return successResponse(RequestParamNames.PayPalNVPResult,nvpResult);
		} else {
			logger.warn("PaymentController:cancelAnSusbscription():"+PayPalUtil.showError(nvpResult));                                                                                                            
			return FailureResponse(RequestParamNames.PayPalNVPResult,nvpResult);
		}
	}
	
//	@RequestMapping(value = InternalOrderPaymentService.DoExpressCheckoutPaymentPath, method = RequestMethod.POST)
//    @ResponseBody
//    public ResponseData DoExpressCheckoutPayment(@RequestParam(value = RequestParamNames.AppId, required = true) String appId,
//    		@RequestParam(value = RequestParamNames.PayPalToken, required = true) String token){
//		HashMap<String,String> checkoutDetails = new HashMap<String,String>();
//		HashMap<String,String> nvpResult = payPalService.CreateRecurringPaymentsProfile(checkoutDetails);
//		
//		String strAck = nvpResult.get("ACK").toString().toUpperCase();
//		if (strAck != null && (strAck.equals("SUCCESS") || strAck.equals("SUCCESSWITHWARNING"))) {
//			
//			return successResponse(RequestParamNames.PayPalNVPResult,nvpResult);
//		} else {
//			String ErrorCode = nvpResult.get("L_ERRORCODE0").toString();
//			String ErrorShortMsg = nvpResult.get("L_SHORTMESSAGE0").toString();
//			String ErrorLongMsg = nvpResult.get("L_LONGMESSAGE0").toString();
//			String ErrorSeverityCode = nvpResult.get("L_SEVERITYCODE0").toString();
//
//			String errorString = "SetExpressCheckout API call failed. " + "<br>Detailed Error Message: " + ErrorLongMsg
//					+ "<br>Short Error Message: " + ErrorShortMsg + "<br>Error Code: " + ErrorCode
//					+ "<br>Error Severity Code: " + ErrorSeverityCode;
//			System.out.println("errorString="+errorString);
//			return failureResponse(MessageConstants.V5_BT_GENERATECLIENTTOKEN_FAILURE_CODE);
//		}
//	}
	
	@RequestMapping(value = InternalOrderPaymentService.CreateRecurringPaymentsProfilePath, method = RequestMethod.POST)
    @ResponseBody
    public ResponseData CreateRecurringPaymentsProfile(@RequestParam(value = RequestParamNames.AppId, required = true) String appId,
    		@RequestParam(value = "TOKEN", required = true) String token,
    		@RequestParam(value = "BILLINGPERIOD", required = true) String billingPeriod,
    		@RequestParam(value = "L_BILLINGAGREEMENTDESCRIPTION0", required = true) String billingDesc,
    		@RequestParam(value = "EMAIL", required = true) String email,
    		@RequestParam(value = "BILLINGFREQUENCY", required = true) String billingFreq,
    		@RequestParam(value = "AMT", required = true) String amount,
    		@RequestParam(value = "TOTALBILLINGCYCLES", required = true) String totalBillingCycles,
    		@RequestParam(value = "TRIALBILLINGFREQUENCY", required = true) String trialBillingFreq,
    		@RequestParam(value = "TRIALBILLINGPERIOD", required = true) String trialBillingPeriod,
    		@RequestParam(value = "TRIALTOTALBILLINGCYCLES", required = true) String trialTotalBillingCycles,
    		@RequestParam(value = "TRIALAMT", required = true) String trialAmount,
    		@RequestParam(value = "PROFILESTARTDATE", required = true) String profieStartDate,
    		@RequestParam(value = "CURRENCYCODE", required = true) String currency,
    		@RequestParam(value = "DESC", required = true) String desc,
    		@RequestParam(value = "iftriedoutalready", required = true) boolean ifTridOut
    		){
		HashMap<String,String> checkoutDetails = new HashMap<String,String>();
		checkoutDetails.put("TOKEN", token);
		checkoutDetails.put("BILLINGPERIOD", billingPeriod); //Day, Week, SemiMonth,Month,Year
		checkoutDetails.put("BILLINGFREQUENCY", billingFreq);
		checkoutDetails.put("TOTALBILLINGCYCLES", totalBillingCycles);// The value is set to 0,  the regular payment period continues until the profile is canceled or deactivated
		checkoutDetails.put("AMT", amount);
		checkoutDetails.put("TRIALBILLINGPERIOD", trialBillingPeriod);
		checkoutDetails.put("TRIALBILLINGFREQUENCY",trialBillingFreq);
		checkoutDetails.put("TRIALTOTALBILLINGCYCLES",trialTotalBillingCycles);
		checkoutDetails.put("TRIALAMT", trialAmount);
		checkoutDetails.put("CURRENCYCODE", currency);
		checkoutDetails.put("currencyCodeType", currency);
		checkoutDetails.put("DESC", desc);
		checkoutDetails.put("EMAIL", email);
		checkoutDetails.put("IFTRIEDOUTALREADY", String.valueOf(ifTridOut));
		//start date  may take up to 24 hours
		checkoutDetails.put("PROFILESTARTDATE", PayPal.calculateProfileFirstStartDate(PayPal.convertDateStringToDate(profieStartDate)));
		logger.info("@@@TOKEN "+token+"'s profieStartDate="+profieStartDate);
		logger.info("@@@cal-ed token"+token+" startDate="+PayPal.calculateProfileFirstStartDate(PayPal.convertDateStringToDate(profieStartDate)));

		HashMap<String,String> nvpResult = payPalService.CreateRecurringPaymentsProfile(checkoutDetails);
		
		String strAck = nvpResult.get("ACK").toString().toUpperCase();
		if (strAck != null && (strAck.equals("SUCCESS") || strAck.equals("SUCCESSWITHWARNING"))) {		
			return successResponse(RequestParamNames.PayPalNVPResult,nvpResult);
		} else {
			logger.warn("PaymentController:CreateRecurringPaymentsProfile():"+PayPalUtil.showError(nvpResult));                                                                                                            
			return FailureResponse(RequestParamNames.PayPalNVPResult,nvpResult);
		}
	}
	
	/**
	 * PayPal --Call PayPal GetExpressCheckoutDetails service
	 * @param appId
	 * @param token
	 * @return
	 */
	@RequestMapping(value = InternalOrderPaymentService.GetExpressCheckoutDetailsPath, method = RequestMethod.GET)
    @ResponseBody
    public ResponseData GetExpressCheckoutDetails(@RequestParam(value = RequestParamNames.AppId, required = true) String appId,
    		@RequestParam(value = RequestParamNames.PayPalToken, required = true) String token){
		
		HashMap<String,String> nvpResult = payPalService.GetExpressCheckoutDetails(token);
		String strAck = nvpResult.get("ACK").toString().toUpperCase();
		if (strAck != null && (strAck.equals("SUCCESS") || strAck.equals("SUCCESSWITHWARNING"))) {
			return successResponse(RequestParamNames.PayPalNVPResult,nvpResult);
		} else {
			logger.warn("PaymentController:GetExpressCheckoutDetails():"+PayPalUtil.showError(nvpResult));                                                                                                            
			return FailureResponse(RequestParamNames.PayPalNVPResult,nvpResult);
		}
	}
	
	/**
	 * PayPal --Call PayPal setExpressCheckout service  
	 * @param appId
	 * @param returnURL
	 * @param cancelURL
	 * @param amount
	 * @param currency
	 * @param desc
	 * @return
	 */
	@RequestMapping(value = InternalOrderPaymentService.SetExpressCheckoutPath, method = RequestMethod.POST)
	@ResponseBody
	public ResponseData setExpressCheckout(@RequestParam(value = RequestParamNames.AppId, required = true) String appId,
 			@RequestParam(value = "RETURNURL", required = true) String returnURL,                                                            
 			@RequestParam(value = "CANCELURL", required = true) String cancelURL,                                                                  
 			@RequestParam(value = "PAYMENTREQUEST_0_AMT", required = true) String amount,  
 			@RequestParam(value = "currencyCodeType", required = true) String currency, 
 			@RequestParam(value = "L_BILLINGTYPE0", required = true) String billingType,
 			@RequestParam(value = "LOCALECODE", required = true) String localeCode,
 			@RequestParam(value = "L_BILLINGAGREEMENTDESCRIPTION0", required = true) String desc, 
 			@RequestParam(value = "noshipping", required = true) String noShipping){
			HashMap<String,String> checkoutDetails = new HashMap<String,String>();
			HashMap<String,String> nvpResult = new HashMap<String,String>();
			checkoutDetails.put("PAYMENTREQUEST_0_AMT", amount);
			checkoutDetails.put("currencyCodeType", currency);
			checkoutDetails.put("noshipping", noShipping);
			checkoutDetails.put("L_BILLINGTYPE0", billingType);
			checkoutDetails.put("L_BILLINGAGREEMENTDESCRIPTION0", desc);
			checkoutDetails.put("LOCALECODE", localeCode);
			nvpResult = payPalService.callSetExpressCheckout(checkoutDetails, returnURL, cancelURL);
			String strAck = nvpResult.get("ACK").toString().toUpperCase();
			if (strAck != null && (strAck.equals("SUCCESS") || strAck.equals("SUCCESSWITHWARNING"))) {
				return successResponse(RequestParamNames.PayPalNVPResult,nvpResult);
			} else {                                                                        
				logger.warn("PaymentController:setExpressCheckout():"+PayPalUtil.showError(nvpResult));                                                                                                            
				return FailureResponse(RequestParamNames.PayPalNVPResult,nvpResult);
			}                                                                                                                                                            
	}
	/**
	 * Generate client token for APP to communicate with Braintree
	 * @param appId
	 * @param btcustomerId
	 * @return
	 */
    @RequestMapping(value = InternalOrderPaymentService.CreateBraintreeClientTokenPath, method = RequestMethod.GET)
    @ResponseBody
    public ResponseData generateClientToken(
    		@RequestParam(value = RequestParamNames.AppId, required = true) String appId,
    		@RequestParam(value = RequestParamNames.BTCustomerID,required= true) String btcustomerId) {
    	try{
	    	String clientToken="";
	    	Customer customer = findCustomerByID(btcustomerId);
	    	if(customer == null){
	    		clientToken = appConfig.braintreeGateway().clientToken().generate();
	    		return successResponse(RequestParamNames.BTClientToken,clientToken);
	    	}else{
	        	ClientTokenRequest clientTokenRequest = new ClientTokenRequest()
	        		    .customerId(btcustomerId);
	        	clientToken = appConfig.braintreeGateway().clientToken().generate(clientTokenRequest);
	        	return successResponse(RequestParamNames.BTClientToken,clientToken);
	    	}   
    	}catch(Exception e){
    		return failureResponse(MessageConstants.V5_BT_GENERATECLIENTTOKEN_FAILURE_CODE);
    	}
    }

    /**
     * Create a new Braintree customer which is mapped to an ants user
     * @param appId
     * @param customerID
     * @return
     */
    @RequestMapping(value = InternalOrderPaymentService.CreateBraintreeCustomerPath,method=RequestMethod.POST)
    @ResponseBody
    public ResponseData createCustomer(
    		@RequestParam(value = RequestParamNames.AppId, required = true) String appId,
    		@RequestParam(value = RequestParamNames.UserId,required= true) String antsID) {
    	
    	CustomerRequest request = new CustomerRequest().firstName(antsID);
        
        Result<Customer> result = appConfig.braintreeGateway().customer().create(request);
    	
	   	if(result.isSuccess()){
	   		Customer customer = result.getTarget();
	   		return successResponse(RequestParamNames.BTCustomerID,customer.getId());
	   	}else{
	   		for (ValidationError error : result.getErrors().getAllDeepValidationErrors()) {
	   			logger.error("Creating a Braintree customer for ants user"+" failed:" + error.getMessage());    
		    }
	   		logger.error("Creating a Braintree customer for ants user-{} failed ",antsID);
	   		return failureResponse(MessageConstants.V5_BT_CREATECUSTOMER_FAILURE_CODE);
	   	}
    }    
    
    /**
     * Add a payment method for a Braintree customer
     * @param appId
     * @param customerID
     * @param nonceFromTheClient
     * @return
     */
    @RequestMapping(value = InternalOrderPaymentService.AddPaymentMethodforBraintreeCustomerPath,method=RequestMethod.PUT)
    @ResponseBody
    public ResponseData addPaymentMethodforCustomer(
    		@RequestParam(value = RequestParamNames.AppId, required = true) String appId,
			@RequestParam(value = RequestParamNames.BTCustomerID,required= true) String customerID,
			@RequestParam(value = RequestParamNames.BTNonceFromTheClient,required= true) String nonceFromTheClient) {
    	
    	PaymentMethodRequest request = new PaymentMethodRequest()
    			.customerId(customerID)
    			.paymentMethodNonce(nonceFromTheClient)
    			.options()
    				.failOnDuplicatePaymentMethod(true) 
    		//TODO: enable verifyCard(true) to further investigate how to do car verification
//    				.verifyCard(true)
//    				.makeDefault(true)    //set this payment method as default one
//    			    .verificationMerchantAccountId("the_merchant_account_id")
//    			    .verificationAmount("2.00") //specify a different (from $0 or $1) to use for the authorization
    			.done();
//      if verifyCard set to be true, use the below verification to get the result if the result.isSuccess() is false    			
//    	CreditCardVerification verification = result.getCreditCardVerification();
//    	verification.getStatus();
//    	// processor_declined
//    	verification.getProcessorResponseCode();
//    	// 2000
//    	verification.getProcessorResponseText();

       Result<? extends PaymentMethod> result = appConfig.braintreeGateway().paymentMethod().create(request);
       
	   	if(result.isSuccess()){
	   		PaymentMethod paymentMethod = result.getTarget();
	   		return successResponse(RequestParamNames.BTPaymentMethodTokenResult,paymentMethod.getToken());
	   	}else{
	   		List<ValidationError> paymentMethodErrors = result.getErrors()
	   				.forObject("paymentmethod")
	   				.forObject("options")
	   				.forObject("failOnDuplicatePaymentMethod")
	   				.getAllValidationErrors();
	   		for(ValidationError error : paymentMethodErrors){
	   			if(error.getCode() == ValidationErrorCode.CREDIT_CARD_DUPLICATE_CARD_EXISTS){
	   				logger.warn("The payment method is being added is a duplicated one in Braintree Vault");
	   				return successResponse(RequestParamNames.BTPaymentMethodTokenResult,MessageConstants.V5_BT_DUPLICATEDPAYMENTMETHOD_FAILURE_CODE);
	   			}
	   		}
	   		for (ValidationError error : result.getErrors().getAllDeepValidationErrors()) {
	   			logger.error("Adding a payment method for Braintree customer-{} failed " + customerID);    
		    }
	   		return successResponse(RequestParamNames.BTPaymentMethodTokenResult,MessageConstants.V5_BT_ADDPAYMENTMETHOD_FAILURE_CODE);
	   	}
    }
    
    /**
     * Cancel an subscription
     * @param appId
     * @param subscriptionId
     * @return
     */
    @RequestMapping(value = InternalOrderPaymentService.CancelBraintreeSubscriptionPath,method=RequestMethod.PUT)
    @ResponseBody
    public ResponseData cancelSubscription(
    		@RequestParam(value = RequestParamNames.AppId, required = true) String appId,
    		@RequestParam(value = RequestParamNames.UserId, required = true) String userId,
    		@RequestParam(value = RequestParamNames.BTSubscriptionId, required = true) String subscriptionId){
    	boolean checkSuccess = braintreeService.checkSubscriptionBeforeCancel(subscriptionId);
    	boolean isCancelSuccess = false;
    	if(checkSuccess){
    		isCancelSuccess = braintreeService.cancelAnSubscription(subscriptionId);
	    	if(!isCancelSuccess){
	    		logger.error("The user {} cancel the order {} failed",userId,subscriptionId);
	    		return failureResponse(MessageConstants.V5_BT_CANCELSUBSCRIPTION_FAILURE_CODE);
	    	}
    	}else{
    		logger.error("The user {} cancel the order {} failed",userId,subscriptionId);
    		return failureResponse(MessageConstants.V5_BT_CANCELSUBSCRIPTION_FAILURE_CODE);
    	}
    	
    	OrderInfo orderInfo = orderService.getOrderByCode(subscriptionId);
    	orderInfo.setOrderStatus(OrderStatus.invalid.value());
    	orderInfo.setLastModified(new Date());
    	boolean isUpdateSuccess = orderService.updateOrderStatus(orderInfo);
    	if (!isUpdateSuccess) {
    		return failureResponse(MessageConstants.V5_ODERSTATUS_SYNC_FAILURE_CODE,
    				"reason", "Updating the order-"+subscriptionId + "'s status failed");
    	}
    	
		return successResponse(RequestParamNames.CancelStatus,isCancelSuccess);
	}
    
    /**
     * Create a Braintree Subscription
     * @param appId
     * @param serviceTime
     * @param sku
     * @param paymentMethodString
     * @param latestUsedToken
     * @param ifToken
     * @param customerID
     * @return
     */
    @RequestMapping(value = InternalOrderPaymentService.CreateBraintreeSubscriptionPath,method=RequestMethod.POST)
    @ResponseBody
    public ResponseData newSubscription(
    		@RequestParam(value = RequestParamNames.AppId, required = true) String appId,
    		@RequestParam(value = RequestParamNames.ServiceTime, required = true) String serviceTime,
    		@RequestParam(value = RequestParamNames.SKUSubType, required = true) String sku,
    		@RequestParam(value = RequestParamNames.BTPaymentMethodAppServer, required = true) String paymentMethodString,
    		@RequestParam(value = RequestParamNames.BTIfNonceOrToken, required = true) boolean ifToken,
    		@RequestParam(value = RequestParamNames.BTIfCancelToNew, required = true) boolean ifCancelToNew,
    		@RequestParam(value = RequestParamNames.BTCustomerID,required= true) String customerID,
    		@RequestParam(value = RequestParamNames.BTSubscriptionId,required= true) String previousSubscriptionId,
    		@RequestParam(value = RequestParamNames.OrderNo,required= true) String orderCode
    		) {	
    	BraintreePlanInfo btPlanInfo = braintreeService.getPlanInfoBySkuAndServiceTime(Integer.parseInt(sku), Integer.parseInt(serviceTime));
    	if(btPlanInfo ==null){
    		logger.error("OrderPaymentController:newSubscription():Getting the plan id by sku:{} and serviceTime{} failed",sku,serviceTime);
    		return failureResponse(MessageConstants.V5_BT_GETPLANID_FAILURE_CODE);
    	}
    	
    	SubscriptionRequest request;
    	if(ifToken){//Paymentmethod is created successfully
//    		if(!ifCancelToNew){
    			request = new SubscriptionRequest()
	    				.id(orderCode)
		           		.paymentMethodToken(paymentMethodString)
		           		.planId(btPlanInfo.getPlanId())
		           		.trialPeriod(false);	
//    		}
    	}else{//paymentmethod already exists, use paymentmethodnonce instead 
//    		if(!ifCancelToNew){
    			request = new SubscriptionRequest()
		    			.id(orderCode)
			       		.paymentMethodNonce(paymentMethodString)
			       		.planId(btPlanInfo.getPlanId())
			       		.trialPeriod(false);
//    		}
//    		else{
//    			request = new SubscriptionRequest()
//	    			.id(orderCode)
//		       		.paymentMethodNonce(paymentMethodString)
//		       		.planId(btPlanInfo.getPlanId())
//		       		.trialPeriod(true)
//	           		.trialDuration(braintreeService.getTrialDuration(btPlanInfo.getPlanId()))
//	           		.trialDurationUnit(braintreeService.getTrialDurationUnit(orderCode));
//    		}
        }
       SubscriptionDataResult  subscriptionDataResult = new SubscriptionDataResult();
       Result<Subscription> result = appConfig.braintreeGateway().subscription().create(request);
	   	if(result.isSuccess()){
	   		Subscription subscription  = result.getTarget();
	   		
	   		subscriptionDataResult = BraintreeUtil.subscriptinToSubscriptionDataResult(subscription);
	   		subscriptionDataResult.setCustomerId(customerID);
	   	}else{   		
	   		for (ValidationError error : result.getErrors().getAllDeepValidationErrors()) {
		        logger.error("OrderPaymentController:newSubscription():Creating the customer failed:"+error.getMessage());
		    }
	   		return failureResponse(MessageConstants.V5_BT_NEWSUBSCRIPTION_FAILURE_CODE);
	   	}
	   	
	   	return successResponse(RequestParamNames.BTNewSubscriptionResult,subscriptionDataResult);
    }     
    
    /**
     * Get the subscription info from Braintree
     * @param appId
     * @param subscriptionId
     * @return
     */
    @RequestMapping(value = InternalOrderPaymentService.BraintreeSubscriptionPath,method=RequestMethod.GET)
    @ResponseBody
    public ResponseData getSubscription(
    		@RequestParam(value = RequestParamNames.AppId, required = true) String appId,
    		@RequestParam(value = RequestParamNames.BTSubscriptionId, required = true) String subscriptionId) {
       try{
    	   	Subscription subscription = appConfig.braintreeGateway().subscription().find(subscriptionId);   
    	   	SubscriptionDataResult  subscriptionDataResult = BraintreeUtil.subscriptinToSubscriptionDataResult(subscription);
    	   	String customerId = braintreeService.getCustomerId(subscriptionDataResult.getCustomerId());
    	   	subscriptionDataResult.setCustomerId(customerId);
    	   	BraintreeUtil.covertCreatedDate(subscriptionDataResult, subscription);
    	   	
    	   return successResponse(RequestParamNames.BTSubscriptionInfo,subscriptionDataResult);
       }catch(NotFoundException e){
    	   System.err.println(e.getMessage());
    	   return failureResponse(MessageConstants.V5_ODERSTATUS_SYNC_FAILURE_CODE);
       }catch(Exception e){
    	   System.err.println(e.getMessage());
    	   return failureResponse(MessageConstants.V5_ODERSTATUS_SYNC_FAILURE_CODE);
       }
    }    
    /**
     * Get the transaction history under a certain subscription from Braintree
     * @param appId
     * @param subscriptionId
     * @return
     */
    @RequestMapping(value = InternalOrderPaymentService.BraintreeTranscationsPath,method=RequestMethod.GET)
    @ResponseBody
    public ResponseData getTranscations(
    		@RequestParam(value = RequestParamNames.AppId, required = true) String appId,
    		@RequestParam(value = RequestParamNames.BTSubscriptionId, required = true) String subscriptionId) {
       try{ 
    	   List<Transaction> transactions = braintreeService.getTranscationsUnderSubscription(subscriptionId);
    	   if(transactions == null){
    		  return failureResponse(MessageConstants.V5_ODERSTATUS_SYNC_FAILURE_CODE);
    		  
    	   }
    	   List<BraintreeTransactionDataResult> transactionDataResult = new ArrayList<BraintreeTransactionDataResult>();
    	   	for(Transaction transaction:transactions){
    	   		BraintreeTransactionDataResult dataResult = new BraintreeTransactionDataResult();
    	   		dataResult.setStatus(transaction.getStatus().toString());
    	   		dataResult.setCreatedAt(transaction.getCreatedAt().getTime());
    	   		dataResult.setTransactionAmount(transaction.getAmount().toString());
    	   		transactionDataResult.add(dataResult);
    	   	}
    	   return successResponse(RequestParamNames.BTTransactionList,transactionDataResult);
       }catch(NotFoundException e){
    	   System.err.println(e.getMessage());
    	   return failureResponse(MessageConstants.V5_ODERSTATUS_SYNC_FAILURE_CODE);
       }catch(Exception e){
    	   System.err.println(e.getMessage());
    	   return failureResponse(MessageConstants.V5_ODERSTATUS_SYNC_FAILURE_CODE);
       }
    }       
    
    /**
     * Sync the statuses of the orders from Alipay
     * @param appId
     * @param orderCodes
     * @return
     */
	@RequestMapping(value = InternalOrderPaymentService.SYNCANOrderStatusFromServerPath, method = RequestMethod.GET)
	@ResponseBody
	public ResponseData queryOrderStatusFromOrderPaymentServer(
			@RequestParam(value = RequestParamNames.AppId, required = true) String appId,
			@RequestParam(value = "orderCodes[]", required = true) String[] orderCodes){
		List<OrderInfoDataResult> orderStatusList = paymentService.getOrderStatusFromOrderPaymentServer(orderCodes);
			if(orderStatusList == null){
				logger.warn("No orders were found by these order code");
				return failureResponse(MessageConstants.V5_ODERSTATUS_SYNC_FAILURE_CODE);
			}

		return successResponse(RequestParamNames.OrderStatusList,orderStatusList);
     }
	
//	@RequestMapping(value = "/test/test", method = RequestMethod.GET)
//	@ResponseBody
//	public ResponseData test(
//			@RequestParam(value = RequestParamNames.AppId, required = true) String appId,
//			@RequestParam(value = "ordercodeunit", required = true) String orderCodeUnit){
//		OrderInfo orderInfo = new OrderInfo();
//		orderInfo.setOrderCode(orderCodeUnit);
//		List<OrderInfo> orderStatusList = orderService.getParticalOrdersToSyncInfoFromBTToOrderpayment(orderInfo);
//
//		return successResponse(RequestParamNames.OrderStatusList,orderStatusList);
//     }
	
	/**
	 * Submit cancellation command to Alipay to cancel the unpaid order 
	 * @param appId
	 * @param userId
	 * @param orderCode
	 * @param totalFee
	 * @return
	 */
	@RequestMapping(value = InternalOrderPaymentService.CancelANUnpaidOrderPath, method = RequestMethod.GET)
	@ResponseBody
	public ResponseData cancelOrderfromAlipay(
			@RequestParam(value = RequestParamNames.AppId, required = true) String appId,
			@RequestParam(value = RequestParamNames.UserId, required = true) String userId,
			@RequestParam(value = RequestParamNames.OrderNo, required = true) String orderCode,
			@RequestParam(value = RequestParamNames.TotalPrice, required = true) String totalFee){
		
		OrderInfo orderInfo = orderService.getOrderByCode(orderCode);
		if(orderInfo == null){
			logger.warn("This order {} doesn't exists in the orderpayment system",orderCode);
		}
		
		int actualTotalFee  = orderInfo.getFinalPrice();
		String actualuserId   = orderInfo.getCustomerId();
		
		if(!actualuserId.equals(userId) && !actualuserId.equals(totalFee)){
			logger.warn("This order {} doesn't belong to the user {} or it's total fee {} is wrong,the acutal one is {}.",orderCode,userId,actualTotalFee,actualuserId);
		}
		
		boolean isCancelSuccess = paymentService.cancelOrderFromAlipay(orderCode);
		
		return successResponse(RequestParamNames.CancelStatus,isCancelSuccess);
	}
	
	/**
	 * Calculate signed request strings for App to submit payment to Alipay
	 * @param appId
	 * @param orderCode
	 * @param subject
	 * @param body
	 * @param totalfee
	 * @return
	 */
	@RequestMapping(value = InternalOrderPaymentService.GenerateStringForAlipayPath, method = RequestMethod.GET)
	@ResponseBody
	public ResponseData generateStringForAlipay(
			@RequestParam(value = RequestParamNames.AppId, required = true) String appId,
			@RequestParam(value = RequestParamNames.OrderNo, required = true) String orderCode,
			@RequestParam(value = RequestParamNames.ProductName, required = true) String subject,
			@RequestParam(value = RequestParamNames.ProductDescription, required = true) String body,
			@RequestParam(value = RequestParamNames.TotalPrice, required = true) String totalfee){
		
		OrderInfo orderInfo = orderService.getOrderByCode(orderCode);
		if(orderInfo == null){
			logger.warn("The order-{} doesn't exist in the orderpayment system, and generate Alipay string failed",orderCode);
		}
		
		AlipayStringDataResult alipayString = paymentService.getAlipayString(orderCode, subject, body, totalfee);
		return successResponse(RequestParamNames.AlipayString,alipayString);
	}
	
	private Customer findCustomerByID(String btcustomerId){
	  	try{
    		Customer customer = appConfig.braintreeGateway().customer().find(btcustomerId);
    		return customer;
    	}catch(NotFoundException e){
    		return null;
    	}
	}
	
	public static void main(String[] args) {
		String startDate ="2017-03-31 00:00:01";
		String endDate ="2017-04-19 23:23:23";
		System.out.println("startDate="+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(endDate));
//		System.out.println("endDate="+);
		
	}
}
