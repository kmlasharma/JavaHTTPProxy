import javax.swing.JOptionPane;

/*
 * Class that allows the user to interact with the 
 * proxy by blocking/unblocking websites and viewing 
 * the cache
 */
public class ManagementConsole {

	
	private static Block block;
	private static ProxyCache pcache;
	public ManagementConsole()
	{
		block = new Block();
		while(true)
		{
			int question = JOptionPane.showOptionDialog(null,
					"What would you like to do?", "Java Proxy Blocker",
					JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null,
					new String[] { "Block Website", "Unblock Website", "Show blocked websites", "View the Cache"}, null);

			String website = "";
			switch (question) 
			{
			case 0: // if block chosen
				website = JOptionPane.showInputDialog("Please enter the website to block: eg. www.google.com");
				block.addURL(website);
				break;
			case 1: // if unblock chosen
				website = JOptionPane.showInputDialog("Please enter the website to unblock: eg. www.google.com");
				if(block.removeURL(website)==false)
				{
					JOptionPane.showMessageDialog(null, "That url is not currently blocked");
				}
				break;
			case 2: // if show blocked websites
				JOptionPane.showMessageDialog(null, "The following websites are blocked: " + block.blockedURL);
				break;
			case 3: // if viewing the cache
				JOptionPane.showMessageDialog(null, "The following websites are in the cache: " + pcache.cache);
				break;
			case -1: // if exit button is clicked
				System.exit(0);
				break;

			}
		}
	}

}
