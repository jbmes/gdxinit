package info.silin.gdxinit.entity;

import info.silin.gdxinit.World;
import info.silin.gdxinit.entity.state.KillableByAvatarTouch;
import info.silin.gdxinit.entity.state.enemy.Patrol;
import info.silin.gdxinit.entity.state.enemy.ShootAvatarOnSight;
import info.silin.gdxinit.geo.Collider;
import info.silin.gdxinit.geo.GeoFactory;

import java.util.List;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;

public class Enemy extends Vehicle {

	private static final float MAX_VEL = 4f;
	private static final float MAX_FORCE = 20f;
	static final float SPEED = 2.5f;
	public static final float SIZE = 0.5f;
	private static final float DAMP = 0.90f;
	private static final float MASS = 2f;

	private static float MEMORY_DURATION = 1f;
	private float timeSinceSeenAvatar = 0;
	boolean canSeeAvatar = false;

	private boolean facingLeft = true;

	private float alertness = 0f;

	private Path patrolPath;
	private int currentPathIndex;

	private Weapon weapon = new Weapon();
	private Vector2 lastAvatarPosition;

	public Enemy(Vector2 position) {
		this.position = position;
		this.bounds.height = SIZE;
		this.bounds.width = SIZE;
		this.size = SIZE;
		this.damp = DAMP;
		this.maxVelocity = MAX_VEL;
		this.maxForce = MAX_FORCE;
		this.mass = MASS;
		setState(Patrol.getINSTANCE());
		stateMachine.addGlobalState(KillableByAvatarTouch.getINSTANCE());
		stateMachine.addGlobalState(ShootAvatarOnSight.getINSTANCE());
		// if no patrol path given, create a stub one
		patrolPath = new Path();
		patrolPath.getWaypoints().add(position);
		currentPathIndex = 0;
	}

	public boolean isFacingLeft() {
		return facingLeft;
	}

	public void setFacingLeft(boolean facingLeft) {
		this.facingLeft = facingLeft;
	}

	public void update(float delta) {
		see(delta);
		stateMachine.update(delta);
		if (weapon != null)
			weapon.update(delta);
	}

	// TODO - perhaps better as a global State
	public void see(float delta) {
		Avatar avatar = World.INSTANCE.getAvatar();
		Polygon viewRay = GeoFactory.fromSegment(getBoundingBoxCenter(),
				avatar.getBoundingBoxCenter());

		List<Entity> nonNullBlocks = World.INSTANCE.getLevel()
				.getNonNullBlocks();

		List<Entity> collidingEntities = Collider.getCollidingEntities(
				nonNullBlocks, viewRay);

		canSeeAvatar = collidingEntities.isEmpty();
		if (canSeeAvatar) {
			seingAvatar();
		} else {
			setTimeSinceSeenAvatar(timeSinceSeenAvatar + delta);
		}
	}

	// TODO - common with all shooters - where to encapsulate?
	public void shoot(Vector2 target) {
		if (null == weapon || !weapon.canFire())
			return;

		Vector2 position = getBoundingBoxCenter();
		Vector2 direction = target.cpy().sub(position).nor();

		// shifting the shot source outside the enemy, so no collisions are
		// triggered
		position.add(direction.mul(size));
		weapon.shoot(position, target);
	}

	public float getAlertness() {
		return alertness;
	}

	public void setAlertness(float alertness) {
		this.alertness = alertness;
	}

	public Path getPatrolPath() {
		return patrolPath;
	}

	public void setPatrolPath(Path patrolPath) {
		this.patrolPath = patrolPath;
	}

	public int getCurrentPathIndex() {
		return currentPathIndex;
	}

	public void setCurrentPathIndex(int currentPathIndex) {
		this.currentPathIndex = currentPathIndex;
	}

	public Weapon getWeapon() {
		return weapon;
	}

	public void setWeapon(Weapon weapon) {
		this.weapon = weapon;
	}

	public void setLastAvatarPosition(Vector2 pos) {
		lastAvatarPosition = pos;
	}

	public Vector2 getLastAvatarPosition() {
		return lastAvatarPosition;
	}

	public float getTimeSinceSeenAvatar() {
		return timeSinceSeenAvatar;
	}

	public void setTimeSinceSeenAvatar(float timeSinceSeenAvatar) {
		this.timeSinceSeenAvatar = timeSinceSeenAvatar;
	}

	public boolean forgotAvatar() {
		return timeSinceSeenAvatar > MEMORY_DURATION;
	}

	private void seingAvatar() {
		setLastAvatarPosition(World.INSTANCE.getAvatar().getPosition());
		setTimeSinceSeenAvatar(0);
	}

	public boolean canSeeAvatar() {
		return canSeeAvatar;
	}

	public void setCanSeeAvatar(boolean canSeeAvatar) {
		this.canSeeAvatar = canSeeAvatar;
	}
}