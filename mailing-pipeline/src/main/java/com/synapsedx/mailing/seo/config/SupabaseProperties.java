package com.synapsedx.mailing.seo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("supabase")
public record SupabaseProperties(String url, String anonKey, String serviceRoleKey) {}
