
import java.io.*;
import java.net.*;
import java.lang.reflect.Array;

public class JavaProxy extends Thread
{
	public static final int DEFAULT_PORT = 8001;
	
	private ServerSocket server = null;
	private int thisPort = DEFAULT_PORT;
	private int ptTimeout = ProxyThread.DEFAULT_TIMEOUT;
	
	
	/* here's a main method, in case you want to run this by itself */
	public static void main (String args[])
	{
		int port = DEFAULT_PORT;
		
		
		// create and start the javaproxy thread, using a 20 second timeout
		JavaProxy jp = new JavaProxy(port);	
		jp.start();
		
		ManagementConsole console = new ManagementConsole();
		
		while (true)
		{
			try { Thread.sleep(3000); } catch (Exception e) {}
		}
		
	}
	
	/* the proxy server just listens for connections and creates
	 * a new thread for each connection attempt (the ProxyThread
	 * class really does all the work)
	 */
	//takes in just the port number
	public JavaProxy (int port) 	
	{
		thisPort = port;
	}
	
	public int getPort ()
	{
		return thisPort;
	}
	 
	/* return whether or not the socket is currently open
	 */
	public boolean isRunning ()
	{
		if (server == null)
			return false;
		else
			return true;
	}
	
	/* closeSocket will close the open ServerSocket; use this
	 * to halt a running jProxy thread
	 */
	public void closeSocket ()
	{
		try {
			server.close();
		}  catch(Exception e)  { 
		}
		server = null;
	}
	
	
	public void run()
	{
		try {
			// create a server socket, and loop forever listening for
			// client connections
			server = new ServerSocket(thisPort);
			System.out.println("Started proxy on port " + thisPort);
			
			while (true)
			{
				Socket client = server.accept();
				ProxyThread thread = new ProxyThread(client);
				thread.setTimeout(ptTimeout);
				thread.start();
			}
		}  catch (Exception e)  {
			System.out.println("jProxy Thread error: " + e);
		}
		closeSocket();
	}
	
}


/* 
 * The ProxyThread will take an HTTP request from the client
 * socket and send it to the server
 */
class ProxyThread extends Thread
{
	private Socket pSocket;
	private ProxyCache cache = new ProxyCache();
	private Block block = new Block();
	
	public static final int DEFAULT_TIMEOUT = 20 * 1000;
	private int socketTimeout = DEFAULT_TIMEOUT;
	
	
	public ProxyThread(Socket s)
	{
		pSocket = s;
	}
	
	public void setTimeout (int timeout)
	{
		socketTimeout = timeout * 1000;
	}
	@SuppressWarnings("resource")
	public void run()
	{
		try
		{
			long startTime = System.currentTimeMillis();
			// client streams 
			BufferedInputStream clientIn = new BufferedInputStream(pSocket.getInputStream());
			BufferedOutputStream clientOut = new BufferedOutputStream(pSocket.getOutputStream());
			
			Socket server = null;
			// other variables
			byte[] request = null;
			byte[] response = null;
			int requestLength = 0;
			int responseLength = 0;
			int pos = -1;
			StringBuffer host = new StringBuffer("");
			String hostName = "";
			int hostPort = 80;
			boolean blocked = false;
			
			// get the header info (the web browser won't disconnect after
			// it's sent a request, so make sure the waitForDisconnect
			// parameter is false)
			request = getHTTPData(clientIn, host, false);
			requestLength = Array.getLength(request);
			// separate the host name from the host port, if necessary
			// (like if it's "servername:8000")
			hostName = host.toString(); //get the url that is requested's host name
			pos = hostName.indexOf(":");
			if (pos > 0)
			{
				try { hostPort = Integer.parseInt(hostName.substring(pos + 1)); 
					}  catch (Exception e)  { }
				hostName = hostName.substring(0, pos);
			}
			
			String concat = hostName;
			if(hostName.contains("www.")==false)
			{
				concat = "www." + hostName;
			}
			
			if(block.contains(concat))
			{
				blocked = true;
			}
			
			
			server = new Socket(hostName, hostPort);
			
			
			
			// forward request from client(browser) to server
			if (server != null)
			{
				server.setSoTimeout(socketTimeout);
				BufferedInputStream serverIn = new BufferedInputStream(server.getInputStream()); //request from client
				BufferedOutputStream serverOut = new BufferedOutputStream(server.getOutputStream()); //response from server
				
				if(cache.checkCache(hostName) && !(blocked)) //if host is in the cache, send it back to client
				{
					byte array[] = cache.getResponse(hostName);
					if(array == null) //entry has expired
					{
						serverOut.write(request, 0, requestLength); //send request
						serverOut.flush(); 
						response = getHTTPData(serverIn, true); //get response back
						responseLength = Array.getLength(response); 
						serverIn.close();
						serverOut.close();
						clientOut.write(response, 0, responseLength); //transmit to client
						cache.cacheRequest(hostName, response); //update cache
					}
					else
					{
						System.out.println("URL " + hostName + " retrieved from the cache");
						clientOut.write(array, 0, Array.getLength(array)); 
						serverIn.close();
						serverOut.close();
					}
					
				}
				else if(blocked==false) //if not in cache and not blocked, transmit to client and cache if possible
				{
					serverOut.write(request, 0, requestLength); 
					serverOut.flush(); 
					response = getHTTPData(serverIn, true); 
					responseLength = Array.getLength(response); 
					serverIn.close();
					serverOut.close();
					clientOut.write(response, 0, responseLength); 
					cache.cacheRequest(hostName, response);
				}
				else //it is blocked
				{
					response = serverDenied();
					responseLength = Array.getLength(response); 
					serverIn.close();
					serverOut.close();
					clientOut.write(response, 0, responseLength);
				}
				
				
			}
			
			long endTime = System.currentTimeMillis();
			System.out.println("Request from " + pSocket.getInetAddress().getHostAddress() + 
					" on Port " + pSocket.getLocalPort() + 
					" to host " + hostName + ":" + hostPort + 
					"\n  (" + requestLength + " bytes sent, " + 
					responseLength + " bytes returned, " + 
					Long.toString(endTime - startTime) + " ms elapsed)");
			
			if (response != null && blocked == false)
			{
				System.out.println("REQUEST:\n" + (new String(request)));
			}
			
			// close all the client streams so we can listen again
			clientOut.close();
			clientIn.close();
			pSocket.close();
		}  catch (Exception e)  {
		}

	}
	
	
	private byte[] getHTTPData (InputStream in, boolean waitForDisconnect)
	{
		// get the HTTP data from an InputStream, and return it as
		// a byte array
		// the waitForDisconnect parameter tells us what to do in case
		// the HTTP header doesn't specify the Content-Length of the
		// transmission
		StringBuffer str = new StringBuffer(""); //can modify chars and faster for concatenation
		return getHTTPData(in, str, waitForDisconnect);
	}
	
	
	
