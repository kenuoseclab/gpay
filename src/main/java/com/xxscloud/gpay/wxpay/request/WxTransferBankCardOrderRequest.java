package com.xxscloud.gpay.wxpay.request;

import com.xxscloud.gpay.PayException;
import com.xxscloud.gpay.wxpay.IWxRequest;
import com.xxscloud.gpay.wxpay.IWxResponse;
import com.xxscloud.gpay.wxpay.response.WxTransferBankCardOrderReqsponse;
import lombok.Data;

import java.lang.reflect.Field;
import java.util.TreeMap;

@Data
public class WxTransferBankCardOrderRequest implements IWxRequest {
    private String mchid;
    private String partner_trade_no;
    private String nonce_str;
    private String sign;
    private String enc_bank_no;
    private String enc_true_name;
    private String bank_code;
    private String amount;
    private String desc;


    @Override
    public String toString() {
        final StringBuilder content = new StringBuilder();
        toMap().forEach((k, v) -> {
            if (k.contains("sign") || v == null) {
                return;
            }
            content.append(k).append("=").append(v).append("&");
        });
        if (content.length() > 0) {
            content.delete(content.length() - 1, content.length());
        }
        return content.toString();
    }

    @Override
    public TreeMap<String, Object> toMap() {
        try {
            final TreeMap<String, Object> treeMap = new TreeMap<>();
            for (Field field : this.getClass().getDeclaredFields()) {
                treeMap.put(field.getName(), field.get(this));
            }

            return treeMap;
        } catch (IllegalAccessException e) {
            throw new PayException("数据异常");
        }
    }

    @Override
    public Class<? extends IWxResponse> getResponseClass() {
        return WxTransferBankCardOrderReqsponse.class;
    }
}
