package com.loyaltycards.telegram.exception;

/**
 * 🚫 Исключение для ошибок работы с QR-кодами
 * Используется при ошибках генерации, декодирования и валидации QR-кодов
 */
public class QrCodeException extends Exception {

    public QrCodeException(String message) {
        super(message);
    }

    public QrCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}