	private byte[] getHTTPData (InputStream in, StringBuffer host, boolean waitForDisconnect)
	{
		// get the HTTP data from an InputStream, and return it as
		// a byte array, and also return the Host entry in the header,
		// if it's specified -- note that we have to use a StringBuffer
		// for the 'host' variable, because a String won't return any
		// information
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		streamHTTPData(in, bs, host, waitForDisconnect);
		return bs.toByteArray();
	}
	
	//return header.length + byte count
	private int streamHTTPData (InputStream in, OutputStream out, 
									StringBuffer host, boolean waitForDisconnect)
	{
		// get the HTTP data from an InputStream, and send it to
		// the designated OutputStream
		StringBuffer header = new StringBuffer("");
		String data = "";
		int responseCode = 200;
		int contentLength = 0;
		int pos = -1;
		int byteCount = 0;

		try
		{
			// get the first line of the header, so we know the response code
			data = readLine(in);
			if (data != null)
			{
				header.append(data + "\r\n");
				pos = data.indexOf(" ");
				if ((data.toLowerCase().startsWith("http")) && 
					(pos >= 0) && (data.indexOf(" ", pos+1) >= 0))
				{
					String rcString = data.substring(pos+1, data.indexOf(" ", pos+1));
					try
					{
						responseCode = Integer.parseInt(rcString);
					}  catch (Exception e)  {
					}
				}
			}
			
			// get the rest of the header info
			while ((data = readLine(in)) != null)
			{
				// the header ends at the first blank line
				if (data.length() == 0)
					break;
				header.append(data + "\r\n");
				
				// check for the Host header
				pos = data.toLowerCase().indexOf("host:");
				if (pos >= 0)
				{
					host.setLength(0);
					host.append(data.substring(pos + 5).trim());
				}
				
				// check for the Content-Length header
				pos = data.toLowerCase().indexOf("content-length:");
				if (pos >= 0)
					contentLength = Integer.parseInt(data.substring(pos + 15).trim());
			}
			
			// add a blank line to terminate the header info
			header.append("\r\n");
			// convert the header to a byte array, and write it to our stream
			out.write(header.toString().getBytes(), 0, header.length());
			
			// if the header indicated that this was not a 200 response,
			// just return what we've got if there is no Content-Length,
			// because we may not be getting anything else
			if ((responseCode != 200) && (contentLength == 0))
			{
				out.flush();
				return header.length();
			}

			// get the body, if any; we try to use the Content-Length header to
			// determine how much data we're supposed to be getting, because 
			// sometimes the client/server won't disconnect after sending us
			// information...
			if (contentLength > 0)
				waitForDisconnect = false;
			
			if ((contentLength > 0) || (waitForDisconnect))
			{
				try {
					byte[] buf = new byte[4096];
					int bytesIn = 0;
					while ( ((byteCount < contentLength) || (waitForDisconnect)) 
							&& ((bytesIn = in.read(buf)) >= 0) )
					{
						out.write(buf, 0, bytesIn);
						byteCount += bytesIn;
					}
				}  catch (Exception e)  {
					String errMsg = "Error getting HTTP body: " + e;
				}
			}
		}  catch (Exception e)  {
		}
		//flush the OutputStream and return
		try  {  out.flush();  }  catch (Exception e)  {}
		return (header.length() + byteCount);
	}
	
	
	private String readLine (InputStream in)
	{
		// reads a line of text from an InputStream
		StringBuffer data = new StringBuffer("");
		int c;
		
		try
		{
			// if we have nothing to read, just return null
			in.mark(1);
			if (in.read() == -1)
				return null;
			else
				in.reset();
			
			while ((c = in.read()) >= 0)
			{
				// check for an end-of-line character
				if ((c == 0) || (c == 10) || (c == 13))
					break;
				else
					data.append((char)c);
			}
		
			// deal with the case where the end-of-line terminator is \r\n
			if (c == 13)
			{
				in.mark(1);
				if (in.read() != 10)
					in.reset();
			}
		}  catch (Exception e)  {
			e.printStackTrace();
		}
		
		// and return what we have
		
		return data.toString();
	}
	
	@SuppressWarnings("resource")
	private byte[] serverDenied()
	{
	    File file = new File("forbidden.html");
		FileInputStream in=null;
		try {	
			in = new FileInputStream(file); 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		byte[] buffer = new byte[(int)file.length()];
		try {
			in.read(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return buffer;
	}  
}

