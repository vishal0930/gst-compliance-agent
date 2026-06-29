package com.gstcompliance.exception;

public class DuplicateInvoiceException extends RuntimeException {

    public DuplicateInvoiceException(String message) {
        super(message);
    }

    public DuplicateInvoiceException(String message, Throwable cause) {
        super(message, cause);
    }
}