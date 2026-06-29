// package com.gstcompliance.config;
//
// import org.springframework.batch.core.configuration.JobRegistry;
// import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
// import org.springframework.batch.core.explore.JobExplorer;
// import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
// import org.springframework.batch.core.launch.JobLauncher;
// import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
// import org.springframework.batch.core.repository.JobRepository;
// import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.core.task.SimpleAsyncTaskExecutor;
// import org.springframework.orm.jpa.JpaTransactionManager;
// import org.springframework.transaction.PlatformTransactionManager;
//
// import javax.sql.DataSource;
//
// @Configuration
// public class BatchConfig {
//
//     @Bean
//     public JobRepository jobRepository(DataSource dataSource,
//                                       PlatformTransactionManager transactionManager) throws Exception {
//         JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
//         factory.setDataSource(dataSource);
//         factory.setTransactionManager(transactionManager);
//         factory.afterPropertiesSet();
//         return factory.getObject();
//     }
//
//     @Bean
//     public JobExplorer jobExplorer(DataSource dataSource) throws Exception {
//         JobExplorerFactoryBean factory = new JobExplorerFactoryBean();
//         factory.setDataSource(dataSource);
//         factory.afterPropertiesSet();
//         return factory.getObject();
//     }
//
//     @Bean
//     public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
//         TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
//         launcher.setJobRepository(jobRepository);
//         launcher.setTaskExecutor(new SimpleAsyncTaskExecutor());
//         launcher.afterPropertiesSet();
//         return launcher;
//     }
//
//     @Bean
//     public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor(JobRegistry jobRegistry) {
//         JobRegistryBeanPostProcessor processor = new JobRegistryBeanPostProcessor();
//         processor.setJobRegistry(jobRegistry);
//         return processor;
//     }
//
//     @Bean
//     public PlatformTransactionManager transactionManager(DataSource dataSource) {
//         return new JpaTransactionManager();
//     }
// }