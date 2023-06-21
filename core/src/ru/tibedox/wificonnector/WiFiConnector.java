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

	Texture imgBackGround;

	TextButton btnCreateServer;
	TextButton btnCreateClient;
	TextButton btnSendData;
	TextButton btnExit;

	private InetAddress ipAddress;
	private String myIpAddress = "?";
	Server server;
	Client client;
	boolean isServerStarted;
	boolean isClientStarted;
	String receive, send;
	SampleRequest request;
	SampleResponse response;

	@Override
	public void create () {
		batch = new SpriteBatch();
		camera = new OrthographicCamera();
		camera.setToOrtho(false, SCR_WIDTH, SCR_HEIGHT);
		touch = new Vector3();
		createFont();
		keyboard = new InputKeyboard(SCR_WIDTH, SCR_HEIGHT, 10);

		imgBackGround = new Texture("swamp2.jpg");
		btnCreateServer = new TextButton(font, "Create Server", 50, 600);
		btnCreateClient = new TextButton(font, "Create Client", 50, 400);
		btnSendData = new TextButton(font, "Send Data", 50, 300);
		btnExit = new TextButton(font, "Exit", 50, 200);

	}

	@Override
	public void render() {
		// касания экрана
		if(Gdx.input.justTouched()){
			touch.set(Gdx.input.getX(), Gdx.input.getY(), 0);
			camera.unproject(touch);
			if(btnCreateServer.hit(touch.x, touch.y)) {
				if(!isServerStarted) {
					startServer();
					myIpAddress = detectIP();
					System.out.println("Server started");
				}
			}
			if(btnCreateClient.hit(touch.x, touch.y)){
				if(!isClientStarted) {
					startClient();
					myIpAddress = "192.168.1.139";
					System.out.println("Client started");
				}
			}
			if(btnSendData.hit(touch.x, touch.y)){
				SampleRequest request = new SampleRequest();
				request.text = "запрос";
				client.sendTCP(request);
			}
			if(btnExit.hit(touch.x, touch.y)){
				Gdx.app.exit();
			}
		}

		// события игры


		// вывод изображений
		camera.update();
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		batch.draw(imgBackGround, 0, 0, SCR_WIDTH, SCR_HEIGHT);
		btnCreateServer.font.draw(batch, btnCreateServer.text, btnCreateServer.x, btnCreateServer.y);
		font.draw(batch, "Server's IP: "+myIpAddress, btnCreateServer.x, btnCreateServer.y-100);
		btnCreateClient.font.draw(batch, btnCreateClient.text, btnCreateClient.x, btnCreateClient.y);
		btnSendData.font.draw(batch, btnSendData.text, btnSendData.x, btnSendData.y);
		btnExit.font.draw(batch, btnExit.text, btnExit.x, btnExit.y);
		font.draw(batch, "Принято: "+receive, 500, 450);
		font.draw(batch, "Отправлено: "+send, 500, 350);
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
						System.out.println("IP-адрес устройства: " + ipAddress.getHostAddress());
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
		kryo.register(SampleRequest.class);
		kryo.register(SampleResponse.class);

		server.addListener(new Listener() {
			public void received (Connection connection, Object object) {
				if (object instanceof SampleRequest) {
					SampleRequest request = (SampleRequest)object;
					//System.out.println(request.text);
					request.text += 1;
					receive = request.text;

					SampleResponse response = new SampleResponse();
					//System.out.println(response.text);
					response.text += 1;
					connection.sendTCP(response);
				}
			}
		});
		isServerStarted = true;
	}

	void startClient(){
		client = new Client();
		client.start();
		try {
			client.connect(5000, "192.168.1.139", 54555, 54777); // указываем IP-адрес и порты сервера
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		Kryo kryoClient = client.getKryo();
		kryoClient.register(SampleRequest.class);
		kryoClient.register(SampleResponse.class);

		SampleRequest request = new SampleRequest();
		request.text = "запрос";
		client.sendTCP(request);

		client.addListener(new Listener() {
			public void received (Connection connection, Object object) {
				if (object instanceof SampleResponse) {
					SampleResponse response = (SampleResponse)object;
					System.out.println(response.text);
					response.text += 1;
					receive = response.text;
				}
			}
		});
		isClientStarted = true;
	}
}

class SampleRequest {
	public String text;
}

class SampleResponse {
	public String text;
}