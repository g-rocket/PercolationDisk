package net.clonecomputers.lab.percolation;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import com.carrotsearch.hppc.*;
import com.carrotsearch.hppc.cursors.*;

public class PercolationDisk {
	private LongSet donePoints = new LongOpenHashSet();
	private LongSet thisPoints;
	private LongSet nextPoints;
	private long area;
	private double radius;
	private double pi;
	
	public double calculatePi(double p) {
		nextPoints = new LongOpenHashSet();
		nextPoints.add(point2D(0,0));
		for(int level = 0; !nextPoints.isEmpty() && level < 2000; level++) {
			thisPoints = nextPoints;
			nextPoints = new LongOpenHashSet();
			for(LongCursor p1: thisPoints) {
				try {
					donePoints.add(p1.value);
				} catch (OutOfMemoryError e) {
					return pi;
				}
				area++;
				radius = Math.max(radius, Math.hypot(x(p1.value), y(p1.value)));
				for(Dir d: Dir.values()) {
					long p2 = d.inDir(p1.value);
					if(!donePoints.contains(p2) && Math.random() < p) nextPoints.add(p2);
				}
			}
			pi = ((double)area)/radius/radius;
			//System.out.printf("%d: %.4f\n",level,pi);
		}
		return pi;
	}
	
	private static long point2D(int x, int y) {
		return (((long)x) << 32) | (y & 0xffffffffL);
	}
	
	private static int x(long point) {
		return (int)(point >> 32);
	}
	
	private static int y(long point) {
		return (int)point;
	}

	private enum Dir {
		UP(0,1),
		DOWN(0,-1),
		LEFT(-1,0),
		RIGHT(1,0);
		
		private final int dx, dy;
		private Dir(int dx, int dy) {
			this.dx = dx;
			this.dy = dy;
		}
		
		public long inDir(long p) {
			return point2D(x(p)+dx,y(p)+dy);
		}
	}
	
	static volatile int x;
	static volatile boolean shouldStop = false;
	public static void main(String[] args) throws IOException {
		if(args.length == 0) {
			System.out.println(new PercolationDisk().calculatePi(.618033));
		}
		System.out.println("type 'stop' to stop");
		File saveFile = new File(System.getProperty("user.home"), "percolationDisk.csv");
		saveFile.createNewFile();
		Writer save = new FileWriter(saveFile);
		ExecutorService exec = Executors.newCachedThreadPool();
		exec.execute(new Runnable() {
			@Override public void run() {
				BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
				while(true) {
					try {
						String s = in.readLine();
						if(s != null && s.equalsIgnoreCase("stop")) {
							shouldStop = true;
							break;
						}
					} catch (IOException e) {
						//if(e.getMessage().equals("Stream closed")) break; // expected (not any more)
						throw new RuntimeException(e);
					}
					Thread.yield();
				}
			}
		});
		PercolationDisk useless = new PercolationDisk();
		int i;
		outer:
		for(i = 1; true; i++) {
			x++;
			exec.execute(new PercolationRunner(i,save));
			do {
				if(shouldStop) break outer;
				Thread.yield();
			} while(x >= 4);
		}
		System.out.println("waiting for tasks to terminate (started "+i+")");
		//System.in.close();
		exec.shutdown();
		save.flush();
	}

	private static final double log2 = Math.log(2);
	private static class PercolationRunner implements Runnable {
		private final int i;
		private final Writer save;
		private PercolationRunner(int i, Writer save) {
			this.i = i;
			this.save = save;
		}
		
		@Override public void run() {
			try {
				double level = Math.pow(2, Math.floor(Math.log(i) / log2));
				double p = (2*(i - level) + 1) / (level*2);
				double d = new PercolationDisk().calculatePi(p);
				if(!Double.isInfinite(d)) synchronized(save) {
					save.write(p+", "+d+"\n");
					save.flush();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			System.out.println(i+" done");
			x--;
		}
	}
}
