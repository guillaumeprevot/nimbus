package fr.techgp.nimbus.server;

import javax.servlet.http.HttpServletResponse;

public class HaltException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private final int code;
	private final String body;

	public HaltException() {
		this(HttpServletResponse.SC_OK, null);
	}

	public HaltException(int code) {
		this(code, null);
	}

	public HaltException(int code, String body) {
		super(null, null, false, false);
		this.code = code;
		this.body = body;
	}

	public int getCode() {
		return this.code;
	}

	public String getBody() {
		return this.body;
	}

}
