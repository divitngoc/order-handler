package com.iggroup;

import com.iggroup.handler.DefaultOrderHandler;
import com.iggroup.model.OrderBook;
import com.iggroup.model.Side;
import com.iggroup.producer.OrderProducer;
import com.iggroup.provider.OrderBookProvider;
import com.iggroup.service.TradeService;
import com.iggroup.util.PrinterUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderHandlerApplication {

    /*
     * 
     * ================================== SYMBOL [IGG] ==================================
     * | Order Count| Ask Quantity| Ask Price| Level| Bid price| Bid Quantity| Order Count|
     * |==================================================================================|
     * | 1          | 7           | 25       | 1    | 13       | 3           | 2          |
     * | 3          | 23          | 28       | 2    | 12       | 5           | 1          |
     * | 1          | 5           | 30       | 3    | -        | -           | -          |
     * 
     */
    public static void main(String[] args) throws InterruptedException {
        DefaultOrderHandler orderHandler = new DefaultOrderHandler(OrderBookProvider.getInstance(),
                                                                   TradeService.getInstance());
        startProducer(orderHandler);
        // Thread.sleep(1000);
        startProducer(orderHandler);
        OrderBook iggOrderBook = OrderBookProvider.getInstance()
                                                  .getOrderBookBySymbol("IGG");

        while (true) {
            log.info("Average SELL Price: {} and Average BUY Price: {}", orderHandler.getPrice("IGG", 3, Side.SELL),
                     orderHandler.getPrice("IGG", 3, Side.BUY));
            Thread.sleep(1000);
        }
    }

    /**
     * 
     * Ideally there would be another application that would produce Orders to a MQ,
     * and that our application would read it from there. This just is for
     * simplicity/test sample and for anyone see orderHandler add in action
     * 
     */
    private static void startProducer(DefaultOrderHandler orderHandler) {
        new Thread(() -> {
            OrderProducer producer = new OrderProducer();
            while (true) {
                orderHandler.addOrder(producer.produce());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
