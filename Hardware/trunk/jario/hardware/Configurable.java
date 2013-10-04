/**
 * Fractal Component Plugin Spec v1.1
 * by Jason LaDere (Jario)
 */

package jario.hardware;

public interface Configurable
{
	public Object readConfig(String key);
	public void writeConfig(String key, Object value);
}
