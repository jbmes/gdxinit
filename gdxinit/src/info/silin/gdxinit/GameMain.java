package info.silin.gdxinit;

import info.silin.gdxinit.screens.MenuScreen;

import com.badlogic.gdx.Game;

public class GameMain extends Game {

	public static GameMain instance;

	@Override
	public void create() {
		GameMain.instance = this;
		setScreen(new MenuScreen());
	}
}
