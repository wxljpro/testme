package com.xiaoyi.orderpayment.service.impl;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.h2.schema.Constant;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeCancelRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeCancelResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.xiaoyi.orderpayment.config.AppConfig;
import com.xiaoyi.orderpayment.dao.OrderInfoMapper;
import com.xiaoyi.orderpayment.model.OrderInfo;
import com.xiaoyi.orderpayment.model.OrderInfoExample;
import com.xiaoyi.orderpayment.service.IOrderService;
import com.xiaoyi.orderpayment.service.IPaymentService;
import com.xiaoyi.orderpayment.utilities.bean.AlipayInfoDataResult;
import com.xiaoyi.orderpayment.utilities.bean.AlipayStringDataResult;
import com.xiaoyi.orderpayment.utilities.bean.OrderInfoDataResult;
import com.xiaoyi.orderpayment.utilities.constant.Constants;
import com.xiaoyi.orderpayment.utilities.httpclient.HttpClient;
import com.xiaoyi.orderpayment.utilities.utilities.AlipayAPI;
import com.xiaoyi.orderpayment.utilities.pay.Alipay.AlipayCore;
import com.xiaoyi.orderpayment.utilities.pay.Alipay.AlipaySubmit;
import com.xiaoyi.orderpayment.utilities.pay.Alipay.MD5;
import com.xiaoyi.orderpayment.utilities.pay.Alipay.RSA;

@Service
public class PaymentService implements IPaymentService {
	
	private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
	
	private AppConfig appConfig;
	private IOrderService orderService;
	
	@Autowired
	public PaymentService(AppConfig appConfig,IOrderService orderService) {
		this.appConfig = appConfig;
		this.orderService = orderService;
	}
	
	@Override
	public AlipayStringDataResult getAlipayString(String orderCode,String subject, String body,String totalfee){
		AlipayStringDataResult alipayString = new AlipayStringDataResult();
		
        alipayString.setAlipayString(getOrderInfoString(orderCode,subject,body,totalfee));
        
		return alipayString;
	}
	
	@Override
	public int queryOrderStatusFromAlipay(String orderCode){
		int status = 1;
		Map<String, String> sParaTemp = new HashMap<String, String>();
		sParaTemp.put("service", "single_trade_query");
		sParaTemp.put("partner", Constants.Alipay.ALIPAY_PARTNER);
		sParaTemp.put("_input_charset", Constants.Alipay.ALIPAY_CHARSET);
		sParaTemp.put("out_trade_no", orderCode);
		try{
			String sHtmlText = AlipaySubmit.buildRequest("", "", sParaTemp);
			status = getorderStatusFromAlipay(sHtmlText);
			return status;
		}catch(Exception e){
			e.printStackTrace();
		}
		return status;
	}
	
