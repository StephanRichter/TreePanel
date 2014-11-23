package de.srsoftware.gui.treepanel;

import java.util.ConcurrentModificationException;

import javax.swing.JPanel;


public class TreeThread extends Thread {
	private JPanel mapper=null;
	private int i=200;
	public void die(){
		i=-1;
	}
	
	public void go(){
		if (i>=0) // don't restart after thread was asked to die!
			i=30;
	}	
	
	public void run(){
  	i=25;
  	while (i>=0){
  		try {
  			if (mapper!=null) {
  				mapper.repaint();
  			}
  			if (i>0){
  			  i--;
				  Thread.sleep(100);
  			} else Thread.sleep(1000);
			} catch (InterruptedException e) {				
			} catch (ConcurrentModificationException e){
				try {
					sleep(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
  	}
  }
	
  public void setTreeMapper(JPanel treeMapper){
		mapper=treeMapper;
	}
}