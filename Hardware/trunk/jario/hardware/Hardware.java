/**
 * Copyright 2013 Jason LaDere
 *
 * Component Plugin v1.1
 */

package jario.hardware;

public interface Hardware
{
	public void connect(int port, Hardware hw);
	public void reset();
}
