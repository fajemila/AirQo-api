package airqo;

import airqo.models.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.util.*;

public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    private static final DecimalFormat decimalFormat = new DecimalFormat("#.##");

    public static List<TransformedMeasurement> transformMeasurements(String rawMeasurements, Properties properties) {

        if (rawMeasurements.startsWith("\""))
            rawMeasurements = rawMeasurements.replaceFirst("\"", "");

        if (rawMeasurements.endsWith("\""))
            rawMeasurements = rawMeasurements.substring(0, rawMeasurements.length() - 1);

        rawMeasurements = rawMeasurements.replace("\\\"", "\"");

        logger.info("\n====> Measurements Received {}\n", rawMeasurements);

        try {

            String tenant = properties.getProperty("tenant");

            switch (tenant.trim().toUpperCase()) {
                case "KCCA":
                    return transformKccaMeasurements(rawMeasurements, properties);

                case "AIRQO":
//                        Runnable runnable1 = new InsertMeasurements(transformedMeasurements, baseUrl, tenant);
//                        new Thread(runnable1).start();
                    return transformAirQoMeasurements(rawMeasurements, properties);

                default:
                    return new ArrayList<>();
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static TransformedDeviceMeasurements generateTransformedOutput(List<TransformedMeasurement> transformedMeasurements) {

        logger.info("\n====> Total Measurements to be transformed : {}\n", transformedMeasurements.size());
        logger.info("\n====> Measurements to be transformed : {}\n", transformedMeasurements);

        List<Measurement> measurements = new ArrayList<>();

        transformedMeasurements.forEach(transformedMeasurement -> {

            Measurement measurement;
            try {
                measurement = Measurement.newBuilder()
                        .setDevice(transformedMeasurement.getDevice())
                        .setTenant(getTenant(transformedMeasurement.getTenant()))
                        .setFrequency(getFrequency(transformedMeasurement.getFrequency()))
                        .setTime(transformedMeasurement.getTime())
                        .setSiteId(transformedMeasurement.getSiteId())
                        .setDeviceNumber(transformedMeasurement.getDeviceNumber())
                        .setDeviceId(transformedMeasurement.getDeviceId())
                        .setLocation(location.newBuilder()
                                .setLatitude(transformedMeasurement.getLocation().getLatitude().getValue())
                                .setLongitude(transformedMeasurement.getLocation().getLongitude().getValue())
                                .build())
                        .setPm1(pm1.newBuilder()
                                .setValue(transformedMeasurement.getPm1().getValue())
                                .setCalibratedValue(transformedMeasurement.getPm1().getCalibratedValue())
                                .setStandardDeviationValue(transformedMeasurement.getPm1().getStandardDeviationValue())
                                .setUncertaintyValue(transformedMeasurement.getPm1().getUncertaintyValue())
                                .build())
                        .setPm25(pm2_5.newBuilder()
                                .setValue(transformedMeasurement.getPm2_5().getValue())
                                .setCalibratedValue(transformedMeasurement.getPm2_5().getCalibratedValue())
                                .setStandardDeviationValue(transformedMeasurement.getPm2_5().getStandardDeviationValue())
                                .setUncertaintyValue(transformedMeasurement.getPm2_5().getUncertaintyValue())
                                .build())
                        .setS2Pm25(s2_pm2_5.newBuilder()
                                .setValue(transformedMeasurement.getS2_pm2_5().getValue())
                                .setCalibratedValue(transformedMeasurement.getS2_pm2_5().getCalibratedValue())
                                .setStandardDeviationValue(transformedMeasurement.getS2_pm2_5().getStandardDeviationValue())
                                .setUncertaintyValue(transformedMeasurement.getS2_pm2_5().getUncertaintyValue())
                                .build())
                        .setS2Pm10(s2_pm10.newBuilder()
                                .setValue(transformedMeasurement.getS2_pm10().getValue())
                                .setCalibratedValue(transformedMeasurement.getS2_pm10().getCalibratedValue())
                                .setStandardDeviationValue(transformedMeasurement.getS2_pm10().getStandardDeviationValue())
                                .setUncertaintyValue(transformedMeasurement.getS2_pm10().getUncertaintyValue())
                                .build())
                        .setPm10(pm10.newBuilder()
                                .setValue(transformedMeasurement.getPm10().getValue())
                                .setCalibratedValue(transformedMeasurement.getPm10().getCalibratedValue())
                                .setStandardDeviationValue(transformedMeasurement.getPm10().getStandardDeviationValue())
                                .setUncertaintyValue(transformedMeasurement.getPm10().getUncertaintyValue())
                                .build())
                        .setNo2(no2.newBuilder()
                                .setValue(transformedMeasurement.getNo2().getValue())
                                .setCalibratedValue(transformedMeasurement.getNo2().getCalibratedValue())
                                .setStandardDeviationValue(transformedMeasurement.getNo2().getStandardDeviationValue())
                                .setUncertaintyValue(transformedMeasurement.getNo2().getUncertaintyValue())
                                .build())
                        .setBattery(battery.newBuilder()
                                .setValue(transformedMeasurement.getBattery().getValue()).build())
                        .setAltitude(altitude.newBuilder()
                                .setValue(transformedMeasurement.getAltitude().getValue()).build())
                        .setSpeed(speed.newBuilder()
                                .setValue(transformedMeasurement.getSpeed().getValue()).build())
                        .setSatellites(satellites.newBuilder()
                                .setValue(transformedMeasurement.getSatellites().getValue()).build())
                        .setHdop(hdop.newBuilder()
                                .setValue(transformedMeasurement.getHdop().getValue()).build())
                        .setInternalHumidity(internalHumidity.newBuilder()
                                .setValue(transformedMeasurement.getInternalHumidity().getValue())
                                .build())
                        .setInternalTemperature(internalTemperature.newBuilder()
                                .setValue(transformedMeasurement.getInternalTemperature().getValue())
                                .build())
                        .setExternalHumidity(externalHumidity.newBuilder()
                                .setValue(transformedMeasurement.getExternalHumidity().getValue())
                                .build())
                        .setExternalTemperature(externalTemperature.newBuilder()
                                .setValue(transformedMeasurement.getExternalTemperature().getValue())
                                .build())
                        .setExternalPressure(externalPressure.newBuilder()
                                .setValue(transformedMeasurement.getExternalPressure().getValue())
                                .build())
                        .build();

                measurements.add(measurement);

            } catch (Exception e) {
                e.printStackTrace();
            }

        });

        logger.info("\nTotal Measurements to be sent: " + measurements.size());

        return TransformedDeviceMeasurements.newBuilder()
                .setMeasurements(measurements)
                .build();
    }

    public static List<TransformedMeasurement> transformKccaMeasurements(String rawMeasurements, Properties properties) {

        List<RawKccaMeasurement> deviceMeasurements;

        try {

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
            deviceMeasurements = objectMapper.readValue(rawMeasurements, new TypeReference<>() {
            });

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

        String urlString = properties.getProperty("airqo.base.url", "");
        List<Device> devices = getDevices(urlString, "kcca");

        List<TransformedMeasurement> transformedMeasurements = new ArrayList<>();

        deviceMeasurements.forEach(rawMeasurement -> {

            Device device = getDeviceByName(devices, rawMeasurement.getDeviceCode());
            logger.info(device.getSite().get_id());
            if (!device.getSite().get_id().isEmpty()) {
                try {
                    TransformedMeasurement transformedMeasurement = new TransformedMeasurement();

                    transformedMeasurement.setDevice(rawMeasurement.getDeviceCode());
                    transformedMeasurement.setTenant("kcca");
                    transformedMeasurement.setSiteId(device.getSite().get_id());
                    transformedMeasurement.setDeviceId(device.get_id());
                    transformedMeasurement.setTime(rawMeasurement.getTime());
                    transformedMeasurement.setFrequency(rawMeasurement.getAverage());

                    List<Double> coordinates = rawMeasurement.getLocation().getCoordinates();
                    transformedMeasurement.setLocation(new TransformedLocation() {{
                        setLongitude(new LocationValue(coordinates.get(0)));
                        setLatitude(new LocationValue(coordinates.get(1)));
                    }});

                    transformedMeasurement.setPm10(new TransformedValue() {{
                        setValue(rawMeasurement.getCharacteristics().getPm10ConcMass().getRaw());
                        setCalibratedValue(rawMeasurement.getCharacteristics().getPm10ConcMass().getCalibratedValue());
                    }});

                    transformedMeasurement.setExternalHumidity(new TransformedValue() {{
                        setValue(rawMeasurement.getCharacteristics().getRelHumid().getRaw());
                    }});

                    transformedMeasurement.setExternalTemperature(new TransformedValue() {{
                        setValue(rawMeasurement.getCharacteristics().getTemperature().getRaw());
                    }});

                    transformedMeasurement.setPm2_5(new TransformedValue() {{
                        setValue(rawMeasurement.getCharacteristics().getPm2_5ConcMass().getRaw());
                        setCalibratedValue(rawMeasurement.getCharacteristics().getPm2_5ConcMass().getCalibratedValue());
                    }});

                    transformedMeasurement.setNo2(new TransformedValue() {{
                        setValue(rawMeasurement.getCharacteristics().getNo2Conc().getRaw());
                        setCalibratedValue(rawMeasurement.getCharacteristics().getNo2Conc().getCalibratedValue());
                    }});

                    transformedMeasurement.setPm1(new TransformedValue() {{
                        setValue(rawMeasurement.getCharacteristics().getPm1ConcMass().getRaw());
                        setCalibratedValue(rawMeasurement.getCharacteristics().getPm1ConcMass().getCalibratedValue());
                    }});

                    transformedMeasurements.add(transformedMeasurement);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

        });

        return transformedMeasurements;
    }

    public static List<TransformedMeasurement> transformAirQoMeasurements(String rawMeasurements, Properties properties) {

        List<RawAirQoMeasurement> deviceMeasurements;

        try {

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
            deviceMeasurements = objectMapper.readValue(rawMeasurements, new TypeReference<>() {
            });

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

        List<TransformedMeasurement> transformedMeasurements = new ArrayList<>();

        String urlString = properties.getProperty("airqo.base.url", "");
        List<Device> devices = getDevices(urlString, "airqo");

        deviceMeasurements.forEach(rawMeasurement -> {

            Device device = getDeviceByName(devices, rawMeasurement.getDevice());
            if (!device.getSite().get_id().isEmpty()) {
                try {
                    TransformedMeasurement transformedMeasurement = new TransformedMeasurement();

                    transformedMeasurement.setDevice(rawMeasurement.getDevice());
                    transformedMeasurement.setFrequency(rawMeasurement.getFrequency());
                    transformedMeasurement.setTenant("airqo");

                    transformedMeasurement.setSiteId(device.getSite().get_id());
                    transformedMeasurement.setDeviceId(device.get_id());
                    transformedMeasurement.setDeviceNumber(rawMeasurement.getChannelId());
                    transformedMeasurement.setTime(rawMeasurement.getTime());

                    transformedMeasurement.setLocation(new TransformedLocation() {{
                        setLatitude(new LocationValue(Utils.measurementStringToDouble(rawMeasurement.getLatitude(), "latitude")));
                        setLongitude(new LocationValue(Utils.measurementStringToDouble(rawMeasurement.getLongitude(), "longitude")));
                    }});

                    transformedMeasurement.setPm2_5(new TransformedValue() {{
                        setValue(Utils.measurementStringToDouble(rawMeasurement.getPm25(), "pm2.5"));
                    }});

                    transformedMeasurement.setPm10(new TransformedValue() {{
                        setValue(Utils.measurementStringToDouble(rawMeasurement.getPm10(), "pm10"));
                    }});

                    transformedMeasurement.setS2_pm2_5(new TransformedValue() {{
                        setValue(Utils.measurementStringToDouble(rawMeasurement.getS2Pm25(), "pm2.5"));
                    }});

                    transformedMeasurement.setS2_pm10(new TransformedValue() {{
                        setValue(Utils.measurementStringToDouble(rawMeasurement.getS2Pm10(), "pm10"));
                    }});

                    transformedMeasurement.setAltitude(new TransformedValue() {{
                        setValue(Utils.measurementStringToDouble(rawMeasurement.getAltitude(), "altitude"));
                    }});

                    transformedMeasurement.setSpeed(new TransformedValue() {{
                        setValue(Utils.measurementStringToDouble(rawMeasurement.getSpeed(), "speed"));
                    }});

                    transformedMeasurement.setBattery(new TransformedValue() {{
                        setValue(Utils.measurementStringToDouble(rawMeasurement.getBattery(), "battery"));
                    }});

                    transformedMeasurement.setSatellites(new TransformedValue() {{
                        setValue(Utils.measurementStringToDouble(rawMeasurement.getSatellites(), "satellites"));
                    }});

                    transformedMeasurement.setHdop(new TransformedValue() {{
                        setValue(Utils.measurementStringToDouble(rawMeasurement.getHdop(), "hdop"));
                    }});

                    transformedMeasurement.setExternalHumidity(new TransformedValue() {{
                        setValue(Utils.measurementStringToDouble(rawMeasurement.getExternalHumidity(), "humidity"));
                    }});

                    transformedMeasurement.setExternalPressure(new TransformedValue() {{
                        setValue(Utils.measurementStringToDouble(rawMeasurement.getExternalPressure(), "pressure"));
                    }});

                    transformedMeasurement.setExternalTemperature(new TransformedValue() {{
                        setValue(Utils.measurementStringToDouble(rawMeasurement.getExternalTemperature(), "temperature"));
                    }});

                    transformedMeasurement.setInternalTemperature(new TransformedValue() {{
                        setValue(Utils.measurementStringToDouble(rawMeasurement.getInternalTemperature(), "temperature"));
                    }});

                    transformedMeasurement.setInternalHumidity(new TransformedValue() {{
                        setValue(Utils.measurementStringToDouble(rawMeasurement.getInternalHumidity(), "humidity"));
                    }});

                    transformedMeasurements.add(transformedMeasurement);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });

        return transformedMeasurements;
    }

    public static Properties loadEnvProperties(String propertiesFile) {

        if (propertiesFile == null)
            propertiesFile = "application.properties";

        Properties props = new Properties();

        try (InputStream input = Utils.class.getClassLoader().getResourceAsStream(propertiesFile)) {
            props.load(input);
        } catch (Exception ex) {
            logger.error("Error loading properties file `{}` : {}", propertiesFile, ex.toString());
        }

        Set<String> systemKeys = System.getenv().keySet();

        for (String envKey : systemKeys) {
            if (System.getenv(envKey) != null) {
                String propKey = envKey.trim().toLowerCase();
                props.setProperty(propKey, System.getenv(envKey));
            }
        }

        return props;
    }

    public static frequency getFrequency(String value) {

        if (value == null)
            return null;

        value = value.trim().toLowerCase();

        if (value.equalsIgnoreCase("daily") || value.equalsIgnoreCase("day")
                || value.equalsIgnoreCase("days")) {
            return frequency.daily;
        } else if (value.equalsIgnoreCase("hourly") || value.equalsIgnoreCase("hour")
                || value.equalsIgnoreCase("hours")) {
            return frequency.hourly;
        } else {
            return frequency.raw;
        }
    }

    public static tenant getTenant(String value) {

        if (value == null)
            return null;

        value = value.trim().toLowerCase();

        if (value.equalsIgnoreCase("airqo")) {
            return tenant.airqo;
        } else if (value.equalsIgnoreCase("kcca")) {
            return tenant.kcca;
        } else {
            return null;
        }
    }

    @Deprecated
    public static Double stringToDouble(String s, boolean trim) {

        double aDouble;

        try {
            aDouble = Double.parseDouble(s);
            if (trim)
                return Double.parseDouble(decimalFormat.format(aDouble));
            return aDouble;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static Double measurementStringToDouble(String s, String type) {

        double aDouble;

        try {
            aDouble = Double.parseDouble(s);
            if (type.equalsIgnoreCase("pm2.5") || type.equalsIgnoreCase("pm10")){
                if(aDouble < 1 || aDouble > 1000)
                    return null;
            }

            if (type.equalsIgnoreCase("latitude")){
                if(aDouble < -90 || aDouble > 90)
                    return null;
            }

            if (type.equalsIgnoreCase("longitude")){
                if(aDouble < -180 || aDouble > 180)
                    return null;
            }

            if (type.equalsIgnoreCase("battery")){
                if(aDouble < 2.7 || aDouble > 5)
                    return null;
            }

            if (type.equalsIgnoreCase("altitude")){
                if(aDouble < 0)
                    return null;
            }


            if (type.equalsIgnoreCase("hdop")){
                if(aDouble < 0)
                    return null;
            }

            if (type.equalsIgnoreCase("satellites")){
                if(aDouble < 0 || aDouble > 50)
                    return null;
            }

            if (type.equalsIgnoreCase("temperature")){
                if(aDouble < 0 || aDouble > 45)
                    return null;
            }

            if (type.equalsIgnoreCase("humidity")){
                if(aDouble < 0 || aDouble > 99)
                    return null;
            }

            if (type.equalsIgnoreCase("pressure")){
                if(aDouble < 30 || aDouble > 110)
                    return null;
            }

            return aDouble;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static List<Device> getDevices(String baseUrl, String tenant) {

        logger.info("\n\n********** Fetching Devices **************\n");

        DevicesResponse devicesResponse;

        try {

            String urlString = String.format("%sdevices?tenant=%s&active=yes", baseUrl, tenant);

            HttpClient httpClient = HttpClient.newBuilder()
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(urlString))
                    .setHeader("Accept", "application/json")
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            ObjectMapper objectMapper = new ObjectMapper();
            devicesResponse = objectMapper.readValue(httpResponse.body(), new TypeReference<>() {
            });
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

        logger.info("\n ====> Devices : {}\n", devicesResponse.getDevices());
        return devicesResponse.getDevices();
    }

    public static Device getDeviceByName(List<Device> devices, String name) {

        Optional<Device> optionalDevice = devices.stream().filter(
                deviceFilter -> deviceFilter.getName().trim().equalsIgnoreCase(name.trim())
        ).findFirst();

        return optionalDevice.orElse(new Device());

    }

}
