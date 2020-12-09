package com.servlet.gateway.loadbalancer;

import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.Server;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.List;

/**
 * 负载均衡
 */
public class ZookeeperLoadBalancer extends BaseLoadBalancer {

    private final DiscoveryClient discoveryClient;

    public ZookeeperLoadBalancer(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        updateServerList();
    }

    private void updateServerList() {
        final List<String> services = discoveryClient.getServices();

        services.forEach(t -> {
            final List<ServiceInstance> instanceList = discoveryClient.getInstances(t);
            instanceList.forEach(s -> {
                Server server = new Server(s.isSecure() ? "https://" : "http://", s.getHost(), s.getPort());
                addServer(server);
            });
        });
    }


}
