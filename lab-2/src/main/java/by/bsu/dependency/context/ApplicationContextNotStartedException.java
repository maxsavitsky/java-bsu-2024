package by.bsu.dependency.context;

public class ApplicationContextNotStartedException extends RuntimeException {

    public ApplicationContextNotStartedException() {
    }

    public ApplicationContextNotStartedException(String message) {
        super(message);
    }

    public ApplicationContextNotStartedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApplicationContextNotStartedException(Throwable cause) {
        super(cause);
    }

    public ApplicationContextNotStartedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
