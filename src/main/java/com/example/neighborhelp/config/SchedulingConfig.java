package com.example.neighborhelp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;


/**
 * Configuration class for scheduled tasks in the application.
 *
 * Enables scheduling and configures the thread pool for executing
 * scheduled tasks such as automatic cleanup of expired verification codes.
 *
 * @author NeighborHelp Team
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

    /**
     * Creates and configures a TaskScheduler with a dedicated thread pool.
     *
     * Configuration details:
     * - Pool size: 5 threads (sufficient for maintenance tasks)
     * - Thread name prefix: "scheduled-task-" (facilitates debugging in logs)
     * - Wait for tasks to complete on shutdown: true (ensures graceful shutdown)
     * - Await termination seconds: 60 (maximum wait time for tasks to complete during shutdown)
     * - Automatic initialization: true (scheduler is ready to use immediately)
     *
     * @return Configured TaskScheduler instance ready for use by Spring's scheduling system
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("scheduled-task-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.initialize();
        return scheduler;
    }
}