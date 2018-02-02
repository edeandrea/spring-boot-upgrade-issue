package com.example.springbootupgrade.test.web.servlet;

import java.io.IOException;
import java.util.function.BiConsumer;

import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.PassThroughFilterChain;
import org.springframework.util.Assert;
import org.springframework.web.filter.RequestContextFilter;

/**
 * Utility class which can be used to run requests after a certain filter has passed through Spring's
 * {@link RequestContextFilter} - essentially simulating a request to perform some action after some {@link Filter} has
 * been applied.
 *
 * @author Eric Deandrea June 2017
 */
public final class FilterTestHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(FilterTestHelper.class);

	private FilterTestHelper() {
		super();
	}

	/**
	 * Runs a GET request through a filter, returning a response
	 *
	 * @param filter
	 *          The filter
	 * @return The response
	 */
	public static MockHttpServletResponse runGetRequestThroughFilter(Filter filter) throws ServletException, IOException {
		return runRequestThroughFilter(new MockHttpServletRequest("GET", "/requestURI"), filter);
	}

	/**
	 * Runs some task via a GET request through a filter, returning a response
	 *
	 * @param filter
	 *          The filter
	 * @param task
	 *          The task
	 * @return The response
	 */
	public static MockHttpServletResponse runGetRequestTaskThroughFilter(Filter filter, BiConsumer<HttpServletRequest, HttpServletResponse> task) throws ServletException, IOException {
		return runTaskThroughFilter(new MockHttpServletRequest("GET", "/requestURI"), filter, task);
	}

	/**
	 * Runs some request through a filter
	 *
	 * @param request
	 *          The request
	 * @param filter
	 *          The filter
	 * @return The response
	 */
	public static MockHttpServletResponse runRequestThroughFilter(HttpServletRequest request, Filter filter) throws ServletException, IOException {
		return runTaskThroughFilter(request, filter, (req, res) -> LOGGER.debug("This is the task that is being performed on the other side of the filter"));
	}

	/**
	 * Runs some task through a filter
	 *
	 * @param request
	 *          The request
	 * @param filter
	 *          The filter
	 * @param task
	 *          The task to run
	 * @return The response
	 */
	public static MockHttpServletResponse runTaskThroughFilter(HttpServletRequest request, Filter filter, BiConsumer<HttpServletRequest, HttpServletResponse> task) throws ServletException, IOException {
		Assert.notNull(request, "Request can't be null");
		Assert.notNull(filter, "Filter can't be null");
		MockHttpServletResponse response = new MockHttpServletResponse();
		RequestContextFilter springFilter = new RequestContextFilter();
		springFilter.init(new MockFilterConfig());

		LOGGER.debug("Running request {} through filter {}", ToStringBuilder.reflectionToString(request, ToStringStyle.MULTI_LINE_STYLE), filter.getClass().getName());

		new PassThroughFilterChain(springFilter, new PassThroughFilterChain(filter, new MockFilterChain() {
			@Override
			public void doFilter(ServletRequest request, ServletResponse response) {
				if (task != null) {
					task.accept((HttpServletRequest) request, (HttpServletResponse) response);
				}
			}
		})).doFilter(request, response);

		return response;
	}
}