package Game;

import tage.*;
import tage.input.InputManager;
import tage.networking.IGameConnection.ProtocolType;
import tage.nodeControllers.BounceController;
import tage.nodeControllers.RotationController;
import tage.shapes.*;

import java.lang.Math;
import java.lang.Object;
import java.net.UnknownHostException;
import java.net.InetAddress;


import java.io.*;
import org.joml.*;

import java.util.ArrayList;

public class MyGame extends VariableFrameRateGame
{
	private InputManager im;
	private static Engine engine;

	private boolean paused=false;
	private double lastFrameTime, currFrameTime, elapsTime;

	private GameObject dol, x, y, z, terr, gameBall;
	private ObjShape ghostShape, dolS, linxS, linyS, linzS, terrS, ballS;
	private TextureImage doltx, prize, grass, heightMap;
	private Light light1;

	private int score;
	private final float camDolMinProximity=10f;
	private final float camPrizeProximity=3f;

	private Plane plane;

	private NodeController rc,bc;


	private GhostManager gm;
	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected = false;

	private int sky;

	public MyGame(String serverAddress, int serverPort, String protocol) { 
		super();
		gm = new GhostManager(this);
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		if (protocol.toUpperCase().compareTo("TCP") == 0)
			this.serverProtocol = ProtocolType.TCP;
		else
			this.serverProtocol = ProtocolType.UDP;
	}

	public static void main(String[] args)
	{	MyGame game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
	}

