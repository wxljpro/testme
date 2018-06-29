package com.xiaoyi.orderpayment.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.xiaoyi.orderpayment.config.AppConfig;
import com.xiaoyi.orderpayment.model.RechargeCardInfo;
import com.xiaoyi.orderpayment.service.IAppInfoService;
import com.xiaoyi.orderpayment.service.IProductService;
import com.xiaoyi.orderpayment.service.IRechargeCardService;
import com.xiaoyi.orderpayment.utilities.bean.ProductInfoDataResult;
import com.xiaoyi.orderpayment.utilities.bean.RechargeCardDataResult;
import com.xiaoyi.orderpayment.utilities.bean.ResponseData;
import com.xiaoyi.orderpayment.utilities.constant.MessageConstants;
import com.xiaoyi.orderpayment.utilities.constant.Constants.InternalOrderPaymentService;
import com.xiaoyi.orderpayment.utilities.constant.Constants.RequestParamNames;
import com.xiaoyi.orderpayment.utilities.controller.BaseController;

@RestController
public class RechargeCardController extends BaseController{

	private static final Logger logger = LoggerFactory.getLogger(RechargeCardController.class);
	
	private AppConfig appConfig;
	private IRechargeCardService rechargeCardService;
	private IAppInfoService appInfoService;
	private IProductService productService;
	
	@Autowired
	public RechargeCardController(AppConfig appConfig, IRechargeCardService rechargeCardService,IAppInfoService appInfoService,IProductService productService) {
		this.appConfig = appConfig;
		this.rechargeCardService = rechargeCardService;
		this.appInfoService = appInfoService;
		this.productService = productService;
	}

	@RequestMapping(value = InternalOrderPaymentService.ChargeCardListPath, method = RequestMethod.GET)
	@ResponseBody
	public ResponseData getchargeCardListbyPWs(
			@RequestParam(value = RequestParamNames.AppId, required = true) String appId,
			@RequestParam(value = RequestParamNames.ChargeCardPassword, required = true) String[] passwords
			){
			
			List<RechargeCardDataResult> rechargeCardList = rechargeCardService.getRechargeCardInfo(passwords);
			if(rechargeCardList == null){
				logger.warn("RechargeCardController-getchargeCardListbyPWs():cannot find any recharge card information by the recharge card's passwords");
				return failureResponse(MessageConstants.V5_RECHARGECARDINFO_EMPTY_CODE);
			}

			return successResponse(RequestParamNames.ChargeCardList,rechargeCardList);
	}
	
	@RequestMapping(value = InternalOrderPaymentService.UpdateUsedStatusPath, method = RequestMethod.PUT)
	@ResponseBody
	public ResponseData updateCardUsedStatus(
			@RequestParam(value = RequestParamNames.AppId, required = true) String appId,
			@RequestParam(value = RequestParamNames.ChargeCardPW, required = true) String password
			){
		RechargeCardInfo rechargeCardInfo =rechargeCardService.getRechargeCardInfobyPW(password);
		if(rechargeCardInfo == null){
			logger.warn("RechargeCardController-updateCardUsedStatus(): cannot find any recharge card information by the recharge card's password {}",password);
			return failureResponse(MessageConstants.V5_RECHARGECARDINFO_EMPTY_CODE);
		}
		rechargeCardInfo.setUsed(1);
		rechargeCardInfo.setUsedTime(new Date());

		boolean isUpdateSuccess = rechargeCardService.updateRechargeCardUsedStatus(rechargeCardInfo);
		if (!isUpdateSuccess) {
			return failureResponse(MessageConstants.V5_ODERSTATUS_SYNC_FAILURE_CODE,
					"reason", "Updating the used status of the charge card whose password is {} failed",password);
		}

		return successResponse(RequestParamNames.UpdateStatus,isUpdateSuccess);
	}
	
	@RequestMapping(value = InternalOrderPaymentService.GenerateChargeCardsPath, method = RequestMethod.POST)
	@ResponseBody
	public ResponseData generatechargeCards(
			@RequestParam(value = RequestParamNames.AppId, required = true) String appId,
			@RequestParam(value = RequestParamNames.ChargeCardExpireTime, required = true) String expiredTime,
			@RequestParam(value = RequestParamNames.ProductType, required = true) String productCode,
			@RequestParam(value = RequestParamNames.ServiceTime, required = true) String serviceTime,
			@RequestParam(value = RequestParamNames.SKUSubType, required = true) String subType,
			@RequestParam(value = RequestParamNames.ProductCurrency, required = true) String currency,
			@RequestParam(value = "createdBy", required = true) String createdBy,
			@RequestParam(value = "number", required = true) String number,
			@RequestParam(value = RequestParamNames.SKUId, required = true) String sku
			){
		long startwithID = rechargeCardService.getMaxID();
		System.out.println(startwithID);
		Map<String,String> params = new HashMap<String,String>();
		params.put("sku", sku);
		params.put("expiredTime", expiredTime);
		params.put("productCode", productCode);
		params.put("serviceTime", serviceTime);
		params.put("subType", subType);
		params.put("currency", currency);
		params.put("number", number);
		params.put("createdBy", createdBy);
		
		List<String> pwlist = rechargeCardService.batchInsertRechargecardInfo(params);
		
		long endwithID = rechargeCardService.getMaxID();
		System.out.println(endwithID);
		if(Long.parseLong(number)==(endwithID-startwithID)){
			System.out.println("equals:"+(endwithID-startwithID));
		}else{
			System.out.println("not equals"+(endwithID-startwithID));
//			return failResponse();
		}
		
		return successResponse("pw_list",pwlist);
	}
}
