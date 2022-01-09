package com.gewuwo.logging.errors;

/** Indicates that a request timed out. */
public class AppendTimeoutException extends ProducerException {

  public AppendTimeoutException() {
    super();
  }

  public AppendTimeoutException(String message, Throwable cause) {
    super(message, cause);
  }

  public AppendTimeoutException(String message) {
    super(message);
  }

  public AppendTimeoutException(Throwable cause) {
    super(cause);
  }
}
