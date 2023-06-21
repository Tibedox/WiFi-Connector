package ru.tibedox.wificonnector;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Vector3;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class WiFiConnector extends ApplicationAdapter {
	public static final float SCR_WIDTH = 1280;
	public static final float SCR_HEIGHT = 720;
	
	SpriteBatch batch; 
	OrthographicCamera camera;
	Vector3 touch;
	BitmapFont font;
	InputKeyboard keyboard;
	boolean isEnterIP;

	Texture imgBackGround;
	Texture imgRed;
	Texture imgBlue;

	TextButton btnCreateServer;
	TextButton btnCreateClient;
	TextButton btnExit;

	private InetAddress ipAddress;
	private String ipAddressOfServer = "?";
	Server server;
	Client client;
	boolean isServer;
	boolean isClient;
	Request requestRedClient;
	Response responseBlueServer;

	@Override
	public void create () {
		batch = new SpriteBatch();
		camera = new OrthographicCamera();
		camera.setToOrtho(false, SCR_WIDTH, SCR_HEIGHT);
		touch = new Vector3();
		createFont();
		keyboard = new InputKeyboard(SCR_WIDTH, SCR_HEIGHT, 15);

		imgBackGround = new Texture("swamp.jpg");
		imgRed = new Texture("circlered.png");
		imgBlue = new Texture("circleblue.png");

		btnCreateServer = new TextButton(font, "Create Server", 100, 600);
		btnCreateClient = new TextButton(font, "Create Client", 100, 400);
		btnExit = new TextButton(font, "Exit", 100, 100);

		requestRedClient = new Request();
		responseBlueServer = new Response();
	}

	@Override
	public void render() {
		// касания экрана
		if(Gdx.input.justTouched()){
			touch.set(Gdx.input.getX(), Gdx.input.getY(), 0);
			camera.unproject(touch);

			if(btnCreateServer.hit(touch.x, touch.y) && !isServer && !isClient && !isEnterIP) {
				startServer();
				ipAddressOfServer = detectIP();
			}
			if(btnCreateClient.hit(touch.x, touch.y) && !isServer && !isClient && !isEnterIP){
				isEnterIP = true;
			}
			if(isEnterIP && keyboard.endOfEdit(touch.x, touch.y)) {
				isEnterIP = false;
				ipAddressOfServer = keyboard.getText();
				if(!startClient()){
					isClient = false;
					ipAddressOfServer = "Server not found";
				}
			}
			if(btnExit.hit(touch.x, touch.y) && !isEnterIP){
				Gdx.app.exit();
			}
		}
		if(Gdx.input.isTouched() && !isEnterIP) {
			touch.set(Gdx.input.getX(), Gdx.input.getY(), 0);
			camera.unproject(touch);
		}

		// события игры
		if(isClient){
			requestRedClient.text = "red: ";
			requestRedClient.x = touch.x;
			requestRedClient.y = touch.y;
			client.sendTCP(requestRedClient);
		}
		if(isServer){
			responseBlueServer.text = "blue: ";
			responseBlueServer.x = touch.x;
			responseBlueServer.y = touch.y;
		}

		// вывод изображений
		camera.update();
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		batch.draw(imgBackGround, 0, 0, SCR_WIDTH, SCR_HEIGHT);
		batch.draw(imgRed, requestRedClient.x-50, requestRedClient.y-50, 100, 100);
		batch.draw(imgBlue, responseBlueServer.x-50, responseBlueServer.y-50, 100, 100);
		font.draw(batch, "Server "+ responseBlueServer.text+ (int)responseBlueServer.x+" "+ (int)responseBlueServer.y, 100, 300);
		font.draw(batch, "Client "+ requestRedClient.text+ (int)requestRedClient.x+" "+ (int)requestRedClient.y, 100, 200);

		btnCreateServer.font.draw(batch, btnCreateServer.text, btnCreateServer.x, btnCreateServer.y);
		font.draw(batch, "Server's IP: "+ ipAddressOfServer, btnCreateServer.x, btnCreateServer.y-100);
		btnCreateClient.font.draw(batch, btnCreateClient.text, btnCreateClient.x, btnCreateClient.y);
		btnExit.font.draw(batch, btnExit.text, btnExit.x, btnExit.y);
		if(isEnterIP) {
			keyboard.draw(batch);
		}

		batch.end();
	}

	void createFont(){
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("ubuntumono.ttf"));
		FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
		parameter.characters = "абвгдеёжзийклмнопрстуфхцчшщъыьэюяabcdefghijklmnopqrstuvwxyzАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789][_!$%#@|\\/?-+=()*&.;:,{}\"´`'<>";
		parameter.size = 50;
		parameter.color = Color.ORANGE;
		parameter.borderWidth = 3;
		parameter.borderColor = Color.BLACK;
		font = generator.generateFont(parameter);
	}

	@Override
	public void dispose () {
		batch.dispose();
		keyboard.dispose();
		font.dispose();
		imgBackGround.dispose();
		imgRed.dispose();
		imgBlue.dispose();
	}

	public String detectIP() {
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface networkInterface = interfaces.nextElement();
				Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress address = addresses.nextElement();
					if (!address.isLinkLocalAddress() && !address.isLoopbackAddress() && address.getHostAddress().indexOf(":") == -1) {
						ipAddress = address;
						//System.out.println("IP-адрес устройства: " + ipAddress.getHostAddress());
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}

		if(ipAddress != null){
			return ipAddress.getHostAddress();
		}
		return "";
	}

	void startServer(){
		// отсюда https://www.thetechgame.com/Archives/p=37396394.html
		server = new Server();
		server.start();
		try {
			server.bind(54555, 54777); // указываем порты для прослушивания
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		Kryo kryo = server.getKryo();
		kryo.register(Request.class);
		kryo.register(Response.class);

		server.addListener(new Listener() {
			public void received (Connection connection, Object object) {
				if (object instanceof Request) {
					requestRedClient = (Request)object;
					connection.sendTCP(responseBlueServer);
				}
			}
		});
		isServer = true;
		isClient = false;
	}

	boolean startClient(){
		client = new Client();
		client.start();
		try {
			client.connect(5000, ipAddressOfServer, 54555, 54777); // указываем IP-адрес и порты TCP и UDP сервера
		} catch (IOException e) {
			//throw new RuntimeException(e);
			return false;
		}

		Kryo kryoClient = client.getKryo();
		kryoClient.register(Request.class);
		kryoClient.register(Response.class);

		client.addListener(new Listener() {
			public void received (Connection connection, Object object) {
				if (object instanceof Response) {
					responseBlueServer = (Response)object;
				}
			}
		});
		isClient = true;
		isServer = false;
		return true;
	}
}

class Request {
	public String text = "";
	public float x, y;
}

class Response {
	public String text = "";
	public float x, y;
}