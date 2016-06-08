package main;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamGobbler extends Thread{
	private InputStream is;
	private String type;

	public StreamGobbler(InputStream is, String type){
		this.is = is; 
		this.type = type;
	}
	
	public void run(){
		try{
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line = null;
			
			while((line = br.readLine()) != null){
				OnlineAPE.log.debug(type + "--" + line);
			}
		}catch(Exception e){
			OnlineAPE.log.error(e);
		}
	}
}
