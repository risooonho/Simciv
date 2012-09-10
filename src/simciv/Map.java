package simciv;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.state.StateBasedGame;

import simciv.builds.Build;
import simciv.builds.Warehouse;
import simciv.effects.VisualEffect;
import simciv.rendering.SortedRender;
import simciv.units.Unit;

/**
 * The map contains a terrain and a city (units and buildings).
 * This is the main object of the game, as almost all the data is stored in it.
 * @author Marc
 *
 */
public class Map
{
	public MapGrid grid;
	public View view;
	public PlayerCity playerCity;
	public WorldTime time;
	private EntityMap builds;
	private EntityMap units;
	private transient List<VisualEffect> graphicalEffects;
	private transient boolean fastForward;
	
	public Map(int width, int height)
	{
		grid = new MapGrid(width, height);
		playerCity = new PlayerCity();
		time = new WorldTime();
		builds = new EntityMap();
		units = new EntityMap();
		graphicalEffects = new ArrayList<VisualEffect>();
		view = new View(0, 0, 2);
		view.setMapSize(width, height);
	}

	public boolean isFastForward()
	{
		return fastForward;
	}

	public void setFastForward(boolean e)
	{
		fastForward = e;
	}

	/**
	 * Updates the world, and calls the tick() method on buildings
	 * and units at each time interval (tickTime).
	 * @param delta
	 */
	public void update(GameContainer gc, StateBasedGame game, int delta)
	{
		view.update(gc, delta / 1000.f);
		
		if(fastForward)
			delta *= 4;
		
		time.update(delta);
		
		units.updateAll(gc, game, delta);
		builds.updateAll(gc, game, delta);
		
		// Update graphical effects
		ArrayList<VisualEffect> finishedGraphicalEffects = new ArrayList<VisualEffect>();
		for(VisualEffect e : graphicalEffects)
		{
			e.update(delta);
			if(e.isFinished())
				finishedGraphicalEffects.add(e);
		}
		graphicalEffects.removeAll(finishedGraphicalEffects);
	}

	public void addGraphicalEffect(VisualEffect e)
	{
		graphicalEffects.add(e);
	}
	
	/**
	 * Spawns an unit in the world at (x,y).
	 * It will be active at next world update, because it allows to spawn units
	 * while iterating on the units map.
	 * @param u : unit
	 * @param x : x coordinate in map cells
	 * @param y : y coordinate in map cells
	 */
	public void spawnUnit(Unit u, int x, int y)
	{
		u.setPosition(x, y);
		units.add(u);
	}
	
	public void spawnUnit(Unit u)
	{
		units.add(u);
	}
	
	/**
	 * Places a new building at (x,y) if possible.
	 * It may occupy one or more cells on the map.
	 * @param b : new building
	 * @param x : x origin
	 * @param y : y origin
	 * @return : true if the building has been placed
	 */
	public boolean placeBuild(Build b, int x, int y)
	{
		b.setPosition(x, y);
		if(b.canBePlaced(grid, x, y))
		{
			builds.add(b);
			return true;
		}
		return false;
	}
	
	/**
	 * Erases the building assumed to occupy the cell at (x,y).
	 * @param x
	 * @param y
	 * @return : true if the building has been erased.
	 */
	public boolean eraseBuild(int x, int y)
	{
		int ID = grid.getBuildID(x, y);
		if(ID >= 0)
		{
			Build b = getBuild(ID);
			if(b != null)
			{
				b.dispose();
				return true;
			}
		}
		return false;
	}
	
	public Unit getUnit(int ID)
	{
		return (Unit)(units.get(ID));
	}
	
	public Build getBuild(int ID)
	{
		return (Build)(builds.get(ID));
	}
	
	/**
	 * Get the building occupying the cell at (x, y).
	 * Returns null if there is no building.
	 * @param worldRef
	 * @param x
	 * @param y
	 * @return
	 */
	public Build getBuild(int x, int y)
	{
		if(!grid.contains(x, y))
			return null;
		return getBuild(grid.getCellExisting(x, y).getBuildID());
	}
	
	public Unit getUnit(int x, int y)
	{
		if(!grid.contains(x, y))
			return null;
		return getUnit(grid.getCellExisting(x, y).getUnitID());
	}
	
