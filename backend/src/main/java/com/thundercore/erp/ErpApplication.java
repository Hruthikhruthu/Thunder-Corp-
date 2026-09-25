package com.thundercore.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
/**
 * ThunderCore ERP backend bootstrap.
 *
 * <p>Spring Boot starts component scanning from this package, which wires the
 * domain modules for authentication, inventory, HR, finance, sales, reports,
 * notifications, dashboard analytics, and WebSocket messaging.</p>
 */
public class ErpApplication {
    /**
     * Starts the embedded Spring Boot runtime.
     *
     * @param args command-line arguments forwarded by the JVM or container
     */
    public static void main(String[] args) {
        SpringApplication.run(ErpApplication.class, args);
    }
}
