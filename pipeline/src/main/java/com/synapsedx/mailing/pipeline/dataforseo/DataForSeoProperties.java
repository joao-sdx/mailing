package com.synapsedx.mailing.pipeline.dataforseo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** DataForSEO API credentials, bound from {@code dataforseo.api.*} properties. */
@Component
@ConfigurationProperties("dataforseo")
public class DataForSeoProperties {

  private Api api = new Api();

  public Api getApi() {
    return api;
  }

  public void setApi(Api api) {
    this.api = api;
  }

  public static class Api {

    private String user;
    private String key;

    public String getUser() {
      return user;
    }

    public void setUser(String user) {
      this.user = user;
    }

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }
  }
}
