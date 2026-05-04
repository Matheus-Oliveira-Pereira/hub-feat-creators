package com.hubfeatcreators.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * order=2 ensures @Transactional opens the connection before TenantAspect (order=3) runs, so SET
 * LOCAL and Hibernate filters execute inside the active transaction.
 */
@Configuration
@EnableTransactionManagement(order = 2)
public class TransactionConfig {}
