package cpl.g3c.interactive;

public class ProverException extends RuntimeException {

    public ProverException () {
        super ();
    }

    public ProverException (String msg) {
        super (msg);
    }

    public ProverException (Throwable cause) {
        super (cause);
    }

    public ProverException (String msg, Throwable cause) {
        super (msg, cause);
    }
}
