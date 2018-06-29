<html>
<head>
<meta charset="utf-8" />
<title>IPN Local Testing</title>
</head>
<body>
	<form target="_new" method="post" action="https://paypaldemo.mi-ae.com/ipn.jsp">
	  <input type="hidden" name="mc_gross" value="19.95"/>
	  <input type="hidden" name="protection_eligibility" value="Eligible"/>
	  <input type="hidden" name="address_status" value="confirmed"/>
	  <input type="hidden" name="payer_id" value="LPLWNMTBWMFAY"/>
	  <input type="hidden" name="tax" value="0.00"/>
	  <input type="hidden" name="address_street" value="1 Main St"/>
	  <input type="hidden" name="payment_date" value="20:12:59 Jan 13, 2009 PST"/>	  
	  <input type="hidden" name="payment_status" value="Completed"/>
	  <input type="hidden" name="charset" value="windows-1252"/>
	  <input type="hidden" name="address_zip" value="95131"/>
	  <input type="hidden" name="first_name" value="Test"/>
	  <input type="hidden" name="mc_fee" value="0.88"/>	  
	  <input type="hidden" name="address_country_code" value="US"/>	
	  <input type="hidden" name="address_name" value="Test User"/>	
	  
	  
	  <!-- info about you -->
	  <input type="hidden" name="receiver_emai" value="gm_1231902686_biz@paypal.com"/>
	  <input type="hidden" name="receiver_id" value="S8XGHLYDW9T3S"/>
	  <input type="hidden" name="residence_country" value="US"/>
	  <!-- info about the transaction -->
	  <input type="hidden" name="test_ipn" value="1"/>
	  <input type="hidden" name="transaction_subject " value=""/>
	  <input type="hidden" name="txn_id" value="61E67681CH3238416"/>
	  <input type="hidden" name="txn_type" value="express_checkout"/>  
	  <!-- info about your buyer -->
	  <input type="hidden" name="payer_email" value=" gm_1231902590_per@paypal.com"/>
	  
	  <input type="hidden" name="payer_status" value="verified"/>	  
	  
	  <input type="hidden" name="last_name" value="User"/>
	  <input type="hidden" name="address_city" value="San Jose"/>
	  <input type="hidden" name="address_country" value="United States"/>
	  <input type="hidden" name="address_state" value="CA"/>	
	  	
	  
	  
	  	
	  	
	  <!-- info about the payment -->  
	  <input type="hidden" name="custom" value=""/>
	  <input type="hidden" name="handling_amount" value="0.00"/>
	  <input type="hidden" name="item_name" value=""/>
	  <input type="hidden" name="item_number" value=""/>	  
	  <input type="hidden" name="mc_currency" value="USD"/>
	  
	  
	  
	  <input type="hidden" name="payment_fee" value="0.88"/>
	  <input type="hidden" name="payment_gross" value="19.95"/>	
	  	
	  <input type="hidden" name="payment_type" value="instant"/>
	  
	  <input type="hidden" name="quantity" value="1"/>
	  <input type="hidden" name="shipping" value="0.00"/>
	  
	  <!-- orther info -->	
	  <input type="hidden" name="notify_version" value="2.6"/>
	  
	  <input type="hidden" name="verify_sign" value="AtkOfCXbDm2hu0ZELryHFjY-Vb7PAUvS6nMXgysbElEn9v-1XcmSoGtf"/>
	  <!-- code for other variables to be tested ... -->
	
	  <input type="submit"/>
	</form>
</body>
</html>