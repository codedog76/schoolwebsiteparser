package com.company;

import com.opencsv.CSVWriter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

import com.google.gson.Gson;
import sun.security.krb5.internal.crypto.Des;

public class Main {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";
    private static final String WORK_ADDRESS = "22 Park Lane, Century City, Cape Town";
    private static Gson gson = new Gson();

    private List<School> schoolList;

    public static void main(String[] args) {
        crawl("http://www.schools4sa.co.za/phase/primary-school/western-cape/cape-town.php");
    }

    public static boolean crawl(String url) {
        try {
            Connection schoolNamesConnection = Jsoup.connect(url).userAgent(USER_AGENT); // creates a valid user to browse with, else thinks it is a bot
            Document schoolNamesHtmlDocument = schoolNamesConnection.get(); // get all data on page

            //check if page is valid:
            if (schoolNamesConnection.response().statusCode() == 200) // 200 is the HTTP OK status code
            // indicating that everything is great.
            {
                System.out.println("\n**Visiting** Received web page at " + url);
            }
            if (!schoolNamesConnection.response().contentType().contains("text/html")) {
                System.out.println("**Failure** Retrieved something other than HTML");
                return false;
            }

            Elements schoolNames = schoolNamesHtmlDocument.select("div[class=\"school-name h3\"]");

            int count = 0;
            String csv = "data.csv";

            CSVWriter writer = new CSVWriter(new FileWriter(csv, true));

            String[] titles = { "School Name", "National EMIS No", "Phase", "Email", "Longitude", "Latitude", "Address",
                    "Postal Address", "Telephone Number", "Fax Number", "Cellphone Number", "Distance", "Duration"};
            writer.writeNext(titles);

            for (Element element : schoolNames) {
                School newSchool = new School();
                newSchool.schoolName = Jsoup.parse(element.toString()).text();
                String htmlAddress = "http://www.schools4sa.co.za/school-profile/" + newSchool.schoolName.replace(" ", "-") + "/";
                System.out.println(htmlAddress);
                Connection schoolConnection = Jsoup.connect(htmlAddress).userAgent(USER_AGENT);
                Document schoolHtmlDocument = schoolConnection.get();
                String plainText = schoolHtmlDocument.text();

                if (plainText.contains("National EMIS NO:")) {
                    newSchool.emisNo = plainText.substring(plainText.indexOf("National EMIS NO:") + 17, plainText.indexOf("Phase:")).replaceAll("\u00A0", "").trim();
                    plainText = plainText.substring(plainText.indexOf("Phase:"));
                }
                if (plainText.contains("Phase:")) {
                    newSchool.phase = plainText.substring(plainText.indexOf("Phase:") + 6, plainText.indexOf("Specialise:")).replaceAll("\u00A0", "").trim();
                    plainText = plainText.substring(plainText.indexOf("Specialise:"));
                }
                if (plainText.contains("Email:")) {
                    newSchool.email = plainText.substring(plainText.indexOf("Email:") + 6, plainText.indexOf("Phase:")).replaceAll("\u00A0", "").trim();
                    plainText = plainText.substring(plainText.indexOf("Phase:"));
                }

                htmlAddress = "http://www.southafricanschools.net/school.php?q=" + newSchool.schoolName.replace(" ", "%20") + ".";
                Connection addressConnection = Jsoup.connect(htmlAddress).userAgent(USER_AGENT);
                Document addressHtmlDocument = addressConnection.get();
                plainText = addressHtmlDocument.text();

                if (plainText.contains("Longitutde")) {
                    newSchool.longitude = plainText.substring(plainText.indexOf("Longitutde") + 10, plainText.indexOf("Latitude")).replaceAll("\u00A0", "").trim();;
                    plainText = plainText.substring(plainText.indexOf("Latitude"));
                }
                if (plainText.contains("Latitude")) {
                    newSchool.latitude = plainText.substring(plainText.indexOf("Latitude") + 8, plainText.indexOf("Magisterial District")).replaceAll("\u00A0", "").trim();
                    plainText = plainText.substring(plainText.indexOf("Magisterial District"));
                }
                if (plainText.contains("Address:")) {
                    newSchool.address = plainText.substring(plainText.indexOf("Address:") + 8, plainText.indexOf("Postal Address:")).replaceAll("\u00A0", "").trim();
                    plainText = plainText.substring(plainText.indexOf("Postal Address:"));
                }
                if (plainText.contains("Postal Address:")) {
                    newSchool.postalAddress = plainText.substring(plainText.indexOf("Postal Address:") + 15, plainText.indexOf("Telephone Number")).replaceAll("\u00A0", "").trim();
                    plainText = plainText.substring(plainText.indexOf("Telephone Number"));
                }
                if (plainText.contains("Telephone Number")) {
                    newSchool.telephoneNumber = plainText.substring(plainText.indexOf("Telephone Number") + 16, plainText.indexOf("Fax Number")).replaceAll("\u00A0", "").trim();
                    plainText = plainText.substring(plainText.indexOf("Telephone Number"));
                }
                if (plainText.contains("Fax Number")) {
                    newSchool.faxNumber = plainText.substring(plainText.indexOf("Fax Number") + 10, plainText.indexOf("Cellphone Number")).replaceAll("\u00A0", "").trim();
                    plainText = plainText.substring(plainText.indexOf("Fax Number"));
                }
                if (plainText.contains("Cellphone Number")) {
                    newSchool.cellphoneNumber = plainText.substring(plainText.indexOf("Cellphone Number") + 16, plainText.indexOf("Learner / Teacher")).replaceAll("\u00A0", "").trim();
                    plainText = plainText.substring(plainText.indexOf("Learner / Teacher"));
                }

                String origin = WORK_ADDRESS;
                String destination = newSchool.schoolName + ", Cape Town";

                System.out.println("---------------------------------------------------------------------------------------------");
                count++;
                System.out.println(count +  "/" + schoolNames.size() + " - Destination: " + destination);

                String URL = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin.replaceAll(" ", "+") + "&destination=" + destination.replaceAll(" ", "+") + "&key=AIzaSyDKCC18vdnZOUwElFjkcAHoavpDZF8TH-s";

                Destination staff = gson.fromJson(readJsonFromUrl(URL), Destination.class);
                if(staff.routes.size()!= 0 && staff.routes.get(0).legs.size() != 0) {
                    String distance = staff.routes.get(0).legs.get(0).distance.text.replace("km", "").replace(",", "").trim();
                    System.out.println("Distance: " + distance + " km");
                    if(Double.parseDouble(distance)<40) {
                        String duration = staff.routes.get(0).legs.get(0).duration.text.replace("mins", "").trim();
                        System.out.println("Duration: " + duration + " mins");
                        String[] data = {newSchool.schoolName, newSchool.emisNo, newSchool.phase, newSchool.email, newSchool.longitude, newSchool.latitude, newSchool.address,
                                newSchool.postalAddress, newSchool.telephoneNumber, newSchool.faxNumber, newSchool.cellphoneNumber, distance + " km", duration + " mins"};
                        writer.writeNext(data);
                    } else {
                        String[] data = {newSchool.schoolName, newSchool.emisNo, newSchool.phase, newSchool.email, newSchool.longitude, newSchool.latitude, newSchool.address,
                                newSchool.postalAddress, newSchool.telephoneNumber, newSchool.faxNumber, newSchool.cellphoneNumber, distance + " km"};
                        writer.writeNext(data);
                    }
                }

            }
            writer.close();
            return true; // success!
        } catch (IOException ioe) {
            System.out.println("IOException: " + ioe.getMessage());
            return false;
        }
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static String readJsonFromUrl(String url) throws IOException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            return jsonText;
        } finally {
            is.close();
        }
    }
}
