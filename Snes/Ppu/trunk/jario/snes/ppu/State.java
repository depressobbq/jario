package jario.snes.ppu;

public class State
{
	public int x;
    public int y;

    public int item_count;
    public int tile_count;

    public boolean active;
    public int[][] item = new int[2][];
    public TileItem[][] tile = new TileItem[2][];

    public State()
    {
        for (int i = 0; i < item.length; i++)
        {
            item[i] = new int[32];
        }

        for (int i = 0; i < tile.length; i++)
        {
            tile[i] = new TileItem[34];
            for (int j = 0; j < tile[i].length; j++)
            {
                tile[i][j] = new TileItem();
            }
        }
    }
}
