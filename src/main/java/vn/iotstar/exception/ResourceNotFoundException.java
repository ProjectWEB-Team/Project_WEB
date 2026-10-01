package vn.iotstar.exception;

/** Safe domain-level absence signal for presentation-boundary translation. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource) {
        super(resource + " was not found");
    }
}
