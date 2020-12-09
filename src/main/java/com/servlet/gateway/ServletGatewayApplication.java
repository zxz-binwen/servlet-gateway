package com.servlet.gateway;

import com.servlet.gateway.loadbalancer.ZookeeperLoadBalancer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

@EnableDiscoveryClient
@ServletComponentScan
@SpringBootApplication
public class ServletGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServletGatewayApplication.class, args);
    }

    /**
     * 负载均衡
     *
     * @param discoveryClient
     * @return
     */
    @Bean
    public ZookeeperLoadBalancer zookeeperLoadBalancer(DiscoveryClient discoveryClient) {
        return new ZookeeperLoadBalancer(discoveryClient);
    }
}
