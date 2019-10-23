package cn.chart;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class Sever {
	
	/*
	 * java.net.SeverSocket
	 * 运行在服务端的Socket
	 * SeverSocket有两个主要作用:
	 * 1:向操作系统申请端口,客户端就是通过
	 * 	 这个端口与服务端应用程序建立连接的.
	 * 
	 * 2:监听服务端口,一旦用户端Socket通过
	 * 	 端口连接,这里就会感知到并自动创建
	 * 	 一个Socket与客户端建立连接.
	 * */
	private ServerSocket server;
	private List<PrintWriter> allOut;//该容器用于储存每个客户端的输出流
	
	public Sever() throws Exception{
		try {
			/*
			 * 实例化SeverSocket的同时,向
			 * 系统申请服务端口,若端口已被其他
			 * 应用程序占用,回抛出异常.
			 * */
			server = new ServerSocket(8088);
			allOut = new ArrayList<PrintWriter>();
			Collections.synchronizedList(allOut);
		} catch (Exception e) {
			throw e;
		}
	}

	public void start(){
		try {
			/*
			 * SeverSocket提供了方法:
			 * Socket accept()
			 * 该方法是一个阻塞方法,调用后会一直
			 * 监听端口,直到一个客户端通过该端口
			 * 建立连接,这时accept会返回一个Socket
			 * 通过这个Socket就可以与客户端通讯了
			 * */
			while(true){//循环接收不同客户端的连接
				System.out.println("等待一个客户端连接...");
				Socket socket = server.accept();
				System.out.println("与一个客户端建立了连接!");
				/*
				 * 启动线程,来完成与客户端的交互
				 * */
				ClientHandLer clientHandLer = new ClientHandLer(socket);
				
				Thread t = new Thread(clientHandLer);
				t.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		try {
			Sever sever = new Sever();
			sever.start();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("服务端启动失败");
		}
	}
	/**
	 * 该线程负责与指定客户端进行交互工作
	 * @author soft01
	 * */
	private class ClientHandLer implements Runnable{
		/*
		 * 该线程就是通过这个Socket与指定的客户端
		 * 交互
		 * */
		private Socket socket;
		private String host;
		
		public ClientHandLer(Socket socket){
			this.socket = socket;
			/*
			 * 通过Socket获取远端计算机地址信息
			 * 对于服务端而言,远端指的是
			 * 客户端
			 * */
			InetAddress address = socket.getInetAddress();
			//获取IP地址的字符串形式
			host = address.getHostAddress();
		}
		
		private void sendMessage(String mssage){
			synchronized (allOut) {
				for(PrintWriter e : allOut){
					e.println(host+"说"+mssage);
				}
			}
		}
		
		public void run(){
			
			PrintWriter p = null;
			try {
				//广播消息该用户上线
				
				InputStream is = socket.getInputStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(is,"UTF-8"));
				
				/*
				 * 通过Socket创建输出流,用于将消息发送
				 * 给客户端
				 * */
				
				OutputStream ots = socket.getOutputStream();
				OutputStreamWriter osw = new OutputStreamWriter(ots,"UTF-8");
				p = new PrintWriter(osw,true);
				
				/*
				 * 将输出流存入到共享集合中
				 * */
				synchronized (allOut) {
					allOut.add(p);
				}
				
				sendMessage(host+"上线了"+","+"在线人数"+allOut.size());
				
				/*
				 * 使用br.readLine()读取客户端发过来的
				 * 一行字符串时,由于客户端所在系统不同,那么
				 * 当客户端断开连接时这里执行的结果也不同.
				 * 当Linux的客户断开连接时:
				 * 	br.readLine方法会返回null
				 * 当windows的客户断开连接时:
				 * 	br.readLine方法会直接抛出异常
				 * */
				
				String str = null;
				while((str = br.readLine()) != null){
					System.out.println("服务端收到:===>>"+str);
					/*
					 * 遍历所有allOut集合,将消息发送给
					 * 所有客户端
					 * */
					sendMessage(str);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}finally {
				//处理客户端断开连接操作
				//将该客户端的输出流从共享集合中删除
				synchronized (allOut) {
					allOut.remove(p);
				}
				
				/*
				 * 该客户端下线了
				 * */
				sendMessage(host+"下线了"+","+"当前人数"+allOut.size());
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
