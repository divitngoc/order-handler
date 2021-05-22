package com.iggroup.handler;

import com.iggroup.exception.OrderModificationException;
import com.iggroup.model.Order;
import com.iggroup.model.Side;

public interface OrderHandler {

    void addOrder(Order order);

    void modifyOrder(Order order, Order modifiedOrder) throws OrderModificationException;

    void removeOrder(Order order);

    double getPrice(String symbol, int quantity, Side side);

}
