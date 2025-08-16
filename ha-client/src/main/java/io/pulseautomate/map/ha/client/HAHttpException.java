package io.pulseautomate.map.ha.client;

public class HAHttpException extends Exception {
  private final int status;
  private final String url;
  private final String body;

  public HAHttpException(String message, int status, String url, String body, Throwable cause) {
    super(message, cause);
    this.status = status;
    this.url = url;
    this.body = body;
  }

  public HAHttpException(String message, int status, String url, String body) {
    this(message, status, url, body, null);
  }

  public int status() {
    return status;
  }

  public String url() {
    return url;
  }

  public String body() {
    return body;
  }
}
