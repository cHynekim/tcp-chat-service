import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
			
			Thread inputHdlr = new InputHdlr(socket, is);
			inputHdlr.setDaemon(true);
			inputHdlr.start();
			Scanner scanner = new Scanner(System.in);
			
			while(true) {
				String input = scanner.nextLine();
				if(input.equalsIgnoreCase("file")) {
					System.out.println("File name? >> ");
					String fileName = scanner.nextLine();
					System.out.println("File extension? (remove .)");
					System.out.println("(e.g. aaa.jpg -> type jpg) >> ");
					String fileExt = scanner.nextLine();
					
					File file = new File("src/client_a/" + fileName + "." + fileExt);
					try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file))){
						os.writeUTF("filemsg" + socket.getLocalPort() + "." + fileExt);
						os.flush();
						
						byte[] data = new byte[2048];
						int len;
						while((len = fis.read(data)) != -1) {
							os.writeInt(len);
							os.write(data, 0, len);
							os.flush();
						}
						os.writeInt(-1);
						os.flush();
					}catch (FileNotFoundException e) {
						System.out.println("File Not Found : " + e);
					}
					
					
				} else if(input.equalsIgnoreCase("exit")) {
					os.writeUTF("<<Client" + socket.getLocalPort() + " left this chat>>");
					os.flush();
					scanner.close();
					is.close();
					os.close();
					socket.close();
					break;
					
				} else {
					os.writeUTF("Client" + socket.getLocalPort() + " : " + input);
					os.flush();
				}
				
			}
					
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
