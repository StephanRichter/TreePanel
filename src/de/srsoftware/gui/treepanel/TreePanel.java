package de.srsoftware.gui.treepanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.TreeSet;
import java.util.Vector;
import java.util.zip.DataFormatException;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import de.srsoftware.formula.FormulaInputDialog;
import de.srsoftware.tools.GenericFileFilter;
import de.srsoftware.tools.Tools;
import de.srsoftware.tools.VerticalPanel;
import de.srsoftware.tools.translations.Translations;

/**
 * @author Stephan Richter
 * 
 */
public abstract class TreePanel extends JPanel implements MouseListener, MouseWheelListener {
	private class NavigationThread extends Thread {
		private int direction;

		public NavigationThread(int direction) {
			this.direction = direction;
		}

		@Override
		public void run() {
			super.run();
			try {
				sleep(100);
				switch (direction) {
				case DOWN:
					navigateDown();
					break;

				case UP:
					navigateUp();
					break;
				}
			} catch (InterruptedException e) {}
		}
	}

	private static final long serialVersionUID = -9127677905556355410L;
	private static final int UP = 1;
	private static final int DOWN = -1;
	protected static Color backgroundTraceColor = null;
	protected static Color foregroundTraceColor = null;
	private Vector<ActionListener> actionListeners;
	protected int distance = 100; // Basis-Distanz zwischen den Knoten des Baumes
	private float distanceRatio = 6.5f;
	protected static float fontSize = 18f;
	private TreeSet<String> exportedFiles = null;
	public TreeNode tree; // der Baum, das vom Panel dargestellt wird
	protected Color connectionColor;
	protected static TreeNode cuttedNode = null;

	public static String _(String text) {
		return Translations.get(text);
	}

	public static String _(String key, Object insert) {
		return Translations.get(key, insert);
	}

	protected TreeThread organizerThread; // Thread, der in regelmäßigen Abständen das Layout aktualisiert

	protected boolean updatedSinceLastChange = false;
	protected int fileLoadLevelLimit = 2; // maximale Tiefe von aktuellem Knoten ausgehend, bei der verlinkte Bäume geladen werden

	private Image backgroundImage;
	private JLabel label;
	private JDialog infoDialog;
	private TreeNode draggedNode;

	public TreePanel() {
		super();
		init();
		organizerThread = new TreeThread();
		organizerThread.setTreeMapper(this);
		organizerThread.start();
		addMouseListener(this);
	}

	public TreePanel(boolean arg0) {
		super(arg0);
		init();
	}

	public TreePanel(LayoutManager arg0) {
		super(arg0);
		init();
	}

	public TreePanel(LayoutManager arg0, boolean arg1) {
		super(arg0, arg1);
		init();
	}

	public void addActionListener(ActionListener actionListener) {
		actionListeners.add(actionListener);
	}

	public void appendNewBrother(TreeNode createNewNode) {
		tree.addBrother(createNewNode);
		setTreeTo(createNewNode);
	}

	public void appendNewChild(TreeNode newChild) {
		if (newChild != null) {
			customizeNode(tree);
			tree.addChild(newChild);
			tree.treeChanged();
			updateView();
		}
	}

	public Point center() {
		return new Point(getWidth() / 2, getHeight() / 2);
	}

	public void copy() {
		cuttedNode = tree.clone();
		copyToClipboard();
	}

	public TreeNode currentNode() {
		return tree;
	}

	public void customizeNode(TreeNode node) {
		String text = node.getFormulaCode();
		System.out.println("customizeNode(" + text + ")");
		if (text.endsWith(".imf") || text.endsWith("{.imf}")) {
			text = text.replace("}\\small{", "").replace("\\small{", "").replace("}\\bold{", "");
			text = text.substring(text.lastIndexOf('/') + 1);
			text = text.substring(0, text.lastIndexOf('.'));
			node.setText(text);
		}
	}

