import datetime as dt
from bson import json_util, ObjectId
import json
from datetime import datetime,timedelta
from pymongo import MongoClient
import requests


MONGO_URIX = "mongodb+srv://sserurich:dKZcVkS5PCSpmobo@cluster0-99jha.gcp.mongodb.net/airqo_analytics"
MONGO_URI = "mongodb://localhost:27017/airqo_analytics"
client = MongoClient(MONGO_URI)
db=client['airqo_analytics']


def str_to_date(st):
    """
    Converts a string to datetime
    """
    return datetime.strptime(st, '%Y-%m-%dT%H:%M:%S.%fZ')

def date_to_str(date):
    """
    Converts datetime to a string
    """
    return datetime.strftime(date,'%Y-%m-%dT%H:%M:%S.%fZ')
    
def str_to_date_find(st):
    """
    Converts a string of different format to datetime
    """
    return datetime.strptime(st, '%Y-%m-%dT%H:%M:%SZ')


def update_device_hourly_measurements(): 
    devices_codes =  list(db.devices.find({},{"code": 1, "_id": 0}))
    average='hour'
    for device_code in devices_codes:
        code= device_code['code'] 
        print(code)           
        update_device_hourly_measurements_core(code,average)
    return 'all new hourly measurements saved'


def update_device_hourly_measurements_core(device_code, average):
        '''
        Gets new hourly data for a specific device and inserts into MongoDB
        '''
        last_time = date_to_str(get_last_time_from_device_hourly_measurements(device_code))
        endtime = date_to_str(datetime.now())

        headers = {'x-api-key': 'qJ2INQDcuMhnTdnIi6ofYX5X4vl2YYG4k2VmwUOy', 'Accept-Encoding': 'gzip'}
        base_url = "https://clarity-data-api.clarity.io/v1/measurements?"
        api_url=  "https://clarity-data-api.clarity.io/v1/measurements?startTime="+last_time +  '&endTime='+endtime+"&code="+device_code+"&average="+average
        results = requests.get(api_url, headers=headers)

        results_list = []
        
        json_results = results.json()    
        
        if len(json_results) ==0:
            return 'No new data found'
        elif len(json_results)<500: 
            for i in json_results:
                i['time'] = str_to_date(i['time'])
                i['device']= ObjectId(i['device'])
            results_list.extend(json_results)
        else:
            results_list.extend(json_results)
            while len(json_results) != 0:
                endtime_date = results_list[-1]['time']                
                next_endtime = endtime_date - timedelta(seconds=1)
                api_url = base_url+'startTime='+date_to_str(last_time)+ '&endTime='+date_to_str(next_endtime)+'&code='+device_code + "&average="+average
                results = requests.get(api_url, headers=headers)
                json_results = results.json()
                if len(json_results)==0:
                    pass
                else:
                    for i in json_results:
                        i['time'] = str_to_date(i['time'])
                        i['device']= ObjectId(i['device'])                        
                    results_list.extend(json_results)
        update_hourly_measurements(results_list)


def get_last_time_from_device_hourly_measurements(device_code):
        """ Gets the time of the latest record inserted in hourly device measurements.

        Args:
            device_code: the code used to identify the device.

        Returns:
            time for the last record inserted.

        """ 
        query = {'deviceCode': device_code}
        last_record = list(db.device_hourly_measurements.find(query).sort([('time', -1)]).limit(1))
        if len(last_record)>0:
            last_time = last_record[0]['time']
            return last_time
        else:
            return str_to_date_find('2019-09-01T00:00:00Z')


def update_hourly_measurements(data):
    """
     inserts new hourly measurements if they don't exist in db.
    """
    for i  in data:
        key = {'_id': i['_id']}        
        db.device_hourly_measurements.replace_one(key,i, upsert=True)




if __name__ == '__main__':
    update_device_hourly_measurements()