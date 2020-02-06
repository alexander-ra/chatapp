package edu.uni.ruse.utilities;


/**
 * Enumeration, containing messages that are send to the client. Each message holds two values, the message in English
 * and in Bulgarian.
 * 
 * @author Alexander Andreev
 */
public enum BilingualMessages {
	SEND("Send", "Изпрати"), 
	DISCONNECT("Disconnect", "Напусни"),
	DISCONNECTING("Disconnecting..", "Напускане.."),
	USERS_ONLINE("Users Online:","Свързани потребители:"),
	CHARS_REMAINING("Characters remanining: ", "Оставащи символи: "),
	LOST_CONNECTION("Lost connection with server.","Връзката със сървъра се прекъсна."),
	TRY_TO_RECONNECT("Trying to reconnect","Опит за възстановяване на връзката със сървъра"),
	SUCCESSFUL_RECONNECT("Reconnect successful.","Връзката със сървъра е възстановена успешно."),
	UNSUCCESSFUL_RECONNECT("Reconnect failed.", "Опита да се възстанови връзката беше неуспешен.");

	private final String enMessage;
	private final String bgMessage;

	/**
	 * Constructor, holding two values of the message for each language
	 * 
	 * @param enMessage
	 *            the message in English language.
	 * @param bgMessage
	 *            the message in Bulgarian language.
	 */
	BilingualMessages(String enMessage, String bgMessage) {
		this.enMessage = enMessage;
		this.bgMessage = bgMessage;
	}

	/**
	 * Returns the English message.
	 * 
	 * @return message in english
	 */
	public String inEnglish() {
		return enMessage;
	}

	/**
	 * Return the Bulgarian message.
	 * 
	 * @return message in bulgarian
	 */
	public String inBulgarian() {
		return bgMessage;
	}

	/**
	 * Returns the message in the requested language if available. If not a message telling that the message can't be
	 * retrieved in the requested language is return.
	 * 
	 * @param language to get the message in.
	 * @return the message in the requested language if available.
	 */
	public String inSpecificLang(InterfaceLang language) {
		if (language == InterfaceLang.EN) {
			return enMessage;
		} else if (language == InterfaceLang.BG) {
			return bgMessage;
		} else
			return "The requested message dont have the translation of that language";
	}
}