import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
/*
 * Class to manage the caching and evicting/updating of the websites
 */
public class ProxyCache {
	
	public static Hashtable<String, byte[]> cache = new Hashtable<String, byte[]>(); //url name and response
	public static Hashtable<String, String> evictTimes = new Hashtable<String, String>(); //url name and (time put in + eviction time in seconds)
	DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
	Date time = new Date(); 
	
	/*
	 * Function that takes in the url and it's response, and checks
	 * if it can be cached. If possible, it will be stored in the cache
	 * along with the max amount of time allowed for the cache. Otherwise
	 * it will be printed to the console that the url in un-cachable.
	 */
	public synchronized void cacheRequest(String url, byte[] response)
	{
		boolean cached = false;
		String responseStr = new String(response);
		responseStr = responseStr.toLowerCase();
		String line = "";
		String array[] = responseStr.split("\n");
		String durationInCache="";
		String times = "";
		
		for(int i=0; i<array.length; i++)
		{
			if(array[i].contains("max-age"))
			{
				
				line = array[i];
				int pos = line.indexOf("max-age=") + 1;
				durationInCache = line.substring(pos+7, line.length()-1);
				pos = durationInCache.indexOf(';');
				if (pos != -1) 
					durationInCache = durationInCache.substring(0, pos);
				cache.put(url, response);
				String cachedAt = timeFormat.format(time);
				int duration = Integer.parseInt(durationInCache);
				int hours = duration / 3600;
				int minutes = (duration % 3600) / 60;
				int seconds = duration % 60;
				String durationString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
				times = cachedAt + "," + durationString;
				evictTimes.put(url, times);
				cached = true;
				break;
			}
		}
		if(!cached)
		{
			System.out.println(url + " cannot be cached.");
		}
		
		System.out.println("The cache: " + cache + "\nEvictions: " + evictTimes);
	}

	/*
	 * Returns whether or not a url is cached
	 */
	public synchronized boolean checkCache(String url)
	{
		return cache.containsKey(url);
	}
	
	/*
	 * Function that returns null if url is expired
	 * or returns the response if the url is
	 * still cached
	 */
	public synchronized byte[] getResponse(String url)
	{
		if(isExpired(url)==false)
		{
			return cache.get(url);
		}
		else
		{
			return null;
		}
	}
	
	/*
	 * Function that will return either true or false if a 
	 * given url is expired. The timestamps are compared
	 * and if it is expired, the url will be removed
	 * from both caches.
	 */
	public synchronized boolean isExpired(String url)
	{
		String times[] = (evictTimes.get(url)).split(",");
		String cachedTime = times[0];
		String durationTime = times[1];
		timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date cached = null;
		try {
			cached = timeFormat.parse(cachedTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Date duration = null;
		try {
			duration = timeFormat.parse(durationTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		long sum = cached.getTime() + duration.getTime();
		String result = timeFormat.format(new Date(sum));
		Date expiryTime=null;
		try {
			expiryTime = timeFormat.parse(result);
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		String currentTime = timeFormat.format(time);
		Date current = null;
		try {
			current = timeFormat.parse(currentTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		if(expiryTime.before(current))
		{
		    System.out.println("Entry: " + url + " has expired, and must be re-requested and updated in the cache.");
		    cache.remove(url);
		    evictTimes.remove(url);
		    return true;
		}
		else
		{
			System.out.println("Entry has NOT expired yet.");
			return false;
		}
		
	}
	
	public String getString()
	{
		 Set blockedURLs = cache.keySet();
		 String names = blockedURLs + "";
		 System.out.println(names);
		 return names;
	}


}