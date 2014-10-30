package ca.mcgill.cpslab.cursorsimulator;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.ImageView;

public class CursorService extends Service {
	private static final String MYTAG = "CursorService";
	public static final int SERVER_PORT = 10130;
	private static WindowManager wm;
	private Thread serverThread;
	private ServerSocket serverSocket;
	private Socket updaterSocket;
	private int counter = 0;
	/**
	 * The handler for main UI thread
	 */
	private Handler mHandler = new Handler();
	/**
	 * The repeating routine that updates the cursor position
	 */
	private Runnable cursorUpdater = new Runnable() {
		private final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
				WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
				PixelFormat.TRANSLUCENT);
		private ImageView mView;

		@Override
		public void run() {
			// initialize the view, place it on top of everyone
			if (counter == 0) {
				mView = new ImageView(CursorService.this);
				Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.colored);
				mView.setImageBitmap(bm);
				params.width = 64;
				params.height = 64;
				params.gravity = Gravity.TOP;
				params.x = 300;
				params.y = 600;
				wm.addView(mView, params);
			}
			// repeat 10 times
			if (counter <= 10) {
				mHandler.postDelayed(cursorUpdater, 1000);
				counter++;
				params.x = 20 + counter * 20;
				wm.updateViewLayout(mView, params);
			} else {
				counter = 0;
				wm.removeViewImmediate(mView);
			}
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
	 * Shutdown the cursor updater repeating task
	 */
	synchronized public void shutdownUpdater() {
		if (updaterSocket != null) {
			try {
				updaterSocket.close();
			} catch (IOException e) {
			}
			updaterSocket = null;
		}
	}
	
	/**
	 * A one-shot server socket. Establish only one connection and
	 * then starts the CursorUpdater for cursor position updates. 
	 */
	class MySocketServer implements Runnable {
		@Override
		public void run() {
			try {
				serverSocket = new ServerSocket(SERVER_PORT);
				updaterSocket = serverSocket.accept();
				closeServer();
				cursorUpdater.run();
			} catch (IOException e) {
				Log.d(MYTAG, "Server socket exits");
				return ;
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
		cursorUpdater.run();
		serverThread = new Thread(new MySocketServer());
		serverThread.start();
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		closeServer();
		serverThread.interrupt();
		mHandler.removeCallbacks(cursorUpdater);
		shutdownUpdater();
	}
	
}
