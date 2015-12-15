// $codepro.audit.disable restrictedSuperclasses
package com.patchr.exceptions;

import java.io.IOException;

/**
 * Exception class used when the device is not connected to any network.
 * <p/>
 * Lack of a connection could be because of
 * <p/>
 * - airplane mode,
 * - all networks are disabled
 * - all networks are unavailable
 * - data quota has been exceeded
 */
@SuppressWarnings("ucd")
public class NoNetworkException extends IOException {

	private static final long serialVersionUID = 12L;

	public NoNetworkException() {
	}

	public NoNetworkException(String message) {
		super(message);
	}
}
