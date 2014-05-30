package net.clonecomputers.lab.percolation;

import java.util.*;

import com.carrotsearch.hppc.*;
import com.carrotsearch.hppc.cursors.*;

public class PercolationDisk {
	private LongSet donePoints = new LongOpenHashSet();
	private LongSet thisPoints;
	private LongSet nextPoints;
	private long area;
	private double radius;
	
	public void run(double p) {
		nextPoints = new LongOpenHashSet();
		nextPoints.add(point2D(0,0));
		for(int level = 0; !nextPoints.isEmpty(); level++) {
			thisPoints = nextPoints;
			nextPoints = new LongOpenHashSet();
			for(LongCursor p1: thisPoints) {
				donePoints.add(p1.value);
				area++;
				radius = Math.max(radius, Math.hypot(x(p1.value), y(p1.value)));
				for(Dir d: Dir.values()) {
					long p2 = d.inDir(p1.value);
					if(!donePoints.contains(p2) && Math.random() < p) nextPoints.add(p2);
				}
			}
			
			System.out.println(level+": "+area+" -> "+((double)area)/radius/radius);
		}
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

	public static void main(String[] args) {
		new PercolationDisk().run(.618033);
	}
}
