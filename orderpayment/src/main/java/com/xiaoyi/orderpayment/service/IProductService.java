package com.xiaoyi.orderpayment.service;

import java.util.List;
import java.util.Map;

import com.xiaoyi.orderpayment.model.ProductInfo;
import com.xiaoyi.orderpayment.utilities.bean.ProductInfoDataResult;

public interface IProductService {
  boolean saveProductInfo(ProductInfo productInfo);
  List<ProductInfoDataResult> getProductList(Map<String,Object> params);
  ProductInfo getProductBySKUId(Long skuId);
}
