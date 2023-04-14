package tage.nodeControllers;

import tage.*;
import org.joml.*;
import java.lang.Math;

/**
* This is a node controller that will bounce a game object a certain speed
* and a certain heights
* @author Jonathan Cross
*/
public class BounceController extends NodeController
{
	private Vector3f axis;
    private float up = 1.0f;
    private float height;
    private int ticks;

	/** Creates a rotation controller with vertical axis, and speed=1.0. */
	public BounceController() { super(); }

	/** Creates a rotation controller with rotation axis and speed as specified. */
	public BounceController(Engine e, Vector3f axis, float speed, float height)
	{	super();
		this.axis = new Vector3f(axis);
		this.up = speed;
        this.height = height;
        this.ticks = 1;
	}

	/** sets the rotation speed when the controller is enabled */
	public void setSpeed(float up) { this.up= up; }

	/** This is called automatically by the RenderSystem (via SceneGraph) once per frame
	*   during display().  It is for engine use and should not be called by the application.
	*/
	public void apply(GameObject go)
	{	float elapsedTime = super.getElapsedTime();
		if (ticks % (height * 10) == 0){
            up = up * -1.0f;
        }
        Matrix4f initTranslation = go.getLocalTranslation();
        float translationAmount = elapsedTime * up;
        Vector3f transVector = (new Vector3f(axis)).mul(translationAmount);
        Matrix4f newTranslation = new Matrix4f().translation(transVector);
        newTranslation = newTranslation.mul(initTranslation);
        go.setLocalTranslation(newTranslation);
        ticks++;
	}
}