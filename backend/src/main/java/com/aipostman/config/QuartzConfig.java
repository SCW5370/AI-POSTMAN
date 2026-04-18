package com.aipostman.config;

import com.aipostman.scheduler.BuildDigestJob;
import com.aipostman.scheduler.FetchSourcesJob;
import com.aipostman.scheduler.FinalizeDigestJob;
import com.aipostman.scheduler.SendDigestJob;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.quartz.*;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory {
    private final AutowireCapableBeanFactory beanFactory;

    AutowiringSpringBeanJobFactory(AutowireCapableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
        Object job = super.createJobInstance(bundle);
        beanFactory.autowireBean(job);
        return job;
    }
}

@Configuration
public class QuartzConfig {

    @Bean
    public SpringBeanJobFactory springBeanJobFactory(AutowireCapableBeanFactory beanFactory) {
        return new AutowiringSpringBeanJobFactory(beanFactory);
    }

    @Bean
    public JobDetail fetchSourcesJobDetail() {
        return JobBuilder.newJob(FetchSourcesJob.class).withIdentity("fetchSourcesJob").storeDurably().build();
    }

    @Bean
    public Trigger fetchSourcesTrigger(JobDetail fetchSourcesJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(fetchSourcesJobDetail)
                .withIdentity("fetchSourcesTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 */2 * * ?"))
                .build();
    }

    @Bean
    public JobDetail buildDigestJobDetail() {
        return JobBuilder.newJob(BuildDigestJob.class).withIdentity("buildDigestJob").storeDurably().build();
    }

    @Bean
    public Trigger buildDigestTrigger(JobDetail buildDigestJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(buildDigestJobDetail)
                .withIdentity("buildDigestTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 10 * * * ?"))
                .build();
    }

    @Bean
    public JobDetail finalizeDigestJobDetail() {
        return JobBuilder.newJob(FinalizeDigestJob.class).withIdentity("finalizeDigestJob").storeDurably().build();
    }

    @Bean
    public JobDetail sendDigestJobDetail() {
        return JobBuilder.newJob(SendDigestJob.class).withIdentity("sendDigestJob").storeDurably().build();
    }

    @Bean
    public Trigger finalizeDigestTrigger(JobDetail finalizeDigestJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(finalizeDigestJobDetail)
                .withIdentity("finalizeDigestTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 15 * * * ?"))
                .build();
    }

    @Bean
    public Trigger sendDigestTrigger(JobDetail sendDigestJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(sendDigestJobDetail)
                .withIdentity("sendDigestTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 20 * * * ?"))
                .build();
    }
}
