package com.fitness.aiservice.config.rabbitmq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {
    private final RabbitMQProperties properties;


    @Bean
    public Queue activityQueue() {
        // durable: if RabbitMQ restart the queue message won't be lost
        return new Queue(properties.getQueues().getActivity(), true);
    }

    @Bean
    public DirectExchange activityExchange() {
        return new DirectExchange(properties.getExchanges().getFitness());
    }

    @Bean
    public Binding activityBinding(Queue activityQueue, DirectExchange activityExchange) {
        return BindingBuilder
                .bind(activityQueue)
                .to(activityExchange)
                .with(properties.getRoutingKeys().getActivity());
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        // Convert Java object to JSON
        return new Jackson2JsonMessageConverter();
    }
}
