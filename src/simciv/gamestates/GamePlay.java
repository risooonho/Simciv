package simciv.gamestates;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.StateBasedGame;

import simciv.CityBuilder;
import simciv.Game;
import simciv.IntRange2D;
import simciv.MinimapUpdater;
import simciv.Resource;
import simciv.SoundEngine;
import simciv.Terrain;
import simciv.Vector2i;
import simciv.View;
import simciv.Map;
import simciv.content.Content;
import simciv.ui.BuildMenu;
import simciv.ui.BuildMenuBar;
import simciv.ui.InfoBar;
import simciv.ui.Minimap;
import simciv.ui.IndicatorsBar;
import simciv.ui.base.IActionListener;
import simciv.ui.base.PushButton;
import simciv.ui.base.RootPane;
import simciv.ui.base.UIBasicGameState;
import simciv.ui.base.UIRenderer;
import simciv.ui.base.Widget;
import simciv.ui.base.Window;
import simciv.units.Citizen;

/**
 * Main state of the game
 * @author Marc
 *
 */
public class GamePlay extends UIBasicGameState
{
	private int stateID = -1;
	private View view;
	private Map map;
	private CityBuilder builder;
	private InfoBar infoBar;
	private String debugText = "";
	private Vector2i pointedCell = new Vector2i();
	private Minimap minimap;
	private Window minimapWindow;
	private Window pauseWindow;
	private MinimapUpdater minimapUpdater;
	private BuildMenuBar menuBar;
	private IndicatorsBar indicatorsBar;
	private boolean quitGameRequested = false;
	private boolean paused = false;
	private boolean debugInfoVisible = false;
	private long renderTime;
	private long updateTime;
	
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
	
	public void setMap(Map world)
	{
		this.map = world;
	}

	@Override
	protected void createUI(GameContainer container, final StateBasedGame game) throws SlickException
	{
		UIRenderer.instance().setGlobalScale(2);
		int gs = UIRenderer.instance().getGlobalScale();
		ui = new RootPane(container.getWidth() / gs, container.getHeight() / gs);
		
		// Pause window
		
		pauseWindow = new Window(ui, 0, 0, 150, 85, "Game paused");
		
		PushButton resumeButton = new PushButton(pauseWindow, 0, 10, "Resume game");
		resumeButton.setAlign(Widget.ALIGN_CENTER_X);
		resumeButton.addActionListener(new TogglePauseAction());
		pauseWindow.add(resumeButton);
		
		PushButton saveButton = new PushButton(pauseWindow, 0, 28, "Save game");
		saveButton.setAlign(Widget.ALIGN_CENTER_X);
		saveButton.setEnabled(false);
		pauseWindow.add(saveButton);

		PushButton quitButton = new PushButton(pauseWindow, 0, 54, "Quit game");
		quitButton.setAlign(Widget.ALIGN_CENTER_X);
		quitButton.addActionListener(new QuitGameAction());
		pauseWindow.add(quitButton);
		
		pauseWindow.setOnCloseAction(new TogglePauseAction());
		pauseWindow.setDraggable(false);
		pauseWindow.setVisible(false);
		pauseWindow.setAlign(Widget.ALIGN_CENTER);
		ui.add(pauseWindow);
		
		menuBar = new BuildMenuBar(ui, 10, 10);
		menuBar.cityBuilderRef = builder;
		
		// Minimap
		
		minimapWindow = new Window(ui, 0, 0, 134, 134, "Minimap");
		minimap = new Minimap(minimapWindow, 0, 0, 0, 0);
		minimap.setView(view);
		minimap.setViz(minimapUpdater.getViz());
		minimap.setVisible(true);
		minimapWindow.add(minimap);
		minimapWindow.adaptSize();
		minimapWindow.setAlign(Widget.ALIGN_CENTER);
		minimapWindow.setVisible(false);
		ui.add(minimapWindow);

		// Mouse tool
		menuBar.addMode(Content.images.uiCategCursor, "Pointer", CityBuilder.MODE_CURSOR);
		
		// Erase
		menuBar.addMode(Content.images.uiCategErase, "Erase", CityBuilder.MODE_ERASE);
		
		// Roads
		menuBar.addMode(Content.images.uiCategRoad, "Trace roads", CityBuilder.MODE_ROAD);
		
		// Houses
		menuBar.addMode(Content.images.uiCategHouse, "Place houses", CityBuilder.MODE_HOUSE);
		
		// Food
		BuildMenu foodMenu = new BuildMenu(menuBar, 0, menuBar.getHeight(), 128);
		foodMenu.addBuild("FarmLand");
		menuBar.addCategory(Content.images.uiCategFood, "Food", foodMenu);

		// Industry
		BuildMenu industryMenu = new BuildMenu(menuBar, 0, menuBar.getHeight(), 128);
		industryMenu.addBuild("Warehouse");
		menuBar.addCategory(Content.images.uiCategIndustry, "Industry", industryMenu);
		
		// Administration
		BuildMenu adminMenu = new BuildMenu(menuBar, 0, menuBar.getHeight(), 128);
		adminMenu.addBuild("TaxmenOffice");
		adminMenu.addBuild("ArchitectOffice");
		menuBar.addCategory(Content.images.uiCategAdmin, "Administration", adminMenu);

		// Marketing
		BuildMenu marketMenu = new BuildMenu(menuBar, 0, menuBar.getHeight(), 128);
		marketMenu.addBuild("Market");
		menuBar.addCategory(Content.images.uiCategMarketing, "Marketing and exchanges", marketMenu);
		
		ui.add(menuBar);
		
		// Indicators bar
		
		indicatorsBar = new IndicatorsBar(ui, 0, 0);
		indicatorsBar.setPosition(ui.getWidth() - indicatorsBar.getWidth() - 10, 10);
		ui.add(indicatorsBar);
		
		// Info bar

		infoBar = new InfoBar(ui, 0, 0, 300);
		ui.add(infoBar);
	}

