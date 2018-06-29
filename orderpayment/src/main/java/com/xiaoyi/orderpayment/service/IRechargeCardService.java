package com.xiaoyi.orderpayment.service;

import java.util.List;
import java.util.Map;

import com.xiaoyi.orderpayment.model.RechargeCardInfo;
import com.xiaoyi.orderpayment.utilities.bean.RechargeCardDataResult;

public interface IRechargeCardService {
  List<RechargeCardDataResult> getRechargeCardInfo(String[] password);
  RechargeCardInfo getRechargeCardInfobyPW(String password);
  boolean updateRechargeCardUsedStatus(RechargeCardInfo record);
  List<String> batchInsertRechargecardInfo(Map<String,String> params);
  long getMaxID();
}
