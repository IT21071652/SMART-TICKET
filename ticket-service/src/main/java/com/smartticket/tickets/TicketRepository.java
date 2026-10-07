package com.smartticket.tickets;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    @Query("""
        select t from Ticket t
        where (:status is null or t.status = :status)
          and (:category is null or t.category = :category)
          and (:priority is null or t.priority = :priority)
          and (:query is null or lower(t.title) like lower(concat('%', :query, '%'))
               or lower(t.description) like lower(concat('%', :query, '%'))
               or lower(t.customerEmail) like lower(concat('%', :query, '%')))
        """)
    Page<Ticket> search(@Param("status") String status, @Param("category") String category,
                        @Param("priority") String priority, @Param("query") String query, Pageable pageable);

    @Query("select t.status, count(t) from Ticket t group by t.status")
    List<Object[]> countByStatus();

    @Query("select t.category, count(t) from Ticket t where t.category is not null group by t.category")
    List<Object[]> countByCategory();

    @Query("select t.priority, count(t) from Ticket t where t.priority is not null group by t.priority")
    List<Object[]> countByPriority();
}
