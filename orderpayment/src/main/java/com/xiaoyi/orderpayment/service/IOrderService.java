package com.xiaoyi.orderpayment.service;

import java.util.List;

import com.xiaoyi.orderpayment.model.OrderInfo;

public interface IOrderService {
	boolean generateOrder(OrderInfo orderInfo);
	boolean submitOrder(String orderNo,int paymentType);
	List<OrderInfo> getOrderList(String userId);
	List<OrderInfo> getParticalOrdersToSyncInfoFromBTToOrderpayment(OrderInfo orderInfo);
	OrderInfo getOrderByCode(String orderCode);
	OrderInfo getOrderByPaymentCode(String PaymentCode);
	boolean updateOrderStatus(OrderInfo orderInfo);
}
