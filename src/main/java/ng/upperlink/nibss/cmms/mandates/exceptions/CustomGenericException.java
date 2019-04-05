package ng.upperlink.nibss.cmms.mandates.exceptions;

/*
*  Provides custom generic exception to be used
* */
public class CustomGenericException extends RuntimeException {
    public CustomGenericException(String message) {
        super(message);
    }

    public CustomGenericException() {
    }

    public CustomGenericException(String message, Throwable cause) {
        super(message, cause);
    }

    public CustomGenericException(Throwable cause) {
        super(cause);
    }
}