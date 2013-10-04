/**
 * Fractal Component Plugin Spec v1.1
 * by Jason LaDere (Jario)
 */

package jario.hardware;

public interface Hardware
{
	public void connect(int port, Hardware hw);
	public void reset();
}
