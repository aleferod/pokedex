package com.ballastlane.pokedex.support;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Empties every cache before each test, so a test never sees what an earlier one loaded and the
 * order of tests never matters. Integration tests share one application context per configuration.
 */
public class ClearCachesTestExecutionListener extends AbstractTestExecutionListener {

  @Override
  public void beforeTestMethod(TestContext testContext) {
    testContext
        .getApplicationContext()
        .getBeansOfType(CacheManager.class)
        .values()
        .forEach(
            manager ->
                manager.getCacheNames().stream()
                    .map(manager::getCache)
                    .filter(cache -> cache != null)
                    .forEach(Cache::clear));
  }
}
