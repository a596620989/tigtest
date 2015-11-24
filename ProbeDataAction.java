package com.probe.open.action;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import com.probe.open.util.HttpUtil;
import com.probe.open.util.TreebearCommand;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

@Controller("probeDataAction")
@Scope("prototype")
public class ProbeDataAction   {
	
	
//	@Autowired
//	private OpenThirdInfoDAO openThirdInfoDAO;

	protected static Logger            logger           = LoggerFactory.getLogger("WebLogger"); 

	static int i = 0;

	@RequestMapping("/api/postdataslow.htm")
	public String postdataSlow(HttpServletRequest request, HttpServletResponse response) {
		logger.info("我不会写代码, 我睡了5秒" + i);

		i++;
		try {
			Thread.sleep(1000 * i);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@RequestMapping("/api/debug.htm")
	public String debug(HttpServletRequest request, HttpServletResponse response) {
		logger.info("enter debug");
		System.out.println("enter aa");
		return "debug";
	}
	
	
	/**
	 * 不依赖任何本地的数据, 只是简单的根据用户填写的参数发送post请求
	 * @param request
	 * @return
	 */
	@RequestMapping("/api/do_debug.htm")
	public String doDebug(HttpServletRequest request, ModelMap model, HttpServletResponse httpServletResponse) {
		
		String method = request.getParameter("method");
		String nonce = request.getParameter("nonce");	
//		String signature = request.getParameter("signature");
		String timestamp = request.getParameter("timestamp");
		String probeData = request.getParameter("probeData");	
		String probeSn = request.getParameter("probeSn");	
//		String probeMac = request.getParameter("probeMac");	
		String url = request.getParameter("url");
		String token = request.getParameter("token");
		
		
		CloseableHttpClient httpclient = HttpClients.createDefault();
//		http://hc.apache.org/httpcomponents-client-ga/tutorial/html/fundamentals.html#d5e49
		HttpPost httpPost = new HttpPost(url);
		
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("method", method));
		nvps.add(new BasicNameValuePair("timestamp",timestamp));
		nvps.add(new BasicNameValuePair("nonce", nonce));
		nvps.add(new BasicNameValuePair("signature", sign(timestamp,nonce,token)));
		nvps.add(new BasicNameValuePair("probeData", probeData));
		nvps.add(new BasicNameValuePair("probeSn", probeSn));
		
		CloseableHttpResponse response = null;

		//超过一定时间自动关闭
		int timeout = 5;
		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(timeout * 1000)
				.setConnectionRequestTimeout(timeout * 1000)
				.setSocketTimeout(timeout * 1000)
				.build();
		httpPost.setConfig(config);
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));
			response = httpclient.execute(httpPost);
		} catch (Exception e){
			logger.info(e.getMessage());
			e.printStackTrace();
		}

		try {
			logger.info("第三方服务状态码:" + response.getStatusLine().toString());
			HttpEntity entity = response.getEntity();
			
			String responseJson = EntityUtils.toString(entity);
			
			logger.info("第三方返回值:" + responseJson);
			HttpUtil.outPut(responseJson, httpServletResponse);			
			// do something useful with the response body
			// and ensure it is fully consumed
			EntityUtils.consume(entity);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				response.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	
	/**
	 * 自己模拟的一个第三方实现, 接受postdata请求.
	 * [{ProbeMessage},...,{ProbeMessage}]
	 * @param request
	 * @param response
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	@RequestMapping("/api/postdata.htm")
	public String postdata(HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException {

		String token = "gjA4fd0";
		
		request.setCharacterEncoding("UTF-8");
		String version = request.getParameter("version");
		String method = request.getParameter("method");
		String timestamp = request.getParameter("timestamp");
		String nonce = request.getParameter("nonce");	
		String signature = request.getParameter("signature");	
		String probeMac = request.getParameter("probeMac");	
		String probeSn = request.getParameter("probeSn");	
	
		logger.info(version);
		logger.info(method);
		logger.info(timestamp);
		logger.info(nonce);
		logger.info(signature);
		logger.info(probeMac);
		logger.info(probeSn);
		
		if(signature== null || !signature.equals(sign(timestamp, nonce, token))){
			logger.error("invalidate sign");
			return null;
		}
		
//		String chinese = request.getParameter("chinese");
//		logger.info(chinese);
		if (TreebearCommand.PROBEDATA_POST.equals(method)) {
			String probeData = request.getParameter("probeData");
			logger.info("probeData" + probeData);
			net.sf.json.JSONArray probeArray = (JSONArray) JSONSerializer.toJSON(probeData);
			logger.info("probeArray size:" + probeArray.size());
			for (Object jo : probeArray) {
				JSONObject json = (JSONObject) jo;
				logger.info("devMac: {}", json.get("devMac"));
				logger.info("rssi: {}", json.get("rssi"));
				logger.info("timeStamp: {}", json.get("timeStamp")); 
				logger.info("leave: {}", json.get("leave"));
			}
			try {
				// 响应树熊以便树熊重试
				response.getWriter().println("{\"errcode\":0,\"errmsg\":\"ok\"}");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return null;
	}
	
	@RequestMapping("/sys/monitor.htm")
	public String monitor(HttpServletRequest request, HttpServletResponse response) {
		logger.info("server started");
		return null;
	}
	

	private static String sign(String timestamp, String nonce, String token){
		String[] tempArr = new String[]{token, timestamp, nonce};
		Arrays.sort(tempArr);
		StringBuffer tempStrSB = new StringBuffer();
	    for(String str : tempArr){
	    	tempStrSB.append(str);
	    }
	    String tempStr = tempStrSB.toString();
	    tempStr = DigestUtils.sha1Hex(tempStr);
	    
		return tempStr;
	}
}
