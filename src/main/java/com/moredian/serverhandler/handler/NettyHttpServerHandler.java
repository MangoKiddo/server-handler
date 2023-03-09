package com.moredian.serverhandler.handler;

import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.MemoryAttribute;
import io.netty.util.CharsetUtil;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author wk
 * @date 2022/6/6 09:40
 */
public class NettyHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {


    private final Logger logger = LoggerFactory.getLogger(NettyHttpServerHandler.class);

    public NettyHttpServerHandler(){

    }
    /*
     * 处理请求
     */

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpRequest fullHttpRequest) throws Exception{

        FullHttpResponse response = null;

        HttpMethod method = fullHttpRequest.method();

        String url = fullHttpRequest.uri();
        String decode = URLDecoder.decode(url, "utf-8");
        logger.info("请求URL:" + decode);

        if("/favicon.ico".equals(url)){
            return;
        }
        if(HttpMethod.GET == method){
            Map<String, Object> getParamsFromChannel = getGetParamsFromChannel(fullHttpRequest);
            logger.info("GET请求参数:{} ", getGetParamsFromChannel(fullHttpRequest));
            String msg = "<html><head><title>DEMO</title></head><body>你请求url为：" + decode+"，参数为："+getParamsFromChannel+"</body></html>";
            // 创建http响应
             response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8));
            // 设置头信息
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        }else if(HttpMethod.POST == method){
            Map<String, Object> postParamsFromChannel = getPostParamsFromChannel(fullHttpRequest);

            QueryStringDecoder decoder = new QueryStringDecoder(url);
            Map<String, List<String>> paramList = decoder.parameters();
            for (Map.Entry<String, List<String>> entry : paramList.entrySet()) {
                postParamsFromChannel.put(entry.getKey(), entry.getValue());
            }
            logger.info("POST请求参数:{}", postParamsFromChannel);
            String msg = "<html>head><title>DEMO</title></head><body>你请求url为：" + url+"，参数为："+postParamsFromChannel+"</body></html>";
            response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8));
            // 设置头信息
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        }else{
            response = responseOK(HttpResponseStatus.INTERNAL_SERVER_ERROR, null);
        }
        // 发送响应
        channelHandlerContext.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /*
     * 获取GET方式传递的参数
     */
    private Map<String, Object> getGetParamsFromChannel(FullHttpRequest fullHttpRequest) {

        Map<String, Object> params = new HashMap<String, Object>();

        if (fullHttpRequest.method() == HttpMethod.GET) {
            // 处理get请求
            QueryStringDecoder decoder = new QueryStringDecoder(fullHttpRequest.uri());
            Map<String, List<String>> paramList = decoder.parameters();
            for (Map.Entry<String, List<String>> entry : paramList.entrySet()) {
                params.put(entry.getKey(), entry.getValue().get(0));
            }
            return params;
        } else {
            return null;
        }

    }

    private String getEasyContent(FullHttpRequest fullHttpRequest) throws UnsupportedEncodingException {
        ByteBuf content = fullHttpRequest.content();
        byte[] reqContent = new byte[content.readableBytes()];
        content.readBytes(reqContent);
        String strContent = new String(reqContent, "UTF-8");
        return strContent;
    }

    /*
     * 获取POST方式传递的参数
     */
    private Map<String, Object> getPostParamsFromChannel(FullHttpRequest fullHttpRequest) {

        Map<String, Object> params = new HashMap<String, Object>();

        if (fullHttpRequest.method() == HttpMethod.POST) {
            // 处理POST请求
            String strContentType = fullHttpRequest.headers().get("Content-Type").trim();
            if (strContentType.contains("x-www-form-urlencoded")) {
                params  = getFormParams(fullHttpRequest);
            } else if (strContentType.contains("application/json")) {
                try {
                    params = getJSONParams(fullHttpRequest);
                } catch (UnsupportedEncodingException e) {
                    return null;
                }
            } else {
                return null;
            }
            return params;
        } else {
            return null;
        }
    }

    /*
     * 解析from表单数据（Content-Type = x-www-form-urlencoded）
     */
    private Map<String, Object> getFormParams(FullHttpRequest fullHttpRequest) {
        Map<String, Object> params = new HashMap<String, Object>();
        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), fullHttpRequest);
        List<InterfaceHttpData> postData = decoder.getBodyHttpDatas();
        for (InterfaceHttpData data : postData) {
            if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                MemoryAttribute attribute = (MemoryAttribute) data;
                params.put(attribute.getName(), attribute.getValue());
            }
        }
        return params;
    }

    /*
     * 解析json数据（Content-Type = application/json）
     */
    private Map<String, Object> getJSONParams(FullHttpRequest fullHttpRequest) throws UnsupportedEncodingException {
        Map<String, Object> params = new HashMap<String, Object>();

        ByteBuf content = fullHttpRequest.content();
        byte[] reqContent = new byte[content.readableBytes()];
        content.readBytes(reqContent);
        String strContent = new String(reqContent, "UTF-8");

        JSONObject jsonParams = JSONObject.parseObject(strContent);
        for (Object key : jsonParams.keySet()) {
            params.put(key.toString(), jsonParams.get(key));
        }

        return params;
    }

    private FullHttpResponse responseOK(HttpResponseStatus status, ByteBuf content) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);
        if (content != null) {
            response.headers().set("Content-Type", "text/plain;charset=UTF-8");
            response.headers().set("Content_Length", response.content().readableBytes());
        }
        return response;
    }

    public static String encodeBase64(String data, String key) throws Exception{
        Mac mac = getMac(key);
        byte[] bs = mac.doFinal(data.getBytes("UTF-8"));
        return new String(Base64.encodeBase64(bs), "ISO-8859-1");
    }

    public static Mac getMac(String key) throws Exception{
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA1"));
        return mac;
    }


    public static Map<String, String> handleParam(String str) {
        Map<String, String> param = new HashMap<>(8);
        try{

            String [] url = str.split("\\?");

            String paramStr  = url[1];

            String [] queryParam = paramStr.split("&");

            for(String entry : queryParam){
                String [] temp = entry.split("=", 2);
                param.put(temp[0], URLDecoder.decode(temp[1],"UTF-8"));
//                System.out.println(URLDecoder.decode(temp[1],"UTF-8"));
            }
        }catch (Exception e){

        }
        return param;
    }
}
