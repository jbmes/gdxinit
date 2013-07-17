package info.silin.gdxinit.entity;

import com.badlogic.gdx.math.Vector2;

public class Block extends Entity {

	public static final float SIZE = 1f;

	public Block(Vector2 pos) {
		this.position = pos;
		this.bounds.width = SIZE;
		this.bounds.height = SIZE;
	}
}