package esa.sle.ccsds.utils.cltu;

/**
 * Exception thrown when CLTU encoding/decoding fails
 * 
 * @author ESA SLE Java API Team
 * @version 5.1.6
 */
public class CLTUException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new CLTU exception with the specified detail message
     * 
     * @param message the detail message
     */
    public CLTUException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new CLTU exception with the specified detail message and cause
     * 
     * @param message the detail message
     * @param cause the cause
     */
    public CLTUException(String message, Throwable cause) {
        super(message, cause);
    }
}
