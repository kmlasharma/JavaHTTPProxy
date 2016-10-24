import java.util.ArrayList;

/*
 * Class to manage the blocking/unblocking of URLs.
 */
public class Block {
	
	public static ArrayList<String> blockedURL= new ArrayList<String>();
	
	/*
	 * Method to check that a url isnt already blocked,
	 * and adds it to the blocked list if it is not.
	 */
	public synchronized void addURL(String url)
	{
		if(blockedURL.contains(url)==false)
		{
			if(url.contains("www."))
			{
				blockedURL.add(url);
			}
			else
			{
				blockedURL.add(("www." + url));
			}
			
		}
	}
	
	public synchronized boolean contains(String url)
	{
		return blockedURL.contains(url);
	}
	
	public synchronized boolean removeURL(String url)
	{
		if(url.contains("www.")==false)
		{
			url = "www." + url;
		}
		
		if(blockedURL.contains(url)==true)
		{
			blockedURL.remove(url);
			return true;
		}
		return false; //wasnt blocked in the first place
	}

}