	@Override
	public void enter(GameContainer gc, StateBasedGame game) throws SlickException
	{
		quitGameRequested = false;
		paused = false;
		
		// Create and init minimap
		minimapUpdater = new MinimapUpdater(map);
		map.grid.addListener(minimapUpdater);
		minimapUpdater.updateCompleteViz(map.grid);
		
		// Create CityBuilder
		builder = new CityBuilder(map);
		
		// Create view
		view = new View(0, 0, 2);
		view.setMapSize(map.grid.getWidth(), map.grid.getHeight());
		
		// Because we will always draw the map on the entire screen at each frame
		gc.setClearEachFrame(false);
		
		ui = null;
		super.enter(gc, game); // Note : the UI is created here, it depends on the code above
	}

	@Override
	public void leave(GameContainer gc, StateBasedGame game)
			throws SlickException
	{
		gc.setClearEachFrame(true);
		super.leave(gc, game);
	}

	@Override
	public void update(GameContainer gc, StateBasedGame game, int delta)
			throws SlickException
	{
		// Debug
		long beginUpdateTime = gc.getTime();
		
		// Prevent too large update intervals
		if(delta > 100)
			delta = 100;
		
		Input input = gc.getInput();
		
		if(quitGameRequested)
		{
			game.enterState(Game.STATE_MAIN_MENU);
		}
		
		view.update(gc, delta / 1000.f);
		Terrain.updateTerrains(delta);
		
		if(!paused)
		{
			// Pointed cell
			pointedCell = view.convertCoordsToMap(input.getMouseX(), input.getMouseY());
			builder.cursorMoved(pointedCell);

			map.update(gc, game, delta);
			builder.update(gc);
		}
		
		minimapUpdater.update(delta);
		minimap.setViz(minimapUpdater.getViz());
		
		SoundEngine.instance().update(delta);
		
		indicatorsBar.update(
				Citizen.totalCount,
				Citizen.totalWithJob,
				(int) map.playerCity.getMoney(),
				map.time.getMonthProgressRatio());
		
		// debug
		updateTime = gc.getTime() - beginUpdateTime;
		debugText = "x=" + pointedCell.x + ", y=" + pointedCell.y;
		debugText += "  updateTime=" + updateTime;
		debugText += "  renderTime=" + renderTime;
	}

	@Override
	public void render(GameContainer gc, StateBasedGame game, Graphics gfx)
			throws SlickException
	{		
		// Debug
		long beginRenderTime = gc.getTime();

		// World view
		view.configureGraphicsForWorldRendering(gfx);
		IntRange2D mapRange = view.getMapRange(gc);
		
		/* World */
		
		map.render(gc, game, gfx, mapRange);
		
		/* Builder */
		
		builder.render(gfx);
		
		/* HUD */
				
		gfx.resetTransform();
		gfx.setColor(Color.white);

		gc.setShowFPS(debugInfoVisible);
		if(debugInfoVisible)
			renderDebugInfo(gc, gfx);
		
		infoBar.setPosition(0, ui.getHeight() - infoBar.getHeight());
		infoBar.setText(builder.getInfoText());
		
		// debug
		renderTime = gc.getTime() - beginRenderTime;
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
		{
			try {
				builder.cursorPressed(button, pointedCell);
			} catch (SlickException e) {
				e.printStackTrace();
			}
			if(button == Input.MOUSE_MIDDLE_BUTTON && !minimapWindow.isVisible())
				toggleShowMinimap();
		}
	}
	
	@Override
	public void mouseMoved(int oldx, int oldy, int newx, int newy)
	{
		if(!paused)
		{
			pointedCell = view.convertCoordsToMap(newx, newy);
			builder.cursorMoved(pointedCell);
		}
	}

	@Override
	public void mouseReleased(int button, int x, int y)
	{
		if(!paused)
		{
			try {
				builder.cursorReleased();
			} catch (SlickException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void keyReleased(int key, char c)
	{
		if(key == Input.KEY_G)
			map.grid.toggleRenderGrid();
		if(key == Input.KEY_P || key == Input.KEY_PAUSE || key == Input.KEY_ESCAPE)
			togglePause();
		if(key == Input.KEY_F3)
		{
			debugInfoVisible = !debugInfoVisible;
			if(debugInfoVisible)
				ui.setY(100 / UIRenderer.instance().getGlobalScale());
			else
				ui.setY(0);
		}
		if(key == Input.KEY_TAB)
			toggleShowMinimap();
		if(key == Input.KEY_SPACE)
			map.setFastForward(!map.isFastForward());
		if(key == Input.KEY_NUMPAD0) // Debug
			System.out.println(map.playerCity.getResourceTotal(Resource.WHEAT));
	}
	
	public void togglePause()
	{
		paused = !paused;
		pauseWindow.setVisible(paused);
	}
	
	public void toggleShowMinimap()
	{
		minimapWindow.layout();
		minimapWindow.setVisible(!minimapWindow.isVisible());
	}
	
	// UI Actions
	
	class TogglePauseAction implements IActionListener
	{
		@Override
		public void actionPerformed(Widget sender) {
			togglePause();
		}	
	}
	
	class QuitGameAction implements IActionListener
	{
		@Override
		public void actionPerformed(Widget sender) {
			quitGameRequested = true;
		}
	}

}


