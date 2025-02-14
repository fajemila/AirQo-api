# Calibrate microservice

## To run in a virtual environment

1. Add file `airqo-250220-5149c2aac8f2.json` to `jobs/`. Obtain from a team member or GCP.
1. Create a virtual environment
2. `pip install -r requirements.txt`
3. `flask run`

## To build and run with docker desktop

(Uses "production" dockerfile.)

1. Add file `airqo-250220-5149c2aac8f2.json` to `jobs/`. Obtain from a team member or GCP.
1. `docker build --target=dev -t calibrate .`
2. `docker run -d -p 4001:4001 --env FLASK_APP=app.py --env FLASK_ENV=development --env MONGO_URI=mongodb://localhost:27017 calibrate`

It is implicit that `mongodb` should be installed and running.

## Calibrate Endpoint

Make a "POST" request to http://localhost:4001/api/v1/calibrate with the following raw JSON payload in the body:

```{json}
{
    "datetime": "2020-07-15 13:00:00",
    "raw_values": [
        {
            "device_id":"aq_01", 
            "sensor1_pm2.5": 44.12 , 
            "sensor1_pm10":54.20, 
            "sensor2_pm2.5": 44.12 , 
            "sensor2_pm10":54.20,
            "temperature":25.3, 
            "humidity":62.0 
        },
        {
            "device_id": "aq_02",
            "sensor1_pm2.5": 44.12 , 
            "sensor1_pm10":54.20, 
            "sensor2_pm2.5": 44.12 , 
            "sensor2_pm10":54.20,
            "temperature":25.3, 
            "humidity":62.0 
        },
        {
            "device_id": "aq_03",
            "sensor1_pm2.5": 44.12 , 
            "sensor1_pm10":54.20, 
            "sensor2_pm2.5": 44.12 , 
            "sensor2_pm10":54.20,
            "temperature":25.3, 
            "humidity":62.0 
        }
    ]
}
```


## Calibrate Tool Endpoint

Make a `POST` request to http://localhost:4001/api/v1/calibrate_tool using form data: Form must contain:

 - A csv file containing the data you want to calibrate.
 - Mapping for `datetime`, `pm2_5`, `s2_pm2_5`, `pm10`, `s2_pm10`, `humidity`, `temperature`.

### Sample contents for csv file containing uncalibrated data
![Sample csv file](https://storage.googleapis.com/airqo_open_data/uncalibrated_data.png)

### [Link to the sample file](https://storage.googleapis.com/airqo_open_data/uncalibrated_data.csv)

### Sample form data
![Sample form data](https://storage.googleapis.com/airqo_open_data/calibrate_tool_request.png)