	public void cut() {
		if (tree.parent() != null) {
			cuttedNode = tree;
			tree.cutoff();
			tree.parent().treeChanged();
			if (tree.prev() != null) {
				setTreeTo(tree.prev());
			} else {
				if (tree.next() != null) {
					setTreeTo(tree.next());
				} else {
					setTreeTo(tree.parent());
				}
			}
		}
	}

	public void decreaseDistance() {
		distance -= 10;
		updateView();
	}

	public void deleteActive() {
		TreeNode dummy = tree.prev();
		if (dummy == null) dummy = tree.next();
		if (dummy == null) dummy = tree.parent();
		if (dummy != null) {
			tree.cutoff();
			tree.parent().treeChanged();
			setTreeTo(dummy);
		}
	}

	public void deleteActiveImage() {
		tree.setImage(null);
	}

	public void deleteActiveLink() {
		tree.setLink(null);
	}

	public void editNode() {
		editTree(tree);
		requestFocus();
	}

	public void editTree(TreeNode node) {
		String oldText = node.getFormulaCode();
		customizeNode(node);
		String newText = FormulaInputDialog.readInput(null, _("Change text of current mindmap node"), node.getFormulaCode());
		if ((newText != null) && !newText.equals(oldText)) {
			node.setText(newText);
			updateView();
		}
	}

	public void flushTreeChanges() {
		TreeNode.flushUnsavedChanges();
	}

	public int getDistance() {
		return distance;
	}

	public float getTextSize() {
		return fontSize;
	}

	public boolean hasUnsavedNodes() {
		return TreeNode.existUnsavedNodes();
	}

	public void increaseDistance() {
		distance += 10;
		updateView();
	}

	public void mouseClicked(MouseEvent arg0) {}

	public void mouseEntered(MouseEvent arg0) {}

	public void mouseExited(MouseEvent arg0) {}

	public void mousePressed(MouseEvent arg0) {
		// Bestimmen des geklickten Knotens
		draggedNode = getNodeAt(arg0.getPoint());
		System.out.println(draggedNode);
	}

	public void mouseReleased(MouseEvent arg0) {
		// bei Doppelklick: Aktion auslösen
		if (arg0.getClickCount() > 1) {
			if (tree.getLink() != null)
			// Ausführen, falls Verknüpfung
			Tools.execute(tree.getLink());
			else
			// Bearbeiten, falls normaler Knoten
			editTree(tree);
		} else {
			// Bestimmen des geklickten Knotens
			TreeNode dragTargetNode = getNodeAt(arg0.getPoint());
			
			if (dragTargetNode!=draggedNode){				
				System.out.println("we are dragging!");
				System.out.println(draggedNode+" => "+dragTargetNode);
				TreeNode testNode = dragTargetNode;
				while (testNode.parent()!=null){
					testNode=testNode.parent();
					if (testNode==draggedNode){
						System.out.println("Can not drag a node to an child of itself!");
						return;
					}
				}
				tree=draggedNode;

				if (tree.parent() != null) {
					cuttedNode = tree;
					tree.cutoff();
			
					tree=dragTargetNode;

					if (tree.parent() != null && cuttedNode != null) {
						tree.addBrother(cuttedNode);
						cuttedNode = cuttedNode.clone();
					}
			
					if (tree.parent() != null){
						setTreeTo(tree.parent());
					} else {
						setTreeTo(tree);
					}
				}
				return;
			}

			if (arg0.getButton() == MouseEvent.BUTTON2) {
				// Knoten-Text in Zwischenablage kopieren
				copyToClipboard(draggedNode);
			} else {
				// zu Knoten wechseln oder Bild vergrößern
				if (tree != null) {
					if (tree == draggedNode)
					// wenn geklickter Knoten schon im Zentrum ist: Bild ggf. vergrößern
					showNodeImage();
					else {
						// wenn geklickter Knoten in der Peripherie: zentrieren
						setTreeTo(draggedNode);
					}
				}
			}
		}
	}

