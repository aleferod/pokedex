package com.ballastlane.archfixtures.jul;

import java.util.logging.Logger;

public class UsesJavaUtilLogging {

  private static final Logger LOG = Logger.getLogger(UsesJavaUtilLogging.class.getName());

  public void report(String message) {
    LOG.info(message);
  }
}
