package com.aircandi.objects;

import com.aircandi.objects.ServiceBase.UpdateScope;

import java.io.Serializable;

public class ServiceObject implements Cloneable, Serializable {

	private static final long serialVersionUID = 5341986472204947192L;
	public UpdateScope updateScope;

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}
