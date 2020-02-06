package edu.uni.ruse.utilities;


/**
 * Enumeration, containg messages used as a code to indicate that a trasferred message has additional meaning to it.
 *
 * @author Alexander Andreev
 */
public enum CodeMessages {
    CONREQUEST("CONNECTION_REQUEST:"),
    ADDUSER("ADD_USER:"),
    REMOVEUSER("REMOVE_USER:"),
    CONN_ACCEPTED("CONNECTION_ACCEPTED"),
    CONN_DECLINED("CONNECTION_DECLINED"),
    REFRESH_USERLIST("REFRESH_USERLIST"),
    CHANGE_LANG("CHANGE_LANGUAGE:"),
    WHISPER("/w");

    private final String message;

    /**
     * Constructor, holding two values of the message for each language
     *
     * @param message
     *            the message language.
     */
    CodeMessages(String message) {
        this.message = message;
    }

    /**
     * Returns the code message.
     *
     * @return message
     */
    public String getMessage() {
        return message;
    }

}