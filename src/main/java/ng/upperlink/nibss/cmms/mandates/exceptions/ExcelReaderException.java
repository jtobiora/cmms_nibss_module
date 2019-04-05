package ng.upperlink.nibss.cmms.mandates.exceptions;

/*
* Provides Custom exceptions when reading excel files
* */
public class ExcelReaderException extends RuntimeException{
    public ExcelReaderException(String message) {
        super(message);
    }

    public ExcelReaderException() {
    }

    public ExcelReaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExcelReaderException(Throwable cause) {
        super(cause);
    }
}
