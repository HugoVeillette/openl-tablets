package org.openl.binding.impl.cast;

import org.openl.types.IOpenClass;

public class CastsLinkageCast implements IOpenCast {

	private IOpenCast[] casts;

	public CastsLinkageCast(IOpenCast... casts) {
		if (casts == null) {
			throw new IllegalArgumentException();
		}
		this.casts = casts;
	}

	public Object convert(Object from) {
		if (from == null) {
			return null;
		}
		Object ret = from;
		for (IOpenCast cast : casts) {
			ret = cast.convert(ret);
		}

		return ret;
	}

	public int getDistance(IOpenClass from, IOpenClass to) {
		int distance = 0;
		for (IOpenCast cast : casts) {
		    int d = cast.getDistance(from, to);
		    if (distance < d) {
		        distance = d;
		    }
		}
		return distance;
	}

	public boolean isImplicit() {
		return false;
	}
}
