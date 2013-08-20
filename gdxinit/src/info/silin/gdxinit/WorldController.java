package info.silin.gdxinit;

import info.silin.gdxinit.entity.Avatar;
import info.silin.gdxinit.entity.Avatar.State;
import info.silin.gdxinit.entity.Enemy;
import info.silin.gdxinit.entity.Entity;
import info.silin.gdxinit.entity.Explosion;
import info.silin.gdxinit.entity.Projectile;
import info.silin.gdxinit.geo.Collider;
import info.silin.gdxinit.geo.Collision;
import info.silin.gdxinit.geo.GeoFactory;
import info.silin.gdxinit.renderer.RendererController;
import info.silin.gdxinit.renderer.UIRenderer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.math.Intersector.MinimumTranslationVector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class WorldController {

	private static final float ACCELERATION = 20f;

	private Collider collider = new Collider();

	private static final float DEFAULT_DELTA = 0.01666f;
	private float manualDelta = DEFAULT_DELTA;
	private boolean manualStep = false;

	private ParticleEffect explosionPrototype;

	private boolean fireButtonWasPressed;

	public WorldController() {
		prepareParticles();
	}

	private void prepareParticles() {
		explosionPrototype = new ParticleEffect();
		explosionPrototype.load(Gdx.files.internal("data/hit.p"),
				Gdx.files.internal("data"));
	}

	public void update(float delta) {
		Avatar avatar = World.INSTANCE.getAvatar();
		avatar.setState(State.IDLE);
		// we set the avatar acceleration in the processInput method
		processInput(delta);
		avatar.update(delta);

		List<Collision> collisions = collider.predictCollisions(World.INSTANCE
				.getLevel().getNonNullBlocks(), avatar, delta);
		pushBackEntity(collisions, avatar);
		World.INSTANCE.setCollisions(collisions);

		if (constrainPosition(avatar)) {
			avatar.setState(State.IDLE);
		}

		updateEnemies(delta);
		// TODO - combine projectile & explosions handling
		updateProjectiles(delta);
		checkForNewExplosions();
		updateExplosions(delta);
		filterFinishedExplosions();
		filterDeadEnemies();
	}

	private void pushBackEntity(List<Collision> collisions, Entity entity) {
		for (Collision c : collisions) {
			MinimumTranslationVector translation = c.getTranslation();
			entity.getPosition().add(translation.normal.x * translation.depth,
					translation.normal.y * translation.depth);
		}
	}

	private void updateEnemies(float delta) {
		Avatar avatar = World.INSTANCE.getAvatar();
		for (Enemy e : World.INSTANCE.getEnemies()) {
			e.update(delta);

			if (e.getWeapon().canFire() && canSeeAvatar(e)) {
				e.shoot(avatar.getBoundingBoxCenter());
			}
		}
	}

	private boolean canSeeAvatar(Enemy e) {
		Avatar avatar = World.INSTANCE.getAvatar();
		Polygon viewRay = GeoFactory.fromSegment(e.getBoundingBoxCenter(),
				avatar.getBoundingBoxCenter());

		List<Entity> nonNullBlocks = World.INSTANCE.getLevel()
				.getNonNullBlocks();

		List<Entity> collidingEntities = collider.getCollidingEntities(
				nonNullBlocks, viewRay);

		return collidingEntities.isEmpty();
	}

	private void updateProjectiles(final float delta) {

		List<Projectile> projectiles = World.INSTANCE.getProjectiles();

		for (Projectile p : projectiles) {
			Vector2 position = p.getPosition();
			if (offWorld(position)) {
				p.setState(Projectile.State.IDLE);
				break;
			}

			// colliding with blocks
			List<Collision> collisions = collider.predictCollisions(
					World.INSTANCE.getLevel().getNonNullBlocks(), p, delta);
			if (!collisions.isEmpty()
					&& Projectile.State.FLYING == p.getState()) {
				p.setState(Projectile.State.EXPLODING);
			}

			// colliding with enemies
			ArrayList<Entity> enemyEntities = new ArrayList<Entity>(
					World.INSTANCE.getEnemies());
			List<Collision> enemyCollisions = collider.predictCollisions(
					enemyEntities, p, delta);
			if (!enemyCollisions.isEmpty()
					&& Projectile.State.FLYING == p.getState()) {
				Gdx.app.log("WorlController", "hit an enemy");
				Enemy enemy = (Enemy) enemyCollisions.get(0).getEntity1();
				enemy.setState(Enemy.State.DYING);
				p.setState(Projectile.State.IDLE); // no explosions for enemies
			}

			Enemy target = World.INSTANCE.getLevel().getTarget();
			Collision targetCollision = collider.getCollision(target, p, delta);
			if (targetCollision != null) {
				target.setState(Enemy.State.DYING);
				Gdx.app.log("WorldController",
						"Arrhg! I should have spent more time in my cubicle");
				// TODO - show END menu

				UIRenderer uiRenderer = RendererController.uiRenderer;
				Button button = new TextButton("Start", uiRenderer.skin,
						"default");
				button.setPosition(Gdx.graphics.getWidth() / 2f,
						Gdx.graphics.getHeight() / 2f);
				button.addListener(new ClickListener() {

					@Override
					public void clicked(InputEvent event, float x, float y) {
						World.INSTANCE.restartCurrentLevel();
						super.clicked(event, x, y);
					}
				});
				uiRenderer.stage.addActor(button);
			}
		}

		removeIdleProjectiles(projectiles);
		moveFlyingProjectiles(delta, projectiles);

		World.INSTANCE.setProjectiles(projectiles);
	}

	private void moveFlyingProjectiles(final float delta,
			List<Projectile> projectiles) {
		for (Projectile p : projectiles) {
			if (Projectile.State.FLYING == p.getState())
				p.getPosition().add(p.getVelocity().cpy().mul(delta));
		}
	}

	private void removeIdleProjectiles(List<Projectile> projectiles) {
		for (Iterator<Projectile> iterator = projectiles.iterator(); iterator
				.hasNext();) {
			Projectile projectile = (Projectile) iterator.next();
			if (Projectile.State.IDLE == projectile.getState())
				iterator.remove();
		}
	}

	// Offscreen may perhaps be more appropriate here. There may be points that
	// are in the world but offscreen
	// filtering objects by their 'offscreenness' is problematic when the camera
	// is moving
	// the other way round - on the screen but off the world is less problematic
	private boolean offWorld(Vector2 position) {
		return position.x < 0 || position.x > World.WIDTH || position.y < 0
				|| position.y > World.HEIGHT;
	}

	// get new explosions, set according projectiles to idle
	private void checkForNewExplosions() {

		List<Projectile> projectiles = World.INSTANCE.getProjectiles();

		List<Explosion> explosions = World.INSTANCE.getExplosions();
		for (Projectile p : projectiles) {
			if (Projectile.State.EXPLODING == p.getState()) {

				p.setState(Projectile.State.IDLE);

				ParticleEffect effect = new ParticleEffect();
				effect.load(Gdx.files.internal("data/hit.p"),
						Gdx.files.internal("data"));
				effect.reset();

				Vector2 position = p.getPosition();
				Vector2 velocity = p.getVelocity();

				effect.setPosition(position.x - velocity.x, position.y
						- velocity.y);
				Gdx.app.log("DefaultRenderer#checkForNewExplosions",
						"creating an explosion at " + position.x + ", "
								+ position.y);
				Explosion ex = new Explosion(effect, position,
						velocity.angle() + 90);
				explosions.add(ex);
			}
		}
	}

	private void updateExplosions(float delta) {
		List<Explosion> explosions = World.INSTANCE.getExplosions();
		for (Explosion explosion : explosions) {
			explosion.update(delta);
		}
	}

	private void filterFinishedExplosions() {
		List<Explosion> explosions = World.INSTANCE.getExplosions();
		for (Iterator<Explosion> iterator = explosions.iterator(); iterator
				.hasNext();) {
			Explosion explosion = iterator.next();
			if (explosion.getEffect().isComplete()) {
				iterator.remove();
			}
		}
	}

	private void filterDeadEnemies() {
		List<Enemy> enemies = World.INSTANCE.getEnemies();
		for (Iterator<Enemy> iterator = enemies.iterator(); iterator.hasNext();) {
			Enemy enemy = iterator.next();
			if (Enemy.State.DYING == enemy.getState()) {
				iterator.remove();
			}
		}
	}

	private boolean constrainPosition(Entity entity) {
		boolean wasContrained = false;
		Vector2 position = entity.getPosition();
		if (position.x < 0) {
			position.x = 0;
			wasContrained = true;
		}
		if (position.y < 0) {
			position.y = 0;
			wasContrained = true;
		}
		float size = entity.getSize();
		if (position.x > World.WIDTH - size) {
			position.x = World.WIDTH - size;
			wasContrained = true;
		}
		if (position.y > World.HEIGHT - size) {
			position.y = World.HEIGHT - size;
			wasContrained = true;
		}
		return wasContrained;
	}

	private boolean processInput(float delta) {
		Avatar avatar = World.INSTANCE.getAvatar();

		if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
			avatar.setFacingLeft(true);
			avatar.setState(State.WALKING);
			avatar.getAcceleration().x = -ACCELERATION;

		} else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
			avatar.setFacingLeft(false);
			avatar.setState(State.WALKING);
			avatar.getAcceleration().x = ACCELERATION;

		} else if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
			avatar.setFacingLeft(true);
			avatar.setState(State.WALKING);
			avatar.getAcceleration().y = ACCELERATION;

		} else if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
			avatar.setFacingLeft(false);
			avatar.setState(State.WALKING);
			avatar.getAcceleration().y = -ACCELERATION;
		} else {
			avatar.setState(State.IDLE);
			avatar.getAcceleration().x = 0;
		}

		if (Gdx.input.isButtonPressed(Buttons.LEFT)) {
			fireButtonWasPressed = true;
			avatar.shoot(RendererController.getUnprojectedMousePosition());
		} else {
			if (fireButtonWasPressed) {
				avatar.shoot(RendererController.getUnprojectedMousePosition());
				fireButtonWasPressed = false;
			}
		}
		return false;
	}

	public boolean isManualStep() {
		return manualStep;
	}

	public void setManualStep(boolean manualStep) {
		this.manualStep = manualStep;
	}

	public void step() {
		update(DEFAULT_DELTA);
	}

	public float getManualDelta() {
		return manualDelta;
	}

	public void setManualDelta(float manualDelta) {
		this.manualDelta = manualDelta;
	}
}
