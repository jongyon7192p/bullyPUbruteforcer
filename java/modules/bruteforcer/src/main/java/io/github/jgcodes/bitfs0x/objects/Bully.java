package io.github.jgcodes.bitfs0x.objects;

import com.sun.jna.Pointer;
import io.github.jgcodes.libsm64.math.FloatVector3;
import io.github.jgcodes.libsm64.util.Pointers;

public class Bully {
  private static final int POS_X = 240;
  private static final int POS_Y = 244;
  private static final int POS_Z = 248;
  private static final int H_SPEED = 264;
  private static final int YAW_1 = 280;
  private static final int ACTION = 283; //tentative, this is the pointer from public decomp
  private static final int YAW_2 = 292;

  /**
   * Represents an action that the bully is doing.
   */
  public enum Action {
    /**
     * The bully is circling around its home.
     */
    PATROL(0),
    /**
     * The bully is running towards Mario.
     */
    CHASE_MARIO(1),
    /**
     * The bully is being knocked back from a collision.
     */
    KNOCKBACK(2),
    /**
     * Not sure.
     */
    BACK_UP(3),
    /**
     * The bully is inactive.
     */
    INACTIVE(4),
    /**
     * The bully has just spawned.
     */
    ACTIVATE_AND_FALL(5),
    /**
     * The bully is dying in lava.
     */
    BOIL_DEATH(100),
    /**
     * The bully has fallen out of the map.
     */
    DEATH_PLANE_DEATH(101);

    public final int value;

    Action(int value) {
      this.value = value;
    }

    /**
     * Returns the bully action corresponding the specified integer.
     *
     * @param value the integer
     * @return an {@link Action} corresponding to the specified integer
     * @throws IllegalArgumentException if {@code value} is does not correspond to an action
     */
    public static Action valueOf(int value) {
      for (Action act: Action.values()) {
        if (act.value == value) return act;
      }
      throw new IllegalArgumentException("Not a valid bully state");
    }
  }

  private final Pointer obj;

  public Bully(Pointer obj) {
    this.obj = obj;
  }

  public FloatVector3 getPosition() {
    return new FloatVector3(getX(), getY(), getZ());
  }

  public void setPosition(FloatVector3 pos) {
    setX(pos.x());
    setY(pos.y());
    setZ(pos.z());
  }

  public float getX() {
    return obj.getFloat(POS_X);
  }

  public void setX(float x) {
    obj.setFloat(POS_X, x);
  }

  public float getY() {
    return obj.getFloat(POS_Y);
  }

  public void setY(float y) {
    obj.setFloat(POS_Y, y);
  }

  public float getZ() {
    return obj.getFloat(POS_Z);
  }

  public void setZ(float z) {
    obj.setFloat(POS_Z, z);
  }

  public float getHSpeed() {
    return obj.getFloat(H_SPEED);
  }

  public void setHSpeed(float hSpeed) {
    obj.setFloat(H_SPEED, hSpeed);
  }

  public short getYaw1() {
    return obj.getShort(YAW_1);
  }

  public void setYaw1(short yaw1) {
    obj.setShort(YAW_1, yaw1);
  }

  public short getYaw2() {
    return obj.getShort(YAW_2);
  }

  public void setYaw2(short yaw2) {
    obj.setShort(YAW_2, yaw2);
  }

  /**
   * Do not use. Returns the bully's current action.
   *
   * @return the bully's current action
   */
  public Action getAction() {
    return Action.valueOf(obj.getInt(ACTION));
  }

  /**
   * Do not use. Sets the bully's current action.
   *
   * @param action an action to set
   */
  public void setAction(Action action) {
    obj.setInt(ACTION, action.value);
  }

  @Override
  public String toString() {
    return """
      BULLY | POS=%s, HSPD=%s, YAW=%s\
      """.formatted(
      getPosition(), getHSpeed(), getYaw1()
    );
  }
}
