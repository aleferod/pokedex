package com.ballastlane.pokedex.infrastructure.web;

import com.ballastlane.pokedex.adapter.in.web.correlation.CorrelationIdFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** Registers the servlet filters and fixes their order in the chain. */
@Configuration(proxyBeanMethods = false)
public class WebFilterConfiguration {

  /** Runs first, on every dispatch, so even error responses and async work carry the id. */
  @Bean
  FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
    FilterRegistrationBean<CorrelationIdFilter> registration =
        new FilterRegistrationBean<>(new CorrelationIdFilter());
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    registration.setDispatcherTypes(
        DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
    return registration;
  }
}
