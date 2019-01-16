package com.twodigits.ldapreader;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

public class Ldapcheck {	
	
	private static String LDAP = "";
	private static String SEARCH_BASE = "";	
	private static String SECURITY_AUTHENTICATION = "simple";
	private static String SECURITY_PRINCIPAL = "";
	private static String[] ATTRIBUTES = null;

	public static void main(String[] args) {	
		
		if (args.length != 5) {
			System.out.println("usage: java -jar ldapreader.jar <ldap-server> <search-base> <security-principal> mail,givenName,sn,departmentNumber csv-with-uids.csv");
			System.exit(0);
		}			
		
		//check LDAP
		try {
			
			LDAP = args[0];
			SEARCH_BASE = args[1];
			SECURITY_PRINCIPAL = args[2];
			ATTRIBUTES = args[3].split(",");
			
			String inputfile = args[4];	
			
			//read file			
			System.out.println("reading "+inputfile);
			List<String> qnumbers = Files.readAllLines(new File(inputfile).toPath(), Charset.defaultCharset());
			
			//create users with LDAP
			List<HashMap<String,String>> users = new ArrayList<HashMap<String,String>>();
			long counter = 0;
			for (String qnumber : qnumbers) {
				HashMap<String,String> tmp = readLDAP(qnumber.replaceAll(",", "").replaceAll(";", ""), ATTRIBUTES);
				users.add(tmp);
				counter++;
				if (counter % 10 == 0) System.out.println(counter);
			}
			System.out.println(counter + " done.");
			
			//write result
			String outputfile = inputfile+".out.csv";
			FileWriter fw = new FileWriter(outputfile);	
			fw.write("uid;");
			for (String attribute : ATTRIBUTES) {
				fw.write(attribute+";");
			}
			fw.write("\n");
			for (HashMap<String,String> tmp : users) {
				fw.write(tmp.get("uid")+";");
				for (String attribute : ATTRIBUTES) {
					String value = tmp.get(attribute);
					if (value == null) value = "";
					fw.write(value+";");
				}				
				fw.write("\n");			
			}		 
			fw.close();
			System.out.println(outputfile + " written.");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * call LDAP
	 * @param qNumber
	 * @return
	 * @throws NamingException
	 */
	public static HashMap<String,String> readLDAP(String qnumber, String[] attributes) throws NamingException {					
				
		Hashtable<String,String> env = new Hashtable<String,String>();

		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"com.sun.jndi.ldap.LdapCtxFactory");
		//env.put(Context.SECURITY_AUTHENTICATION, "none");
		env.put(Context.PROVIDER_URL, LDAP);
		
		//extensions
		env.put(Context.SECURITY_AUTHENTICATION, SECURITY_AUTHENTICATION);
		env.put(Context.SECURITY_PRINCIPAL, SECURITY_PRINCIPAL);
		
		SearchControls scUserData = new SearchControls();

		scUserData.setSearchScope(SearchControls.SUBTREE_SCOPE);
		scUserData.setReturningAttributes(attributes);
		
		DirContext ctx = new InitialDirContext(env);
		NamingEnumeration<SearchResult> en = ctx.search(SEARCH_BASE, "uid={0}",
				new Object[] { qnumber }, scUserData);

		ctx.close();
		
		HashMap<String,String> user = new HashMap<String,String>();
		user.put("uid", qnumber);
		
		if (en.hasMore()) {
			
			SearchResult r = (SearchResult) en.next();
			Attributes a = r.getAttributes();			
			
			for (String attribute : attributes) {
				user.put(attribute, a.get(attribute) != null ? (String) a.get(attribute).get()	: "");
			}
			
		}	
		
		return user;
	}

}
