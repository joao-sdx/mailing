package com.synapsedx.mailing.seonewsparse.model;

import java.nio.file.Path;

public record Article(Path path, String title, String body) {}
