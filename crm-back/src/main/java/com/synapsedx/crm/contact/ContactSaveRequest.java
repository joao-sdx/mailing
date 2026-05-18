package com.synapsedx.crm.contact;

public record ContactSaveRequest(
    String firstName, String lastName, String email, String phone, String jobTitle, Long companyId) {}
