package com.xiaoyi.orderpayment.service.impl;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xiaoyi.orderpayment.dao.OrderInfoMapper;
import com.xiaoyi.orderpayment.dao.ProductInfoMapper;
import com.xiaoyi.orderpayment.model.OrderInfo;
import com.xiaoyi.orderpayment.model.OrderInfoExample;
import com.xiaoyi.orderpayment.model.ProductInfo;
import com.xiaoyi.orderpayment.model.ProductInfoExample;
import com.xiaoyi.orderpayment.service.IOrderService;

import com.xiaoyi.orderpayment.config.AppConfig;

@Service
public class OrderService implements IOrderService {

	private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
	
	private AppConfig appConfig;
	
	@Autowired
	public OrderService(AppConfig appConfig) {
		this.appConfig = appConfig;
	}
	
	@Override
	public List<OrderInfo> getParticalOrdersToSyncInfoFromBTToOrderpayment(OrderInfo orderInfo){
		SqlSession session = appConfig.sqlSessionFactory().openSession();
		try {
			
			OrderInfoMapper mapper = session.getMapper(OrderInfoMapper.class);
			List<OrderInfo> results = mapper.selectByLikeOrderCodeUnit(orderInfo);
			
			return results.isEmpty() ? null : results;
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		return null;
	}
	
	@Override
	public boolean updateOrderStatus(OrderInfo orderInfo){
		SqlSession session = appConfig.sqlSessionFactory().openSession();
		try{
			OrderInfoMapper mapper = session.getMapper(OrderInfoMapper.class);
			OrderInfoExample orderInfoExample = new OrderInfoExample();
			String orderCode =orderInfo.getOrderCode();
			orderInfoExample.createCriteria().andOrderCodeEqualTo(orderCode);
			try{
				int result = mapper.updateByOrderCode(orderInfo);
				if(result != 1){
					return false;
				}
			}catch(Exception e){
				logger.error("update the order "+orderCode+" 's status from Alipay failed: {}", e.getMessage());
				return false;
			}
			session.commit();
			return true;
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		return false;
	}
	
	@Override
	public OrderInfo getOrderByCode(String orderCode){
		SqlSession session = appConfig.sqlSessionFactory().openSession();
		try {
			OrderInfoMapper mapper = session.getMapper(OrderInfoMapper.class);
			OrderInfoExample orderInfoExample = new OrderInfoExample();
			orderInfoExample.createCriteria().andOrderCodeEqualTo(orderCode);
			List<OrderInfo> results = mapper.selectByExample(orderInfoExample);
			
			return results.isEmpty() ? null : results.get(0);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		return null;
	}
	
	@Override
	public boolean generateOrder(OrderInfo orderInfo) {	
		SqlSession session = appConfig.sqlSessionFactory().openSession();
		try {
			
			OrderInfoMapper mapper = session.getMapper(OrderInfoMapper.class);
			try{
				int result = mapper.insertSelective(orderInfo);
				if (result != 1) {
					return false;
				}				
			}catch(Exception e){
				logger.error("insert generate order failed: {}", e.getMessage());
				return false;
			}
			
			session.commit();
			return true;
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		return false;
	}
	
	@Override
	public OrderInfo getOrderByPaymentCode(String paymentCode){
		SqlSession session = appConfig.sqlSessionFactory().openSession();
		try {
			OrderInfoMapper mapper = session.getMapper(OrderInfoMapper.class);
			OrderInfoExample orderInfoExample = new OrderInfoExample();
			orderInfoExample.createCriteria().andPaymentCodeEqualTo(paymentCode);
			List<OrderInfo> results = mapper.selectByExample(orderInfoExample);
			
			return results.isEmpty() ? null : results.get(0);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		return null;
	}
	
	@Override
	public boolean submitOrder(String orderNo,int paymentType) {
		OrderInfo orderInfo = getOrderInfo(orderNo);
		
		Date date = new Date();
		orderInfo.setLastModified(date);
		orderInfo.setDatePurchased(date);
		orderInfo.setOrderStatus(1);
		orderInfo.setPaymentType(paymentType);
		
		return true;
	}
	
	@Override
	public List<OrderInfo> getOrderList(String userId) {
		SqlSession session = appConfig.sqlSessionFactory().openSession();
		try{
			OrderInfoMapper mapper = session.getMapper(OrderInfoMapper.class);
			OrderInfoExample orderInfoExample = new OrderInfoExample();
			orderInfoExample.createCriteria().andCustomerIdEqualTo(userId);
			List<OrderInfo> results = mapper.selectByExample(orderInfoExample);
			
			return results.isEmpty() ? null : results;
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		return null;
	}
	
	private OrderInfo getOrderInfo(String orderCode) {
		SqlSession session = appConfig.sqlSessionFactory().openSession();
		try {
			
			OrderInfoMapper mapper = session.getMapper(OrderInfoMapper.class);
			OrderInfoExample orderInfoExample = new OrderInfoExample();
			orderInfoExample.createCriteria().andOrderCodeEqualTo(orderCode);
			List<OrderInfo> results = mapper.selectByExample(orderInfoExample);
			
			return results.isEmpty() ? null : results.get(0);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		return null;
	}
	
}
