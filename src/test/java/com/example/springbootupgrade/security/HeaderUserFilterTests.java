package com.example.springbootupgrade.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.ServletException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.example.springbootupgrade.autoconfig.ConfigurationAutoConfiguration;
import com.example.springbootupgrade.autoconfig.CustomSecurityAutoConfiguration;
import com.example.springbootupgrade.config.MyAppSecurityProperties;
import com.example.springbootupgrade.test.web.servlet.FilterTestHelper;

@RunWith(SpringRunner.class)
@ActiveProfiles("header-user-filter-tests")
@WebAppConfiguration
@SpringBootTest(classes = { HeaderUserFilterTests.Config.class }, properties = {
	"mycompany.enabled=true", "debug=true"
})
public class HeaderUserFilterTests {
	private static final Logger LOGGER = LoggerFactory.getLogger(HeaderUserFilterTests.class);

	@Autowired
	@Qualifier("springSecurityFilterChain")
	private Filter springSecurityFilterChain;

	@Autowired
	private ApplicationContext appContext;

	@Autowired
	private MyAppSecurityProperties myAppSecurityProperties;

	@Test
	public void testAuthenticationFilterCorrect() {
		// @formatter:off
		assertThat(this.appContext.getBean("acidAuthenticationFilter", Filter.class))
			.as("acidAuthenticationFilter should be of type %s", HeaderUserFilter.class)
			.isExactlyInstanceOf(HeaderUserFilter.class);
		// @formatter:on
	}

	@Test
	public void testMissingHeader() throws ServletException, IOException {
		MockHttpServletResponse response = FilterTestHelper.runGetRequestThroughFilter(this.springSecurityFilterChain);
		LOGGER.debug("Response = {}", ToStringBuilder.reflectionToString(response, ToStringStyle.MULTI_LINE_STYLE));

		// @formatter:off
		assertThat(response.getStatus())
			.as("Return should be 401 because no header was sent")
			.isEqualTo(HttpStatus.UNAUTHORIZED.value());
		// @formatter:on
	}

	@Test
	public void testHeaderThere() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/requestURI");
		request.addHeader(HeaderUserFilter.DEFAULT_HEADER_NAME, "user");

		MockHttpServletResponse response = FilterTestHelper.runRequestThroughFilter(request, this.springSecurityFilterChain);

		LOGGER.debug("Response = {}", ToStringBuilder.reflectionToString(response, ToStringStyle.MULTI_LINE_STYLE));

		// @formatter:off
		assertThat(response.getStatus())
			.as("Return should be a 200")
			.isEqualTo(HttpStatus.OK.value());
		// @formatter:on
	}

	@Configuration
	@Profile("header-user-filter-tests")
	@ImportAutoConfiguration({ ConfigurationAutoConfiguration.class, CustomSecurityAutoConfiguration.class, SecurityAutoConfiguration.class})
	public static class Config {
		@Bean
		public UserDetailsService userDetailsService() {
			UserDetailsService userDetailsService = Mockito.mock(UserDetailsService.class);

			BDDMockito
				.given(userDetailsService.loadUserByUsername(BDDMockito.anyString()))
				.willReturn(User.withUsername("user").password("n/a").authorities(new GrantedAuthority[0]).build());

			return userDetailsService;
		}
	}
}