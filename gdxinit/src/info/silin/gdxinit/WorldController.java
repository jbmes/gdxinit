package info.silin.gdxinit;

import info.silin.gdxinit.entity.Avatar;
import info.silin.gdxinit.entity.Enemy;
import info.silin.gdxinit.entity.Entity;
import info.silin.gdxinit.entity.Explosion;
import info.silin.gdxinit.entity.Projectile;
import info.silin.gdxinit.geo.Collider;
import info.silin.gdxinit.geo.Collision;
import info.silin.gdxinit.geo.GeoFactory;
import info.silin.gdxinit.renderer.RendererController;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.math.Intersector.MinimumTranslationVector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;

public class WorldController {

	private Collider collider = new Collider();

	private static final float DEFAULT_DELTA = 0.01666f;
	private float manualDelta = DEFAULT_DELTA;
	private boolean manualStep = false;

	private ParticleEffect explosionPrototype;

	public WorldController() {
		prepareParticles();
	}

	private void prepareParticles() {
		explosionPrototype = new ParticleEffect();
		explosionPrototype.load(Gdx.files.internal("data/hit.p"),
				Gdx.files.internal("data"));
	}

	public void update(float delta) {

		InputEventHandler.processAvatarInput();

		if (World.State.PAUSED == World.INSTANCE.getState())
			return;

		Avatar avatar = World.INSTANCE.getAvatar();
		avatar.setState(Avatar.State.IDLE);
		avatar.update(delta);

		List<Collision> collisions = collider.predictCollisions(World.INSTANCE
				.getLevel().getNonNullBlocks(), avatar, delta);
		pushBackEntity(collisions, avatar);
		World.INSTANCE.setCollisions(collisions);

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
			processBlockCollisions(delta, p);
			processEnemyCollisions(delta, p);
			processTargetCollisions(delta, p);
		}

		removeIdleProjectiles(projectiles);
		moveFlyingProjectiles(delta, projectiles);

		World.INSTANCE.setProjectiles(projectiles);
	}

	private void processTargetCollisions(float delta, Projectile p) {
		Enemy target = World.INSTANCE.getLevel().getTarget();
		if (Enemy.State.DYING == target.getState())
			return;
		Collision targetCollision = collider.getCollision(target, p, delta);
		if (targetCollision != null) {
			target.setState(Enemy.State.DYING);
			Gdx.app.log("WorldController",
					"Arrhg! I should have spent more time at the office");
			pause();
		}
	}

	private void processEnemyCollisions(float delta, Projectile p) {
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
	}

	private List<Collision> processBlockCollisions(final float delta,
			Projectile p) {
		List<Collision> collisions = collider.predictCollisions(World.INSTANCE
				.getLevel().getNonNullBlocks(), p, delta);
		if (!collisions.isEmpty() && Projectile.State.FLYING == p.getState()) {
			p.setState(Projectile.State.EXPLODING);
		}
		return collisions;
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
	// Filtering objects by their 'offscreenness' is problematic when the camera
	// is moving. On the screen but off the world is less problematic
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

	public void pause() {

		World.INSTANCE.setState(World.State.PAUSED);
		RendererController.uiRenderer.showEndLevelDialog();
	}

	public void togglePause() {
		if (World.State.PAUSED == World.INSTANCE.getState()) {
			unpause();
			return;
		}
		pause();
	}

	private void unpause() {
		World.INSTANCE.setState(World.State.RUNNING);
		RendererController.uiRenderer.hideEndLevelDialog();
	}
}
