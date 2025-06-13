package com.dx.liferay.inventory.exception;

/**
 * Custom exception class for handling file processing related errors in the inventory system.
 * This exception is thrown when operations involving file processing fail or encounter errors.
 */
public class FileProcessingException extends Exception {

    /**
     * Constructs a new FileProcessingException with the specified detail message.
     *
     * @param message the detail message explaining the cause of the exception
     */
    public FileProcessingException(String message) { super(message); }

    /**
     * Constructs a new FileProcessingException with the specified detail message and cause.
     *
     * @param message the detail message explaining the cause of the exception
     * @param cause the underlying cause of this exception
     */

    public FileProcessingException(String message, Throwable cause) { super(message, cause); }
}