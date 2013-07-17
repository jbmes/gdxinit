package info.silin.gdxinit.renderer;

import info.silin.gdxinit.World;
import info.silin.gdxinit.entity.Block;
import info.silin.gdxinit.geo.Collider;

import java.util.List;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;

public class CollisionRenderer {

	private static final Color BLOCK_COLOR = new Color(1, 0, 0, 1);
	private static final Color AVATAR_COLOR = new Color(0, 1, 0, 1);

	private ShapeRenderer renderer = new ShapeRenderer();
	private RendererController rendererController;

	private Collider collider = new Collider();

	private World world;

	public CollisionRenderer(World world, RendererController rendererController) {
		this.world = world;
		this.rendererController = rendererController;
	}

	public void render(Camera cam, float delta) {
		renderer.setProjectionMatrix(cam.combined);
		renderer.begin(ShapeType.FilledRectangle);
		renderBlocks(delta);
		renderBob(delta);
		renderer.end();
	}

	private void renderBob(float delta) {
		renderer.setColor(AVATAR_COLOR);
		Rectangle rect = collider.predictBoundingBox(world.getAvatar(), delta);
		renderer.filledRect(rect.x, rect.y, rect.width, rect.height);
	}

	private void renderBlocks(float delta) {
		List<Block> collidingBlocks = collider.getCollidingBlocks(
				rendererController.getDrawableBlocks(2, 2), world.getAvatar(),
				delta);
		for (Block block : collidingBlocks) {
			renderBlock(block);
		}
	}

	private void renderBlock(Block block) {
		Rectangle rect = block.getBoundingBox();
		renderer.setColor(BLOCK_COLOR);
		renderer.filledRect(rect.x, rect.y, rect.width, rect.height);
	}
}
