import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
				String msg = is.readUTF();
				if(msg.startsWith("filemsg")) {
					System.out.println("Receiving " + msg + " ...");
					String filePath = "src/client" + this.socket.getLocalPort();
					Path dir = Paths.get(filePath);
					if(!Files.exists(dir)) Files.createDirectories(dir);
					
					File file = new File(filePath + "/" + msg);
					byte[] data = new byte[2048];
					int len;
					try (BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(file))){
						while((len = is.readInt()) != -1) {
							is.read(data, 0, len);
							fos.write(data, 0, len);
							fos.flush();
						}
						fos.flush();
					}catch (EOFException e) { e.printStackTrace(); }
					System.out.println("Received!");
					
				}else System.out.println(msg);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