	public void mouseWheelMoved(MouseWheelEvent arg0) {
		if (arg0.getWheelRotation() == -1) {
			this.setTextLarger();
			distance *= 1.1;
		} else if (arg0.getWheelRotation() == 1) {
			this.setTextSmaller();
			distance *= 0.9;
		}
	}

	public void navigateDown() {
		if (tree.next() != null) setTreeTo(tree.next());
		else {
			TreeNode dummy = tree.parent();
			if (dummy != null) {
				setTreeTo(dummy);
				sheduleNavigation(DOWN);
			}
		}
	}

	public void navigateLeft() {
		if (tree.parent() != null) setTreeTo(tree.parent());
	}

	public void navigateRight() {
		// System.out.println("navigateRight");
		if (tree.getLink() != null) Tools.execute(tree.getLink());
		if (tree.firstChild() != null) setTreeTo(tree.firstChild());
	}

	public void navigateToEnd() {
		if (tree.firstChild() != null) {
			TreeNode dummy = tree.firstChild();
			while (dummy.next() != null)
				dummy = dummy.next();
			setTreeTo(dummy);
		}
	}

	public void navigateToRoot() {
		while (tree.parent() != null)
			setTreeTo(tree.parent());
	}

	public void navigateUp() {
		if (tree.prev() != null) setTreeTo(tree.prev());
		else {
			TreeNode dummy = tree.parent();
			if (dummy != null) {
				setTreeTo(dummy);
				sheduleNavigation(UP);
			}

		}
	}

	public void paint(Graphics g) {
		super.paint(g);
		if (tree == null && backgroundImage != null) g.drawImage(backgroundImage, (this.getWidth() - backgroundImage.getWidth(this)) / 2, (this.getHeight() - backgroundImage.getHeight(this)) / 2, this);
	}

	public void paste() {
		if (tree.parent() != null && cuttedNode != null) {
			appendNewBrother(cuttedNode);
			cuttedNode = cuttedNode.clone();
		}
	}

	public void pushThread() {
		if (organizerThread != null) organizerThread.go();
	}

	public void questForFileToSaveTree(TreeNode node) {
		String guessedName = Tools.deleteNonFilenameChars(node.getText() + ".imf");
		String choosenFilename = Tools.saveDialog(this, _("save as"), guessedName, new GenericFileFilter(_("mindmap file"), "*.imf"));
		if (choosenFilename == null) node.treeChanged();
		else {
			if (!choosenFilename.toUpperCase().endsWith(".IMF") && !choosenFilename.toUpperCase().endsWith(".MM")) {
				choosenFilename += ".imf";
			}
			if (!(new File(choosenFilename)).exists() || JOptionPane.showConfirmDialog(null, _("The file you selected already exists. Overwrite it?"), _("Warning"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				try {
					URL u = new URL("file://" + choosenFilename);
					System.out.println(u);
					if (!node.saveTo(u)) JOptionPane.showMessageDialog(null, _("Sorry, I was not able to save the file as \"#\"!", choosenFilename), _("Error while trying to save"), JOptionPane.OK_OPTION);
					else {
						sendActionEvent(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "SetTitle:" + node.getRoot().nodeFile()));
					}
				} catch (MalformedURLException e) {
					System.out.println(_("Sorry, I was not able to build an URL from \"#\"!", choosenFilename));
					node.treeChanged();
				}
			}
		}
	}

	public void refreshView() {
		if (tree.getNodeImage() != null) {
			tree.getNodeImage().reload();
			updateView();
		}
	}

	public void saveCurrentFork() {
		questForFileToSaveTree(tree);
		if (tree.parent() != null) tree.parent().treeChanged();
	}

	public void saveNodes() {
		TreeSet<TreeNode> unsavedNodes = TreeNode.saveChangedNodes();
		while (!unsavedNodes.isEmpty())
			questForFileToSaveTree(pollFirst(unsavedNodes));
		propagateCurrentFile();
	}

	public void saveRoot() {
		questForFileToSaveTree(tree.getSuperRoot());
	}

	public void setBackground(Color bg) {
		super.setBackground(bg);
		connectionColor = Tools.colorComplement(bg);
	}

	public void setBackgroundImage(Image image) {
		backgroundImage = image;
	}

	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		distance = (int) (width / distanceRatio);
		updateView();
	}

