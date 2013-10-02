/**
 * Copyright 2013 Jason LaDere
 *
 * Originally based on Justin Bozalina's XNA port
 * of byuu's bsnes v073.
 */

package jario.snes.cpu;

public class Channel
{
	// $420b
	public boolean dma_enabled;

	// $420c
	public boolean hdma_enabled;

	// $43x0
	public boolean direction;
	public boolean indirect;
	public boolean unused;
	public boolean reverse_transfer;
	public boolean fixed_transfer;
	public int transfer_mode;

	// $43x1
	public int dest_addr;

	// $43x2-$43x3
	public int source_addr;

	// $43x4
	public int source_bank;

	// $43x5-$43x6
	public int indirect_addr;
	public int transfer_size() { return indirect_addr; }
	public int transfer_size_decremented() { return --indirect_addr; }
	public void transfer_size(int s) { indirect_addr = s; }

	// $43x7
	public int indirect_bank;

	// $43x8-$43x9
	public int hdma_addr;

	// $43xa
	public int line_counter;

	// $43xb/$43xf
	public int unknown;

	// internal state
	public boolean hdma_completed;
	public boolean hdma_do_transfer;
}
