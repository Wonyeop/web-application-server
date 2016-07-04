package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import model.User;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
	private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);
	
	private Socket connection;

	public RequestHandler(Socket connectionSocket) {
		this.connection = connectionSocket;
	}

	public void run() {
		log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(), connection.getPort());
		
		try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
			// TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
			
			BufferedReader br =new BufferedReader(new InputStreamReader(in));
			String line;
			String[] request = null;
			
			while( (line = br.readLine()) != null ){
				
				request = line.split("\\s+");
				log.debug("Print Log: " + line);
				if( line.startsWith("GET") ){
					get200(out, request);
				}
				if( line.startsWith("POST") ){
					post(request[1], br);
					get302(out, "GET /index.html".split("\\s+"));
				}
			}
			
			
//			log.debug("Print Log: " + request[0]);
//			log.debug("Print Log: " + request[1]);
			
			
			
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	private void post(String path, BufferedReader br) throws IOException {
//		br.readLine();	//	host
//		br.readLine();	//	connection
		int clen = getContentLength(br);
		
		log.debug("Print Log: " + path);
		
		String line;
		while( (line=br.readLine()) != null ){
			if( line.isEmpty() )
				break;
		}
		
		

		log.debug("Print Log: Read Http BODY");
		String signInInfo = new IOUtils().readData(br, clen);
		log.debug("Print Log: " + signInInfo);
		
		log.debug("Print Log: End reading Http BODY");
		
		Map<String, String> info = new HttpRequestUtils().parseQueryString(signInInfo);
		
		String[] enteredInfo = signInInfo.split("\\&");
//		log.debug("Print Log: " + enteredInfo[0]);
//		log.debug("Print Log: " + enteredInfo[1]);
//		log.debug("Print Log: " + enteredInfo[2]);
//		log.debug("Print Log: " + enteredInfo[3]);
//		
//		String uID = enteredInfo[0].split("=")[1];
//		String pwd = enteredInfo[1].split("=")[1];
//		String name = enteredInfo[2].split("=")[1];
//		String email = enteredInfo[3].split("=")[1];
		
		User user = new User(info.get("userId"), info.get("password"), info.get("name"), info.get("email"));
	}

	private int getContentLength(BufferedReader br) throws IOException {
		String line;
		while( (line=br.readLine()) != null ){
			if( line.startsWith("Content-Length") ){
				break;
			}
		}
		return Integer.parseInt(line.split("\\:")[1].trim());
	}

	private void get302(OutputStream out, String[] request) throws IOException {
		log.debug("Print Log: Success read a file");
		String page_path = request[1];
		DataOutputStream dos = new DataOutputStream(out);
		byte[] body = Files.readAllBytes(new File("./webapp" + page_path).toPath());
		response302Header(dos, body.length);
		responseBody(dos, body);
	}
	
	private void get200(OutputStream out, String[] request) throws IOException {
		log.debug("Print Log: Success read a file");
		String page_path = request[1];
		DataOutputStream dos = new DataOutputStream(out);
		byte[] body = Files.readAllBytes(new File("./webapp" + page_path).toPath());
		response200Header(dos, body.length);
		responseBody(dos, body);
	}
	
	private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
		try {
			dos.writeBytes("HTTP/1.1 200 OK \r\n");
			dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
			dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	
	private void response302Header(DataOutputStream dos, int lengthOfBodyContent) {
		try {
			dos.writeBytes("HTTP/1.1 302 OK \r\n");
			dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
			dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	
	private void responseBody(DataOutputStream dos, byte[] body) {
		try {
			dos.write(body, 0, body.length);
			dos.flush();
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
}