	@Override
	public void loadShapes()
	{	dolS = new ImportedModel("steve.obj");
		linxS = new Line(new Vector3f(0f,0f,0f), new Vector3f(3f,0f,0f));
		linyS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,3f,0f));
		linzS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,0f,-3f));
		plane = new Plane();
		terrS = new TerrainPlane(1000);
		ballS = new Sphere(10);
		ghostShape = new ImportedModel("steve.obj");

	}

	

	@Override
	public void loadTextures()
	{	doltx = new TextureImage("steve.png");
		prize = new TextureImage("prize.png");
		grass = new TextureImage("grass-pattern.jpg");
		heightMap = new TextureImage("terrMap2.jpg");
	}

	@Override
	public void buildObjects()
	{	Matrix4f initialTranslation, initialScale;

		// create world axes
		x = new GameObject(GameObject.root(), linxS);
		y = new GameObject(GameObject.root(), linyS);
		z = new GameObject(GameObject.root(), linzS);

		//set world axes' colors
		x.getRenderStates().setColor(new Vector3f(1f, 0f, 0f));
		y.getRenderStates().setColor(new Vector3f(0f, 1f, 0f));
		z.getRenderStates().setColor(new Vector3f(0f, 0f, 1f));


		terr = new GameObject(GameObject.root(), terrS, grass);
		Matrix4f terrScale = (new Matrix4f()).scaling(40f,10f,40f);
		terr.setLocalScale(terrScale);
		terr.setHeightMap(heightMap);
		
		// build dolphin in the center of the window
		dol = new GameObject(GameObject.root(), dolS, doltx);
		initialTranslation = (new Matrix4f()).translation(0,0,0);
		initialScale = (new Matrix4f()).scaling(3.0f);
		dol.setLocalTranslation(initialTranslation);
		dol.setLocalScale(initialScale);

		
	}

	@Override
	public void initializeLights()
	{	Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);
		light1 = new Light();
		light1.setLocation(new Vector3f(5.0f, 4.0f, 2.0f));
		(engine.getSceneGraph()).addLight(light1);
	}

	@Override
	public void loadSkyBoxes(){
		sky = (engine.getSceneGraph()).loadCubeMap("sky");
		(engine.getSceneGraph()).setActiveSkyBoxTexture(sky);
		(engine.getSceneGraph()).setSkyBoxEnabled(true);
	}

	@Override
	public void initializeGame()
	{	
		score = 0;
		lastFrameTime = System.currentTimeMillis();
		currFrameTime = System.currentTimeMillis();
		elapsTime = 0.0;

		rc = new RotationController(engine, new Vector3f(0,1,0), .001f);
		

		bc = new BounceController(engine, new Vector3f(0,1,0), .001f, 5f);

		(engine.getSceneGraph()).addNodeController(rc);
		(engine.getSceneGraph()).addNodeController(bc);
		(engine.getRenderSystem()).setWindowDimensions(1900,1000);



		//INPUT SECTION
		im = engine.getInputManager();
		FwdAction fwdAction = new FwdAction(this);
		TurnAction turnAction = new TurnAction(this);
		

		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.A, turnAction,
		InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.D, turnAction,
		InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllGamepads(net.java.games.input.Component.Identifier.Axis.X, turnAction,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.W, fwdAction,
			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.S, fwdAction,
		InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		im.associateActionWithAllGamepads(net.java.games.input.Component.Identifier.Axis.Y, fwdAction,
		InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		

		setupNetworking();
	}

	@Override
	public void update()
	{		
		
		lastFrameTime = currFrameTime;
		currFrameTime = System.currentTimeMillis();
		if (!paused) elapsTime += (currFrameTime - lastFrameTime) / 1000.0;
		mountCam();

		// build and set HUD
		int elapsTimeSec = Math.round((float)elapsTime);
		String scoreDisplayStr = "Score: " + Integer.toString(score);
		
		Vector3f dolWorldPosition = dol.getWorldLocation();
		String locationString = 
			"X: " + String.valueOf(dolWorldPosition.x()) +
			"Y: " + String.valueOf(dolWorldPosition.y()) +
			"Z: " + String.valueOf(dolWorldPosition.z());
			
		Vector3f hud1Color = new Vector3f(0,1,0);
		Vector3f hud2Color = new Vector3f(1,0,0);

		int width = (int) Math.ceil((engine.getRenderSystem().getGLCanvas().getWidth()/37.68));
		int height = (int) Math.ceil((engine.getRenderSystem().getGLCanvas().getHeight()/1.325));
		(engine.getHUDmanager()).setHUD1(scoreDisplayStr, hud1Color, 600, 15); 
		(engine.getHUDmanager()).setHUD2(locationString, hud2Color, width, height);

		// update input manager
		im.update((float) elapsTime);// can prob take out
		updateScore();

		// double score if finished in under a minute and a half
		if(elapsTimeSec <90 && score == 3) score = score*2;

		//update character location
		Vector3f avLoc = dol.getLocalLocation();
		float newHeight = terr.getHeight(avLoc.x(),avLoc.z());
		dol.setLocalLocation(new Vector3f(avLoc.x(),newHeight, avLoc.z()));
		
		//update ball location if below terrain
		if (gameBall != null){
			float ballX = gameBall.getLocalLocation().x();
			float ballY = gameBall.getLocalLocation().y();
			float ballZ = gameBall.getLocalLocation().z();
			float terrHeight = terr.getHeight(ballX, ballZ);
			boolean ballBelow = (ballY - terrHeight) < 0;
			float ballHeight = ballBelow ? ballY + (terrHeight-ballY): ballY ;
			if (ballBelow) gameBall.setLocalLocation(new Vector3f(ballX, ballHeight, ballZ));
		}

		processNetworking((float) elapsTime);
	}

	@Override
	public void createViewports(){
		(engine.getRenderSystem()).addViewport("MAIN",0,0,1,1);
		// ------------- positioning the main camera -------------
		(engine.getRenderSystem().getViewport("MAIN").getCamera()).setLocation(new Vector3f(0,0,5));
	}


	public GameObject getAvatar() { return dol; }

	public Engine getEngine() { return engine; }

	public void updateScore(){
		
	}

	public void mountCam(){
		Vector3f loc, fwd, up, right;
		Camera cam;
		cam = (engine.getRenderSystem().getViewport("MAIN").getCamera());
		loc = dol.getWorldLocation(); //TODO one of these may help with rolling
		fwd = dol.getWorldForwardVector();
		up = dol.getWorldUpVector();
		right = dol.getWorldRightVector();
		cam.setU(right);
		cam.setV(up);
		cam.setN(fwd);
		cam.setLocation(loc.add(up.mul(4f)).add(fwd.mul(-5f)));
	}


	public float checkProximity(float x1, float x2){
		return Math.abs(x2-x1); 
	}

	public boolean camCloseToDol(){
		Camera cam = (engine.getRenderSystem().getViewport("MAIN").getCamera());
		Vector3f dolLoc = dol.getWorldLocation();
		Vector3f camLoc = cam.getLocation();
		return withinDistance(camLoc, dolLoc, camDolMinProximity);
	}

	public boolean withinDistance(Vector3f v1, Vector3f v2, float dist){
		float dX = checkProximity(v1.x(), v2.x());
		float dY = checkProximity(v1.y(), v2.y());
		float dZ = checkProximity(v1.z(), v2.z());
		if (dX > dist || dY > dist || dZ>dist){
			return false;
		}
		return true;
	}


	public void placeCamBehindAv(){
		//get avatars position -val in the 
		Vector3f dolLoc = dol.getLocalLocation();
		Vector3f newLoc = dolLoc.add(new Vector3f(0,10f,0));
		Camera cam = (engine.getRenderSystem().getViewport("MAIN").getCamera());
		cam.setLocation(newLoc);
		cam.lookAt(dol);
	}

	public GameObject getTerrain(){
		return terr;
	}

	public GhostManager getGhostManager(){
		return this.gm;
	}

	public ObjShape getGhostShape(){
		return this.ghostShape;
	}

	public TextureImage getGhostTexture(){
		return this.doltx;
	}

	public Vector3f getPlayerPosition() { 
		return dol.getWorldLocation(); 
	}

	public void setIsConnected(boolean connected){
		this.isClientConnected = connected;
	}

	private void setupNetworking()
	{	isClientConnected = false;	
		try {	
			protClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
		} 	
		catch (UnknownHostException e) {	
			e.printStackTrace();
		}	
		catch (IOException e) {	
			e.printStackTrace();
		}
		if (protClient == null){	
			System.out.println("missing protocol host");
		}
		else{	
			// Send the initial join message with a unique identifier for this client
			System.out.println("sending join message to protocol host");
			protClient.sendJoinMessage();
		}
	}

	protected void processNetworking(float elapsTime)
	{	// Process packets received by the client from the server
		if (protClient != null)
			protClient.processPackets();
	}

	public ProtocolClient getProtClient(){
		return this.protClient;
	}

	public void createBall(Vector3f ballPosition){
		gameBall = new GameObject(GameObject.root(), ballS, prize);
		gameBall.setLocalLocation(ballPosition);
	}

	public Vector3f getBallLoc(){
		return gameBall.getLocalLocation();
	}

	public void setBallLoc(Vector3f loc){
		gameBall.setLocalLocation(loc);
	}

}