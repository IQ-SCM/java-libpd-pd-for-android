/**
 * 
 * @author Peter Brinkmann (peter.brinkmann@gmail.com)
 * 
 * For information on usage and redistribution, and for a DISCLAIMER OF ALL
 * WARRANTIES, see the file, "LICENSE.txt," in this distribution.
 *
 * Very simple top-level class that runs pd as an activity.  This is just a proof of concept; eventually,
 * we want to run pd as a service.
 *
 */

package org.puredata.android;

import java.io.IOException;
import java.util.Arrays;

import org.puredata.android.io.PdAndroidThread;
import org.puredata.core.PdBase;
import org.puredata.core.utils.PdDispatcher;
import org.puredata.core.utils.PdListener;
import org.puredata.core.utils.PdUtils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;


public class PdAndroidTest extends Activity {

	private class TestListener implements PdListener {
		private String tag;
		TestListener(String t) {
			tag = "Pd " + t;
		}
		@Override
		public void receiveSymbol(String symbol) {
			Log.i(tag, "symbol: " + symbol);
		}
		@Override
		public void receiveMessage(String symbol, Object[] args) {
			Log.i(tag, "symbol: " + symbol + ", args: " + Arrays.toString(args));
		}
		@Override
		public void receiveList(Object[] args) {
			Log.i(tag, "list: " + Arrays.toString(args));
		}
		@Override
		public void receiveFloat(float x) {
			Log.i(tag, "float: " + x);
		}
		@Override
		public void receiveBang() {
			Log.i(tag, "bang!");
		}
	}
	
	private String patch = null;
	private PdDispatcher dispatch = new PdDispatcher() {
		@Override
		public void print(String s) {
			Log.i("Pd Dispatch", s);
		}
	};
	private WakeLock wakeLock = null;

	@Override
	protected void onStart() {
		super.onStart();
		PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "pd wakelock");
		dispatch.addListener("spam", new TestListener("Spam"));
		dispatch.addListener("eggs", new TestListener("Eggs"));
		PdBase.setReceiver(dispatch);
		Resources res = getResources();
		try {
			patch = PdUtils.openPatch(res.getString(R.string.patch), res.getString(R.string.folder));
		} catch (IOException e) {
			throw new RuntimeException(e.toString());
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		setContentView(R.layout.main);
		wakeLock.acquire();
		Resources res = getResources();
		try {
			PdAndroidThread.startThread(res.getInteger(R.integer.sampleRate),
					res.getInteger(R.integer.inChannels),
					res.getInteger(R.integer.outChannels),
					res.getInteger(R.integer.ticksPerBuffer), false);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		PdBase.sendBang("foo");
		PdBase.sendFloat("foo", 3.1415f);
		PdBase.sendSymbol("bar", "elephant");
		PdBase.sendList("bar", new Object[] { new Integer(5), "katze", new Float(1.414) });
		PdBase.sendMessage("bar", "boing", new Object[] { new Integer(5), "katze", new Float(1.414) });
	}

	@Override
	protected void onPause() {
		super.onPause();
		PdAndroidThread.stopThread();
		wakeLock.release();
	}

	protected void onStop() {
		super.onStop();
		PdUtils.closePatch(patch);
		dispatch.release();
		PdBase.release();
	}
}
