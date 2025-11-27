import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class ClientA {
	public static void main(String[] args) {
		System.out.println("<<Client A>>");
		try {
			Socket socket = new Socket(InetAddress.getByName("localhost"), 10000);
			System.out.println("Connected! : " + socket.getInetAddress() + ":" + socket.getPort());
			
			DataInputStream is = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			DataOutputStream os = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			
			Scanner scanner = new Scanner(System.in);
			
			while(true) {
				String input = scanner.nextLine();
				os.writeUTF(input);
				os.flush();
				
				if(input.equalsIgnoreCase("exit")) break;
				
				String msg;
				if((msg = is.readUTF()) != null) System.out.println(msg);
			}
			
					
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
