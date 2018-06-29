package com.xiaoyi.orderpayment.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.filter.CharacterEncodingFilter;

import com.braintreegateway.BraintreeGateway;
import com.xiaoyi.orderpayment.utilities.filter.IAuthInfoService;
import com.xiaoyi.orderpayment.utilities.filter.RequestValidator;
import com.xiaoyi.orderpayment.utilities.httpclient.HttpClient;
import com.xiaoyi.orderpayment.utilities.httpclient.IHttpClient;
import com.xiaoyi.orderpayment.utilities.pay.Braintree.BraintreeGatewayFactory;


@Configuration
public class AppConfig {
	private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
	
	@Configuration
	@ConfigurationProperties(ignoreUnknownFields = true, prefix = "database")
	public static class DatasourceConfig{
		private String configPath;

		public DatasourceConfig() {}

		public String getConfigPath() {
			return configPath;
		}

		public void setConfigPath(String configPath) {
			this.configPath = configPath;
		}
	}
	
	@Configuration
	@ConfigurationProperties(ignoreUnknownFields = true, prefix = "paypal")
	public static class PayPalConfig{
		private String propertiesPath;
		
		public String getPayPalPropertiesPath(){
			return propertiesPath;
		}
		
		public void setPayPalPropertiesPath(String propertiesPath){
			this.propertiesPath = propertiesPath;
		}
	}
	
	@Configuration
	@ConfigurationProperties(ignoreUnknownFields = true, prefix = "braintree")
	public static class BraintreeConfig{
		private String configPath;

		public BraintreeConfig() {}

		public String getConfigPath() {
			return configPath;
		}

		public void setConfigPath(String configPath) {
			this.configPath = configPath;
		}
	}
	
	@Configuration
	@ConfigurationProperties(ignoreUnknownFields = true, prefix = "mybatis")
		public static class MybatisConfig {
			private String configXmlPath;

			public MybatisConfig() {}

			public String getConfigXmlPath() {
				return configXmlPath;
			}

			public void setConfigXmlPath(String configXmlPath) {
				this.configXmlPath = configXmlPath;
			}
		}
	
	private IHttpClient httpClient;
	public static BraintreeGateway gateway;
	
	@Autowired
	public BraintreeConfig braintreeConfig;
	
	@Autowired
	public PayPalConfig payPalConfig;
	
	@Autowired
	public DatasourceConfig datasourceConfig;
	
	@Autowired
	public MybatisConfig mybatisConfig;
	
	@Autowired
	private IAuthInfoService appInfoService;
	
	public AppConfig() {
		this.httpClient = new HttpClient();
	}
	
	public IHttpClient getHttpClient() {
		return this.httpClient;
	}

	public void setHttpClient(IHttpClient httpClient) {
		this.httpClient = httpClient;
	}

	@Bean
	@Lazy(true)
	public BraintreeGateway braintreeGateway() {
		try{
			File configFile = Resources.getResourceAsFile(braintreeConfig.getConfigPath());
			if(configFile.exists() && !configFile.isDirectory()){
				return BraintreeGatewayFactory.fromConfigFile(configFile);
			}else{
				return BraintreeGatewayFactory.fromConfigMapping(System.getenv());
			}
		}catch(NullPointerException e){
			logger.error("{}: error load Braintree configuartion", e.getMessage());
			e.printStackTrace();
			return null;
		}catch (IOException e){
			logger.error("{}: error load Braintree configuartion", e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	@Bean
	@Lazy(true)
	public Properties payPalProperties(){
		try {
			logger.info("initialize the paypal perperties");
			Properties properties = Resources.getResourceAsProperties("paypal.properties");
			return properties;
		} catch (IOException e) {
			logger.error("{}: error load configuartion", e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	@Bean
	@Lazy(true)
	public SqlSessionFactory sqlSessionFactory() {
		try {
			logger.info("initialize the sql session factory");
			InputStream inputStream = Resources.getResourceAsStream(mybatisConfig.getConfigXmlPath());
			Properties properties = Resources.getResourceAsProperties(datasourceConfig.getConfigPath());
			SqlSessionFactory sqlSessionFactory =
					new SqlSessionFactoryBuilder().build(inputStream, properties);
			return sqlSessionFactory;
		} catch (IOException e) {
			logger.error("{}: error load configuartion", e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	@Bean
	@Lazy(true)
	public CharacterEncodingFilter characterEncodingFilter(){
		CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
        characterEncodingFilter.setEncoding("UTF-8");
        characterEncodingFilter.setForceEncoding(true);
        return characterEncodingFilter;
	}
	
	@Bean
	@Lazy(true)
	public FilterRegistrationBean filterRegistrationBean() {
		RequestValidator requestValidator = new RequestValidator(appInfoService);
	    FilterRegistrationBean registrationBean = new FilterRegistrationBean();
	    registrationBean.setFilter(requestValidator);
	    registrationBean.setUrlPatterns(Arrays.asList("/server/*"));
	    return registrationBean;
	}
}
