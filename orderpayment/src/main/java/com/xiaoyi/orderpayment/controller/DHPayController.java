package com.xiaoyi.orderpayment.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.xiaoyi.orderpayment.config.AppConfig;
import com.xiaoyi.orderpayment.model.OrderInfo;
import com.xiaoyi.orderpayment.service.IAppInfoService;
import com.xiaoyi.orderpayment.service.IOrderService;
import com.xiaoyi.orderpayment.service.IProductService;
import com.xiaoyi.orderpayment.utilities.bean.AppInfoData;
import com.xiaoyi.orderpayment.utilities.bean.OrderPaymentType;
import com.xiaoyi.orderpayment.utilities.bean.OrderStatus;
import com.xiaoyi.orderpayment.utilities.bean.ResponseData;
import com.xiaoyi.orderpayment.utilities.constant.Constants;
import com.xiaoyi.orderpayment.utilities.constant.MessageConstants;
import com.xiaoyi.orderpayment.utilities.constant.Constants.InternalOrderPaymentService;
import com.xiaoyi.orderpayment.utilities.constant.Constants.RequestParamNames;
import com.xiaoyi.orderpayment.utilities.controller.BaseController;
import com.xiaoyi.orderpayment.utilities.pay.DHPay.DigestUtil;

@RestController
public class DHPayController extends BaseController {
	private static final Logger logger = LoggerFactory.getLogger(DHPayController.class);
	
	private AppConfig appConfig;
	private IAppInfoService appInfoService;
	private IOrderService orderService;
	private IProductService productService;
	
	@Autowired
	public DHPayController(AppConfig appConfig, IOrderService orderService,
			IProductService productService,IAppInfoService appInfoService) {
		this.appConfig = appConfig;
		this.orderService = orderService;
		this.productService = productService;
		this.appInfoService = appInfoService;
	}
	
	/**
	 * 
	 * @param appId
	 * @param response
	 * @param request
	 * @return
	 */
	@RequestMapping(value =InternalOrderPaymentService.DHPayNotifyURL, method=RequestMethod.GET)
	@ResponseBody
	 public ResponseData receiveNotification(
		@RequestParam(value = RequestParamNames.AppId, required = false) String appId,
		HttpServletResponse response,HttpServletRequest request){
		HashMap<String, String> returnParams = setRequestParams(request);
		String status  =""; String orderNo = "";
		if(returnParams != null){
			String hash    = returnParams.get(Constants.RequestParamNames.DHPayHASH);
			orderNo = returnParams.get(Constants.RequestParamNames.DHPayOrderNo);
			status  = returnParams.get(Constants.RequestParamNames.DHPayStatus);

			OrderInfo orderInfo = orderService.getOrderByCode(orderNo);
			if(orderInfo == null){
				logger.warn("The order code {} doesn't exist on orderpayment db",orderNo);
				return FailureResponse(MessageConstants.V5_INVALID_ORDERNO__CODE,"No order whose order code is"+orderNo);
			}
			if(status.equals(Status.Success.toString())){
				System.out.println("@@@@@@@@@@DHPayNotifyURL receive the notification:");
				orderInfo.setLastModified(new Date());
				orderInfo.setOrderStatus(OrderStatus.paid.value());
				orderInfo.setPaymentCode(returnParams.get(Constants.RequestParamNames.DHPayRefNo));
				orderService.updateOrderStatus(orderInfo);
				
				AppInfoData appInfoData = new AppInfoData();
				appInfoData.setEventType(Constants.CallbackEventType.OrdeStatusSync);
				appInfoData.setOrderCode(orderNo);
				appInfoData.setOrderStatus(OrderStatus.paid.value());
				appInfoData.setPaymentType(OrderPaymentType.DHPay.value());
				appInfoData.setOperation(returnParams.get(Constants.RequestParamNames.DHPayRefNo));
				appInfoService.syncOrderStatusFromThirdPart(appInfoData);
				
				response.setHeader("Content-type","text/html;charset=UTF-8");
				PrintWriter writer;
				try {
					writer = response.getWriter();
					writer.write("success");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					logger.warn("@@@@@@@@@@DHPayNotifyURL Exception");
					return FailureResponse(-1,"DHPayNotifyURL Exception: the order code"+ orderNo);
				}
				return successResponse(0,"succesful");
			} 
		}
		return FailureResponse(55555,"The status of DPPay for order"+orderNo+":"+status);
	}
	
	@SuppressWarnings("rawtypes")
	public HashMap<String, String> setRequestParams(HttpServletRequest request){
		Map properties = request.getParameterMap();
		
		HashMap<String,String> returnMap = new HashMap<String,String>();
		Iterator entries = properties.entrySet().iterator();
		Map.Entry entry;
		String name ="";
		String value ="";
		while(entries.hasNext()){
			entry =(Map.Entry)entries.next();
			name =(String)entry.getKey();
			Object valueObj = entry.getValue();
			if(null == valueObj){
				value ="";
			}else if(valueObj instanceof String[]){
				String[] values = (String[])valueObj;
				for(int i=0; i<values.length;i++){
					value = values[i] +",";
				}
				value = value.substring(0,value.length()-1);
			}else{
				value = valueObj.toString();
			}
			returnMap.put(name, value);
		}
		return returnMap;
	}
	public static void main(String[] args){
		String status = "01";
		if(status.equals(Status.Success.toString())){
			System.out.println("Yes");
		}
	}
	public static enum Status {//交易状态； 00 处理中 01 成功 02 失败
		INProgress("00",0), Success("01",1),Failed("02",2);
		private String status;
		private int index;
		
		private Status(String status, int index){
			this.status = status;
			this.index  = index;
		}
		@Override
		public String toString(){
			return this.status;
		}
	}
}
