package de.srsoftware.gui.treepanel;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import de.srsoftware.formula.FormulaFont;
import de.srsoftware.tools.ObjectComparator;

public class RootTreePanel extends TreePanel {

	private static final long serialVersionUID = 1L;
	private int dist = 10;
	protected int distance = 50; // Basis-Distanz zwischen den Knoten des Mindmap
	
	public RootTreePanel() {
		super();
		TreeNode.setCentered(false);
	}
	
	public RootTreePanel(TreePanel mindmapPanel) {
		this();
		setParametersFrom(mindmapPanel);
	}

	public void paint(Graphics g) {
		super.paint(g);
		if (tree != null) {
			boolean wasFolded=tree.isFolded();
			tree.setFolded(false);
			if (tree.nodeFile() != null) {
				try {
					tree.loadFromFile();
				} catch (FileNotFoundException e) {
					System.out.println(_("File not found: ")+ e.getMessage());
				} catch (IOException e) {
					System.out.println(_("Error while loading: ") + e.getMessage());
				} catch (DataFormatException e) {
					System.out.println(_("File type not supported: ") + e.getMessage());
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
			}
			TreeNode child;
			FormulaFont ff = new FormulaFont(g.getFont()).withSize(fontSize);
			Dimension d=tree.nodeDimension(g, this,ff);
			Point center=new Point((getWidth()-d.width)/3,getHeight()/2);
			tree.moveTowards(center);
			if (wasFolded){
				child=tree.firstChild();
				while (child!=null) {
					child.setOrigin(tree.getOrigin());
					child=child.next();
				}
			}
			if (!updatedSinceLastChange) tree.resetDimension();
			Point leftCenter=tree.getOrigin();
			leftCenter.y+=d.height/2;
			d.height=paint((Graphics2D)g,tree,ff, true).height;
			paintFamily((Graphics2D) g,tree,leftCenter,d, ff);
		}
	}

	@Override
	public void toogleFold() {
		TreeNode child = tree.firstChild();
		while (child!=null) {
			child.setFolded(true);
			child=child.next();
		}
		updateView();
	}

	private void drawConnection(Graphics gr, Point p1, Point p2) {
		Graphics2D g = (Graphics2D) gr;
		Stroke str = g.getStroke();
		g.setStroke(new BasicStroke(g.getFont().getSize()/5));
		g.setColor(connectionColor);
		int dx=p2.x-p1.x;
		int dy=p1.y-p2.y;
		if (dy>0){
			g.drawArc(p1.x-dx/2, p2.y, dx, dy, 270, 90);
			g.drawArc(p1.x+dx/2, p2.y, dx, dy, 90, 90);
		} else {
			g.drawArc(p1.x-dx/2, p1.y, dx, -dy, 0, 90);
			g.drawArc(p1.x+dx/2, p1.y, dx, -dy, 180, 90);
		}
		g.setStroke(str);
	}

	private Dimension paint(Graphics2D g, TreeNode mindmap,FormulaFont font, boolean draw) {
		if (!this.contains(mindmap.getOrigin())){
			mindmap.setFolded(true);
			return new Dimension(0,0);
		}
		if (!updatedSinceLastChange) mindmap.resetDimension();
		Dimension ownDim = mindmap.nodeDimension(g, this,font);
		if (draw) mindmap.paint(g, this,font); 
		font=font.scale(0.8f);

		Dimension childDim = mindmap.isFolded()?new Dimension(0,0):paintChildren(g, null, mindmap, font, false);
		Dimension result = new Dimension(ownDim.width + childDim.width + distance, Math.max(ownDim.height, childDim.height));
		if (draw) {
			Point leftCenter = mindmap.getOrigin();
						
			leftCenter.x += ownDim.width;
			leftCenter.y += ownDim.height/2;
			if (mindmap.isFolded()){ 
				Stroke str = g.getStroke();
				g.setStroke(new BasicStroke(g.getFont().getSize()/5));
				g.drawOval(leftCenter.x, leftCenter.y-dist/2, 7, 7);
				g.setStroke(str);
			} else paintChildren(g, leftCenter, mindmap, font,true);
		}
		return result;
	}

	private Dimension paintChildren(Graphics2D g, Point leftCenter, TreeNode mindmap, FormulaFont font, boolean draw) {
		int width = 0;
		int height = dist;
		TreeNode child = mindmap.firstChild();
		while (child != null) {
			Dimension d = paint(g,child, font,false);
			height += d.height+dist;
			width = Math.max(width, d.width);
			child = child.next();
		}
		if (draw) {
			height-=dist;
			Point currentOrigin=new Point(leftCenter);
			currentOrigin.y-=height/2;
			currentOrigin.x+=distance;
			child = mindmap.firstChild();
			while (child != null) {
				Dimension d=paint(g,child,font,false);
				Dimension d2=child.nodeDimension(g, this,font);
				Point target=new Point(currentOrigin);
				target.y+=(d.height-d2.height)/2;
				child.moveTowards(target);
				paint(g,child,font, true);
				currentOrigin.y+=d.height+dist;
				Point p=child.getOrigin();
				p.y+=d2.height/2;
				p.x-=2;
				drawConnection(g,leftCenter,p);
				child = child.next();
			}
		}
		return new Dimension(width, height);
	}

	private void paintFamily(Graphics2D g, TreeNode mindmap,Point leftCenter, Dimension mindmapDimension,FormulaFont font) {
		TreeNode parent=mindmap.parent();
		TreeSet<Point> points=new TreeSet<Point>(ObjectComparator.get());
		if (parent!=null){
			int x=leftCenter.x;
			int y=leftCenter.y;
			mindmap.resetDimension();
			Dimension mindmapDim = mindmap.nodeDimension(g, this,font);
			Point p=mindmap.getOrigin();
			p.y+=mindmapDim.height/2;
			points.add(p);
			
			TreeNode sibling=mindmap;
			int height=mindmapDimension.height;
			y+=(mindmapDimension.height)/2+dist;
			while ((sibling=sibling.next())!=null){
				Dimension siblingDim=sibling.nodeDimension(g, this,font);
				sibling.moveTowards(x, y);
				p=sibling.getOrigin();
				p.y+=siblingDim.height/2;
				points.add(p);
				sibling.paint(g, this,font);
				if (sibling.firstChild()!=null){ 
					Stroke str = g.getStroke();
					g.setStroke(new BasicStroke(g.getFont().getSize()/5));
					g.drawOval(sibling.getOrigin().x+siblingDim.width, sibling.getOrigin().y+siblingDim.height/2-5, 7, 7);
					g.setStroke(str);
				}
				height+=siblingDim.height+dist;
				y+=siblingDim.height+dist;
			}
			
			sibling=mindmap;
			y=leftCenter.y-(mindmapDimension.height)/2;
			
			while ((sibling=sibling.prev())!=null){
				Dimension siblingDim=sibling.nodeDimension(g, this,font);
				height+=siblingDim.height+dist;
				y-=siblingDim.height+dist;
				sibling.moveTowards(x, y);
				p=sibling.getOrigin();
				p.y+=siblingDim.height/2;
				points.add(p);
				sibling.paint(g, this,font);
				
				if (sibling.firstChild()!=null){ 
					Stroke str = g.getStroke();
					g.setStroke(new BasicStroke(g.getFont().getSize()/5));
					g.drawOval(sibling.getOrigin().x+siblingDim.width, sibling.getOrigin().y+siblingDim.height/2-5, 7, 7);
					g.setStroke(str);
				}
				
			}			
			mindmapDimension=new Dimension(10,height);
			parent.resetDimension();
			Dimension parentDimension=parent.nodeDimension(g, this,font);
			leftCenter.y=y+height/2+parentDimension.height;
			leftCenter.x-=parentDimension.width+distance;
			parent.moveTowards(leftCenter);
			p=parent.getOrigin();
			p.x+=parentDimension.width;
			p.y+=parentDimension.height/2;
			for (Iterator<Point> it = points.iterator();it.hasNext();) drawConnection(g, p, it.next());
			parent.paint(g, this,font);
			paintFamily(g, parent, leftCenter, mindmapDimension,font);
		}
	}
}
