package com.urbanblack.paymentservice.service;

import com.urbanblack.paymentservice.entity.Payment;
import com.urbanblack.paymentservice.util.PayuHashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds PayU form data for UPI-only payment.
 * Money goes to single PayU merchant account.
 */
@Service
@Slf4j
public class PayuFormService {

    @Value("${payu.key:}")
    private String payuKey;

    @Value("${payu.salt:}")
    private String payuSalt;

    @Value("${payu.url:https://secure.payu.in/_payment}")
    private String payuUrl;

    @Value("${payu.success-url:}")
    private String successUrl;

    @Value("${payu.failure-url:}")
    private String failureUrl;

    /**
     * Build PayU form params for UPI-only payment.
     * pg=UPI restricts to UPI options on PayU page.
     */
    public Map<String, String> buildPayuForm(Payment payment, String userEmail, String userName) {
        if (payuKey == null || payuKey.isBlank() || payuSalt == null || payuSalt.isBlank()) {
            throw new IllegalStateException("PayU not configured: PAYU_KEY and PAYU_SALT required");
        }

        String txnId = payment.getTxnId();
        BigDecimal amount = payment.getTotalAmount();
        String productInfo = "Ride-" + payment.getRideId();
        String email = userEmail != null && !userEmail.isBlank() ? userEmail : "user@urbanblack.com";
        String firstname = userName != null && !userName.isBlank() ? userName : "Customer";

        // Hash format: key|txnid|amount|productinfo|firstname|email|||||||||||salt
        String hashStr = payuKey + "|" + txnId + "|" + amount + "|" + productInfo + "|" + firstname + "|" + email +
                "|||||||||||" + payuSalt;
        String hash = PayuHashUtil.sha512Hex(hashStr);

        Map<String, String> form = new LinkedHashMap<>();
        form.put("key", payuKey);
        form.put("txnid", txnId);
        form.put("amount", amount.toString());
        form.put("productinfo", productInfo);
        form.put("firstname", firstname);
        form.put("email", email);
        form.put("hash", hash);
        form.put("surl", successUrl != null && !successUrl.isBlank() ? successUrl : "https://urbanblack.com/payment/success");
        form.put("furl", failureUrl != null && !failureUrl.isBlank() ? failureUrl : "https://urbanblack.com/payment/failure");
        form.put("pg", "UPI");  // UPI only
        form.put("mode", "UPI");

        return form;
    }

    public String getPayuUrl() {
        return payuUrl;
    }
}
