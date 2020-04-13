package com.xxscloud.gpay.wxpay;


import com.xxscloud.gpay.PayIOException;
import com.xxscloud.gpay.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;


import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Iterator;
import java.util.Objects;


@Slf4j
public class WeChatPayClient {
    private String appId;
    private String merchantId;
    private String key;
    private byte[] certificate;
    private final HttpClient httpClient;


    public WeChatPayClient(String appId, String merchantId, String key) {
        this.appId = appId;
        this.merchantId = merchantId;
        this.key = key;
        httpClient = HttpClientBuilder.create().build();
    }

    public WeChatPayClient(String appId, String merchantId, String key, byte[] certificate) {
        this.appId = appId;
        this.merchantId = merchantId;
        this.key = key;
        this.certificate = certificate;

        try {
            final KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(certificate), merchantId.toCharArray());
            final SSLContext sslcontext = SSLContexts.custom().loadKeyMaterial(keyStore, merchantId.toCharArray()).build();
            httpClient = HttpClientBuilder.create().setSSLContext(sslcontext).build();
        } catch (Exception e) {
            throw new PayIOException("证书错误, " + e.getMessage());
        }
    }

    public String getAppId() {
        return appId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getKey() {
        return key;
    }

    public byte[] getCertificate() {
        return certificate;
    }

    public <T extends IWxResponse> T execute(String url, IWxRequest request) {
        request.setSign(DigestUtils.md5Hex(request.toString() + "&key=" + this.key).toUpperCase());
        final HttpPost httpPost = new HttpPost(url);
        final String xml = toXml(request);
        final StringEntity requestEntity = new StringEntity(xml, "UTF-8");
        httpPost.setEntity(requestEntity);
        if (log.isDebugEnabled()) {
            log.debug("[GPAY] request: {}", xml);
        }
        try {
            final HttpResponse response = httpClient.execute(httpPost);
            final String responseString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            if (log.isDebugEnabled()) {
                log.debug("[GPAY] response: {}", responseString);
            }
            final IWxResponse result = toBean(responseString, request.getResponseClass());
            if (Objects.equals(result.getReturn_code(), "FAIL")) {
                throw new PayIOException(result.getReturn_msg());
            }
            if (Objects.equals(result.getResult_code(), "FAIL")) {
                throw new PayIOException(result.getErr_code_des());
            }
            return (T) result;
        } catch (IOException e) {
            throw new PayIOException(e);
        }
    }

    public Object sdkExecute(IWxRequest request) {
        request.setSign(DigestUtils.md5Hex(request.toString() + "&key=" + this.key).toUpperCase());
        return request;
    }

    private String toXml(IWxRequest request) {
        final StringBuilder xml = new StringBuilder("<xml>\n");
        request.toMap().forEach((k, v) -> {
            if (v == null) {
                return;
            }
            xml.append("<").append(k).append(">").append(v).append("</").append(k).append(">").append("\n");
        });
        xml.append("</xml>");
        return xml.toString();
    }

    private <T> IWxResponse toBean(String xml, Class<T> clazz) {
        try {
            final Document document = DocumentHelper.parseText(xml);
            final Element root = document.getRootElement();
            final T obj = clazz.newInstance();
            final Iterator it = root.elementIterator();
            while (it.hasNext()) {
                Element element = (Element) it.next();
                for (Field field : clazz.getDeclaredFields()) {
                    if (Objects.equals(field.getName(), element.getName())) {
                        Method m = null;
                        for (Method method : clazz.getMethods()) {
                            if (method.getName().toLowerCase().contains(("set" + field.getName()).toLowerCase())) {
                                m = method;
                                break;
                            }
                        }
                        if (m != null) {
                            m.invoke(obj, element.getText());
                        }
                    }
                }
            }
            return (IWxResponse) obj;
        } catch (IllegalAccessException | InstantiationException | DocumentException | InvocationTargetException e) {
            throw new PayIOException("解析响应内容失败");
        }
    }


    JsonObject toBean(String xml) {
        try {
            final Document document = DocumentHelper.parseText(xml);
            final Element root = document.getRootElement();
            final JsonObject obj = new JsonObject();
            final Iterator it = root.elementIterator();
            while (it.hasNext()) {
                Element element = (Element) it.next();
                obj.put(element.getName(), element.getText());
            }
            return obj;
        } catch (DocumentException e) {
            throw new PayIOException("解析响应内容失败");
        }
    }


}
