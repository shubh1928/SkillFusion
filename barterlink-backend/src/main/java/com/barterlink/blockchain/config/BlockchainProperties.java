package com.barterlink.blockchain.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "barterlink.blockchain")
public class BlockchainProperties {

    private String network;
    private long chainId;
    private String rpcUrl;
    private String tokenAddress;
    private String escrowAddress;
    private String protocolSignerAddress;

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public long getChainId() {
        return chainId;
    }

    public void setChainId(long chainId) {
        this.chainId = chainId;
    }

    public String getRpcUrl() {
        return rpcUrl;
    }

    public void setRpcUrl(String rpcUrl) {
        this.rpcUrl = rpcUrl;
    }

    public String getTokenAddress() {
        return tokenAddress;
    }

    public void setTokenAddress(String tokenAddress) {
        this.tokenAddress = tokenAddress;
    }

    public String getEscrowAddress() {
        return escrowAddress;
    }

    public void setEscrowAddress(String escrowAddress) {
        this.escrowAddress = escrowAddress;
    }

    public String getProtocolSignerAddress() {
        return protocolSignerAddress;
    }

    public void setProtocolSignerAddress(String protocolSignerAddress) {
        this.protocolSignerAddress = protocolSignerAddress;
    }
}