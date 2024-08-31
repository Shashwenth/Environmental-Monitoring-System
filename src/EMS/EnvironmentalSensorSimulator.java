package EMS;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import org.json.JSONObject;


public class EnvironmentalSensorSimulator {
	
	private static Properties properties = new Properties();
	private static String API_KEY;
    public EnvironmentalSensorSimulator() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                return;
            }

            // Load a properties file from class path
            properties.load(input);
            //System.out.println(properties.getProperty("api.key"));
            API_KEY =String.valueOf(properties.getProperty("api.key"));
            //System.out.println(API_KEY);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    
    private static final String LOCATION = "Jersey%20City";
    
    private Connection connect() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/Envi_Monitoring_Sys";
        String user = "root";
        String password = properties.getProperty("db.password");
        return DriverManager.getConnection(url, user, password);
    }

    private String fetchWeatherData() throws Exception {
        String urlString = "http://api.openweathermap.org/data/2.5/weather?q=" + LOCATION + "&appid=" + API_KEY + "&units=metric";
        System.out.println(urlString);
        @SuppressWarnings("deprecation")
		URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            conn.disconnect();
            return content.toString();
        } else {
            throw new Exception("Failed to fetch weather data. HTTP response code: " + responseCode);
        }
    }

    private void parseAndStoreWeatherData(String jsonResponse, Connection conn) throws SQLException {
        JSONObject jsonObject = new JSONObject(jsonResponse);
        double temperature = jsonObject.getJSONObject("main").getDouble("temp");
        double humidity = jsonObject.getJSONObject("main").getDouble("humidity");
        double windSpeed = jsonObject.getJSONObject("wind").getDouble("speed");
        String weatherDescription = jsonObject.getJSONArray("weather").getJSONObject(0).getString("description");

        insertData(conn, temperature, humidity, windSpeed, weatherDescription);
    }

    private void insertData(Connection conn, double temperature, double humidity, double windSpeed, String weatherDescription) throws SQLException {
        String query = "INSERT INTO weather_data (temperature, humidity, wind_speed, weather_description) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setDouble(1, temperature);
            pstmt.setDouble(2, humidity);
            pstmt.setDouble(3, windSpeed);
            pstmt.setString(4, weatherDescription);
            pstmt.executeUpdate();
        }
    }

    public void runSimulation() {
        try (Connection conn = connect()) {
            while (true) {
                String jsonResponse = fetchWeatherData();
                parseAndStoreWeatherData(jsonResponse, conn);
                Thread.sleep(5000); // Fetch data every 5 seconds
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new EnvironmentalSensorSimulator().runSimulation();
    }
}
