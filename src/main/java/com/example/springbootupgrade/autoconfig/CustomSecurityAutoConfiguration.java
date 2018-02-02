package com.example.springbootupgrade.autoconfig;

import javax.servlet.Filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import com.example.springbootupgrade.config.MyAppSecurityProperties;
import com.example.springbootupgrade.security.HeaderUserFilter;

@Configuration
@EnableWebSecurity
public class CustomSecurityAutoConfiguration extends WebSecurityConfigurerAdapter {
	private static final Logger LOGGER = LoggerFactory.getLogger(CustomSecurityAutoConfiguration.class);

	@Autowired
	private SecurityProperties securityProperties;

	@Autowired(required = false)
	private UserDetailsService userDetailsService;

	@Value("${management.context-path:}")
	private String actuatorRoot;

	@Value("${server.context-path:/}")
	private String serverContextPath;

	@Autowired(required = false)
	private CsrfTokenRepository csrfTokenRepository;

	@Autowired
	private MyAppSecurityProperties myAppSecurityProperties;

	@Bean(name = "myAuthenticationFilter")
	@ConditionalOnBean(UserDetailsService.class)
	public Filter authenticationFilter(MyAppSecurityProperties myAppSecurityProperties) throws Exception {
		if (myAppSecurityProperties.isEnabled()) {
			return new HeaderUserFilter(authenticationManagerBean());
		}
		else {
			throw new IllegalStateException("Should be enabled!");
		}
	}

	@Bean
	@ConditionalOnMissingBean(CsrfTokenRepository.class)
	@ConditionalOnProperty(prefix = "security", name = "enable-csrf", matchIfMissing = true)
	public CsrfTokenRepository myCsrfTokenRepository(MyAppSecurityProperties myAppSecurityProperties) {
		if (myAppSecurityProperties.isEnabled()) {
			return CookieCsrfTokenRepository.withHttpOnlyFalse();
		}
		else {
			throw new IllegalStateException("Should be enabled!");
		}
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		// @formatter:off
		http
			.cors().and()
			.logout()
				.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
				.permitAll();

		String contentSecurityPolicy = this.securityProperties.getHeaders().getContentSecurityPolicy();

		if (StringUtils.isNotBlank(contentSecurityPolicy)) {
			@SuppressWarnings("rawtypes")
			HeadersConfigurer.ContentSecurityPolicyConfig contentSecurityPolicyConfig = http.headers().contentSecurityPolicy(contentSecurityPolicy);

			if (this.securityProperties.getHeaders().getContentSecurityPolicyMode() == SecurityProperties.Headers.ContentSecurityPolicyMode.REPORT_ONLY) {
				contentSecurityPolicyConfig.reportOnly();
			}
		}

		if (this.csrfTokenRepository != null) {
			LOGGER.info("Wiring in csrf protection because a {} was found", CsrfTokenRepository.class.getName());
			http.csrf().csrfTokenRepository(this.csrfTokenRepository);
		}
		else {
			LOGGER.info("NOT wiring in csrf protection because a {} was NOT found", CsrfTokenRepository.class.getName());
			http.csrf().disable();
		}

		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);

		if (this.userDetailsService != null) {
			Filter authenticationFilter = authenticationFilter(this.myAppSecurityProperties);

			LOGGER.info("{} was found - enabling authentication", UserDetailsService.class.getName());

			http
				.addFilterBefore(authenticationFilter, AbstractPreAuthenticatedProcessingFilter.class)
				.exceptionHandling()
					.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
					.accessDeniedHandler(new AccessDeniedHandlerImpl()).and()
				.authorizeRequests()
					.anyRequest().fullyAuthenticated();
		}
		else {
			LOGGER.info("NO {} was found - NOT enabling authentication", UserDetailsService.class.getName());
			http.authorizeRequests().anyRequest().permitAll();
		}
		//@formatter:on
	}

	@Override
	public void configure(WebSecurity webSecurity) throws Exception {
		if (StringUtils.isNotBlank(this.actuatorRoot)) {
			// Let the actuators bypass security if they are all grouped under
			webSecurity.ignoring().antMatchers(String.format("%s/**", this.actuatorRoot));
		}
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		if (this.userDetailsService != null) {
			LOGGER.info("{} was found - enabling authentication provider", UserDetailsService.class.getName());
			PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
			provider.setPreAuthenticatedUserDetailsService(new UserDetailsByNameServiceWrapper<>(this.userDetailsService));

			auth.authenticationProvider(provider);
		}
		else {
			LOGGER.info("NO {} was found - NOT enabling pre-authentication authentication provider", UserDetailsService.class.getName());
			super.configure(auth);
		}
	}

	@Configuration
	@EnableGlobalMethodSecurity(prePostEnabled = true)
	@ConditionalOnClass({ PermissionEvaluator.class })
	@ConditionalOnBean({ PermissionEvaluator.class })
	static class MethodSecurityAutoConfiguration {

	}
}