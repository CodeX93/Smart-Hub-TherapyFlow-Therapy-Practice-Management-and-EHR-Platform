package com.smart.therapy.flow.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.smart.therapy.flow.common.tenant.TenantContextTaskDecorator;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Default task executor for general async operations.
     * Handles moderate load scenarios.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(taskDecorator());
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Dedicated executor for client creation post-commit processing.
     * Optimized for high-volume client onboarding (1000+ concurrent requests).
     * 
     * Capacity calculation:
     * - Core threads: 50 (always active)
     * - Max threads: 200 (scales up under load)
     * - Queue: 1000 (buffers requests)
     * - Total capacity: 50 + 150 (max-core) + 1000 (queue) = 1200 concurrent tasks
     * 
     * This ensures we can handle 1000 concurrent client creations with headroom.
     */
    @Bean(name = "clientCreatedExecutor")
    public Executor clientCreatedExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(taskDecorator());
        executor.setCorePoolSize(50);  // Increased from default 10
        executor.setMaxPoolSize(200);  // Increased from default 50
        executor.setQueueCapacity(1000); // Increased from default 100
        executor.setThreadNamePrefix("client-created-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120); // Longer shutdown wait for in-flight tasks
        // Use CallerRunsPolicy to prevent task rejection - caller thread executes if queue full
        // This ensures no events are lost, though it may slow down the main transaction thread
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Email executor for sending emails via SES.
     * SES can handle high throughput, so we scale this appropriately.
     */
    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(taskDecorator());
        executor.setCorePoolSize(10);  // Increased from 5
        executor.setMaxPoolSize(50);   // Increased from 20
        executor.setQueueCapacity(200); // Increased from 50
        executor.setThreadNamePrefix("email-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Notification executor for processing notifications.
     */
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(taskDecorator());
        executor.setCorePoolSize(10);  // Increased from 5
        executor.setMaxPoolSize(50);    // Increased from 20
        executor.setQueueCapacity(200); // Increased from 50
        executor.setThreadNamePrefix("notification-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Dedicated executor for user creation post-commit processing.
     * Handles password generation, email sending, and audit logging.
     */
    @Bean(name = "userCreatedExecutor")
    public Executor userCreatedExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(taskDecorator());
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("user-created-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Executor for tenant-scoped work that must not inherit the caller thread's Hibernate session
     * (for example Stripe Connect webhooks handled on platform routes).
     */
    @Bean(name = "tenantIsolationExecutor")
    public Executor tenantIsolationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("tenant-isolation-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Executor for user update post-commit processing (audit logging).
     */
    @Bean(name = "userUpdatedExecutor")
    public Executor userUpdatedExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(taskDecorator());
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("user-updated-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean
    public TaskDecorator taskDecorator() {
        return new TenantContextTaskDecorator();
    }
}

