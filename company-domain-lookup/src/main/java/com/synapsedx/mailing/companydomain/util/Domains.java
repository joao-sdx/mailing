package com.synapsedx.mailing.companydomain.util;

import java.net.URI;
import java.util.Locale;

public final class Domains {

  private Domains() {}

  public static String extractHost(String input) {
    if (input == null || input.isBlank()) {
      return "";
    }
    var trimmed = input.trim();
    if (!trimmed.contains("://")) {
      trimmed = "https://" + trimmed;
    }
    try {
      var uri = URI.create(trimmed);
      var host = uri.getHost();
      if (host == null || host.isBlank() || !host.contains(".")) {
        return "";
      }
      if (host.startsWith("www.")) {
        host = host.substring(4);
      }
      return host.toLowerCase(Locale.ROOT);
    } catch (IllegalArgumentException e) {
      return "";
    }
  }
}
