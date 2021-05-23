# order-handler

Adds new orders via producer, then the orders are added to the orderbook via OrderHandler and then added to the trade queue.

The TradeOrderConsumer will take the order from the trade queue and remove it and execute the trade if necessary with the priority of price and arrival datetime of order.

## Prerequisites
- Java 11
- lombok

### Structure on OrderBook
- OrderBooks contains both sell and buy orders, sorted by acsending and descending respectively by price using ConcurrentSkipListMap
- Orders within the same price level are sorted by ArrivalDateTime using ConcurrentSkipListSet.
- Finally, an OrderBookProvider will provide the OrderBook for a specific symbol/ticker using ConcurrentHashMap.

### GetPrice
Loops through the sorted collections and get the best average price for that quantity.

Time Complexity: O(n + m) where n is the price level and m are the sum of orders which corresponds to -> O(N)
