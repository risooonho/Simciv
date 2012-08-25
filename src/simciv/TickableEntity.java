package simciv;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.state.StateBasedGame;

/**
 * Entity with a special update system :
 * In this game, updates are discretized into ticks :
 * update() uses real-time parameters, tick() does not.
 * tick() is called at each tick time interval, usually more than the frame time (each second for example).
 * Tick times are specific to each entity.
 * @author Marc
 *
 */
public abstract class TickableEntity extends Entity
{
	private int nbTicks;
	private int timeBeforeNextTick;

	public TickableEntity(Map m)
	{
		super(m);
		timeBeforeNextTick = getTickTime();
	}

	public int getTicks()
	{
		return nbTicks;
	}
	
	/**
	 * Returns the tick ratio.
	 * This value is always increasing, reaching 1 before next call to tick(),
	 * and turns back to zero after each tick().
	 * For example, if the entity has waited the half of its time before tick again,
	 * getK() will return 0.5.
	 * @return ratio between 0 and 1
	 */
	public final float getK()
	{
		return (float)timeBeforeNextTick / (float)(getTickTime());
	}
	
	/**
	 * Converts seconds into ticks looking towards the entity
	 * @param s : seconds
	 * @return
	 */
	public final int secondsToTicks(float s)
	{
		return (int) ((1000.f * s) / getTickTime());
	}
		
	/**
	 * Returns the time interval between each behavior update.
	 * It can be used to increase its speed for example.
	 * @return : time per tick in milliseconds
	 */
	protected abstract int getTickTime();

	protected void tickEntity()
	{
		tick();
	}
	
	/**
	 * Called regularly to make the entity "live" and execute its tasks.
	 */
	protected abstract void tick();
	
	@Override
	public void update(GameContainer gc, StateBasedGame game, int delta)
	{
		super.update(gc, game, delta);
		timeBeforeNextTick -= delta;
		
		if(timeBeforeNextTick < 0)
		{
			timeBeforeNextTick += getTickTime();
			tickEntity();
			nbTicks++;
		}
	}

}
