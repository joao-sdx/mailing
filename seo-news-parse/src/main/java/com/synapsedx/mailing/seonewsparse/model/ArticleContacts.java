package com.synapsedx.mailing.seonewsparse.model;

import java.nio.file.Path;
import java.util.List;

public record ArticleContacts(Path sourceArticle, List<PersonRow> rows) {}
