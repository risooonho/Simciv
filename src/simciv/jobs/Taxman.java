package simciv.jobs;

import java.util.List;

import org.newdawn.slick.Graphics;
import org.newdawn.slick.SpriteSheet;

import simciv.Game;
import simciv.builds.Build;
import simciv.builds.House;
import simciv.builds.Workplace;
import simciv.content.Content;
import simciv.units.Citizen;

public class Taxman extends Job
{
	private static SpriteSheet unitSprites;
	
	public Taxman(Citizen citizen, Workplace workplace)
	{
		super(citizen, workplace);
		if(unitSprites == null)
			unitSprites = new SpriteSheet(Content.images.unitTaxman, Game.tilesSize, Game.tilesSize);
	}

	@Override
	public void tick()
	{
		float totalMoneyCollected = 0;
		List<Build> builds = me.getMap().getBuildsAround(me.getX(), me.getY());
		for(Build b : builds)
		{
			if(b.isHouse())
			{
				float moneyCollected = ((House)b).payTaxes();
				totalMoneyCollected += moneyCollected;
			}
		}
		me.getMap().playerCity.gainMoney((int) totalMoneyCollected);
	}

	@Override
	public void onBegin()
	{
		me.enterBuilding(workplaceRef);
	}

	@Override
	public byte getID()
	{
		return Job.TAXMAN;
	}

	@Override
	public void renderUnit(Graphics gfx)
	{
		me.defaultRender(gfx, unitSprites);
	}

	@Override
	public int getIncome()
	{
		return 16;
	}

	@Override
	public int getTickTimeOverride()
	{
		return 200; // Taxmen are a bit slower
	}

}
