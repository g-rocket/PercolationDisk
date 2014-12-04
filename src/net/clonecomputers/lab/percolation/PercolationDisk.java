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
	private long perimeter;
	private boolean isValid;
	private double p;
	private Writer output;
	
	public PercolationDisk(double p) {
		this(p, null);
	}
	
	public PercolationDisk(double p, Writer output) {
		this.p = p;
		this.output = output;
	}

	public void calculate() {
		nextPoints = new LongOpenHashSet();
		nextPoints.add(point2D(0,0));
		isValid = false;
		for(int level = 0; !nextPoints.isEmpty() && level <= 2500; level++) {
			thisPoints = nextPoints;
			nextPoints = new LongOpenHashSet();
			perimeter = 0;
			for(LongCursor p1: thisPoints) {
				try {
					donePoints.add(p1.value);
				} catch (OutOfMemoryError e) {
					isValid = true;
					return;
				}
				area++;
				perimeter++;
				radius = Math.max(radius, Math.hypot(x(p1.value), y(p1.value)));
				for(Dir d: Dir.values()) {
					long p2 = d.inDir(p1.value);
					if(!donePoints.contains(p2) && Math.random() < p) nextPoints.add(p2);
				}
			}
			if(level%100 == 0 && output != null) {
				synchronized (output) {
					try {
						output.append(String.format("%f, %d, %f, %d, %d\n", p, level, radius, area, perimeter));
						output.flush();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
			//System.out.printf("%d: %.4f\n",level,pi);
		}
		if(nextPoints.isEmpty()) {
			isValid = false;
			return;
		}
		isValid = true;
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
		if(args.length == 1) {
			System.out.println(new PercolationDisk(Double.parseDouble(args[0]), null).calculatePi());
			System.exit(0);
		}
		System.out.println("type 'stop' to stop");
		File saveFile;
		int n = 0;
		do {
			saveFile = new File(System.getProperty("user.home"), "percolationDisk"+n+++".csv");
		} while(!saveFile.createNewFile());
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
		PercolationDisk useless = new PercolationDisk(0, null);
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

	private double calculatePi() {
		calculate();
		return ((double)area)/radius/radius;
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
			double level = Math.pow(2, Math.floor(Math.log(i) / log2));
			double p = (2*(i - level) + 1) / (level*2);
			PercolationDisk perc = new PercolationDisk(p, save);
			perc.calculate();
			System.out.println(i+" done");
			x--;
		}
	}
}
