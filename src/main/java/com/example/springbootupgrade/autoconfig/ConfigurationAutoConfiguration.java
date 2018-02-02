package com.example.springbootupgrade.autoconfig;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.example.springbootupgrade.config.MyAppSecurityProperties;

@Configuration
@EnableConfigurationProperties({ MyAppSecurityProperties.class})
public class ConfigurationAutoConfiguration {
}