package de.srsoftware.gui.treepanel;

import javax.swing.JPanel;


/**
 * @author Stephan Richter
 * This class is used to perform the animations.
 * It is normally perfoms a repaint of the NodeTree every second.
 * Once it is activated, it performs 30 repaints within 3 seconds.
 */
public class TreeThread extends Thread {
	private JPanel mapper=null;
	private int i=200;
	
	
	/**
	 * ask the running thread to die. 
	 */
	public void die(){
		i=-1;
	}
	
	/**
	 * ask the running thread to increase the update frequence for n iterations
	 */
	public void go(){
		if (i>=0) // don't restart after thread was asked to die!
			i=30;
	}	
	
	/* 
	 * the actual thread. performs an update every second, unless
	 * i>0 (set by go() ). In each iteration, the mappers repaint()
	 * method is called once.
	 */
	public void run(){
  	i=25;
  	while (i>=0){
  		try {
  			if (mapper!=null) mapper.repaint();
  			if (i>0){
  			  i--;
				  Thread.sleep(100);
  			} else Thread.sleep(1000);
			} catch (InterruptedException e) {}
  	}
  }
	
  public void setTreeMapper(JPanel treeMapper){
		mapper=treeMapper;
	}
}