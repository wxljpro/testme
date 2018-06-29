package com.xiaoyi.orderpayment.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xiaoyi.orderpayment.config.AppConfig;
import com.xiaoyi.orderpayment.dao.RechargeCardInfoMapper;
import com.xiaoyi.orderpayment.model.ProductInfo;
import com.xiaoyi.orderpayment.model.RechargeCardInfo;
import com.xiaoyi.orderpayment.model.RechargeCardInfoExample;
import com.xiaoyi.orderpayment.service.IProductService;
import com.xiaoyi.orderpayment.service.IRechargeCardService;
import com.xiaoyi.orderpayment.utilities.bean.RechargeCardDataResult;
import com.xiaoyi.orderpayment.utilities.constant.Constants;

@Service
public class RechargeCardService implements IRechargeCardService {
	private static final Logger logger = LoggerFactory.getLogger(RechargeCardService.class);
	
	private AppConfig appConfig;	
	private IProductService productService;
	
	@Autowired
	public RechargeCardService(AppConfig appConfig,IProductService productService){
		this.appConfig = appConfig;
		this.productService = productService;
	}
	
	@Override
	public List<RechargeCardDataResult> getRechargeCardInfo(String[] passwords){
		List<String> passwordList = new ArrayList<String>();
		for(String password:passwords){
			passwordList.add(password);
		}
		List<RechargeCardDataResult>  rechargeCardDataResultList = new ArrayList<RechargeCardDataResult>();
		SqlSession session = appConfig.sqlSessionFactory().openSession();
		try {
			RechargeCardInfoMapper mapper = session.getMapper(RechargeCardInfoMapper.class);
			RechargeCardInfoExample rechargeCardInfoExample = new RechargeCardInfoExample();
			rechargeCardInfoExample.createCriteria().andPwIn(passwordList);
			List<RechargeCardInfo> results = mapper.selectByExample(rechargeCardInfoExample);
			
			if(!results.isEmpty()){
				Map<String,String> params = null;
				for(RechargeCardInfo rechargeCardInfo:results){
					RechargeCardDataResult RechargeCardDataResult = new RechargeCardDataResult();
					RechargeCardDataResult.setExpireTime(rechargeCardInfo.getExpiredTime());
					RechargeCardDataResult.setPW(rechargeCardInfo.getPw());
					RechargeCardDataResult.setSN(rechargeCardInfo.getSn());
					RechargeCardDataResult.setSku(rechargeCardInfo.getSku());
					RechargeCardDataResult.setUsedFlag(rechargeCardInfo.getUsed());
					ProductInfo product = productService.getProductBySKUId(rechargeCardInfo.getSku());
					RechargeCardDataResult.setProductId(product.getId());
					
					rechargeCardDataResultList.add(RechargeCardDataResult);
				}
				return rechargeCardDataResultList;
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		return null;
	}

	@Override
	public boolean updateRechargeCardUsedStatus(RechargeCardInfo record){
		SqlSession session = appConfig.sqlSessionFactory().openSession();
		try{
			RechargeCardInfoMapper mapper = session.getMapper(RechargeCardInfoMapper.class);
			RechargeCardInfoExample rechargeCardInfoExample = new RechargeCardInfoExample();
			String password = record.getPw();
			rechargeCardInfoExample.createCriteria().andPwEqualTo(password);
		
			try{
				int result = mapper.updateUsedStatusByPW(record);
				if(result != 1){
					return false;
				}
			}catch(Exception e){
				logger.error("update the recharge card use status, whose password is "+password+" failed: {}", e.getMessage());
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
	public RechargeCardInfo getRechargeCardInfobyPW(String password){
		SqlSession session = appConfig.sqlSessionFactory().openSession();
		try{
			RechargeCardInfoMapper mapper = session.getMapper(RechargeCardInfoMapper.class);
			RechargeCardInfoExample rechargeCardInfoExample = new RechargeCardInfoExample();
			rechargeCardInfoExample.createCriteria().andPwEqualTo(password);
				
			List<RechargeCardInfo> results = mapper.selectByExample(rechargeCardInfoExample);
			return results.isEmpty() ? null : results.get(0);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		return null;
	}
	
	
	@Override
	public List<String> batchInsertRechargecardInfo(Map<String,String> params){
		List<String> pwList = new ArrayList<String>();
		List<RechargeCardInfo> rechargecardList = new ArrayList<RechargeCardInfo>();
//		if(params.containsKey("number")&&params.containsKey("currency")&&
//				params.containsKey("sku")&&params.containsKey("expiredTime")&&
//				params.containsKey("serviceTime")&&params.containsKey("subType")&&params.containsKey("productCode")){
			for(int i=0;i<Integer.parseInt(params.get("number"));i++){
				RechargeCardInfo rechargecard = new RechargeCardInfo();
				rechargecard.setId(0L);
				rechargecard.setExpiredTime(new Date(Long.parseLong(formatExpiredTime(params.get("expiredTime")))));
				rechargecard.setCurrency(params.get("currency"));
				rechargecard.setPrice(0);
				rechargecard.setSku(Long.parseLong(params.get("sku")));
				rechargecard.setUsed(0);
				rechargecard.setCreatedTime(new Date());
				rechargecard.setCreatedBy(Integer.parseInt(params.get("createdBy")));
				
				String sn=formatRechargeCardSN(params.get("productCode"))+formatRechargeCardSN(params.get("subType"))+formatRechargeCardSN(params.get("serviceTime"))+createData(11);
				String pw=createData(18);
				rechargecard.setPw(pw);
				rechargecard.setSn(sn);
				rechargecardList.add(rechargecard);
				pwList.add(pw);
			}	
//		}
		SqlSession session = appConfig.sqlSessionFactory().openSession();
		session.insert("batchInsertRechargecardInfo",rechargecardList);
		session.commit();
		session.close();
		return pwList;
	}
	
	@Override
	public long getMaxID(){
		SqlSession session = appConfig.sqlSessionFactory().openSession();
		try{
			RechargeCardInfoMapper mapper = session.getMapper(RechargeCardInfoMapper.class);
			long result =mapper.selectForRecordwithMaxId();
			return result;
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		return 0L;
	}
	
	private String createData(int length) {
	   StringBuilder sb=new StringBuilder();
	   Random rand=new Random();
	   for(int i=0;i<length;i++)
	   {
	      sb.append(rand.nextInt(10));
	   }
	   String data=sb.toString();
	   return data;
	}   
	
	private String formatExpiredTime(String expireTime){
		String formattedData="";
		int i = expireTime.length();
		if(i==10){
			formattedData=expireTime+"000";
		}else if(i==13){
			return expireTime;
		}else{
			return "1647645499000";
		}
		return formattedData;
	}
	
	private String formatRechargeCardSN(String data){
		String formattedData="";
		int i = data.length();
		if(i==1){
		    formattedData="0"+data;
		}else if(i==2){
			return data;
		}else{
			logger.error("the length of the servicetime, productCode, and subType should be less than 3.");
			formattedData="00";
		}
		return formattedData;
	}
	
//	public static void main(String args[]){
//		Date date1= new Date();
//		long d1 = date1.getTime();
//		long d = 1463598000000L;
//		Date date= new Date(d);
//		System.out.println(date.toString());
//		System.out.println(d1);
//		System.out.println(date1.toString());
//	}
}
