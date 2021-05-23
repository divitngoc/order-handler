package com.iggroup.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;

import com.iggroup.model.Order;
import com.iggroup.model.OrderBook;

import dnl.utils.text.table.TextTable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PrinterUtils {

    private static final String[] HEADERS = new String[] { "Order Count", "Ask Quantity", "Ask Price", "Level", "Bid price", "Bid Quantity", "Order Count" };

    private PrinterUtils() {}

    public static void printStatus(final OrderBook orderBook) {
        printStatus(orderBook, Math.max(orderBook.getBuyOrders().values().size(),
                                        orderBook.getSellOrders().values().size()));
    }

    /**
     * 
     * Print orderbook for a specific symbol in a nice format up to the maxPriceLevel, see below for example:
     * 
     * <pre>
     * printStatus(order, 3);
     *   ================================== SYMBOL [IGG] ==================================
     *   ___________________________________________________________________________________
     *   | Order Count| Ask Quantity| Ask Price| Level| Bid price| Bid Quantity| Order Count|
     *   |==================================================================================|
     *   | 1          | 7           | 25       | 1    | 13       | 3           | 2          |
     *   | 3          | 23          | 28       | 2    | 12       | 5           | 1          |
     *   | 1          | 5           | 30       | 3    | -        | -           | -          |
     * </pre>
     * 
     */
    public static void printStatus(final OrderBook orderBook, final int maxPriceLevel) {
        Collection<NavigableSet<Order>> buyOrders = orderBook.getBuyOrders().values();
        Collection<NavigableSet<Order>> sellOrders = orderBook.getSellOrders().values();
        Iterator<NavigableSet<Order>> buyIt = buyOrders.iterator();
        Iterator<NavigableSet<Order>> sellIt = sellOrders.iterator();
        final String[][] data = new String[maxPriceLevel][];
        int level = 0;
        while (level++ < maxPriceLevel){
            List<String> row = new ArrayList<>();
            row.addAll(getDataFromOrders(sellIt));
            row.add(String.valueOf(level));

            List<String> buyData = getDataFromOrders(buyIt);
            Collections.reverse(buyData);
            row.addAll(buyData);

            data[level - 1] = row.toArray(new String[0]);
        }
        log.info("\n================================== SYMBOL [{}] ==================================", orderBook.getSymbol());
        new TextTable(HEADERS, data).printTable();
    }

    private static List<String> getDataFromOrders(Iterator<NavigableSet<Order>> sellIt) {
        List<String> data = new ArrayList<>();
        if (sellIt.hasNext()) {
            Collection<Order> sellOrders = sellIt.next();
            mapToPAndQ(sellOrders).ifPresentOrElse(pair -> {
                data.add(String.valueOf(sellOrders.size()));
                data.add(pair.getRight().toString());
                data.add(pair.getLeft().toPlainString());
            }, () -> addEmptyDataOrder(data));
        } else {
            addEmptyDataOrder(data);
        }
        return data;
    }

    private static void addEmptyDataOrder(List<String> row) {
        row.add("-");
        row.add("-");
        row.add("-");
    }

    /**
     * 
     * Returns a pair with price and combined quantity of an order for that price
     * 
     * @param sellOrders
     * @return
     */
    private static Optional<Pair<BigDecimal, Integer>> mapToPAndQ(Collection<Order> sellOrders) {
        return sellOrders.stream()
                         .map(order -> Pair.of(order.getPrice().get(), order.getQuantity().get()))
                         .reduce((a, b) -> Pair.of(a.getLeft(), a.getRight() + b.getRight()));
    }

}
