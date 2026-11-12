package com.example.springboot.repository;

import java.util.List;

import com.example.springboot.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    // Derived query: Spring Data builds the JPQL from the method name.
    List<Trade> findBySymbolOrderByIdAsc(String symbol);

    // Explicit JPQL, for when the method name would get unreadable.
    @Query("select coalesce(sum(t.shares), 0) from Trade t where t.userId = :userId and t.type = :type")
    long totalSharesByUserAndType(@Param("userId") Integer userId, @Param("type") String type);
}