	@Override
	public boolean cancelOrderFromAlipay(String orderCode) {
		boolean isSuccess = false;
		Map<String, String> sParaTemp = new HashMap<String, String>();
		sParaTemp.put("service", "close_trade");
		sParaTemp.put("partner", Constants.Alipay.ALIPAY_PARTNER);
		sParaTemp.put("_input_charset", Constants.Alipay.ALIPAY_CHARSET);
		sParaTemp.put("out_trade_no", orderCode);
		
		try{
			String sHtmlText = AlipaySubmit.buildRequest("", "", sParaTemp);
			isSuccess = getOrderStatusFromAlipay(sHtmlText);
			return isSuccess;
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return isSuccess;
	}
	
	@Override
	public List<OrderInfoDataResult> getOrderStatusFromOrderPaymentServer(String[] orderCodes){
		List<String> orderCodeStringList = new ArrayList<String>();
		for(String orderCode:orderCodes){
			orderCodeStringList.add(orderCode);
			//sync the order status from Alipay too before sync the status to APP server
			OrderInfo orderInfo = orderService.getOrderByCode(orderCode);
			orderInfo.setOrderStatus(queryOrderStatusFromAlipay(orderCode));
			orderService.updateOrderStatus(orderInfo);
		}
		List<OrderInfoDataResult>  orderDataResultList = new ArrayList<OrderInfoDataResult>();
		SqlSession session = appConfig.sqlSessionFactory().openSession();
		try {
			OrderInfoMapper mapper = session.getMapper(OrderInfoMapper.class);
			OrderInfoExample orderInfoExample = new OrderInfoExample();
			orderInfoExample.createCriteria().andOrderCodeIn(orderCodeStringList);
			List<OrderInfo> results = mapper.selectByExample(orderInfoExample);
			
			if(!results.isEmpty()){
				for(OrderInfo orderInfo:results){
					OrderInfoDataResult orderDataResult = new OrderInfoDataResult();
					orderDataResult.setOrderNo(orderInfo.getOrderCode());
					orderDataResult.setOrderStatus(orderInfo.getOrderStatus());
					orderDataResultList.add(orderDataResult);
				}
				return orderDataResultList;
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		return null;
	}
	
	@Override
	public List<OrderInfoDataResult> getSingleTradeStatusFromAlipay(String[] orderCodes){
		List<OrderInfoDataResult> orderList = new ArrayList<OrderInfoDataResult>();
		for(String orderCode:orderCodes){
			OrderInfoDataResult orderDateResult = new OrderInfoDataResult();
			orderDateResult.setOrderNo(orderCode);
			orderDateResult.setOrderStatus(getSingleTradeStatusFromAlipay(orderCode));
		}
		return orderList;
	}
	
	private static Map<String,String> getOrderInfoMap(String orderCode,String subject, String body,int totalfee){
		Map<String,String> orderInfo = new HashMap<String,String>();
		
		orderInfo.put("partner", "\""+Constants.Alipay.ALIPAY_PARTNER+"\"");
		orderInfo.put("seller_id", "\""+Constants.Alipay.ALIPAY_SELLER_ACCOUNT+"\"");
		orderInfo.put("out_trade_no", "\""+orderCode+"\"");
		orderInfo.put("subject", "\""+subject+"\"");
		orderInfo.put("body", "\""+body+"\"");
		orderInfo.put("total_fee", "\""+totalfee+"\"");
		orderInfo.put("notify_url", "\""+Constants.Alipay.NOTIFY_URL+"\"");
		orderInfo.put("service", "\""+Constants.Alipay.ALIPAY_PAYSERVICE+"\"");
		orderInfo.put("payment_type", "\"1\"");
		orderInfo.put("_input_charset", "\""+Constants.Alipay.ALIPAY_APP_INPUT_CHARSET+"\"");
		orderInfo.put("it_b_pay", "\""+Constants.Alipay.ALIPAY_APP_PAY_TIMEOUT+"\"");
		orderInfo.put("return_url", "\""+Constants.Alipay.ALIPAY_APP_PAY_RETURNURL+"\"");
		orderInfo.put("paymethod", "\""+Constants.Alipay.ALIPAY_APP_PAY_PAYMETHOD+"\"");
		
		return orderInfo;
	}
	
	private static String getOrderInfoString(String orderCode,String subject, String body,String totalfee){
		  // 绛剧害鍚堜綔鑰呰韩浠絀D
        String orderInfo = "partner=" + "\"" + Constants.Alipay.ALIPAY_PARTNER + "\"";

        // 绛剧害鍗栧鏀粯瀹濊处鍙�
        orderInfo += "&seller_id=" + "\"" + Constants.Alipay.ALIPAY_SELLER_ACCOUNT + "\"";

        // 鍟嗘埛缃戠珯鍞竴璁㈠崟鍙�
        orderInfo += "&out_trade_no=" + "\"" + orderCode + "\"";

        // 鍟嗗搧鍚嶇О
        orderInfo += "&subject=" + "\"" + subject + "\"";

        // 鍟嗗搧璇︽儏
        orderInfo += "&body=" + "\"" + body + "\"";

        // 鍟嗗搧閲戦
        orderInfo += "&total_fee=" + "\"" + totalfee + "\"";

        // 鏈嶅姟鍣ㄥ紓姝ラ�氱煡椤甸潰璺緞
        orderInfo += "&notify_url=" + "\"" + Constants.Alipay.NOTIFY_URL +"\"";

        // 鏈嶅姟鎺ュ彛鍚嶇О锛� 鍥哄畾鍊�
        orderInfo += "&service=\""+ Constants.Alipay.ALIPAY_PAYSERVICE +"\"";

        // 鏀粯绫诲瀷锛� 鍥哄畾鍊�
        orderInfo += "&payment_type=\"1\"";

        // 鍙傛暟缂栫爜锛� 鍥哄畾鍊�
        orderInfo += "&_input_charset=\""+ Constants.Alipay.ALIPAY_APP_INPUT_CHARSET +"\"";

        // 璁剧疆鏈粯娆句氦鏄撶殑瓒呮椂鏃堕棿
        // 榛樿30鍒嗛挓锛屼竴鏃﹁秴鏃讹紝璇ョ瑪浜ゆ槗灏变細鑷姩琚叧闂��
        // 鍙栧�艰寖鍥达細1m锝�15d銆�
        // m-鍒嗛挓锛宧-灏忔椂锛宒-澶╋紝1c-褰撳ぉ锛堟棤璁轰氦鏄撲綍鏃跺垱寤猴紝閮藉湪0鐐瑰叧闂級銆�
        // 璇ュ弬鏁版暟鍊间笉鎺ュ彈灏忔暟鐐癸紝濡�1.5h锛屽彲杞崲涓�90m銆�
        orderInfo += "&it_b_pay=\""+ Constants.Alipay.ALIPAY_APP_PAY_TIMEOUT +"\"";

        // extern_token涓虹粡杩囧揩鐧绘巿鏉冭幏鍙栧埌鐨刟lipay_open_id,甯︿笂姝ゅ弬鏁扮敤鎴峰皢浣跨敤鎺堟潈鐨勮处鎴疯繘琛屾敮浠�
        // orderInfo += "&extern_token=" + "\"" + extern_token + "\"";

        // 鏀粯瀹濆鐞嗗畬璇锋眰鍚庯紝褰撳墠椤甸潰璺宠浆鍒板晢鎴锋寚瀹氶〉闈㈢殑璺緞锛屽彲绌�
        orderInfo += "&return_url=\""+ Constants.Alipay.ALIPAY_APP_PAY_RETURNURL +"\"";

        // 璋冪敤閾惰鍗℃敮浠橈紝闇�閰嶇疆姝ゅ弬鏁帮紝鍙備笌绛惧悕锛� 鍥哄畾鍊� 锛堥渶瑕佺绾︺�婃棤绾块摱琛屽崱蹇嵎鏀粯銆嬫墠鑳戒娇鐢級
        // orderInfo += "&paymethod=\"expressGateway\"";

        String sign = RSA.sign(orderInfo, Constants.Alipay.ALIPAY_PRIVATE_KEY, Constants.Alipay.ALIPAY_APP_INPUT_CHARSET);
        orderInfo += "&sign=\""+URLEncoder.encode(sign)+"\"";
        orderInfo += "&sign_type=\""+Constants.Alipay.ALIPAY_SIGN_TYPE+"\"";
        
        return orderInfo;
	}
	
	private static String getOrderInfoString1(String orderCode,String subject, String body,String totalfee){
		  // 绛剧害鍚堜綔鑰呰韩浠絀D
      String orderInfo = "partner=" + "\"" + Constants.Alipay.ALIPAY_PARTNER + "\"";

      // 绛剧害鍗栧鏀粯瀹濊处鍙�
      orderInfo += "&seller_id=" + "\"" + Constants.Alipay.ALIPAY_SELLER_ACCOUNT + "\"";

      // 鍟嗘埛缃戠珯鍞竴璁㈠崟鍙�
      orderInfo += "&out_trade_no=" + "\"" + orderCode + "\"";

      // 鍟嗗搧鍚嶇О
      orderInfo += "&subject=" + "\"" + subject + "\"";

      // 鍟嗗搧璇︽儏
      orderInfo += "&body=" + "\"" + body + "\"";

      // 鍟嗗搧閲戦
      orderInfo += "&total_fee=" + "\"" + totalfee + "\"";

      // 鏈嶅姟鍣ㄥ紓姝ラ�氱煡椤甸潰璺緞
      orderInfo += "&notify_url=" + "\"" + Constants.Alipay.NOTIFY_URL +"\"";

      // 鏈嶅姟鎺ュ彛鍚嶇О锛� 鍥哄畾鍊�
      orderInfo += "&service=\""+ Constants.Alipay.ALIPAY_PAYSERVICE +"\"";

      // 鏀粯绫诲瀷锛� 鍥哄畾鍊�
      orderInfo += "&payment_type=\"1\"";

      // 鍙傛暟缂栫爜锛� 鍥哄畾鍊�
      orderInfo += "&_input_charset=\""+ Constants.Alipay.ALIPAY_APP_INPUT_CHARSET +"\"";

      // 璁剧疆鏈粯娆句氦鏄撶殑瓒呮椂鏃堕棿
      // 榛樿30鍒嗛挓锛屼竴鏃﹁秴鏃讹紝璇ョ瑪浜ゆ槗灏变細鑷姩琚叧闂��
      // 鍙栧�艰寖鍥达細1m锝�15d銆�
      // m-鍒嗛挓锛宧-灏忔椂锛宒-澶╋紝1c-褰撳ぉ锛堟棤璁轰氦鏄撲綍鏃跺垱寤猴紝閮藉湪0鐐瑰叧闂級銆�
      // 璇ュ弬鏁版暟鍊间笉鎺ュ彈灏忔暟鐐癸紝濡�1.5h锛屽彲杞崲涓�90m銆�
      orderInfo += "&it_b_pay=\""+ Constants.Alipay.ALIPAY_APP_PAY_TIMEOUT +"\"";

      // extern_token涓虹粡杩囧揩鐧绘巿鏉冭幏鍙栧埌鐨刟lipay_open_id,甯︿笂姝ゅ弬鏁扮敤鎴峰皢浣跨敤鎺堟潈鐨勮处鎴疯繘琛屾敮浠�
      // orderInfo += "&extern_token=" + "\"" + extern_token + "\"";

      // 鏀粯瀹濆鐞嗗畬璇锋眰鍚庯紝褰撳墠椤甸潰璺宠浆鍒板晢鎴锋寚瀹氶〉闈㈢殑璺緞锛屽彲绌�
      orderInfo += "&return_url=\""+ Constants.Alipay.ALIPAY_APP_PAY_RETURNURL +"\"";

      // 璋冪敤閾惰鍗℃敮浠橈紝闇�閰嶇疆姝ゅ弬鏁帮紝鍙備笌绛惧悕锛� 鍥哄畾鍊� 锛堥渶瑕佺绾︺�婃棤绾块摱琛屽崱蹇嵎鏀粯銆嬫墠鑳戒娇鐢級
      // orderInfo += "&paymethod=\"expressGateway\"";

      String sign = RSA.sign(orderInfo, Constants.Alipay.ALIPAY_PRIVATE_KEY, Constants.Alipay.ALIPAY_APP_INPUT_CHARSET);
      orderInfo += "&sign=\""+URLEncoder.encode(sign)+"\"";
      orderInfo += "&sign_type=\""+Constants.Alipay.ALIPAY_SIGN_TYPE+"\"";
      
      return orderInfo;
	}
	
	public Integer getSingleTradeStatusFromAlipay(String orderNo){
	     Map<String,String> params = new HashMap<String,String>();
	     params.put("service", "single_trade_query");
	     params.put("partner", Constants.Alipay.ALIPAY_PARTNER);
	     params.put("_input_charset", Constants.Alipay.ALIPAY_CHARSET);
	     params.put("out_trade_no", orderNo);	//   params.put("trade_no", value);
	     String requestUrl = AlipayAPI.createLinkString(params);
	     String signature = MD5.sign(requestUrl, Constants.Alipay.ALIPAY_PRIVATE_KEY, 
	    		 Constants.Alipay.ALIPAY_CHARSET);
	     params.put("sign", signature);
	     params.put("sign_type", Constants.Alipay.ALIPAY_SIGN_TYPE);
	     
	     HttpClient httpClient = new HttpClient();
	     String response = httpClient.get(Constants.Alipay.ALIPAY_GATEWAY, params);
	     Integer status = getorderStatusFromAlipay(response);
	     
	     return status;
	}
	
	public boolean getOrderStatusFromAlipay(String response){
		boolean isSuccess =false;
		
		 SAXBuilder saxBuilder = new SAXBuilder();
	     try {
	    	    org.jdom.Document doc = saxBuilder.build(new StringReader(response));
	    	    Element root = doc.getRootElement();
	    	    String namedChildren = root.getChildText("is_success");
	    	    if(namedChildren.equals("T")){
	    	    	isSuccess = true;
	    	    }else if(namedChildren.equals("F")){
	    	    	isSuccess = false;
	    	    }
	     }catch(JDOMException e){
	    	// handle JDOMException
	    		e.printStackTrace();
	    	} catch (IOException e) {
	    	    // handle IOException
	    		e.printStackTrace();
	     }
		return isSuccess;
	}
	
	public int getorderStatusFromAlipay(String response){
		 int status = 1;
		 SAXBuilder saxBuilder = new SAXBuilder();
	     try {
	    	    org.jdom.Document doc = saxBuilder.build(new StringReader(response));
	    	    Element root = doc.getRootElement();
	    	    String namedChildren = root.getChildText("is_success");
	    	    Element orderStatus = (Element)XPath.selectSingleNode(doc, "//alipay/response/trade/trade_status");
		    	if(orderStatus != null){
		    		String actualStatusText = orderStatus.getText();
			    	if(namedChildren.equals("T")){
			    		if(actualStatusText.equals("TRADE_FINISHED"))
			    			status=2;
			    		if(actualStatusText.equals("TRADE_SUCCESS"))
			    			status=2;
			    	 }else if(namedChildren.equals("F")){
			    		 if(actualStatusText.equals("TRADE_CANCEL"))
			    			 status =-1;
			    		 if(actualStatusText.equals("TRADE_CLOSED"))
			    			 status =-1;
			    	 }
		    	}
	    	} catch (JDOMException e) {
	    	    // handle JDOMException
	    		e.printStackTrace();
	    	} catch (IOException e) {
	    	    // handle IOException
	    		e.printStackTrace();
	    	}
	     return status;
	}
	
	public static int queryOrderStatusFromAlipay1(String orderCode){
		int status = 1;
		Map<String, String> sParaTemp = new HashMap<String, String>();
		sParaTemp.put("service", "single_trade_query");
		sParaTemp.put("partner", Constants.Alipay.ALIPAY_PARTNER);
		sParaTemp.put("_input_charset", Constants.Alipay.ALIPAY_CHARSET);
		sParaTemp.put("out_trade_no", orderCode);
		try{
			String sHtmlText = AlipaySubmit.buildRequest("", "", sParaTemp);
			status = getorderStatusFromAlipay1(sHtmlText);
			return status;
		}catch(Exception e){
			e.printStackTrace();
		}
		return status;
	}
	
	public static int getorderStatusFromAlipay1(String response){
		 int status = 1;
		 SAXBuilder saxBuilder = new SAXBuilder();
	     try {
	    	    org.jdom.Document doc = saxBuilder.build(new StringReader(response));
	    	    Element root = doc.getRootElement();
	    	    String namedChildren = root.getChildText("is_success");
	    	    Element orderStatus = (Element)XPath.selectSingleNode(doc, "//alipay/response/trade/trade_status");
		    	if(orderStatus != null){
		    		String actualStatusText = orderStatus.getText();
			    	if(namedChildren.equals("T")){
			    		if(actualStatusText.equals("TRADE_FINISHED"))
			    			status=2;
			    		if(actualStatusText.equals("TRADE_SUCCESS"))
			    			status=2;
			    	 }else if(namedChildren.equals("F")){
			    		 if(actualStatusText.equals("TRADE_CANCEL"))
			    			 status =-1;
			    		 if(actualStatusText.equals("TRADE_CLOSED"))
			    			 status =-1;
			    	 }
		    	}
	    	} catch (JDOMException e) {
	    	    // handle JDOMException
	    		e.printStackTrace();
	    	} catch (IOException e) {
	    	    // handle IOException
	    		e.printStackTrace();
	    	}
	     return status;
	}
	
	public static void main(String[] args) {
			 
	}
}
