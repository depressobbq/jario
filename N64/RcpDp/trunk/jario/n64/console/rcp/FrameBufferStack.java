/**
 * Copyright 2005, 2013 Jason LaDere
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Originally based on Project64 code.
 *
 */

package jario.n64.console.rcp;

public class FrameBufferStack
{
	// public static class FrameBuffer {
	// // used by Gdp
	// public int startAddress;
	// // used by GLN64jPlugin
	// public boolean changed;
	// // used by TextureCache
	// public CachedTexture texture;
	// public int size;
	// public int width;
	// public int height;
	//
	// public float scaleX;
	// public float scaleY;
	// public int endAddress;
	// public FrameBuffer higher;
	// public FrameBuffer lower;
	// }

	// public FrameBuffer top;
	// public FrameBuffer bottom;
	// private int numBuffers;

	// public void init() {
	// top = null;
	// bottom = null;
	// numBuffers = 0;
	// }

	// public void removeBottom() {
	// OpenGl.cache.remove(bottom.texture);
	//
	// if (bottom == top)
	// top = null;
	//
	// bottom = bottom.higher;
	//
	// if (bottom != null)
	// bottom.lower = null;
	//
	// numBuffers--;
	// }

	// public void remove(FrameBuffer buffer) {
	// if ((buffer == bottom) && (buffer == top)) {
	// top = null;
	// bottom = null;
	// } else if (buffer == bottom) {
	// bottom = buffer.higher;
	//
	// if (bottom != null)
	// bottom.lower = null;
	// } else if (buffer == top) {
	// top = buffer.lower;
	//
	// if (top != null)
	// top.higher = null;
	// } else {
	// buffer.higher.lower = buffer.lower;
	// buffer.lower.higher = buffer.higher;
	// }
	//
	// numBuffers--;
	// }

	// public void addTop(FrameBuffer newtop) {
	// newtop.lower = top;
	// newtop.higher = null;
	//
	// if (top != null)
	// top.higher = newtop;
	//
	// if (bottom == null)
	// bottom = newtop;
	//
	// top = newtop;
	//
	// numBuffers++;
	// }

	// public void moveToTop(FrameBuffer newtop) {
	// if (newtop == top)
	// return;
	//
	// if (newtop == bottom) {
	// bottom = newtop.higher;
	// bottom.lower = null;
	// } else {
	// newtop.higher.lower = newtop.lower;
	// newtop.lower.higher = newtop.higher;
	// }
	//
	// newtop.higher = null;
	// newtop.lower = top;
	// top.higher = newtop;
	// top = newtop;
	// }

	// Extra ///////////////////////////////////////////////////////////////////

	// // called by OpenGlGdp.viUpdateScreen, TextureCache.loadTile/Block
	// public FrameBuffer findBuffer(int address) {
	// FrameBuffer buffer = top;
	//
	// while (buffer != null) {
	// if ((buffer.startAddress <= address) &&
	// (buffer.endAddress >= address))
	// return buffer;
	// buffer = buffer.lower;
	// }
	//
	// return null;
	// }

	// // called by TextureCache,prune
	// public FrameBuffer removeBuffer(int address) {
	// FrameBuffer buffer = bottom;
	//
	// while (buffer != null) {
	// if (buffer.startAddress == address) {
	// // buffer.texture = null;
	// remove(buffer);
	// return buffer;
	// }
	// buffer = buffer.higher;
	// }
	// return null;
	// }
}