	public void setBounds(Rectangle r) {
		setBounds(r.x, r.y, r.width, r.height);
	}

	public void setCurrentBackgroundColor(Color c) {
		if (c != null) tree.setBGColor(c);
	}

	public void setCurrentForegroundColor(Color c) {
		if (c != null) tree.setForeColor(c);
	}

	public void setDistance(int d) {
		distance = d;
	}

	public void setImageOfCurrentNode(NodeImage nodeImage) {
		if (nodeImage != null) tree.setNodeImage(nodeImage);
	}

	public void setLinkOfCurrentNode(URL link) {
		tree.setLink(link);
	}

	public void setSize(Dimension d) {
		setSize(d.width, d.height);
	}

	public void setSize(int width, int height) {
		super.setSize(width, height);
		distance = (int) (width / distanceRatio);
		updateView();
	}

	public void setTextLarger() {
		fontSize *= 1.1;
		updateView();
	}

	public void setTextSize(float fs) {
		fontSize = fs;
	}

	public void setTextSmaller() {
		fontSize *= 0.9;
		updateView();
	}

	public void setTree(TreeNode root) {
		tree = root;
		organizerThread.go();
	}

	public void showNodeDetails() {
		JOptionPane.showMessageDialog(this, tree.getFullInfo(), _("Information"), JOptionPane.INFORMATION_MESSAGE);
		this.requestFocus();
	}
	
	private class ExportThread extends Thread{

		private String folder;
		private boolean onlyCurrent;
		private int maxDepth;
		private boolean interactive;
		private boolean singleFile;
		private boolean noMultiFollow;

		public ExportThread(String folder, boolean onlyCurrent, int maxDepth, boolean interactive, boolean singleFile, boolean noMultipleFollow) {
			this.folder=folder;
			this.onlyCurrent=onlyCurrent;
			this.maxDepth=maxDepth;
			this.interactive=interactive;
			this.singleFile=singleFile;
			this.noMultiFollow=noMultipleFollow;
		}

		@Override
		public void run() {
			TreeNode root = tree.getSuperRoot();
			try {
				sleep(2000);
	      writeHtmlFile(root, folder, 1, onlyCurrent, maxDepth, interactive, singleFile, noMultiFollow);
				infoDialog.setVisible(false);
				setTree(root.reload());
      } catch (IOException e) {
	      e.printStackTrace();
      } catch (DataFormatException e) {
	      // TODO Auto-generated catch block
	      e.printStackTrace();
      } catch (URISyntaxException e) {
	      // TODO Auto-generated catch block
	      e.printStackTrace();
      } catch (InterruptedException e) {
	      // TODO Auto-generated catch block
	      e.printStackTrace();
      }
		}
	}


	public void startHtmlExport(String folder, boolean onlyCurrent, int maxDepth, boolean interactive, boolean singleFile, boolean noMultipleFollow) throws IOException, DataFormatException, URISyntaxException {
		exportedFiles = new TreeSet<String>();

		ExportThread exportThread = new ExportThread(folder,onlyCurrent,maxDepth,interactive,singleFile,noMultipleFollow);
		exportThread.start();
	  infoDialog = new JDialog((Frame)null,_("exporting mindmaps"));
	  VerticalPanel vp = new VerticalPanel();
	  vp.add(label=new JLabel(_("Starting to export your selected mindmap in two seconds...                 ")));
	  vp.skalieren();
	  infoDialog.getContentPane().add(vp);
	  infoDialog.pack();
	  infoDialog.setModal(false);
	  infoDialog.setVisible(true);	
	  
	}
	
	public void stopOrganizing() {
		organizerThread.die();
	}

	public abstract void toogleFold();

