package com.example.ordersystem.common.service;

import com.example.ordersystem.common.dtos.StockRabbitDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class StockRabbitmqService {
    // private final RabbitTemplate template;

    // public StockRabbitmqService(RabbitTemplate template) {
    //     this.template = template;
    // }

    // //    mq에 rdb동기화관련 메시지 발행
    // public void puslish(StockRabbitDto dto) {
    //     template.convertAndSend(RabbitmqConfig.STOCK_DECREASE_QUEUE, dto);
    // }



}
