package com.example.springbootupgrade.security;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedCredentialsNotFoundException;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

public class HeaderUserFilter extends RequestHeaderAuthenticationFilter {
	private static final Logger LOGGER = LoggerFactory.getLogger(HeaderUserFilter.class);
	public static final String DEFAULT_HEADER_NAME = "MY_SM_USER";

	@Autowired
	public HeaderUserFilter(AuthenticationManager authenticationManager) {
		this(authenticationManager, DEFAULT_HEADER_NAME);
	}

	@Autowired
	public HeaderUserFilter(AuthenticationManager authenticationManager, String principalHeaderName) {
		super();

		setAuthenticationManager(authenticationManager);
		setCheckForPrincipalChanges(true);
		setExceptionIfHeaderMissing(false);
		setPrincipalRequestHeader(principalHeaderName);
	}

	@Override
	protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
		try {
			return super.getPreAuthenticatedPrincipal(request);
		}
		catch (PreAuthenticatedCredentialsNotFoundException ex) {
			LOGGER.debug("Cleared security context - no principal header found in the request");
			SecurityContextHolder.clearContext();

			throw ex;
		}
	}
}