	public boolean traceBGColor() {
		if (backgroundTraceColor == null) {
			backgroundTraceColor = tree.getBGColor();
			return true;
		}
		backgroundTraceColor = null;
		return false;
	}

	public boolean traceForeColor() {
		if (foregroundTraceColor == null) {
			foregroundTraceColor = tree.getForeColor();
			return true;
		}
		foregroundTraceColor = null;
		return false;
	}

	public void updateView() {
		updatedSinceLastChange = false;
		organizerThread.go();
		repaint();
	}

	public String writeHtmlFile(TreeNode root, String folder, int depth, boolean onlyCurrent, int maxDepth, boolean interactive, boolean singleFile, boolean noMultipleFollow) throws IOException {
		String path = root.nodeFile().getFile();
		String filename = path.substring(path.lastIndexOf("/") + 1) + ".html";
		path = folder + filename;
		if ((maxDepth == 0 || depth < maxDepth) && !exportedFiles.contains(path)) {
			exportedFiles.add(path);
			BufferedWriter htmlFile = new BufferedWriter(new FileWriter(path));
			writeHtmlHeader(htmlFile);
			htmlFile.write("<body>\n");
			exportNodeToHtml(root, htmlFile, folder, depth, onlyCurrent, maxDepth, interactive, singleFile, noMultipleFollow);
			htmlFile.write("</body>\n");
			closeHtmlFile(htmlFile);
		}
		if (label!=null && filename!=null){
			label.setText(filename);
		}
		return path;
	}

	private void closeHtmlFile(BufferedWriter htmlFile) throws IOException {
		htmlFile.write("</html>");
		htmlFile.close();
	}

	private void copyToClipboard() {
		copyToClipboard(tree);
	}

	private void exportNodeToHtml(TreeNode node, BufferedWriter htmlFile, String folder, int depth, boolean onlyCurrent, int maxDepth, boolean interactive, boolean singleFile, boolean noMultipleFollow) throws IOException {
		htmlFile.write(node.getText());
		if (node.firstChild() != null) {
			htmlFile.write("<ul>\n");
			TreeNode child = node.firstChild();
			while (child != null) {
				htmlFile.write("<li>");
				if (child.nodeFile() == null) {
					exportNodeToHtml(child, htmlFile, folder, depth, onlyCurrent, maxDepth, interactive, singleFile, noMultipleFollow);
				} else {
					if (onlyCurrent) {
						htmlFile.write(child.getText());
					} else {
						boolean include = depth < maxDepth;
						if (interactive) {
							System.out.println(_("Warning: interactive export not supported, yet."));
							// include = Abfrage
						}
						if (include) {
							try {
								child.loadFromFile();
							} catch (FileNotFoundException fnfe) {} catch (NullPointerException npwe) {} catch (DataFormatException e) {} catch (URISyntaxException e) {}
						}
						if (singleFile) {
							exportNodeToHtml(child, htmlFile, folder, depth + 1, onlyCurrent, maxDepth, interactive, singleFile, noMultipleFollow);
						} else {
							if (child.hasBeenLoadedFromFile()) {
								String lnk = writeHtmlFile(child, folder, depth + 1, onlyCurrent, maxDepth, interactive, singleFile, noMultipleFollow);
								htmlFile.write("<a href=\"file://" + lnk + "\">" + child.getText() + "</a>");
							} else {
								htmlFile.write(child.getText());
							}
						}
					}
				}
				htmlFile.write("</li>");
				child = child.next();
			}
			htmlFile.write("</ul>\n");
			node.cutoff();
		}
	}

	private void init() {
		actionListeners = new Vector<ActionListener>();
		this.setBackground(new Color(0, 155, 255));
		addMouseWheelListener(this);
		backgroundImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/de/srsoftware/intellimind/intelliMind.gif"));
	}

	private TreeNode pollFirst(TreeSet<TreeNode> ts) {
		TreeNode result = ts.first();
		ts.remove(result);
		return result;
	}

	private void sheduleNavigation(int direction) {
		(new NavigationThread(direction)).start();

	}

