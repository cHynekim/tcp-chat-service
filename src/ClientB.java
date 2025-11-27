import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class ClientB {
	public static void main(String[] args) {
		System.out.println("<<Client B>>");
		try {
			Socket socket = new Socket(InetAddress.getByName("localhost"), 10000);
			System.out.println("Connected! : " + socket.getInetAddress() + ":" + socket.getPort());
			
			DataInputStream is = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			DataOutputStream os = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			
			Thread inputHdlr = new InputHdlr(socket, is);
			inputHdlr.setDaemon(true);
			inputHdlr.start();
			Scanner scanner = new Scanner(System.in);
			
			while(true) {
				String input = scanner.nextLine();
				os.writeUTF(input);
				os.flush();
				
				if(input.equalsIgnoreCase("exit")) {
					scanner.close();
					is.close();
					os.close();
					socket.close();
					break;
				} 
			}
					
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
