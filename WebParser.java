/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webparser;

import java.io.*;
import java.net.URL;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

/**
 * @author Angus Young
 */
public class WebParser {

    private static String websiteURL = "";
    private static boolean websiteURLNotValid = true;
    private static String folder = "c:\\webParserResults";
    private static boolean folderNotValid = true;
    private static boolean skipSameName = false;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //Variables
        String answer; //Used to store user input
        int numberOfPages = 20; //default limit on pages to scan
        boolean notValid = true;
        
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        
        try {
            //While the URL is considered not valid
            while (websiteURLNotValid){
                System.out.println("Enter the site you wish to extract pictures from:");
                websiteURL = br.readLine();
                if (websiteURL.length() < 1) {
                    System.out.println("Please enter a URL ");
                } else if (!websiteURL.startsWith("http://")) {
                    websiteURL = "http://" + websiteURL;
                    websiteURLNotValid = checkWebsite(websiteURL);
                } else {
                    websiteURLNotValid = checkWebsite(websiteURL);
                }
            }
            //Ask if they want to change from the normal directory
            System.out.println("Would you like to use an alternative folder then " + folder + " ?");
            answer = br.readLine();
            if (answer.startsWith("y") || answer.startsWith("Y")) {
                //While the directory
                while (folderNotValid) {
                    System.out.println("What folder would like the pictures be saved to ?");
                    folder = br.readLine();
                    if (new File(folder.split(":")[0] + ":/").exists()) {
                        folderNotValid = false;
                        System.out.println("The new folder for the pictures will be saved here " + folder);
                    } else {
                        System.out.println("Sorry we are not picking up the drive " + folder);
                    }
                }
            }

            //If the folder doesn't exist then create one
            if (!new File(folder).isDirectory()) {
                new File(folder).mkdirs();
            }

            //If the website supports multiple pages
            if (websiteURL.contains("tumblr") || websiteURL.contains("imgur")) {
                System.out.println("We have detected that the website you have entered supports multiple pages \nWould you like to set a custom page limit (Default is 20) ?");
                answer = br.readLine();
                //If the answer starts with y then enter
                if (answer.startsWith("y") || answer.startsWith("Y")) {
                    while (notValid) // While the input is not valid keep asking
                    {
                        System.out.println("How many pages would like to scrape for images ? (Use number value eg. 11)");
                        answer = br.readLine();
                        //Check and make sure the input is valid
                        try {
                            numberOfPages = Integer.parseInt(answer);
                            notValid = false;
                        } catch (NumberFormatException e) {
                            System.out.println("Please enter a number value (11) *without brackets ");
                        }
                    }
                }
            }
            //See if user wants to skip files with the same name
            System.out.println("Would you like to skip files with the same name ?");
            answer = br.readLine();
            if(answer.startsWith("y") || answer.startsWith("Y")){skipSameName = true;}
        } catch (IOException ex) {
            Logger.getLogger(WebParser.class.getName()).log(Level.SEVERE, null, ex);
        }

        //Try and connect to the website
        try {
            //Connect to the website and get html
            Document doc = Jsoup.connect(websiteURL).timeout(10000).get();

            //Get all the elements with img tag
            Elements links = doc.getElementsByTag("IMG");

            //If the website is tumblr scan multiple pages for content
            if (websiteURL.contains("tumblr") || websiteURL.contains("imgur")) {
                for (int num = 2; num <= numberOfPages; num++) {
                    try {
                        if (websiteURL.endsWith("\\") || websiteURL.endsWith("/")) {
                            doc = Jsoup.connect(websiteURL + "page/" + num).timeout(10000).get();
                        } else {
                            doc = Jsoup.connect(websiteURL + "/page/" + num).timeout(10000).get();
                        }
                    } catch (IOException ex) {
                        System.out.println("Found all relevant pages");
                        break;
                    }
                    links.addAll(doc.getElementsByTag("IMG"));

                    if (websiteURL.endsWith("\\") || websiteURL.endsWith("/")) {
                        System.out.println("Getting content from " + websiteURL + "page/" + num + "\n");
                    } else {
                        System.out.println("Getting content from " + websiteURL + "/page/" + num + "\n");
                    }
                }
            }
            System.out.println("There is " + links.size() + " possible pictures found.");

            //For each image download and save
            int i = 0;
            for (Element el : links) {
                i++;
                System.out.println("Picutre #" + i);
                String src = el.absUrl("src");
                //fix for imgur
                if (websiteURL.contains("imgur")) {
                    src = src.replace("b.", ".");
                }
                System.out.println("Image found src att is : " + src + "\n");
                getAndSaveImages(src);
            }
        } catch (IOException ex) {
            System.err.println("Sorry there was an error while trying to retieve the images :( \n");
            Logger.getLogger(WebParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Gets and downloads a image to the specified folder
     *
     * @param src The link to the image being downloaded
     */
    private static void getAndSaveImages(String src) {
        int randNum; //Number dupilcate pictures

        //Exctract the name of the image from the src attribute
        try {
            int indexname = src.lastIndexOf("/");

            if (indexname == src.length()) {
                src = src.substring(1, indexname);
            }
            indexname = src.lastIndexOf("/");
            String name = src.substring(indexname, src.length());

            //If the name contains a "?" clean name up
            if (name.contains("?")) {
                name = name.split("\\?")[0];
            }
            // if the name already exists then keep incrementing till a unique one is found
            while (new File(folder + name).isFile() && !skipSameName) {
                //if the file doesn't have an extension just tack on duplicate number
                randNum = randInt(0, 800);
                if (name.contains(".")) {
                    name = name.split("\\.")[0] + randNum + "." + name.split("\\.")[1];
                } else {
                    name = name.split("\\.")[0] + randNum;
                }
            }

            if ("/".equals(name) || "".equals(name) || "\\".equals(name) || "/impixu".equals(name)) {
                System.out.println("Sorry there is no real link to a picture \n");
            }
            else if(new File(folder + name).isFile() && skipSameName){
                System.out.println("Skipping file with the same name");
            }
            else {
                //Open a URL Stream
                URL url = new URL(src);
                InputStream in = url.openStream();
                OutputStream out = new BufferedOutputStream(new FileOutputStream(folder + name));

                for (int b; (b = in.read()) != -1;) {
                    out.write(b);
                }
                out.close();
                in.close();
            }
        } catch (IOException ex) {
            System.err.println("Sorry unable to get that image there was an error \n");
            Logger.getLogger(WebParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Produces a random int for handling images with the same name
     *
     * @param min the lowest number for the random value
     * @param max the highest number for the random value
     * @return randomNum a random generated by the method
     */
    public static int randInt(int min, int max) {
        Random rand = new Random();
        int randomNum = rand.nextInt((max - min) + 1) + min;
        return randomNum;
    }

    /**
     * Checks to see if the website is valid and alive
     *
     * @param websiteURL the url of the website
     * @return true if site is down
     */
    public static boolean checkWebsite(String websiteURL) {
        try {
            Jsoup.connect(websiteURL).timeout(10000).get();
            System.out.println("Connection successfull \n");
            return false;
        } catch (IOException ex) {
            System.out.println("Sorry we could not connect to the site "
                    + websiteURL + "please re-enter the url and try again. \n");
            return true;
        }
    }

}
