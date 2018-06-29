package com.xiaoyi.orderpayment.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.util.EncodingUtil;

import com.xiaoyi.orderpayment.utilities.constant.Constants;

public class DHPayUtil {

	static public String httpcall(String invoiceNo)
	{
	    StringBuilder respText = new StringBuilder("");
	    try
	    {
	    	String reqURL = Constants.DHPay.MERCHANT_QUERY_URL+invoiceNo;
	    	StringBuffer authSB = new StringBuffer();
	    	authSB.append(Constants.DHPay.MERCHANT_ID);
	    	authSB.append(":");
	    	authSB.append(Constants.DHPay.MERCHANT_SECRET);
	    	byte[] preAuthStr = EncodingUtil.getBytes(authSB.toString(), "UTF-8");
	    	String authen = Base64.encodeBase64String(preAuthStr);
	    			
	        URL postURL = new URL(reqURL);
	     
	        HttpURLConnection conn = (HttpURLConnection)postURL.openConnection();

	        // Set connection parameters. We need to perform input and output,
	        // so set both as true.
	        conn.setDoInput (true);
	        conn.setDoOutput (true);

	        // Set the content type we are POSTing. We impersonate it as
	        // encoded form data
	        conn.setRequestProperty("Content-Type", "application/json");
	        conn.setRequestProperty("Authorization", authen);

	        //conn.setRequestProperty( "Content-Type", type );
//	        conn.setRequestProperty( "Content-Length", String.valueOf( encodedData.length()) );
	        conn.setRequestMethod("POST");
	       
	        // get the output stream to POST to.
	        DataOutputStream output = new DataOutputStream( conn.getOutputStream());
//	        output.writeBytes( encodedData.toString() );
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
	            System.out.println("respsonse text:"+URLDecoder.decode(respText.toString()));
	            
	            
	        }
	        return URLDecoder.decode(respText.toString());
	    }
	    catch( IOException e )
	    {
	        // handle the error here
	        return null;
	    }
	}
	
	public static void main(String[] args){
		System.out.println(httpcall("20171222080025119992"));
	}
	
    public static String encodeSign(byte[] key) {
        try {
            return new String(Base64.encodeBase64(key), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
}
