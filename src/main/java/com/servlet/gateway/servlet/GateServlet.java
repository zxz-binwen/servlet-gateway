package com.servlet.gateway.servlet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * 服务网关路由规则
 * /{service-name}/{service-uri}
 *
 * @author sqm
 * @date 2020/12/9
 **/
@WebServlet(name = "gateway", urlPatterns = "/gateway/*")
public class GateServlet extends HttpServlet {

    @Autowired
    private DiscoveryClient discoveryClient;

    private ServiceInstance randomChooseServiceInstance(String serviceName) {
        // 获取服务实例列表（服务IP、端口、是否为HTTPS）
        List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceName);
        int size = serviceInstances.size();
        int index = new Random().nextInt(size);
        return serviceInstances.get(index);
    }

    // Proxy -> POST GET
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // 创建客户端转发
        RestTemplate restTemplate = new RestTemplate();

        String pathInfo = req.getPathInfo();

        String[] parts = StringUtils.split(pathInfo.substring(1), "/");
        // 获取服务名称
        String serviceName = parts[0];
        // 获取服务URI
        String serviceURI = "/" + parts[1];
        // 随机选择一台实例
        ServiceInstance serviceInstance = randomChooseServiceInstance(serviceName);
        // 构建目标服务URL ->scheme://ip:port/serviceURI
        String targetURL = buildTargetURI(serviceInstance, serviceURI, req);

        // 构造Request实体
        RequestEntity<byte[]> requestEntity = null;
        try {
            requestEntity = createRequestEntity(req, targetURL);
            ResponseEntity<byte[]> responseEntity = restTemplate.exchange(requestEntity, byte[].class);

            writeHeaders(responseEntity, resp);
            writeBody(responseEntity, resp);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private String buildTargetURI(ServiceInstance serviceInstance, String serviceURI, HttpServletRequest req) {
        StringBuilder builder = new StringBuilder();
        builder.append(serviceInstance.isSecure() ? "https://" : "http://")
                .append(serviceInstance.getHost())
                .append(":")
                .append(serviceInstance.getPort())
                .append(serviceURI);

        String queryString = req.getQueryString();
        if (StringUtils.hasLength(queryString)) {
            builder.append("?").append(queryString);
        }
        return builder.toString();
    }

    private void writeHeaders(ResponseEntity<byte[]> responseEntity, HttpServletResponse resp) {

        HttpHeaders headers = responseEntity.getHeaders();

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String headerName = entry.getKey();
            List<String> headerValues = entry.getValue();
            for (String headerValue : headerValues) {
                resp.addHeader(headerName, headerValue);
            }
        }

    }

    private void writeBody(ResponseEntity<byte[]> responseEntity, HttpServletResponse resp) throws IOException {
        if (responseEntity.hasBody()) {
            byte[] body = responseEntity.getBody();

            ServletOutputStream outputStream = resp.getOutputStream();
            outputStream.write(body);
            outputStream.flush();
        }
    }

    private RequestEntity<byte[]> createRequestEntity(HttpServletRequest req, String targetURL) throws URISyntaxException, IOException {

        String method = req.getMethod();

        HttpMethod httpMethod = HttpMethod.resolve(method);
        byte[] body = createRequestBody(req);
        MultiValueMap<String, String> headers = createRequestHeaders(req);
        return new RequestEntity<>(body, headers, httpMethod, new URI(targetURL));
    }

    private byte[] createRequestBody(HttpServletRequest req) throws IOException {
        ServletInputStream inputStream = req.getInputStream();
        return StreamUtils.copyToByteArray(inputStream);
    }

    /**
     * 创建请求头
     *
     * @param req
     * @return
     */
    private MultiValueMap<String, String> createRequestHeaders(HttpServletRequest req) {

        HttpHeaders headers = new HttpHeaders();
        ArrayList<String> headerNames = Collections.list(req.getHeaderNames());
        for (String headerName : headerNames) {
            ArrayList<String> headerValues = Collections.list(req.getHeaders(headerName));
            for (String headerValue : headerValues) {
                headers.add(headerName, headerValue);
            }
        }
        return headers;
    }

    public static void main(String[] args) throws URISyntaxException {
        RestTemplate restTemplate = new RestTemplate();

        String url = "https://www.baidu.com/";

        RequestEntity<Object> requestEntity = new RequestEntity<>(HttpMethod.GET, new URI(url));
        ResponseEntity<byte[]> responseEntity =
                restTemplate.exchange(url, HttpMethod.GET, requestEntity, byte[].class);

        System.out.println(responseEntity);
    }
}
