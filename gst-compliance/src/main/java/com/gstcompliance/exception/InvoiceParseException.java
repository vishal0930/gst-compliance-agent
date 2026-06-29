package com.gstcompliance.exception;

public class InvoiceParseException extends RuntimeException {
    private final String fileKey;
    private final String invoiceNumber;

    public InvoiceParseException(String message) {
        super(message);
        this.fileKey = null;
        this.invoiceNumber = null;
    }

    public InvoiceParseException(String message, String fileKey) {
        super(message);
        this.fileKey = fileKey;
        this.invoiceNumber = null;
    }

    public InvoiceParseException(String message, String fileKey, String invoiceNumber) {
        super(message);
        this.fileKey = fileKey;
        this.invoiceNumber = invoiceNumber;
    }

    public String getFileKey() {
        return fileKey;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }
}