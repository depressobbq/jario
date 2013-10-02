/**
 * Copyright 2013 Jason LaDere
 *
 * Component Plugin v1.1
 */

package jario.hardware;

public interface Configurable
{
	public Object readConfig(String key);
	public void writeConfig(String key, Object value);
}
