package com.patchr.objects;

import java.io.Serializable;

public class Recipient implements Serializable {

	public String name;
	public String email;
	public String id;

	public Recipient(String id, String name, String email) {
		this.id = id;
		this.name = name;
		this.email = email;
	}

	@Override public String toString() {
		return name;
	}
}
