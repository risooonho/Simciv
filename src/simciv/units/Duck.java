package simciv.units;

import org.newdawn.slick.Graphics;
import org.newdawn.slick.SpriteSheet;

import simciv.Game;
import simciv.Map;
import simciv.content.Content;
import simciv.movements.RandomMovement;

public class Duck extends Unit
{
	private static SpriteSheet sprites;
	
	public Duck(Map m)
	{
		super(m);
		setMovement(new RandomMovement());
		
		if(sprites == null)
			sprites = new SpriteSheet(Content.images.unitDuck, Game.tilesSize, Game.tilesSize);
	}

	@Override
	protected void renderUnit(Graphics gfx)
	{
		defaultRender(gfx, sprites);
	}

	@Override
	protected void tick()
	{
	}

	@Override
	public void onDestruction()
	{
	}

}


