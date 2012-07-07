package simciv.states;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.StateBasedGame;

import simciv.CityBuilder;
import simciv.ContentManager;
import simciv.IntRange2D;
import simciv.MapGenerator;
import simciv.Road;
import simciv.Terrain;
import simciv.Vector2i;
import simciv.View;
import simciv.World;
import simciv.ui.IActionListener;
import simciv.ui.Menu;
import simciv.ui.MenuItem;
import simciv.ui.RootPane;
import simciv.ui.ToolButton;
import simciv.ui.ToolButtonGroup;
import simciv.ui.UIBasicGameState;
import simciv.ui.UIRenderer;

/**
 * Main state of the game
 * @author Marc
 *
 */
public class GamePlay extends UIBasicGameState
{
	private int stateID = -1;
	private View view;
	private World world;
	private CityBuilder builder;
	private String debugText = "";
	private Vector2i pointedCell = new Vector2i();
	private ToolButtonGroup buildCategoryButtonsGroup;
	private boolean closeRequested = false;
	private boolean paused = false;
	private boolean debugInfoVisible = false;
	
	public GamePlay(int stateID)
	{
		this.stateID = stateID;
	}
	
	@Override
	public int getID()
	{
		return stateID;
	}
	
	@Override
	public void init(GameContainer container, StateBasedGame game) throws SlickException
	{
	}

	@Override
	protected void createUI(GameContainer container, final StateBasedGame game) throws SlickException
	{
		ui = new RootPane(1200, 900);
		
		buildCategoryButtonsGroup = new ToolButtonGroup();
		
		ToolButton eraseButton = new ToolButton(ui, 10, 10, buildCategoryButtonsGroup);
		eraseButton.setActionListener(new ChangeBuildCategoryAction(CityBuilder.MODE_ERASE));
		eraseButton.icon = ContentManager.instance().getImage("ui.categErase");
		buildCategoryButtonsGroup.add(eraseButton);
		ui.add(eraseButton);

		ToolButton traceRoadsButton = new ToolButton(ui, 34, 10, buildCategoryButtonsGroup);
		traceRoadsButton.setActionListener(new ChangeBuildCategoryAction(CityBuilder.MODE_ROAD));
		traceRoadsButton.icon = ContentManager.instance().getImage("ui.categRoad");
		buildCategoryButtonsGroup.add(traceRoadsButton);
		ui.add(traceRoadsButton);
		
		ToolButton buildHousesButton = new ToolButton(ui, 58, 10, buildCategoryButtonsGroup);
		buildHousesButton.setActionListener(new ChangeBuildCategoryAction(CityBuilder.MODE_HOUSE, "House"));
		buildHousesButton.icon = ContentManager.instance().getImage("ui.categHouse");
		buildCategoryButtonsGroup.add(buildHousesButton);
		ui.add(buildHousesButton);
		
		ToolButton foodBuildsButton = new ToolButton(ui, 82, 10, buildCategoryButtonsGroup);

		Menu foodBuildsMenu = new Menu(ui, 10, 34, 128);
		foodBuildsMenu
			.add(new MenuItem(foodBuildsMenu, "Water source"), new SelectBuildAction(foodBuildsButton, "House"))
			.add(new MenuItem(foodBuildsMenu, "Hunters"), new SelectBuildAction(foodBuildsButton, "House"))
			.add(new MenuItem(foodBuildsMenu, "Farm land"), new SelectBuildAction(foodBuildsButton, "FarmLand"))
			.setNullActionListener(new SelectBuildAction(foodBuildsButton, null));
		foodBuildsMenu.setVisible(false);
		ui.add(foodBuildsMenu);

		foodBuildsButton.setActionListener(new ChangeBuildCategoryAction(CityBuilder.MODE_ROAD, foodBuildsMenu));
		buildCategoryButtonsGroup.add(foodBuildsButton);
		ui.add(foodBuildsButton);
	}

	@Override
	public void enter(GameContainer gc, StateBasedGame game) throws SlickException
	{
		Terrain.initialize();
		Road.loadContent();
		CityBuilder.loadContent();

		world = new World(64, 64);
		builder = new CityBuilder(world);
		view = new View(0, 0, 2);
		
		MapGenerator mapgen = new MapGenerator(131183);
		mapgen.generate(world.map);
		
		super.enter(gc, game);
	}

