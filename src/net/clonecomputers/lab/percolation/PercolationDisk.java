package net.clonecomputers.lab.percolation;

import net.clonecomputers.lab.util.*;

import java.util.*;

import com.carrotsearch.hppc.*;

public class PercolationDisk {
	private Set<Point2D> donePoints = new HashSet<Point2D>(100);
	private Set<Point2D> thisPoints;
	private Set<Point2D> nextPoints;
	private long area;
	
	public void run(double p) {
		nextPoints = new HashSet<Point2D>();
		nextPoints.add(new Point2D(0,0));
		for(int level = 0; true; level++) {
			thisPoints = nextPoints;
			nextPoints = new HashSet<Point2D>();
			
			for(Point2D p1: thisPoints) {
				donePoints.add(p1);
				for(Dir d: Dir.values()) {
					Point2D p2 = d.inDir(p1);
					if(!donePoints.contains(p2) && Math.random() < p) nextPoints.add(p2);
				}
			}
			
			area += thisPoints.size();
			System.out.println(level+": "+((double)area)/level/level);
		}
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
		
		public Point2D inDir(Point2D p) {
			return new Point2D(p.x + dx, p.y + dy);
		}
	}

	public static void main(String[] args) {
		new PercolationDisk().run(.7);
	}
}
