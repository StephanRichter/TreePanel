package de.srsoftware.gui.treepanel;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import de.srsoftware.tools.Tools;
import de.srsoftware.tools.translations.Translations;

public class NodeImage {
	private static String _(String key, Object insert) {
		return Translations.get(key, insert);
	}
	private URL imageUrl = null;
	
	private Image image = null;

	public NodeImage(URL url) {
		load(url);
	}

	public NodeImage clone(){
		try {
			return new NodeImage(new URL(imageUrl.toString()));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	public Dimension getDimension(ImageObserver observer) {
		return new Dimension(image.getWidth(observer), image.getHeight(observer));
	}

	public Dimension getResizedDimension(int maxSquareSize, ImageObserver observer) {
		if (image==null) return new Dimension();
		int width = image.getWidth(observer);
		int height = image.getHeight(observer);
		if (height > width && height > maxSquareSize) return new Dimension(width * maxSquareSize / height, maxSquareSize);
		if (height < width && width > maxSquareSize) return new Dimension(maxSquareSize, height * maxSquareSize / width);
		return new Dimension(width, height);
	}
	
	public URL getUrl(){
		return imageUrl;
	}

	public void load(URL url) {
		if (!Tools.fileIsLocal(url)) {
			try {
				imageUrl=new URL(url.toString().replace(" ", "%20"));
			} catch (MalformedURLException e1) {
				imageUrl = url;
			}
			if (!(new File(imageUrl.getFile())).exists()) System.out.println(_("Could not find image #!",imageUrl)); 
		} else {
			imageUrl = url;
			File testFile=new File(imageUrl.getFile());
			if (!testFile.exists()){
				String [] names = { testFile.getName() };
				System.out.print("searching for "+names[0]);
				URL searchedFile=Tools.searchFiles(names, testFile.getParent());
				if (searchedFile!=null) imageUrl=searchedFile;
			}

		}
		
		image = Toolkit.getDefaultToolkit().getImage(imageUrl);
	}

	public void paint(Graphics g, ImageObserver obs, Point pos) {
		g.drawImage(image, pos.x, pos.y, 100, 200, obs);
	}

	public void paint(Graphics g, ImageObserver observer, Point pos, Dimension size) {		
		g.drawImage(image, pos.x, pos.y, size.width, size.height, observer);
	}

	public void reload() {
		// TODO Auto-generated method stub
		//System.out.println("reloading image file "+imageUrl);
		image = Toolkit.getDefaultToolkit().createImage(imageUrl);
	}

	public String toString() {
		return imageUrl.toString();
	}

}