	/**
	 * Draws a part of the world within the specified map range
	 * @param mapRange
	 * @param gc
	 * @param gfx
	 */
	public void render(GameContainer gc, StateBasedGame game, Graphics gfx)
	{
		view.configureGraphicsForWorldRendering(gfx);
		IntRange2D mapRange = view.getMapRange(gc);
		
		SortedRender renderMgr = new SortedRender();
		
		grid.registerElementsForSortedRender(mapRange, renderMgr);

		if(!gc.getInput().isKeyDown(Input.KEY_1))
		{
			// Register buildings
			for(Entity b : builds.asCollection())
			{
				if(mapRange.intersects(
						b.getX(),
						b.getY(),
						b.getX() + b.getWidth() - 1,
						b.getY() + b.getHeight() - 1))
					renderMgr.add(b);
			}
		}

		if(!gc.getInput().isKeyDown(Input.KEY_2))
		{
			// Register units
			for(Entity u : units.asCollection())
			{
				if(u.isVisible() && mapRange.contains(u.getX(), u.getY()))
					renderMgr.add(u);
			}
		}
		
//		long timeBefore = System.currentTimeMillis(); // for debug
		
		// Draw elements in the right order
		grid.renderGround(mapRange, gc, gfx); // Ground at first
		renderMgr.render(gc, game, gfx); // Objects, builds, units		
		// Draw effects on the top
		for(VisualEffect e : graphicalEffects)
			e.render(gfx);
		
//		long time = System.currentTimeMillis() - timeBefore; // for debug
//		if(gc.getInput().isKeyDown(Input.KEY_T))
//			System.out.println(time);
	}

	public Warehouse getFreeWarehouse(int x, int y)
	{
		List<Build> list = getBuildsAround(x, y);
		for(Build b : list)
		{
			if(Warehouse.class.isInstance(b))
			{
				if(b.isAcceptResources())
					return (Warehouse) b;
			}
		}
		return null;
	}
	
	/**
	 * Get a list of buildings around the given position
	 * @param worldRef
	 * @param x
	 * @param y
	 * @return list of buildings
	 */
	public ArrayList<Build> getBuildsAround(int x, int y)
	{
		Build b;
		ArrayList<Build> list = new ArrayList<Build>();
		
		b = getBuild(x-1, y);
		if(b != null)
			list.add(b);
		b = getBuild(x+1, y);
		if(b != null)
			list.add(b);
		b = getBuild(x, y-1);
		if(b != null)
			list.add(b);
		b = getBuild(x, y+1);
		if(b != null)
			list.add(b);
		
		return list;
	}
	
	public ArrayList<Build> getBuildsAround(int x0, int y0, int w, int h)
	{
		Build b;
		ArrayList<Build> list = new ArrayList<Build>();
		int x, y;
		
		for(x = x0-1; x <= x0 + w; x++)
		{
			// Top
			y = y0 - 1;
			b = getBuild(x, y);
			if(b != null)
				list.add(b);
			
			// Bottom
			y = y0 + h;
			b = getBuild(x, y);
			if(b != null)
				list.add(b);
		}

		for(y = y0; y < y0 + h; y++)
		{
			// Left
			x = x0 - 1;
			b = getBuild(x, y);
			if(b != null)
				list.add(b);

			// Right
			x = x0 + w;
			b = getBuild(x, y);
			if(b != null)
				list.add(b);
		}
		
		return list;
	}
	
	// Saving
	
	public void writeToSave(DataOutputStream dos) throws IOException
	{
		ObjectOutputStream oos = new ObjectOutputStream(dos);
		
		// Save grid
		System.out.println("Saving terrain...");
		oos.writeObject(grid);
		
		// Save view
		System.out.println("Saving view info...");
		oos.writeObject(view);
		
		// Save builds
		System.out.println("Saving builds...");
		oos.writeObject(builds);
		
		// Save units
		System.out.println("Saving units...");
		oos.writeObject(units);
		
		// Save city
		System.out.println("Saving city info...");
		oos.writeObject(playerCity);
		
		// Save world time
		System.out.println("Saving time info...");
		oos.writeObject(time);
		
		oos.flush();
	}
	
	public void readFromSave(DataInputStream dis) throws IOException, ClassNotFoundException
	{
		ObjectInputStream ois = new ObjectInputStream(dis);
		
		// Load grid
		System.out.println("Loading terrain...");
		grid = (MapGrid)ois.readObject();
		
		// Load view
		System.out.println("Loading view info...");
		view = (View)ois.readObject();

		// Load builds
		System.out.println("Loading builds...");
		builds = (EntityMap)ois.readObject();
		
		// Load units
		System.out.println("Loading units...");
		units = (EntityMap)ois.readObject();
		
		// Load city
		System.out.println("Loading city info...");
		playerCity = (PlayerCity)ois.readObject();
		
		// Load world time
		System.out.println("Loading time info...");
		time = (WorldTime)ois.readObject();
		
		System.out.println("Recomputing data...");		
		recomputeData();
	}
	
	/**
	 * Recomputes computed transient data.
	 * Must be called after deserialisation.
	 */
	public void recomputeData()
	{
		view.setMapSize(grid.getWidth(), grid.getHeight());
		
		Collection<Entity> entities = units.asCollection();
		for(Entity e : entities)
			e.setMap(this);

		entities = builds.asCollection();
		for(Entity e : entities)
			e.setMap(this);
		
		playerCity.recomputeData(builds.asCollection());
	}
	
}




