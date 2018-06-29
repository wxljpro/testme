package com.xiaoyi.orderpayment.service;

import java.util.List;

import com.xiaoyi.orderpayment.utilities.bean.AlipayInfoDataResult;
import com.xiaoyi.orderpayment.utilities.bean.AlipayStringDataResult;
import com.xiaoyi.orderpayment.utilities.bean.OrderInfoDataResult;

public interface IPaymentService {
	public List<OrderInfoDataResult> getSingleTradeStatusFromAlipay(String[] orderCodes);
	public List<OrderInfoDataResult> getOrderStatusFromOrderPaymentServer(String[] orderCodes);
	public boolean cancelOrderFromAlipay(String orderCode);
	public int queryOrderStatusFromAlipay(String orderCode);
	public AlipayStringDataResult getAlipayString(String orderCode,String subject, String body,String totalfee);
}
