package info.silin.gdxinit.renderer;

import info.silin.gdxinit.World;
import info.silin.gdxinit.entity.Avatar;
import info.silin.gdxinit.entity.Enemy;
import info.silin.gdxinit.entity.Entity;
import info.silin.gdxinit.entity.Path;
import info.silin.gdxinit.entity.Projectile;

import java.text.DecimalFormat;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.ui.Label;

public class DebugRenderer {

	private static final int VECTOR_MAGNIFICATION_FACTOR = 2;
	private static final Color BLOCK_COLOR = Color.CYAN;
	private static final Color AVATAR_COLOR = Color.GREEN;
	private static final Color ENEMY_COLOR = Color.RED;
	private static final Color PROJECTILE_COLOR = new Color(0.8f, 0.8f, 0, 1);

	private ShapeRenderer shapeRenderer = new ShapeRenderer();

	private TextRenderer textRenderer = new TextRenderer();
	private GridRenderer gridRenderer = new GridRenderer();

	private DecimalFormat format = new DecimalFormat("#.##");
	private Label debugInfo;

	public DebugRenderer() {
		debugInfo = createDebugInfo();
		RendererController.uiRenderer.stage.addActor(debugInfo);
	}

	private Label createDebugInfo() {
		debugInfo = new Label("debug label", RendererController.uiRenderer.skin);
		debugInfo.setPosition(0, Gdx.graphics.getHeight() / 2);
		debugInfo.setColor(0.8f, 0.8f, 0.2f, 1f);
		debugInfo.setSize(100, 100);
		return debugInfo;
	}

	public void draw(Camera cam) {

		shapeRenderer.setProjectionMatrix(cam.combined);
		shapeRenderer.identity();

		gridRenderer.drawGrid(cam);

		shapeRenderer.begin(ShapeType.Rectangle);
		drawBlocks();
		drawAvatar();
		drawEnemies();
		shapeRenderer.end();

		drawPatrolPaths();

		gridRenderer.drawGridNumbers(cam);

		drawMouse(cam);

		drawAvatarVectors();

		debugInfo.setText(createInfoText());

		drawShotRays();
		drawProjectiles();

		drawAvatarText(cam);
	}

	private void drawBlocks() {
		for (Entity block : World.INSTANCE.getBlocksAroundAvatar(10)) {
			drawBlock(block);
		}
	}

	private void drawBlock(Entity block) {
		Rectangle rect = block.getBoundingBox();
		shapeRenderer.setColor(BLOCK_COLOR);
		shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);
	}

	private void drawAvatar() {
		Entity avatar = World.INSTANCE.getAvatar();
		Rectangle rect = avatar.getBoundingBox();
		shapeRenderer.setColor(AVATAR_COLOR);
		shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);
	}

	private void drawEnemies() {

		for (Enemy e : World.INSTANCE.getEnemies()) {
			if (e.getState() != Enemy.State.DYING) {
				Rectangle rect = e.getBoundingBox();
				shapeRenderer.setColor(ENEMY_COLOR);
				shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);
			}
		}
	}

	private void drawPatrolPaths() {

		shapeRenderer.begin(ShapeType.Line);
		shapeRenderer.setColor(ENEMY_COLOR);

		for (Enemy e : World.INSTANCE.getEnemies()) {
			if (Enemy.State.PATROL == e.getState()) {

				Path patrolPath = e.getPatrolPath();
				List<Vector2> waypoints = patrolPath.getWaypoints();

				for (int i = 0; i < waypoints.size() - 1; i++) {
					Vector2 start = waypoints.get(i);
					Vector2 stop = waypoints.get(i + 1);
					shapeRenderer.line(start.x, start.y, stop.x, stop.y);
				}
			}
		}

		shapeRenderer.end();
	}

	private void drawMouse(Camera cam) {
		int x = Gdx.input.getX();
		int y = Gdx.input.getY();

		Vector3 mousePos = new Vector3(x, y, 1);
		cam.unproject(mousePos);

		shapeRenderer.begin(ShapeType.Circle);
		shapeRenderer.circle(mousePos.x, mousePos.y, 0.2f, 10);
		shapeRenderer.end();
	}

	private void drawAvatarVectors() {

		Entity avatar = World.INSTANCE.getAvatar();
		Rectangle rect = avatar.getBoundingBox();

		shapeRenderer.begin(ShapeType.Line);

		float centerX = rect.x + rect.width / 2;
		float centerY = rect.y + rect.height / 2;

		shapeRenderer.setColor(AVATAR_COLOR);
		Vector2 velocity = avatar.getVelocity();
		shapeRenderer.line(centerX, centerY, centerX + velocity.x
				* VECTOR_MAGNIFICATION_FACTOR, centerY + velocity.y
				* VECTOR_MAGNIFICATION_FACTOR);
		shapeRenderer.setColor(Color.BLUE);
		Vector2 acc = avatar.getAcceleration();
		shapeRenderer.line(centerX, centerY, centerX + acc.x
				* VECTOR_MAGNIFICATION_FACTOR, centerY + acc.y
				* VECTOR_MAGNIFICATION_FACTOR);

		shapeRenderer.end();
	}

	private StringBuilder createInfoText() {

		StringBuilder debugText = new StringBuilder("debug info: \n");
		Avatar avatar = World.INSTANCE.getAvatar();
		Vector2 acceleration = avatar.getAcceleration();
		debugText.append(format.format(acceleration.x) + ", "
				+ format.format(acceleration.y) + "\n");
		Vector2 velocity = avatar.getVelocity();
		debugText.append(format.format(velocity.x) + ", "
				+ format.format(velocity.y) + "\n");
		debugText.append("shots alive: "
				+ World.INSTANCE.getProjectiles().size() + "\n");
		return debugText;
	}

	private void drawShotRays() {
		List<Ray> shotRays = World.INSTANCE.getShotRays();
		shapeRenderer.begin(ShapeType.Line);
		for (Ray ray : shotRays) {
			shapeRenderer
					.line(ray.origin.x, ray.origin.y, ray.origin.x
							+ ray.direction.x * 10, ray.origin.y
							+ ray.direction.y * 10);
		}
		shapeRenderer.end();
	}

	private void drawProjectiles() {
		List<Projectile> projectiles = World.INSTANCE.getProjectiles();

		shapeRenderer.begin(ShapeType.Rectangle);
		shapeRenderer.setColor(PROJECTILE_COLOR);
		for (Projectile p : projectiles) {
			Rectangle boundingBox = p.getBoundingBox();
			shapeRenderer.rect(boundingBox.x, boundingBox.y, boundingBox.width,
					boundingBox.height);
		}
		shapeRenderer.end();
	}

	private void drawAvatarText(Camera cam) {
		Vector2 avatarPosition = World.INSTANCE.getAvatar().getPosition();
		Vector3 projectedPos = new Vector3(avatarPosition.x, avatarPosition.y,
				1);

		// transform avatar position into screen coords
		cam.project(projectedPos);

		String newText = "pos: x: " + format.format(avatarPosition.x) + ", y: "
				+ format.format(avatarPosition.y);

		textRenderer.draw(newText, projectedPos.x, projectedPos.y);
	}
}
