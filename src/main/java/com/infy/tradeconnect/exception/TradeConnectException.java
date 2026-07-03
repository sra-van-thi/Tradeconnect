package com.infy.tradeconnect.exception;

public class TradeConnectException extends RuntimeException {

    public TradeConnectException() {
        super();
    }

    public TradeConnectException(String message) {
        super(message);
    }

    public TradeConnectException(String message, Throwable cause) {
        super(message, cause);
    }
}
