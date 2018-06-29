package com.xiaoyi.orderpayment.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;

import com.xiaoyi.orderpayment.config.AppConfig;

public class PayPalUtil {
	
	String gvVersion = "";
	String gvAPIPassword = "";
	String gvAPIUserName ="";
	String gvAPISignature ="";
	String gvBNCode ="";
	String gvAPIEndpoint = "";
	public String sellerEmail = "";
	
	public PayPalUtil(AppConfig appConfig){
		Properties properties = appConfig.payPalProperties();
		gvVersion = properties.getProperty("API_VERSION");
		gvAPIPassword = properties.getProperty("PP_PASSWORD");
		gvAPIUserName =properties.getProperty("PP_USER");
		gvAPISignature =properties.getProperty("PP_SIGNATURE");
		gvBNCode =properties.getProperty("SBN_CODE");
		gvAPIEndpoint = properties.getProperty("PP_NVP_ENDPOINT");	
		sellerEmail = properties.getProperty("PP_SELLER_EMAIL");
		
		java.lang.System.setProperty("https.protocols", properties.getProperty("SSL_VERSION_TO_USE"));
	}
	public boolean isSet(Object value){
		return (value !=null && value.toString().length()!=0);
	}
	
	/*********************************************************************************
	  * httpcall: Function to perform the API call to PayPal using API signature
	  * 	@ methodName is name of API  method.
	  * 	@ nvpStr is nvp string.
	  * returns a NVP string containing the response from the server.
	*********************************************************************************/
	public HashMap<String, String> httpcall( String methodName, String nvpStr)
	{

	    String version = "2.3";
	    String agent = "Mozilla/4.0";
	    StringBuilder respText = new StringBuilder("");
	    HashMap nvp = null; 

	    //deformatNVP( nvpStr );
	    StringBuilder encodedData = new StringBuilder("METHOD=").append(methodName).append("&VERSION=").append(gvVersion)
	    		.append("&PWD=").append(gvAPIPassword)
	    		.append("&USER=").append(gvAPIUserName)
	    		.append("&SIGNATURE=").append(gvAPISignature)
	    		.append(nvpStr).append("&BUTTONSOURCE=").append(gvBNCode);

	    try
	    {
	        URL postURL = new URL(gvAPIEndpoint);
	     
	        HttpURLConnection conn = (HttpURLConnection)postURL.openConnection();

	        // Set connection parameters. We need to perform input and output,
	        // so set both as true.
	        conn.setDoInput (true);
	        conn.setDoOutput (true);

	        // Set the content type we are POSTing. We impersonate it as
	        // encoded form data
	        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	        conn.setRequestProperty( "User-Agent", agent );

	        //conn.setRequestProperty( "Content-Type", type );
	        conn.setRequestProperty( "Content-Length", String.valueOf( encodedData.length()) );
	        conn.setRequestMethod("POST");
	       
	        // get the output stream to POST to.
	        DataOutputStream output = new DataOutputStream( conn.getOutputStream());
	        output.writeBytes( encodedData.toString() );
	        output.flush();
	        output.close ();

	        // Read input from the input stream.
	        int rc = conn.getResponseCode();
	        if ( rc != -1)
	        {
	            BufferedReader is = new BufferedReader(new InputStreamReader( conn.getInputStream()));
	            String line = null;
	            while(((line = is.readLine()) !=null))
	            {
	                respText.append(line);
	            }
//	            System.out.println("respsonse text:"+URLDecoder.decode(respText.toString()));
	            nvp = deformatNVP(respText.toString());
	        }
	        return nvp;
	    }
	    catch( IOException e )
	    {
	        // handle the error here
	        return null;
	    }
	}
	    /*********************************************************************************
	     * deformatNVP: Function to break the NVP string into a HashMap
	     * 	pPayLoad is the NVP string.
	     * returns a HashMap object containing all the name value pairs of the string.
	   *********************************************************************************/
	   public HashMap deformatNVP( String pPayload)
	   {
	       HashMap nvp = new HashMap();
	       StringTokenizer stTok = new StringTokenizer( pPayload, "&");
	       while (stTok.hasMoreTokens())
	       {
	           StringTokenizer stInternalTokenizer = new StringTokenizer( stTok.nextToken(), "=");
	           if (stInternalTokenizer.countTokens() == 2)
	           {           String key;
	   			try {
	   				key = URLDecoder.decode(stInternalTokenizer.nextToken(), "UTF-8");
	   	            String value;
	   				value = URLDecoder.decode(stInternalTokenizer.nextToken(), "UTF-8");
	   				nvp.put( key.toUpperCase(), value );
	   			} catch (UnsupportedEncodingException e) {
	   				// TODO Auto-generated catch block
	   				e.printStackTrace();
	   			}			
	               
	           }
	       }
	       return nvp;
	   }
	   
	   public  String encode(Object object){
			try {
				return URLEncoder.encode((String) object, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return (String) object;
		} 
	   
	   public static String showError(HashMap<String,String> nvpResult){
		   String errorString ="";
		   try{
			String ErrorCode = nvpResult.get("L_ERRORCODE0").toString();
			String ErrorShortMsg = nvpResult.get("L_SHORTMESSAGE0").toString();
			String ErrorLongMsg = nvpResult.get("L_LONGMESSAGE0").toString();
			String ErrorSeverityCode = nvpResult.get("L_SEVERITYCODE0").toString();                                                                                                                                                                                                                                
			errorString = "SetExpressCheckout API call failed. " 
					+ " Detailed Error Message: " + ErrorLongMsg
					+ " Short Error Message: " + ErrorShortMsg 
					+ " Error Code: " + ErrorCode
					+ " Error Severity Code: " + ErrorSeverityCode; 
			
		   }catch(Exception e){
			   e.printStackTrace();
			   return errorString;
		   }
		   return errorString;
	   }
	   
	   public static void main(String args[]){
		   String errorString ="SetExpressCheckout API call failed. " 
					+ " Detailed Error Message: " + "hello world"
					+ " Short Error Message: " + "hello" 
					+ " Error Code: " + "000011"
					+ " Error Severity Code: " + "000012";
		   
		   String detailedErrorMsg = errorString.substring(errorString.indexOf(": ")+1,errorString.indexOf(" Short Error Message")).trim();
		   System.out.println(detailedErrorMsg);
	   }
}
