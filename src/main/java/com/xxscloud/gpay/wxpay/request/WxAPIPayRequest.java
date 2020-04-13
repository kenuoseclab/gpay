package com.xxscloud.gpay.wxpay.request;

import com.xxscloud.gpay.PayException;
import com.xxscloud.gpay.wxpay.IWxRequest;
import com.xxscloud.gpay.wxpay.IWxResponse;
import com.xxscloud.gpay.wxpay.response.WxCreateOrderResponse;
import lombok.Data;

import java.lang.reflect.Field;
import java.util.TreeMap;

@Data
public class WxAPIPayRequest implements IWxRequest {
    private String appId;
    private String timeStamp;
    private String nonceStr;
    private String packages;
    private String signType;
    private String paySign;


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
    public void setSign(String value) {
        this.paySign = value;
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
        return WxCreateOrderResponse.class;
    }
}
