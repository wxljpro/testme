package com.xiaoyi.orderpayment.dao;

import com.xiaoyi.orderpayment.model.AppInfo;
import com.xiaoyi.orderpayment.model.AppInfoExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AppInfoMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tb_app_info
     *
     * @mbggenerated Wed Nov 08 16:31:08 CST 2017
     */
    int countByExample(AppInfoExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tb_app_info
     *
     * @mbggenerated Wed Nov 08 16:31:08 CST 2017
     */
    int deleteByExample(AppInfoExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tb_app_info
     *
     * @mbggenerated Wed Nov 08 16:31:08 CST 2017
     */
    int deleteByPrimaryKey(Long id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tb_app_info
     *
     * @mbggenerated Wed Nov 08 16:31:08 CST 2017
     */
    int insert(AppInfo record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tb_app_info
     *
     * @mbggenerated Wed Nov 08 16:31:08 CST 2017
     */
    int insertSelective(AppInfo record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tb_app_info
     *
     * @mbggenerated Wed Nov 08 16:31:08 CST 2017
     */
    List<AppInfo> selectByExample(AppInfoExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tb_app_info
     *
     * @mbggenerated Wed Nov 08 16:31:08 CST 2017
     */
    AppInfo selectByPrimaryKey(Long id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tb_app_info
     *
     * @mbggenerated Wed Nov 08 16:31:08 CST 2017
     */
    int updateByExampleSelective(@Param("record") AppInfo record, @Param("example") AppInfoExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tb_app_info
     *
     * @mbggenerated Wed Nov 08 16:31:08 CST 2017
     */
    int updateByExample(@Param("record") AppInfo record, @Param("example") AppInfoExample example);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tb_app_info
     *
     * @mbggenerated Wed Nov 08 16:31:08 CST 2017
     */
    int updateByPrimaryKeySelective(AppInfo record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table tb_app_info
     *
     * @mbggenerated Wed Nov 08 16:31:08 CST 2017
     */
    int updateByPrimaryKey(AppInfo record);
}