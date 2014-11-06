package ca.mcgill.cpslab.cursorsimulator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.ImageView;

public class CursorService extends Service {
	private static final int UPDATE_IMAGE = 100;
	private static final String MYTAG = "CursorService";
	public static final int SERVER_PORT = 10130;
	private static WindowManager wm;
	private Thread serverThread;
	private ServerSocket serverSocket;
	private Handler handler = new Handler(Looper.getMainLooper()) {
		private final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
				WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
				PixelFormat.TRANSLUCENT);
		private ImageView mView;
		private boolean firstTime = true;
		@Override
		  public void handleMessage(Message msg) {
		    if(msg.what == UPDATE_IMAGE){
				int x = msg.arg1;
				int y = msg.arg2;
				Log.d(MYTAG, "x="+x+" y="+y);
				if (firstTime) {
					mView = new ImageView(CursorService.this);
					Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.colored);
					mView.setImageBitmap(bm);
					params.width = 64;
					params.height = 64;
					params.gravity = Gravity.TOP | Gravity.START;
					params.x = x;
					params.y = y;
					wm.addView(mView, params);
					firstTime = false;
				} else {
					params.x = x;
					params.y = y;
					wm.updateViewLayout(mView, params);
				}	      
		    }
		    super.handleMessage(msg);
		  }
	};
	
	/**
	 * Close the server socket that is listening
	 */
	synchronized public void closeServer() {
		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			serverSocket = null;
		}
	}
	
	/**
	 * A one-shot server socket. Establish only one connection and
	 * then starts the CursorUpdater for cursor position updates. 
	 */
	class MySocketServer extends Thread {
		@Override
		public void run() {
			while (true) {
				ObjectInputStream ois = null;
				try {
					serverSocket = new ServerSocket(SERVER_PORT);
					Socket updaterSocket = serverSocket.accept();
					Log.d(MYTAG, "connected");
					ois = new ObjectInputStream(updaterSocket.getInputStream());
				} catch (IOException e) {
					Log.d(MYTAG, "Server socket exits");
					continue;
				}
				try {
					while (true) {
						int x = ois.readInt();
						int y = ois.readInt();
						Message msg = handler.obtainMessage();
					    msg.what = UPDATE_IMAGE;
					    msg.arg1 = x;
					    msg.arg2 = y;
					    handler.sendMessage(msg);
					}
				} catch (IOException e) {
					Log.d(MYTAG, "Updater finishes");
					return ;
				}
			}
		}
		
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		wm = (WindowManager) getSystemService(WINDOW_SERVICE);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		serverThread = new MySocketServer();
		serverThread.start();
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		closeServer();
		serverThread.interrupt();
	}
	
}
