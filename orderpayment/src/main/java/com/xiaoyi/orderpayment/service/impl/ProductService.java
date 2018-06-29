package com.xiaoyi.orderpayment.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xiaoyi.orderpayment.dao.ProductInfoMapper;
import com.xiaoyi.orderpayment.model.ProductInfo;
import com.xiaoyi.orderpayment.model.ProductInfoExample;
import com.xiaoyi.orderpayment.service.IProductService;
import com.xiaoyi.orderpayment.utilities.bean.ProductInfoDataResult;
import com.xiaoyi.orderpayment.utilities.constant.Constants;

import com.xiaoyi.orderpayment.config.AppConfig;

@Service
public class ProductService implements IProductService {
	
	private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
	
	private AppConfig appConfig;	
	
	@Autowired
	public ProductService(AppConfig appConfig){
		this.appConfig = appConfig;
	}
	
	@Override
	public boolean saveProductInfo(ProductInfo productInfo){
		SqlSession session = appConfig.sqlSessionFactory().openSession();
		try {
			ProductInfoMapper mapper = session.getMapper(ProductInfoMapper.class);
			try{
				int result = mapper.insertSelective(productInfo);
				if(result !=1){
					return false;
				}
			}catch(Exception e){
				logger.error("insert produt info failed:{}",e.getMessage());
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
	public ProductInfo getProductBySKUId(Long skuId){
		SqlSession session = appConfig.sqlSessionFactory().openSession();
		try{
			List<ProductInfo> result = new ArrayList<ProductInfo>();
			ProductInfoMapper mapper = session.getMapper(ProductInfoMapper.class);
			ProductInfoExample productInfoExample = new ProductInfoExample();
			productInfoExample.createCriteria().andProductSkuEqualTo(skuId);
			result = mapper.selectByExampleWithBLOBs(productInfoExample);
			return result.get(0);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			session.close();
		}
		return null;
	}
	
	@Override
	public List<ProductInfoDataResult> getProductList(Map<String,Object> params){
			SqlSession session = appConfig.sqlSessionFactory().openSession();
			try{
				ProductInfoMapper mapper = session.getMapper(ProductInfoMapper.class);
				ProductInfoExample productInfoExample = new ProductInfoExample();
								
				List<ProductInfo> results = new ArrayList<ProductInfo>();
				if(params.containsKey(Constants.RequestParamNames.ProductCurrency)
						&&params.containsKey(Constants.RequestParamNames.ProductChannel)
						&&params.containsKey(Constants.RequestParamNames.ProductId)){
					Integer productChannel  = Integer.parseInt(params.get(Constants.RequestParamNames.ProductChannel).toString());
					String  productCurrency = params.get(Constants.RequestParamNames.ProductCurrency).toString();
					Long    productId 		= Long.parseLong(params.get(Constants.RequestParamNames.ProductId).toString());
					productInfoExample.createCriteria().andProductAvailibleEqualTo(1).andIdEqualTo(productId).andProductChannelEqualTo(productChannel).andCurrencyEqualTo(productCurrency);
				}
				
				if(params.containsKey(Constants.RequestParamNames.ProductId)
						&& !params.containsKey(Constants.RequestParamNames.ProductChannel) 
						&& !params.containsKey(Constants.RequestParamNames.ProductCurrency)){
					Long    productId 		= Long.parseLong(params.get(Constants.RequestParamNames.ProductId).toString());
					productInfoExample.createCriteria().andProductAvailibleEqualTo(1).andIdEqualTo(productId);
				}
				
				if(!params.containsKey(Constants.RequestParamNames.ProductId) 
						&& params.containsKey(Constants.RequestParamNames.ProductChannel)
						&& params.containsKey(Constants.RequestParamNames.ProductCurrency)){
					Integer productChannel  = Integer.parseInt(params.get(Constants.RequestParamNames.ProductChannel).toString());
					String  productCurrency = params.get(Constants.RequestParamNames.ProductCurrency).toString();
					productInfoExample.createCriteria().andProductAvailibleEqualTo(1).andCurrencyEqualTo(productCurrency).andProductChannelEqualTo(productChannel);
				}
				
				if(!params.containsKey(Constants.RequestParamNames.ProductId) 
						&& !params.containsKey(Constants.RequestParamNames.ProductChannel)
						&& !params.containsKey(Constants.RequestParamNames.ProductCurrency)){
					productInfoExample.createCriteria().andProductAvailibleEqualTo(1);
				}
				
				if(params.containsKey(Constants.RequestParamNames.SKUId) 
						&& params.containsKey(Constants.RequestParamNames.ProductChannel)
						&& !params.containsKey(Constants.RequestParamNames.ProductId)
						&& !params.containsKey(Constants.RequestParamNames.ProductCurrency)){
					Integer productChannel  = Integer.parseInt(params.get(Constants.RequestParamNames.ProductChannel).toString());
					Long    productSku      = Long.parseLong(params.get(Constants.RequestParamNames.SKUId).toString());
					productInfoExample.createCriteria().andProductChannelEqualTo(productChannel).andProductSkuEqualTo(productSku);
				}
				results = mapper.selectByExampleWithBLOBs(productInfoExample);
				List<ProductInfoDataResult> productList = new ArrayList<ProductInfoDataResult>();
				
				if(results.isEmpty()){
					productList = null;
				}else{
					for(ProductInfo tmp:results) {
						ProductInfoDataResult product = new ProductInfoDataResult();
						product.setProductId(tmp.getId());
						product.setProductName(tmp.getProductName() == null? "":tmp.getProductName());
						product.setProductSKU(tmp.getProductSku() == null? 0:tmp.getProductSku());
						product.setProductChannel(tmp.getProductChannel() == null? 0 :tmp.getProductChannel());
						product.setProductPrice(tmp.getProductPrice() == null? 0:tmp.getProductPrice());
						product.setProductCurrency(tmp.getCurrency() == null? "":tmp.getCurrency());
						product.setProductType(tmp.getProductType() == null? 0:tmp.getProductType());
						product.setProductDescription(tmp.getProductDescription() == null? "":tmp.getProductDescription());
						product.setProductImg(tmp.getProductImg()==null? "":tmp.getProductImg());
						product.setProductIdentifier(tmp.getIapIdentifier() == null?"":tmp.getIapIdentifier());
						
						productList.add(product);
					}
				}
				return productList;
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				session.close();
			}
			return null;
     }
	
}
