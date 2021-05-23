package com.iggroup;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.iggroup.handler.DefaultOrderHandler;
import com.iggroup.handler.OrderHandler;
import com.iggroup.model.Order;
import com.iggroup.model.Side;
import com.iggroup.producer.OrderProducer;
import com.iggroup.provider.OrderBookProvider;
import com.iggroup.trade.consumer.TradeOrderConsumer;
import com.iggroup.util.PrinterUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderHandlerApplication {

    public static void main(String[] args) throws InterruptedException {
        BlockingQueue<Order> tradeQueue = new LinkedBlockingQueue<>();
        OrderHandler orderHandler = new DefaultOrderHandler(OrderBookProvider.getInstance(), tradeQueue);
        startOrderProducer(orderHandler, "IGG");
        startOrderProducer(orderHandler, "IGG");
        startOrderProducer(orderHandler, "IGG");
        startTradeConsumer(orderHandler, tradeQueue);
        startTradeConsumer(orderHandler, tradeQueue);
        startTradeConsumer(orderHandler, tradeQueue);

        while (true) {
            PrinterUtils.printStatus(OrderBookProvider.getInstance().getOrderBookBySymbol("IGG"));
            log.info("Best average SELL Price: {} and best average BUY Price: {} for quantity of [{}] for {}", 
                     orderHandler.getPrice("IGG", 3, Side.SELL),
                     orderHandler.getPrice("IGG", 3, Side.BUY), 3, "IGG");
            log.info("TradingQueue {}", tradeQueue);
            Thread.sleep(1000);
        }
    }

    /**
     * 
     * Ideally there would be another application that would produce Orders to a MQ,
     * and that our application would read it from there. This just is for
     * simplicity/test sample and for anyone see this application in action
     * 
     */
    private static void startOrderProducer(OrderHandler orderHandler, String symbol) {
        new Thread(() -> {
            Random r = new Random();
            OrderProducer producer = new OrderProducer();
            while (true) {
                orderHandler.addOrder(producer.produce(symbol));
                try {
                    Thread.sleep((r.nextInt(5) + 5) * 100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private static void startTradeConsumer(OrderHandler orderHandler, BlockingQueue<Order> tradeQueue) {
        new Thread(new TradeOrderConsumer(OrderBookProvider.getInstance(), tradeQueue, orderHandler::removeOrder)).start();
    }

}
