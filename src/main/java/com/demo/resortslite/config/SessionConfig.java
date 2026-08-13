package com.demo.resortslite.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Spring Session Configuration for Amazon ElastiCache Redis
 * 
 * FIXED cr-java-0065: This configuration enables distributed session management using
 * Amazon ElastiCache for Redis, replacing in-memory HTTP session storage.
 * 
 * Benefits:
 * - Stateless application instances: Sessions are stored in Redis, not in application memory
 * - Horizontal scaling: All EC2 instances share the same session store
 * - High availability: ElastiCache provides automatic failover and replication
 * - Session persistence: Sessions survive application restarts and auto-scaling events
 * - Load balancer compatibility: No need for sticky sessions with AWS ALB/NLB
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class SessionConfig {
    // Spring Session automatically configures Redis-backed session repository
    // Session data is serialized and stored in Redis with automatic expiration
}
