import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class InputHdlr extends Thread{
	Socket socket;
	DataInputStream is;
	
	public InputHdlr(Socket socket, DataInputStream is) {
		this.socket = socket;
		this.is = is;
	}

	public void run() {
		try {
			while(true) {
				String msg;
				if((msg = is.readUTF()) != null) System.out.println(msg);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
