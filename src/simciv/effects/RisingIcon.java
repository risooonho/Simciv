package simciv.effects;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;

/**
 * Displays an image rising and fading
 * @author Marc
 *
 */
public class RisingIcon extends VisualEffect
{
	private Color color;
	private Image sprite;
	private int duration;
	private int time;

	public RisingIcon(int x, int y, Image sprite)
	{
		super(x, y);
		color = new Color(255, 255, 255, 1);
		duration = 1000;
		this.sprite = sprite;
	}
	
	public void renderEffect(Graphics gfx)
	{
		gfx.drawImage(sprite, 0, 0, color);
	}

	@Override
	public void update(int delta)
	{
		float k = (float)time / (float)duration;
		posY -= (1.f - k) * 64.f * (float)delta / 1000.f;
		time += delta;
		color.a = 1.f - k;
	}

	@Override
	public boolean isFinished()
	{
		return time >= duration;
	}
	
}






