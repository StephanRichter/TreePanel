package de.srsoftware.gui.treepanel;

import java.awt.Color;
import java.awt.Dimension;
import java.net.URL;

import de.srsoftware.formula.Formula;
import de.srsoftware.formula.FormulaFont;

public class NodeContent {
	private Formula formula;
	private Color backgroundColor = null; // color for filling the nodes
	private NodeImage nodeImage = null; // holds the node's image, if given
	private Color foregroundColor = null; // color for text
	private URL link = null; // holds a given link
	private boolean nodeFileHasBeenLoaded = false; // is set to false, for nodes, that only point to files, and to true, if the file has been loaded

	public NodeContent clone() {
		NodeContent clone = new NodeContent(formula);
		clone.backgroundColor = backgroundColor;
		clone.foregroundColor = foregroundColor;
		clone.nodeImage = nodeImage.clone();
		clone.link = link;
		clone.nodeFileHasBeenLoaded = clone.nodeFileHasBeenLoaded;
		return clone;
	}

	public NodeContent(Formula formula) {
		setFormula(formula);
		setForegroundColor(Color.black);
		setBackgroundColor(Color.white);
		nodeFileHasBeenLoaded = false;
	}

	public NodeContent(String text) {
		this((text == null) ? null : new Formula(text));
	}

	public Formula getFormula() {
		return formula;
	}

	public void setFormula(Formula formula) {
		this.formula = formula;
	}

	public void setFormula(String text) {
		this.formula = new Formula(text);
	}

	public Color getBackgroundColor() {
		return backgroundColor;
	}

	public boolean setBackgroundColor(Color newColor) {
		if (backgroundColor != newColor) {
			backgroundColor = newColor;
			return true;
		}
		return false;
	}

	public NodeImage getNodeImage() {
		return nodeImage;
	}

	public void setNodeImage(NodeImage nodeImage) {
		this.nodeImage = nodeImage;
	}

	public Color getForegroundColor() {
		return foregroundColor;
	}

	public boolean setForegroundColor(Color newColor) {
		if (foregroundColor != newColor) {
			foregroundColor = newColor;
			return true;
		}
		return false;
	}

	public URL getLink() {
		return link;
	}

	public void setLink(URL link) {
		this.link = link;
	}

	public boolean hasBeenLoaded() {
		return nodeFileHasBeenLoaded;
	}

	public void setLoaded() {
		this.nodeFileHasBeenLoaded = true;
	}

	public String getFormulaCode() { // get the code
		return (formula != null) ? formula.toString() : null;
	}

	public String getText() { // get the text only, without formatting
		return (formula != null) ? formula.getText() : null;
	}

	public boolean hasFormula() {
		return formula != null;
	}

	public Dimension getSize(FormulaFont font) {
		if (hasFormula()) {
			return formula.getSize(font);
		}
		return null;
	}

	public boolean hasNodeImage() {
		return nodeImage != null;
	}

	public boolean hasLink() {
		return link != null;
	}

	public void setNodeImage(URL fileUrl) {
		nodeImage = (fileUrl == null) ? null : new NodeImage(fileUrl);
	}

}
