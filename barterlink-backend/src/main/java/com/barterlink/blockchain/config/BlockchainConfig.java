package com.barterlink.blockchain.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
public class BlockchainConfig {

    @Bean
    public Web3j web3j(BlockchainProperties properties) {
        return Web3j.build(
                new HttpService(properties.getRpcUrl())
        );
    }
}