	@Override
	public void leave(GameContainer gc, StateBasedGame game)
			throws SlickException
	{
		super.leave(gc, game);
	}

	@Override
	public void update(GameContainer gc, StateBasedGame game, int delta)
			throws SlickException
	{
		// Prevent too large update intervals
		if(delta > 100)
			delta = 100;
		
		Input input = gc.getInput();
		
		if(closeRequested)
			gc.exit();
		
		view.update(gc, delta / 1000.f);
		builder.cursorMoved(pointedCell);
		Terrain.updateTerrains(delta);
		
		if(!paused)
		{
			// Pointed cell
			pointedCell = view.convertCoordsToMap(input.getMouseX(), input.getMouseY());

			world.update(delta);
			builder.update(gc);
		}
		
		debugText = "x=" + pointedCell.x + ", y=" + pointedCell.y;
	}

	@Override
	public void render(GameContainer gc, StateBasedGame game, Graphics gfx)
			throws SlickException
	{		
		// World view
		view.configureGraphicsForWorldRendering(gfx);
		IntRange2D mapRange = view.getMapRange(gc);
		
		/* World */
		
		world.render(mapRange, gc, gfx);
		
		/* Builder */
		
		builder.render(gfx);
		
		/* HUD */
				
		UIRenderer.instance().setGlobalScale(2);
		gfx.resetTransform();
		gfx.setColor(Color.white);

		gc.setShowFPS(debugInfoVisible);
		if(debugInfoVisible)
			renderDebugInfo(gc, gfx);

		if(paused)
			gfx.drawString("PAUSE", 10, 70);
	}
	
	public void renderDebugInfo(GameContainer gc, Graphics gfx)
	{		
		gfx.setColor(new Color(0, 0, 0, 128));
		gfx.fillRect(0, 0, gc.getWidth(), 100);
		gfx.setColor(Color.white);
		
		builder.renderDebugInfo(gfx);
				
		gfx.drawString(debugText, 10, 50);
		gfx.drawString(
				"MEM total(used):   "
				+ (Runtime.getRuntime().totalMemory() / 1000000)
				+ "(" + ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000)
				+ ") MB",
				600, 10);
	}
	
	@Override
	public void mousePressed(int button, int x, int y)
	{		
		if(!paused)
			builder.cursorPressed(button, pointedCell);
	}
	
	@Override
	public void mouseMoved(int oldx, int oldy, int newx, int newy)
	{
		if(!paused)
			builder.cursorMoved(pointedCell);
	}

	@Override
	public void mouseReleased(int button, int x, int y)
	{
		if(!paused)
			builder.cursorReleased();
	}

	@Override
	public void keyReleased(int key, char c)
	{
		if(key == Input.KEY_G)
			world.map.toggleRenderGrid();
		if(key == Input.KEY_P)
			paused = !paused;
		if(key == Input.KEY_F3)
		{
			debugInfoVisible = !debugInfoVisible;
			if(debugInfoVisible)
				ui.setY(100 / UIRenderer.instance().getGlobalScale());
			else
				ui.setY(0);
		}
	}
	
	// UI Actions
	
	class ChangeBuildCategoryAction implements IActionListener
	{
		private int category;
		private String buildingString;
		private Menu buildsMenu;
		
		public ChangeBuildCategoryAction(int categ) {
			this.category = categ;
		}

		public ChangeBuildCategoryAction(int categ, Menu buildsMenu) {
			this.category = categ;
			this.buildsMenu = buildsMenu;
		}
		
		public ChangeBuildCategoryAction(int categ, String buildingString) {
			this.category = categ;
			this.buildingString = buildingString;
		}
		
		@Override
		public void actionPerformed() {
			builder.setMode(category);
			if(buildingString != null)
				builder.setBuildingString(buildingString);
			if(buildsMenu != null)
				buildsMenu.setVisible(true);
		}
	}
	
	class SelectBuildAction implements IActionListener
	{
		private String buildingString;
		private ToolButton categButton;
		
		public SelectBuildAction(ToolButton categButton, String buildingString) {
			this.buildingString = buildingString;
			this.categButton = categButton;
		}
		
		@Override
		public void actionPerformed() {
			if(buildingString != null)
			{
				builder.setMode(CityBuilder.MODE_BUILDS);
				builder.setBuildingString(buildingString);
			}
			categButton.select(false);
		}
	}

}


