/**
 * Fractal Component Plugin Spec v1.1
 * by Jason LaDere (Jario)
 */

package jario.hardware;

public interface Bus16bit
{
	public short read16bit(int address);
	public void write16bit(int address, short data);
}
