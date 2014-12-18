package de.srsoftware.gui.treepanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.ImageObserver;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.zip.DataFormatException;

import de.srsoftware.formula.Formula;
import de.srsoftware.formula.FormulaFont;
import de.srsoftware.tools.FileRecoder;
import de.srsoftware.tools.Filefilter;
import de.srsoftware.tools.ObjectComparator;
import de.srsoftware.tools.Tools;
import de.srsoftware.tools.translations.Translations;
import de.srsoftware.xmlformatter.XmlFormatter;

public class TreeNode {
	
	private TreeNode parent = null; // this variable holds the pointer to the parent node, if given
	private TreeNode firstChild = null;// this variable holds the pointer to the current node's first child
	private TreeNode lastChild = null;// this variable holds the pointer to the current node's last child
	private TreeNode nextBrother = null;// this variable holds the pointer to the current node's next brother, if given
	private TreeNode previousBrother = null;// this variable holds the pointer to the current node's previous brother, if given
	private TreeNode referencedNode = null;
	private boolean folded = true; // determines whether a node's children shall be shown
	private Point origin = null; // the node's current drawing position
	private URL nodeFile = null; // hold the URL of the file, which saves this node
	private NodeContent content = null; // the node's content
	private int numChildren = 0; // the number of children the node has
	private static boolean centered = false; // determines, whether =>origin specifies the center or the upper left corner of the node
	private static Color swappedColor = Color.white;
	private boolean shrinkLargeImages = true;
	private int maxBackupNumber = 10;
	private boolean canBeChanged = true;
	
	
	private static TreeSet<TreeNode> changedNodes = new TreeSet<TreeNode>(new ObjectComparator());
	public static boolean existUnsavedNodes() {
		return changedNodes.size() > 0;
	}
	public static void flushUnsavedChanges() {
		changedNodes.clear();
	}
	public static TreeNode nodeOpenAndChanged(URL url) {
		if (url==null) return null;
		for (TreeNode n : changedNodes) {
			URL u = n.nodeFile();
			if ((u != null) && (url.equals(u)) && n.content!=null) return n;
		}
		return null;
	}
	public static TreeSet<TreeNode> saveChangedNodes() {
		// TODO Auto-generated method stub
		TreeSet<TreeNode> result = new TreeSet<TreeNode>(new ObjectComparator());
		while (!changedNodes.isEmpty()) {
			TreeNode dummy = pollFirst(changedNodes);
			if (!dummy.canBeChanged) continue;
			if (!dummy.save()) result.add(dummy);
			if (nodeOpenAndChanged(dummy.nodeFile) != null) {
				System.out.println(_("Warning! The File # has been concurrently edited at two or more places. Only changes of one instance will be saved to #!\nChanges of other instances will be saved to backup files in the same folder!", new Object[]{dummy.nodeFile,dummy.nodeFile}));
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
		return result;
	}
	public static void setCentered(boolean c) {
		centered = c;
	}
	public static void swapColor(Graphics g) {
		Color dummy = g.getColor();
		g.setColor(swappedColor);
		swappedColor = dummy;
	}
	private static String _(String text) { 
		return Translations.get(text);
	}
	private static String _(String key, Object insert) {
		return Translations.get(key, insert);
	}
	private static boolean isSymbolicLink(File file) throws IOException {
	  if (file == null)
	    throw new NullPointerException("File must not be null");
	  File canon;
	  if (file.getParent() == null) {
	    canon = file;
	  } else {
	    File canonDir = file.getParentFile().getCanonicalFile();
	    canon = new File(canonDir, file.getName());
	  }
	  return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
	}
	private static TreeNode pollFirst(TreeSet<TreeNode> tree) {
		TreeNode result = tree.first();
		tree.remove(result);
		return result;
	}
	private static URL resolveSymLinks(URL fileUrl) throws URISyntaxException, IOException {
		File path = new File(fileUrl.toString().substring(5));
		if (isSymbolicLink(path))	{
			System.out.println(fileUrl+" refers to symlink");
	    File target = path.getCanonicalFile();	    
//			File target = readSymbolicLink(path);
/*			if (path.isAbsolute()){
				if (!target.isAbsolute())	target=path.getParent().resolve(target);
			} else target=target.toAbsolutePath();*/
			fileUrl = new URL("file:"+target);
		}
		return fileUrl;
	}


	/**
	 * create a new node with empty formula
	 */
	public TreeNode() {
		this(null, null);
	}

	/**
	 * create a new node with empty formula at position origin
	 */
	public TreeNode(Point origin) {
		this(null, origin);
	}

	/**
	 * create a new node with the given text
	 * 
	 * @param text the text for the formula
	 */
	public TreeNode(String text) {
		this(text, null);
	}

	/**
	 * create a new node with the given text
	 * 
	 * @param text the text for the formula
	 */
	public TreeNode(String text,Point origin) {
		content = new NodeContent(text);
		this.origin = (origin != null) ? origin : new Point(3000, 500);
	}

	public void addBrother(TreeNode brother) {
		if (brother==null) return;
		if (parent==null) return;
		
		brother.nextBrother = nextBrother;
		if (nextBrother != null) nextBrother.previousBrother = brother;
		brother.previousBrother = this;
		brother.parent = parent;
		nextBrother = brother;
		if (brother.nextBrother == null)	parent.lastChild = brother;
		parent.numChildren++;
		brother.treeChanged();
	}

	/**
	 * append the given TreeNode to the current one
	 * 
	 * @param newChild the TreeNode to be appended
	 */
	public void addChild(TreeNode newChild) {
		
		newChild.parent = this;
		lastChild = newChild;
		if (firstChild == null) {
			firstChild = newChild;
			lastChild.previousBrother=null;
		} else {
			TreeNode dummy = firstChild;
			while (dummy.nextBrother != null)	dummy = dummy.nextBrother;
			dummy.nextBrother = newChild;
			lastChild.previousBrother = dummy;
		}
		numChildren++;
	}

	public void changeImageShrinkOption() {
		// TODO Auto-generated method stub
		shrinkLargeImages = !shrinkLargeImages;
	}

	public TreeNode clone() {
		TreeNode result = new TreeNode(this.getFormulaCode(),this.getOrigin());
		result.content=this.content.clone();
		return result;
	}

	public void cutoff() {
		TreeNode parentOfNodeToIsolate = parent();
		TreeNode nextBrotherOfNodeToIsolate = next();
		TreeNode previousBrotherOfNodeToIsolate = prev();
		if (nextBrotherOfNodeToIsolate != null) nextBrotherOfNodeToIsolate.previousBrother = previousBrotherOfNodeToIsolate;
		if (previousBrotherOfNodeToIsolate != null) previousBrotherOfNodeToIsolate.nextBrother = nextBrotherOfNodeToIsolate;
		if (parentOfNodeToIsolate != null) {
			if (this == parentOfNodeToIsolate.firstChild) parentOfNodeToIsolate.firstChild = nextBrotherOfNodeToIsolate;
			if (this == parentOfNodeToIsolate.lastChild) parentOfNodeToIsolate.lastChild = previousBrotherOfNodeToIsolate;
			parentOfNodeToIsolate.numChildren--;
		}
	}

	public void doNotShrinkImages() {
		shrinkLargeImages = false;
	}

	public TreeNode firstChild() {
		return firstChild;
	}

	public Color getBGColor() {
		return content.getBackgroundColor();
	}

	public Color getColorFromCode(String code) {
		// System.out.println(code);
		if (code.startsWith("#")) code = code.substring(1);
		int r = Integer.decode("0x" + code.substring(0, 2)).intValue();
		int g = Integer.decode("0x" + code.substring(2, 4)).intValue();
		int b = Integer.decode("0x" + code.substring(4, 6)).intValue();
		Color result = new Color(r, g, b);
		return result;
	}

	public Color getForeColor() {
		return content.getForegroundColor();
	}

	public String getFormulaCode() { // get the code
		return content.getFormulaCode();
	}

	public Object getFullInfo() {
		String text = getText();
		int i = 0;
		int l = text.length();
		while (i < l) {
			if (i % 50 == 0) {
				text = text.substring(0, i) + "\n" + text.substring(i + 1);
			}
			i++;
		}
		URL rootUrl = getRoot().nodeFile;
		String rootFile=(rootUrl==null)?null:rootUrl.toString();
		
		return _("Node in File:\n#\n\nText:\n#\n\nImage:\n#\n\nLink:\n#\n\nText color: #\nBackground color: #",new Object[]{ Tools.shorten(rootFile),Tools.shorten(getText()),content.getNodeImage(),content.getLink(),content.getForegroundColor(),content.getBackgroundColor()});

	}

	public URL getLink() {
		return content.getLink();
	}

	public NodeImage getNodeImage() {
		return content.getNodeImage();
	}

	public int getNumChildren() {
		// TODO Auto-generated method stub
		return numChildren;
	}

	public Point getOrigin() {
		return new Point(origin);
	}

	public TreeNode getRoot() {
		TreeNode result = this;		
		while (result.parent != null && result.nodeFile == null)
			result = result.parent();
		return result;
	}

	public TreeNode getSuperRoot() {
		// TODO Auto-generated method stub
		TreeNode result = this;
		while (result.parent != null)
			result = result.parent;
		return result;
	}

	@SuppressWarnings("deprecation")
	public URL getTemporaryUrl() throws MalformedURLException {
		return (new File("intelliMind3.tmp.imf")).toURL();
	}

	public String getText() { // get the text only, without formatting
		return content.getText();
	}
	
	public String getTextWithoutPath() {
		String text=getText();
		if (text.startsWith("file:") || text.startsWith("/"))	{
			text=text.substring(text.lastIndexOf('/')+1);
			text=text.substring(0,text.lastIndexOf('.'));
		}
		
		return text;
	}

	public boolean hasBeenLoadedFromFile() {
		return content.hasBeenLoaded();
	}
	
	public boolean hasUnsavedChanges() {
		return (changedNodes.contains(this.getRoot()));
	}

	public boolean isFolded() {
		return (numChildren>0 && folded) || (nodeFile!=null && !content.hasBeenLoaded());
	}

	public TreeNode lastChild() {
		return lastChild;
	}

	public Vector<TreeNode> linkedNodes() {
		Vector<TreeNode> result = new Vector<TreeNode>();
		if (this.parent != null) result.add(this.parent);
		TreeNode dummy = this.firstChild;
		while (dummy != null) {
			result.add(dummy);
			dummy = dummy.nextBrother;
		}
		return result;
	}

	public void loadFromFile() throws FileNotFoundException, IOException, DataFormatException, URISyntaxException {	
		if (this.nodeFile != null) {
			loadFromFile(this.nodeFile);
		}
	}

	public void loadFromFile(URL fileUrl) throws FileNotFoundException, IOException, DataFormatException, URISyntaxException {
		if (!content.hasBeenLoaded()) {
			content.setLoaded();
			if (!Tools.fileIsLocal(fileUrl)) {
				try {
					fileUrl = new URL(fileUrl.toString().replace(" ", "%20"));
				} catch (MalformedURLException e1) {}
			}
			if (!Tools.fileExists(fileUrl)) {
				throw new FileNotFoundException(fileUrl.toString());
			} else {
				
				fileUrl=resolveSymLinks(fileUrl);

				TreeNode n = nodeOpenAndChanged(fileUrl);

				if (n != null) {
					// TODO wenn ein Baum geöffnet wird, das schon offen, geändert und noch ncht gespeichert ist:
					// dieses Baum in eine temporäre Datei schreiben, und diese öffnen
					URL temp = getTemporaryUrl();
					if (n.saveTo(temp)) {
						n.nodeFile = fileUrl;
						if (Tools.fileIsIntelliMindFile(fileUrl)) loadFromIntellimindFile(temp);
						if (Tools.fileIsFreeMindFile(fileUrl)) loadFromFreemindFile(temp);
						changedNodes.remove(n);
						this.nodeFile = fileUrl;
						this.treeChanged();
					} else {
						if (Tools.fileIsIntelliMindFile(fileUrl)) loadFromIntellimindFile(fileUrl);
						if (Tools.fileIsFreeMindFile(fileUrl)) loadFromFreemindFile(fileUrl);
					}
				} else {
					if (isFolder(fileUrl)) loadFolder(fileUrl); else
					if (Tools.fileIsKeggUrl(fileUrl)) loadKeggFile(fileUrl); else
					if (Tools.fileIsIntelliMindFile(fileUrl)) loadFromIntellimindFile(fileUrl); else
					if (Tools.fileIsFreeMindFile(fileUrl)) loadFromFreemindFile(fileUrl); else
						throw new DataFormatException(fileUrl.toString());
				}
			}
		}
	}
	
	public void moveTowards(int x, int y) {
		setOrigin((3*origin.x + x) / 4,(3*origin.y + y) / 4);
	}
	
	public void moveTowards(Point target) {
		moveTowards(target.x, target.y);
	}
	public void moveTowardsY(int y) {	
		setOrigin(origin.x,(origin.y+y)/2);
	}

	public TreeNode next() {
		return nextBrother;
	}

	public Dimension nodeDimension(Graphics g, ImageObserver obs, FormulaFont font) {
		return paint(g, obs, font, false);
	}

	public URL nodeFile() {
		return nodeFile;
	}

	public Dimension paint(Graphics g, ImageObserver obs, FormulaFont font) {
		return paint(g, obs, font, true);
	}
	
	public Dimension paint(Graphics g, ImageObserver obs, FormulaFont font, boolean draw) {
		if (content.hasFormula()) {
			Dimension formulaDimension = content.getSize(font);
			if (formulaDimension.width < 10 && content.hasNodeImage()) {
				formulaDimension.width = 300;
			}
			Dimension imageDimension = (content.hasNodeImage()) ? ((shrinkLargeImages) ? content.getNodeImage().getResizedDimension(formulaDimension.width, obs) : content.getNodeImage().getDimension(obs)) : (new Dimension());
			Dimension nodeDimension = new Dimension(Math.max(formulaDimension.width, imageDimension.width)+4, formulaDimension.height + imageDimension.height+4);
			Point upperLeft = (centered) ? new Point(origin.x - nodeDimension.width / 2, origin.y - nodeDimension.height / 2) : origin;
			if (draw) {
				// the following lines draw arcs besides the node, if the node contains a link
				if (content.hasLink()) {
					g.drawArc(upperLeft.x - (nodeDimension.height / 2), upperLeft.y - 2, nodeDimension.height , nodeDimension.height , 90, 180);
					g.drawArc(upperLeft.x + nodeDimension.width  - (nodeDimension.height / 2), upperLeft.y - 2, nodeDimension.height , nodeDimension.height, 270, 180);
				}
				swapColor(g);
				g.setColor(content.getBackgroundColor());
				g.fillRoundRect(upperLeft.x - 2, upperLeft.y - 2, nodeDimension.width, nodeDimension.height , 5, 5);
				swapColor(g);
				g.setColor(content.getForegroundColor());
				font=font.color(content.getForegroundColor());
				g.drawRoundRect(upperLeft.x - 2, upperLeft.y - 2, nodeDimension.width, nodeDimension.height , 5, 5);
				
				if (formulaDimension.width > imageDimension.width) {
					g.drawImage(content.getFormula().image(font), upperLeft.x, upperLeft.y + imageDimension.height, obs);
				} else {
					g.drawImage(content.getFormula().image(font), upperLeft.x + (imageDimension.width - formulaDimension.width) / 2, upperLeft.y + imageDimension.height, obs);
				}
				if (content.hasNodeImage()) {
					g.drawString("\u270D", upperLeft.x + 2, upperLeft.y + g.getFontMetrics().getHeight() + 2);
					content.getNodeImage().paint(g, obs, upperLeft, imageDimension);
				}
			}
			return nodeDimension;
		}
		return null;
	}

	public void paintWithoutImages(Graphics g, ImageObserver obs,FormulaFont font) {
		if (content.hasFormula()) {
			Dimension d = content.getSize(font);
			Point upperLeft = (centered) ? new Point(origin.x - d.width / 2, origin.y - d.height / 2) : origin;
			swapColor(g);
			g.setColor(content.getBackgroundColor());
			g.fillRoundRect(upperLeft.x - 2, upperLeft.y - 2, d.width + 2, d.height + 2, 5, 5);
			swapColor(g);
			g.setColor(content.getForegroundColor());
			g.drawRoundRect(upperLeft.x - 2, upperLeft.y - 2, d.width + 2, d.height + 2, 5, 5);
			g.drawImage(content.getFormula().image(font), upperLeft.x, upperLeft.y, obs);
		}
	}

	public TreeNode parent() {
		return parent;
	}

	public TreeNode prev() {
		return previousBrother;
	}

	public TreeNode referencedNode() {
		return referencedNode;
	}

	public boolean referencesOtherNode() {
		return (referencedNode != null);
	}

	public TreeNode reload() throws FileNotFoundException, IOException, DataFormatException, URISyntaxException {
		// TODO Auto-generated method stub
		TreeNode result = new TreeNode();
		result.nodeFile = this.nodeFile;
		result.loadFromFile();
		return result;
	}

	/**
	 * replaces teh oldNod by this node
	 * @param oldNode
	 */
	public void replace(TreeNode oldNode) {
		// TODO Auto-generated method stub
		this.parent = oldNode.parent();
		this.nextBrother = oldNode.next();
		this.previousBrother = oldNode.prev();
		if (this.parent != null) {
			if (this.parent.firstChild == oldNode) this.parent.firstChild = this;
			if (this.parent.lastChild == oldNode) this.parent.lastChild = this;
		}
		if (this.nextBrother != null) this.nextBrother.previousBrother = this;
		if (this.previousBrother != null) this.previousBrother.nextBrother = this;
	}

	public void resetDimension() {
		// TODO: remove
	}

	public boolean saveTo(URL fileUrl) {
		// TODO Auto-generated method stub
		nodeFile = fileUrl;
		String s = fileUrl.toString();
		s = s.substring(s.indexOf(":") + 1);
		// System.out.println("Versuche nach " + s + " zu speichern");
		File f = new File(s);
		try {
			f.createNewFile();
			this.save();
			content.setLoaded();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return false;
		}
		return true;
	}

	public void setBGColor(Color c) {
		// System.out.println("Hintergrundfarbe wird gesetzt...");
		if (content.setBackgroundColor(c)) {
			treeChanged();
		}
	}

	public void setFolded(boolean folded) {
		this.folded = folded;
	}

	public void setForeColor(Color c) {
		if (content.setForegroundColor(c)){
			treeChanged();
		}
	}

	public void setFormula(Formula f) {
		content.setFormula(f);
		treeChanged();
	}

	public void setImage(URL fileUrl) {
		content.setNodeImage(fileUrl);
		treeChanged();
	}

	public void setLink(URL link) {
		content.setLink(link);
		treeChanged();
	}

	public void setNodeImage(NodeImage nodeImage) {
		content.setNodeImage(nodeImage);
		treeChanged();
	}

	public void setOrigin(int x, int y){
		setOrigin(new Point(x,y));
	}

	public void setOrigin(Point newOrigin) {
		origin = newOrigin;
	}

	public void setText(String tx) {
		content.setFormula(tx);
		treeChanged();
	}

	public void shrinkImages() {
		shrinkLargeImages = true;
	}

	public String toString() {
		return toString(0);
	}

	public void treeChanged() {
		if (!this.canBeChanged) return; 
		TreeNode dummy = this;
		while (dummy != null && dummy.nodeFile == null) { // sucht nach der Wurzel des aktuellen Teilbaums
			if (dummy.parent() == null && dummy.canBeChanged) changedNodes.add(dummy); // falls die Wurzel selbst noch nicht gespeichert wurde
			dummy = dummy.parent;
		}
		if (dummy != null && dummy.nodeFile != null && dummy.canBeChanged) {
			changedNodes.add(dummy); // fügt die (schon mal gespeicherte) wurzel des aktuellen Teilbaums zur Speicher-Liste hinuzu
		}
	}

	public void waitForLoading() {
		System.out.println(_("Waiting for #",this.getText()));
		while (this.nodeFile!=null && !hasBeenLoadedFromFile()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void addSubstances(TreeNode substances, String list) {
		String[] parts = list.split(" \\+ ");
		for (int i=0; i<parts.length; i++){
			TreeNode child = new TreeNode(parts[i]);
			try{
				child.nodeFile=new URL("http://www.genome.jp/dbget-bin/www_bget?"+parts[i].trim());
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			substances.addChild(child);
		}
	}

	private void backup(String filename) {
		String backupFilename = filename + "." + Tools.getDateTime();
		File f = new File(filename);
		f.renameTo(new File(backupFilename));
		limitOldBackups(f);
	}

	private String extractImageFromTag(String txt) throws MalformedURLException {
		// TODO Auto-generated method stub
		String file = Tools.getTagProperty(txt, "src");
		int i = txt.indexOf("<img src=");
		int j = txt.indexOf(">", i);
		txt = txt.substring(0, i) + txt.substring(j + 1);
		URL imageUrl = Tools.getURLto(this.getRoot().nodeFile.toString(), file);
		content.setNodeImage(imageUrl);
		return txt;
	}

	private String formatFormula(String s) {
		return s.replaceAll("(\\D)(\\d)", "$1\\\\_{$2").replaceAll("(\\d)(\\D)", "$1}$2").replaceAll("(\\d)$","$1}");
	}

	private boolean isFolder(URL fileUrl) {
		File f=new File(fileUrl.getFile());
		return f.isDirectory();
	}

	private void limitOldBackups(File f) {
		File[] files = f.getParentFile().listFiles(new Filefilter(f.getName()));
		java.util.Arrays.sort(files);
		int number = files.length;
		number -= maxBackupNumber;
		for (File file : files) {
			if (--number >= 0) file.delete();
		}
	}

	@SuppressWarnings("deprecation")
	private void loadFolder(URL fileUrl) throws FileNotFoundException, MalformedURLException, IOException {
		content.setLoaded();
		File f=new File(fileUrl.getFile());
		this.setText(f.getName());
		this.canBeChanged=false;
		this.setBGColor(Color.yellow);
		File [] subs=f.listFiles();
		TreeMap<String,File> files=new TreeMap<String,File>(ObjectComparator.get());
		TreeMap<String,File> directories=new TreeMap<String,File>(ObjectComparator.get());
		for (int i=0; i<subs.length; i++){
			File file=subs[i];
			String name=file.getName();
			if (!name.startsWith(".")) {
				if (file.isDirectory()){
					directories.put(name, file);
				} else {
					files.put(name, file);
				}
			}
		}
		for (Iterator<String> it = directories.keySet().iterator(); it.hasNext();){
			String name=it.next();			
			TreeNode child=new TreeNode(name);
			child.canBeChanged =false;
			child.nodeFile=directories.get(name).toURL();
			child.setBGColor(Color.yellow);
			this.addChild(child);
		}
		for (Iterator<String> it = files.keySet().iterator(); it.hasNext();){
			String name=it.next();			
			TreeNode child=new TreeNode(name);
			child.canBeChanged =false;
			child.nodeFile=files.get(name).toURL();
			child.setBGColor(Color.cyan);
			this.addChild(child);
		}
	}

	private void loadFromFreemindFile(URL fileUrl) throws IOException {
		// TODO Auto-generated method stub
		// System.out.println("loading intelliMind file " + fileUrl);
		fileUrl = Tools.fixUrl(fileUrl);
		try {
			BufferedReader fileReader = new BufferedReader(new InputStreamReader(fileUrl.openStream(), "UTF-8"));
			int waitTime = 1;
			while (!fileReader.ready()) {
				Thread.sleep(waitTime);
				waitTime *= 2;
				System.out.println(_("File is not available at the moment. Will try again in #ms...",waitTime));
				if (waitTime > 16000) throw new IOException(_("# not ready to be read!",fileUrl));
			}
			TreeNode root = this;
			root.nodeFile = fileUrl;
			readTreeFile(fileReader);

			fileReader.close();
		} catch (InterruptedException e) {
			throw new IOException(e.getMessage());
		}
	}

	private void loadFromIntellimindFile(URL fileUrl) throws FileNotFoundException, IOException {
		fileUrl = Tools.fixUrl(fileUrl);
		FileRecoder.recode(fileUrl);
		
		try {
			BufferedReader fileReader = new BufferedReader(new InputStreamReader(fileUrl.openStream(), "UTF-8"));
			int waitTime = 1;
			while (!fileReader.ready()) {
				Thread.sleep(waitTime);
				waitTime *= 2;
				System.out.println(_("File is not available at the moment. Will try again in #ms...",waitTime));
				if (waitTime > 16000) throw new IOException(_("# not ready to be read!",fileUrl));
			}
			TreeNode root = this;
			root.nodeFile = fileUrl;
			TreeNode node = root;
			String line;
			while (fileReader.ready()) {				
				line = fileReader.readLine();
				if (line.equals("[Root]")) {}
				if (line.equals("[Child]")) {
					node.addChild(new TreeNode(this.origin));
					node = node.firstChild();
				}
				if (line.equals("[Brother]")) {
					node.parent().addChild(new TreeNode(this.origin));
					node = node.next();
				}
				if (line.equals("[UP]")) {
					if (node.parent()!=null){
						node = node.parent();
					} else System.out.println(_("Tree corrupt: UP-command found while at root node."));
				}

				if (line.startsWith("text=")) {
					node.content.setFormula(line.substring(5));
				}
				if (line.startsWith("content=")) {
					String content = line.substring(8).replace("\\", "/");
					if (content.startsWith("Link:")) {
						try {
							node.content.setLink(Tools.getURLto(this.getRoot().nodeFile.toString(), content.substring(5)));
						} catch (MalformedURLException e) {
							Tools.message(_("external link (#) could not be resolved!",content.substring(5)));
						}
					} else { // eingebundener Teilbaum
						try {
							URL nodeURL = Tools.getURLto(this.getRoot().nodeFile.toString(), content);
							node.nodeFile = nodeURL;
						} catch (MalformedURLException e) {
							Tools.message(_("embedded tree (#) could not be resolved!",content));
						}

					}
				}
				if (line.startsWith("image=")) {
					String content = line.substring(6).replace("\\", "/");
					try {
						URL imageUrl = Tools.getURLto(this.getRoot().nodeFile.toString(), content);
						node.content.setNodeImage(imageUrl);
					} catch (MalformedURLException e) {
						Tools.message(_("was not able to resolve path to file (#)!",content));
					}
				}
				if (line.startsWith("Color1=")) {
					try {
						int r = Integer.decode("0x" + line.substring(14, 16)).intValue();
						int g = Integer.decode("0x" + line.substring(12, 14)).intValue();
						int b = Integer.decode("0x" + line.substring(10, 12)).intValue();
						node.content.setForegroundColor(new Color(r, g, b));
					} catch (Exception e) {
						node.content.setForegroundColor(Tools.lookupColor(line.substring(7)));
					}
				}
				if (line.startsWith("Color2=")) {
					try {
						int r = Integer.decode("0x" + line.substring(14, 16)).intValue();
						int g = Integer.decode("0x" + line.substring(12, 14)).intValue();
						int b = Integer.decode("0x" + line.substring(10, 12)).intValue();
						node.content.setBackgroundColor(new Color(r, g, b));
					} catch (Exception e) {
						node.content.setBackgroundColor(Tools.lookupColor(line.substring(7)));
					}
				}
			}
			fileReader.close();
		} catch (InterruptedException e) {
			throw new IOException(e.getMessage());
		}
	}

	private void loadKeggEquation(String equation) {
		String[] sides=equation.split("<=>");
		TreeNode substrates=new TreeNode("Substrates");
		addSubstances(substrates,sides[0]);
		addChild(substrates);
		TreeNode products=new TreeNode("Products");
		addSubstances(products,sides[1]);
		addChild(products);
	}

	private void loadKeggFile(URL fileUrl) throws IOException {
		System.out.println("loading "+fileUrl);
		String url=fileUrl.toString();
		if (url.startsWith("http://www.genome.jp/dbget-bin/www_bget?R") ||url.startsWith("http://www.genome.jp/dbget-bin/www_bget?rn:R")) loadKeggReaction(fileUrl);
		if (url.startsWith("http://www.genome.jp/dbget-bin/www_bget?C") ||url.startsWith("http://www.genome.jp/dbget-bin/www_bget?cpd:C")) loadKeggSubstance(fileUrl);
		TreeNode child=new TreeNode("\\=>  browse \\=> ");
		child.content.setLink(fileUrl);
		addChild(child);
		nodeFile=fileUrl;
	}

	private void loadKeggReaction(URL fileUrl) throws IOException {
		String[] lines=XmlFormatter.loadDocument(fileUrl).split("\n");
		String name=null;
		for (int i=0; i<lines.length; i++){			
			if (lines[i].contains("<nobr>Name</nobr>")) name=Tools.removeHtml(lines[++i]).replaceAll(";$", "");
			if (lines[i].contains("<nobr>Definition</nobr>") && name==null) name=Tools.removeHtml(lines[++i]);
			if (lines[i].contains("<nobr>Equation</nobr>")) loadKeggEquation(Tools.removeHtml(lines[++i]));
		}
		if (name==null) name="unnamed reaction";
		name=name.replace("<=>", "\\<=> ");
		content.setFormula("Reaction:\\n "+name);
	}

	private void loadKeggSubstance(URL fileUrl) throws IOException {
		String[] lines=XmlFormatter.loadDocument(fileUrl).split("\n");
		for (int i=0; i<lines.length; i++){
			if (lines[i].contains("<nobr>Formula</nobr>")){
				
				addChild(new TreeNode(formatFormula(Tools.removeHtml(lines[++i]))));
			}
			if (lines[i].contains("<nobr>Name</nobr>")) {
				content.setFormula("Substance:\\n "+Tools.removeHtml(lines[++i]).replaceAll(";$", ""));
				TreeNode otherNames = null;
				while (!lines[++i].contains("</div>")){
					if (otherNames==null) addChild(otherNames=new TreeNode("other names"));
					otherNames.addChild(new TreeNode(Tools.removeHtml(lines[i]).replaceAll(";$", "")));
				}
			}
			if (lines[i].contains("<nobr>Reaction</nobr>")) {
				TreeNode reactions=null;
				while (!lines[++i].contains("</div>")){
					if (reactions==null) addChild(reactions=new TreeNode("Reactions"));
					String[] ids=Tools.removeHtml(lines[i]).split(" ");
					for (int k=0; k<ids.length; k++){
						TreeNode reaction = new TreeNode(ids[k].trim());
						reaction.nodeFile=new URL("http://www.genome.jp/dbget-bin/www_bget?"+ids[k].trim());
						reactions.addChild(reaction);
					}
				}
			}
		}
	}

	private boolean readTreeFile(BufferedReader file) throws IOException {
		while (file.ready() && content.hasFormula()) {
			String tag = Tools.readNextTag(file);
			if (tag==null){
				System.out.println("empty tag found!");
				continue;
			}
			if (tag.equals("<icon BUILTIN=\"button_cancel\"/>")) this.parent.content.setFormula("\\rgb{ff0000,\\nok }" + this.parent.content.getFormulaCode());
			if (tag.equals("<icon BUILTIN=\"idea\"/>")) this.parent.content.setFormula("\\info " + this.parent.content.getFormulaCode());
			if (tag.equals("<icon BUILTIN=\"messagebox_warning\"/>") || tag.equals("<icon BUILTIN=\"clanbomber\"/>")) this.parent.content.setFormula("\\bomb " + this.parent.content.getFormulaCode());
			if (tag.equals("<icon BUILTIN=\"forward\"/>")) this.parent.content.setFormula("\\rgb{0099ff,\\=> }" + this.parent.content.getFormulaCode());
			if (tag.equals("<icon BUILTIN=\"button_ok\"/>")) this.parent.content.setFormula("\\rgb{00aa00,\\ok }" + this.parent.content.getFormulaCode());
			if (tag.equals("<icon BUILTIN=\"pencil\"/>")) this.parent.content.setFormula("\\rgb{bb0000,\\pen }" + this.parent.content.getFormulaCode());
			if (tag.equals("<icon BUILTIN=\"back\"/>")) this.parent.content.setFormula("\\rgb{0099ff,\\<= }" + this.parent.content.getFormulaCode());
			if (tag.equals("<icon BUILTIN=\"help\"/>")) this.parent.content.setFormula("\\rgb{000099,\\bold{(?)}} " + this.parent.content.getFormulaCode());
			if (tag.equals("<icon BUILTIN=\"ksmiletris\"/>")) this.parent.content.setFormula("\\rgb{008888,\\smile }" + this.parent.content.getFormulaCode());
			if (tag.equals("<icon BUILTIN=\"stop\"/>")) this.parent.content.setFormula("\\rgb{ff0000,\\nokbox }" + this.parent.content.getFormulaCode());

			if (tag.startsWith("<node")) {
				String txt = Tools.htmlToUnicode(Tools.getTagProperty(tag, "TEXT"));

				String colString = Tools.getTagProperty(tag, "COLOR");
				if (colString != null) content.setForegroundColor(getColorFromCode(colString));

				String colString2 = Tools.getTagProperty(tag, "BACKGROUND_COLOR");
				if (colString2 != null) content.setBackgroundColor(getColorFromCode(colString2));

				if (txt.contains("<img src=")) txt = extractImageFromTag(txt);
				content.setFormula(txt);
				if (tag.endsWith("/>")) return true;
			}
			if (tag.equals("</node>")) return false;
		}
		while (file.ready()) {
			TreeNode dummy = new TreeNode(this.origin);
			dummy.parent = this;
			if (!dummy.readTreeFile(file)) return true;
			this.addChild(dummy);
		}
		return true;
	}

	private boolean save() {
		if (nodeFile != null) {
			System.out.println("saving "+nodeFile);
			try {
				String filename = nodeFile.getFile();
				if (Tools.fileIsLocal(nodeFile) && Tools.fileExists(nodeFile) && !filename.contains(".tmp.")) {
					backup(filename);
				}
				OutputStreamWriter outFile = new OutputStreamWriter(new FileOutputStream(filename), "UTF-8");
				outFile.write("[Encoding]\r\n");
				outFile.write("UTF-8\r\n");
				outFile.write("[Root]\r\n");
				saveNode(outFile, nodeFile);
				if (firstChild != null) {
					outFile.write("[Child]\r\n");
					firstChild.saveTree(outFile, nodeFile);
					outFile.write("[UP]\r\n");
				}
				outFile.close();
				changedNodes.remove(this);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	private void saveNode(OutputStreamWriter file, URL filename) throws IOException {
		// System.out.println("TreeNode.saveNode("+filename+" , "+node.getText()+");");
		file.write("text=" + this.getFormulaCode() + "\r\n");
		if (content.hasNodeImage()) {
			Tools.getRelativePath(filename, content.getNodeImage().getUrl());
			file.write("image=" + Tools.getRelativePath(filename, content.getNodeImage().getUrl()) + "\r\n");
		}
		if (content.hasLink()) file.write("content=Link:" + Tools.getRelativePath(filename, content.getLink()) + "\r\n");
		if (nodeFile != null && !nodeFile.equals(filename)) {
			file.write("content=" + Tools.getRelativePath(filename, nodeFile) + "\r\n");
		}
		if (!content.getForegroundColor().equals(Color.BLACK)) file.write("Color1=" + Tools.colorToString(content.getForegroundColor()) + "\r\n");
		if (!content.getBackgroundColor().equals(Color.WHITE)) file.write("Color2=" + Tools.colorToString(content.getBackgroundColor()) + "\r\n");
	}

	private void saveTree(OutputStreamWriter outFile, URL filename) throws IOException {
		// TODO Auto-generated method stub
		saveNode(outFile, filename);
		if (firstChild != null && nodeFile == null) {
			outFile.write("[Child]\r\n");
			firstChild().saveTree(outFile, filename);
			outFile.write("[UP]\r\n");
		}
		if (nextBrother != null) {
			outFile.write("[Brother]\r\n");
			nextBrother.saveTree(outFile, filename);
		}
	}

	private String toString(int l) {
		if (l > 10) return "...";
		String result = "{";
		if (content.hasFormula()) result += content.getFormula();
		TreeNode dummy = firstChild();
		while (dummy != null) {
			result += dummy.toString(l + 1);
			dummy = dummy.next();
		}
		result += "}";
		return result;
	}
}
