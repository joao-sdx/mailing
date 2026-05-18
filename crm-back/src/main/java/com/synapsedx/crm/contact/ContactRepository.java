package com.synapsedx.crm.contact;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactRepository extends JpaRepository<Contact, Long> {

  @Query(
      value =
          """
          SELECT c FROM Contact c LEFT JOIN FETCH c.company
          WHERE :q = ''
             OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(c.lastName)  LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(c.email)     LIKE LOWER(CONCAT('%', :q, '%'))
          """,
      countQuery =
          """
          SELECT COUNT(c) FROM Contact c
          WHERE :q = ''
             OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(c.lastName)  LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(c.email)     LIKE LOWER(CONCAT('%', :q, '%'))
          """)
  Page<Contact> search(@Param("q") String q, Pageable pageable);
}
