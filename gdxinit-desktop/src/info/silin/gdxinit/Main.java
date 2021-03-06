package info.silin.gdxinit;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.tools.imagepacker.TexturePacker2;
import com.badlogic.gdx.tools.imagepacker.TexturePacker2.Settings;

public class Main {
	public static void main(String[] args) {

		Settings settings = new Settings();
		settings.maxWidth = 512;
		settings.maxHeight = 512;
		TexturePacker2.process(settings, "../gdxinit-android/assets/images",
				"../gdxinit-android/assets/images/textures", "textures");

		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
		cfg.title = "gdxinit";
		cfg.useGL20 = true;
		cfg.width = 480;
		cfg.height = 320;

		new LwjglApplication(new Screens(), cfg);
	}
}