	private void writeHtmlHeader(BufferedWriter htmlFile) throws IOException {
		htmlFile.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		htmlFile.write("<html>\n<head>\n<title>" + tree.getText() + "</title>\n</head>\n");
	}

	protected void copyToClipboard(TreeNode node) {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(new StringSelection(node.getText() + '\n'), null);
	}

	protected TreeNode getNodeAt(Point point) {
		// Start: Distanz von Click zu zentralem Knoten prüfen
		if (tree == null) return null;
		TreeNode clickedNode = tree;
		double minDistance = point.distance(tree.getOrigin());

		// Dann alle direkten Kinder des Zentralen Knotens prüfen
		TreeNode dummy = tree.firstChild();
		while (dummy != null) {
			double distance = point.distance(dummy.getOrigin());
			if (distance < minDistance) {
				clickedNode = dummy;
				minDistance = distance;
			}

			// alle Enkel des zentralen Knotens prüfen
			TreeNode dummy2 = dummy.firstChild();
			while (dummy2 != null) {
				distance = point.distance(dummy2.getOrigin());
				if (distance < minDistance) {
					clickedNode = dummy2;
					minDistance = distance;
				}
				dummy2 = dummy2.next();
			}

			dummy = dummy.next();
		}

		// Elter des zentralen Knotens prüfen
		if (tree.parent() != null) {
			dummy = tree.parent();

			// groß-Elter prüfen
			if (dummy.parent() != null) {
				double distance = point.distance(dummy.parent().getOrigin());
				if (distance < minDistance) {
					clickedNode = dummy.parent();
					minDistance = distance;
				}
			}

			// Kinder des Elter prüfen
			TreeNode dummy2 = dummy.firstChild();
			while (dummy2 != null) {
				double distance = point.distance(dummy2.getOrigin());
				if (distance < minDistance) {
					clickedNode = dummy2;
					minDistance = distance;
				}
				dummy2 = dummy2.next();
			}

			double distance = point.distance(dummy.getOrigin());
			if (distance < minDistance) {
				clickedNode = dummy;
				minDistance = distance;
			}
		}
		return clickedNode;
	}

	protected void propagateCurrentFile() {
		// System.out.println("propagateCurrentFile");
		URL path = tree.getRoot().nodeFile();

		if (path != null) {
			String title = path.toString();
			if (tree.hasUnsavedChanges()) title += " (*)";
			// System.out.println("set title to "+title);
			sendActionEvent(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "SetTitle:" + title));
		}
	}

	protected void sendActionEvent(ActionEvent actionEvent) {
		for (ActionListener a : actionListeners)
			a.actionPerformed(actionEvent);
	}

	protected void setParametersFrom(TreePanel treePanel) {
		tree = treePanel.currentNode();
		connectionColor = treePanel.connectionColor;
		this.setBackground(treePanel.getBackground());
	}

	protected void setTreeTo(TreeNode newNode) {
		// System.out.println("setTreeTo("+newNode.getText()+")");
		if (newNode == null) return; // falls kein Knoten zum zentrieren übergeben wurde: abbrechen
		if (backgroundTraceColor != null) newNode.setBGColor(backgroundTraceColor); // falls die Hintergrundfarbe verschleppt wird: Hintergrundfarbe zum Ziel-Knoten übertragen
		if (foregroundTraceColor != null) newNode.setForeColor(foregroundTraceColor); // falls die Vordergrundfarbe verschleppt wird: Vordergrundfarbe zum Ziel-Knoten übertragen
		tree.shrinkImages();// falls Bild des alten Zentrums groß war: verkleinern
		tree = newNode; // neuen Knoten zentrieren
		propagateCurrentFile(); // die frohe Nachricht vom neuen Knoten verbreiten
		updateView(); // Ansicht aktualisieren
	}

	protected void showNodeImage() {
		if (tree.getNodeImage() != null) {
			tree.changeImageShrinkOption();
			updateView();
		}
	}
}
