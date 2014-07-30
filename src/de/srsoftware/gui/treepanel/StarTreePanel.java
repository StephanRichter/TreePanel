package de.srsoftware.gui.treepanel;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.zip.DataFormatException;

public class StarTreePanel extends TreePanel {
	private static final long serialVersionUID = -3898710876180470991L;
	public static double cos(double pDegree) {
		return Math.cos(Math.PI * pDegree / 180);
	}
	public static double sin(double pDegree) {
		return Math.sin(Math.PI * pDegree / 180);
	}

	private int levelLimit = 5; // Zahl der maximalen Tiefe, die ausgehend vom aktuellen Knoten angezeigt wird

	private float parentDistanceFactor = 1.7f;

	public StarTreePanel() {
		super();
		TreeNode.setCentered(true);
	}

	public StarTreePanel(TreePanel mindmapPanel) {
		this();
		setParametersFrom(mindmapPanel);
	}

	public boolean organize() {
		float startangle = 50f;
		if (tree.parent() == null && tree.getNumChildren() == 1) startangle += 180f;
		Point middle=new Point(this.getWidth() / 2, this.getHeight() / 2);
		organize(tree, null, middle, distance, startangle, 0);
		return true;
	}

	public void paint(Graphics g) {
		super.paint(g);
		paint((Graphics2D)g, tree, null, 0,fontSize);
		updatedSinceLastChange = true;
	}

	public void repaint() {
		if (tree != null) organize();
		super.repaint();
	}

	@Override
	public void toogleFold() {
	}

	private void drawConnection(Graphics g, int x, int y, int x2, int y2) {
		g.setColor(connectionColor);
		g.drawLine(x, y, x2, y2);
	}

	private Point getTargetPos(Point origin, double angle, int dist) {
		int x = (int) (origin.x + 2 * dist * -cos(angle));
		int y = (int) (origin.y + dist * -sin(angle));
		return new Point(x, y);
	}

	private void organize(TreeNode node, TreeNode comingFrom, Point origin, int distance, double angle, int level) {
		if (level < fileLoadLevelLimit) {
			if (node.nodeFile() != null) {
				try {
					node.loadFromFile();
				} catch (FileNotFoundException e) {
					System.out.println(_("File not found: ") + e.getMessage());
				} catch (IOException e) {
					System.out.println(_("Error while loading: ") + e.getMessage());
				} catch (DataFormatException e) {
					System.out.println(_("File type not supported: ") + e.getMessage());
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
			}
		}
		if (level <= levelLimit) {			
			boolean hasParent = node.parent() != null;
			
			/** calculate angle between linked nodes **/
			int numLinks = node.getNumChildren() + ((hasParent) ? 1 : 0);
			double angleDiff = (numLinks == 0) ? 0 : 360 / numLinks;

			if (comingFrom == null || comingFrom == node.parent()) {
				if (hasParent && (node.parent() != comingFrom)) {
					Point targetPos = getTargetPos(origin, angle, (int) (distance * parentDistanceFactor));
					organize(node.parent(), node, targetPos, distance / 3, angle + 180, level + 1);
				}
				angle += angleDiff;

				TreeNode child = node.firstChild();
				while (child != null) {
					if (child != comingFrom) {
						Point targetPos = getTargetPos(origin, angle, distance);
						organize(child, node, targetPos, distance / 3, angle + 180, level + 1);
						angle += angleDiff;
					}
					child = child.next();
				}
			} else { // comingFrom ist eines der Kinder
				TreeNode child = comingFrom.next();
				angle += angleDiff;
				while (child != null) {
					if (child != comingFrom) {
						Point targetPos = getTargetPos(origin, angle, distance);
						organize(child, node, targetPos, distance / 3, angle + 180, level + 1);
						angle += angleDiff;
					}
					child = child.next();
				}
				if (hasParent) {
					Point targetPos = getTargetPos(origin, angle, (int) (distance * parentDistanceFactor));
					organize(node.parent(), node, targetPos, distance / 3, angle + 180, level + 1);
					angle += angleDiff;
				}
				child = comingFrom;
				while (child.prev() != null)
					child = child.prev();
				while (child != null && child != comingFrom) {
					if (child != comingFrom) {
						Point targetPos = getTargetPos(origin, angle, distance);
						organize(child, node, targetPos, distance / 3, angle + 180, level + 1);
						angle += angleDiff;
					}
					child = child.next();
				}

			}
		}
		node.moveTowards(origin);
	}

	private void paint(Graphics2D g, TreeNode node, TreeNode doNotTraceThis, int level,float fontSize) {
		if (node != null && level < levelLimit) {
			if (doNotTraceThis != null) {
				if (doNotTraceThis.parent() == node) {
					fontSize*=5/6;
				} else
					fontSize/=2;
			}
			Point origin = node.getOrigin();
			/*
			 * MindmapNode dummy = node.firstChild(); while (dummy != null) { if (dummy != doNotTraceThis) { Point org = dummy.getOrigin(); if (level < levelLimit - 1) g.drawLine(origin.x, origin.y, org.x, org.y); paint(g, dummy, node, level + 1); } dummy = dummy.next(); } dummy = node.parent(); if (dummy != null && dummy != doNotTraceThis) { Point org = dummy.getOrigin(); if (level < levelLimit - 1) g.drawLine(origin.x, origin.y, org.x, org.y); paint(g, dummy, node, level + 1); }
			 */
			
			TreeNode dummy = node.lastChild();
			while (dummy!=null) {
				if (dummy != doNotTraceThis) {
					Point org = dummy.getOrigin();
					g.setStroke(new BasicStroke(g.getFont().getSize()/4));
					if (level < levelLimit - 1) drawConnection(g,origin.x, origin.y, org.x, org.y);
					paint(g, dummy, node, level + 1,fontSize);
				}
				dummy = dummy.prev();
			}
			dummy = node.parent();
			if (dummy != null && dummy != doNotTraceThis) {
				Point org = dummy.getOrigin();
				g.setStroke(new BasicStroke(g.getFont().getSize()/3));
				if (level < levelLimit - 1) drawConnection(g,origin.x, origin.y, org.x, org.y);
				paint(g, dummy, node, level + 1,fontSize);
			}
			g.setColor(connectionColor);
			g.setStroke(new BasicStroke(g.getFont().getSize()/7));
			if (!updatedSinceLastChange) node.resetDimension();
			if (level < 2)
				node.paint(g, this,fontSize);
			else
				node.paintWithoutImages(g, this,fontSize);
		}
	}

}
