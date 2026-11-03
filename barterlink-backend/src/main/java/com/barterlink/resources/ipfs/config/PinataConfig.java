package com.barterlink.resources.ipfs.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class PinataConfig {

    @Value("${pinata.jwt}")
    private String jwt;

}