package de.lightbolt.meeting.utils;

import de.lightbolt.meeting.data.config.ReflectionUtils;
import de.lightbolt.meeting.data.config.UnknownPropertyException;

import javax.annotation.Nullable;

public abstract class Resolvable {

	@Nullable
	public Object resolve(String propertyName) throws UnknownPropertyException {
		var result = ReflectionUtils.resolveField(propertyName, this);
		return result.map(pair -> {
			try {
				return pair.first().get(pair.second());
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				return null;
			}
		}).orElse(null);
	}
}
