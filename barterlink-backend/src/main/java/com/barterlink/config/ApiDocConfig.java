package com.barterlink.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Auth Application build by Shubham Gopale.",
                description = "Generic auth app that can be used with any application.",
                contact = @Contact(
                        name = "Shubham Gopale",
                        url = "",
                        email = "shubhamgopale16@gmail.com"
                ),
                version = "1.0",
                summary = "This app is very useful if you don't want to create auth app from scratch."



        )
        ,
        security = {
                @SecurityRequirement(
                        name="bearerAuth"
                )
        }


)

@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer", //Authorization: Bearer htokenaswga,
        bearerFormat = "JWT"

)
public class ApiDocConfig {
}
