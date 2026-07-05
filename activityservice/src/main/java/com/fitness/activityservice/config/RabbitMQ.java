package com.fitness.activityservice.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQ {
    @Bean
    public Queue activityQueue() {
        // durable: if RabbitMQ restart the queue message won't be lost
        return new Queue("app.activity.queue", true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        // Convert Java object to JSON
        return new Jackson2JsonMessageConverter();
    }
}
