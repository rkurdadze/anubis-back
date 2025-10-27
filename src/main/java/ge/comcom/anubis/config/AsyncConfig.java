package ge.comcom.anubis.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.context.annotation.Bean;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    // is not used for the moment
    @Bean(name = "fullTextSearchExecutor")
    public Executor fullTextSearchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);           // минимальное кол-во потоков
        executor.setMaxPoolSize(5);            // максимум (при нагрузке)
        executor.setQueueCapacity(100);        // очередь задач
        executor.setThreadNamePrefix("FTS-");  // префикс имени потока
        executor.setRejectedExecutionHandler(
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );
        executor.initialize();
        return executor;
    }
}