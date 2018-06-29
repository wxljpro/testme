package com.xiaoyi.orderpayment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xiaoyi.orderpayment.model.ProductInfo;
import com.xiaoyi.orderpayment.service.IAppInfoService;
import com.xiaoyi.orderpayment.service.IProductService;
import com.xiaoyi.orderpayment.utilities.bean.ProductInfoDataResult;
import com.xiaoyi.orderpayment.utilities.bean.ResponseData;
import com.xiaoyi.orderpayment.utilities.constant.Constants;
import com.xiaoyi.orderpayment.utilities.constant.Constants.InternalOrderPaymentService;
import com.xiaoyi.orderpayment.utilities.constant.Constants.RequestParamNames;
import com.xiaoyi.orderpayment.utilities.constant.MessageConstants;
import com.xiaoyi.orderpayment.utilities.controller.BaseController;

import com.xiaoyi.orderpayment.utilities.bean.AppInfoData;

import com.xiaoyi.orderpayment.config.AppConfig;

@RestController
public class ProductController extends BaseController{

	private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
	
	private AppConfig appConfig;
	private IProductService productService;
	private IAppInfoService appInfoService;
	
	@Autowired
	public ProductController(AppConfig appConfig, IProductService productService,IAppInfoService appInfoService) {
		this.appConfig = appConfig;
		this.productService = productService;
		this.appInfoService = appInfoService;
	}
	
	/**
	 * Online products, it's now unavailable, directly configure the products info on DB instead 
	 * TODO:This service will be used to setup a web pages to make this visually and easily
	 * @param appId
	 * @param productName
	 * @param productPrice
	 * @param productDesc
	 * @return
	 */
	@RequestMapping(value = InternalOrderPaymentService.OnlineProductPath, method = RequestMethod.POST)
	@ResponseBody
	public ResponseData onlineProduct(
			@RequestParam(value = RequestParamNames.AppId, required = true) String appId,
			@RequestParam(value = RequestParamNames.ProductName, required = true) String productName,
			@RequestParam(value = RequestParamNames.ProductPrice, required = true) String productPrice,
			@RequestParam(value = RequestParamNames.ProductDescription, required = false) String productDesc
			){
			ProductInfo productInfo = new ProductInfo();
			
			productInfo.setProductName(productName);
			productInfo.setProductPrice(Integer.parseInt(productPrice));
			productInfo.setProductDescription(productDesc);
			//Our products or services may be categorized based on the products especially for Home, Sport, and Car etc...
			//such as 11 the first 1 means Home, the second 1 means the first product in Home 
			int    productType  = 1;
			productInfo.setProductType(productType);
			productInfo.setDateCreated(new Date());
			productInfo.setDateModified(new Date());
			productInfo.setProductAvailible(1);
			
			
			if (!productService.saveProductInfo(productInfo)) {
				return failureResponse(MessageConstants.V5_ONLINE_PRODUCT_FAILURE_CODE,
						"reason", "Online an product failed");
			}
			
			return successResponse();
	}
	
	/**
	 * Get product list
	 * @param appId
	 * @param channel
	 * @param currency
	 * @param productId
	 * @return
	 */
	@RequestMapping(value = InternalOrderPaymentService.ListProductPath, method = RequestMethod.GET)
	@ResponseBody
	public ResponseData listProducts(
			@RequestParam(value = RequestParamNames.AppId, required = true) String appId,
			@RequestParam(value = RequestParamNames.ProductChannel, required = false) String channel,
			@RequestParam(value = RequestParamNames.ProductCurrency, required = false) String currency,
			@RequestParam(value = RequestParamNames.ProductSKU, required = false) String productSku,
			@RequestParam(value = RequestParamNames.ProductId, required = false) String productId
			){
		Map<String, Object> paramList = new HashMap<String,Object>();
		if(!currency.isEmpty()){
			paramList.put(Constants.RequestParamNames.ProductCurrency, currency);
		}
		if(!channel.isEmpty()){
			paramList.put(Constants.RequestParamNames.ProductChannel, channel);
		}
		if(!productId.isEmpty()){
			paramList.put(Constants.RequestParamNames.ProductId, productId);
		}
		if(!productSku.isEmpty()){
			paramList.put(Constants.RequestParamNames.SKUId, productSku);
		}
		List<ProductInfoDataResult> productList = productService.getProductList(paramList);
		if(productList == null && !productId.isEmpty()){
			logger.warn("cannot find the product by the product ID = {}"+ productId);
			return failureResponse(MessageConstants.V5_VALIDATOR_PRODUCTID_FAILURE_CODE);
		}

		return successResponse(RequestParamNames.ProductList,productList);
	}
}
