import json
from datetime import datetime,timedelta
import requests
from config import Config
from app import mongo
from pymongo import MongoClient

class ClarityApi():    
    """ Helper class that handles all functionality
     for retrieving data from clarity api as well as
      saving them to application database.
    """

    def __init__(self):
        """Initialises the class"""

    def get_last_time_from_device_hourly_measurements(self, device_code):
        """ Gets the time of the latest record inserted in hourly device measurements.

        Args:
            device_code: the code used to identify the device.

        Returns:
            time for the last record inserted.

        """           
        query = {'deviceCode': device_code}
        last_record = list(mongo.db.device_hourly_measurements.find(query).sort([('time', -1)]).limit(1))
        last_time = last_record[0]['time']
        return last_time

    """
    def update_device_hourly_measurements(self, device_code):
        '''
        Gets new data for a specific device and inserts into MongoDB
        '''
        last_time = self.get_last_time_from_device_hourly_measurements(device_code)
        endtime = self.date_to_str(datetime.now())

        headers = {'x-api-key': Config.CLARITY_API_KEY}
        api_url=  Config.CLARITY_API_BASE_URL + "/measurements?startTime="+last_time+ '&endTime='+endtime+"&code="+code+"&average="+average+"&limit="+str(limit)
        results = requests.get(api_url, headers=headers)
        device_measurements = results.json()       
      
    """
       
    def get_all_devices(self):
        """
        gets all the devices info from clarity api.
        returns the devices info in json format.
        """
        headers = {'x-api-key': Config.CLARITY_API_KEY}
        api_url=  Config.CLARITY_API_BASE_URL + "/devices"
        results = requests.get(api_url, headers=headers)
        return results.json(), results.status_code 


    def get_device_measurements(self, code):
            """
             gets the measurements from a clarity device with the specified code.
            """
            parameters = {
            "code":code,         
            }
            return parameters #TODO: be updated.
    
    def save_clarity_devices(self):
        """
         saves the clarity devices to airqo_analytics mongodb
        """
        headers = {'x-api-key': Config.CLARITY_API_KEY}
        api_url=  Config.CLARITY_API_BASE_URL + "/devices"
        results = requests.get(api_url, headers=headers)
        devices = results.json()
        if results.status_code  == 200: 
            for i in range(0, len(results.json())):
                #print(i)                
                print('[!] Inserting - ', devices[i])
                mongo.db.devices.insert(devices[i])

    def save_clarity_device_hourly_measurements(self,average,code,startTime, limit ):
        """
         saves the measurements for the specified clarity device to airqo_analytics mongodb
         https://clarity-data-api.clarity.io/v1/measurements?startTime=2019-09-27T13:00:00Z&code=ANQ16PZJ&average=hour&limit=20000
        """
        headers = {'x-api-key': Config.CLARITY_API_KEY}
        api_url=  Config.CLARITY_API_BASE_URL + "/measurements?startTime="+startTime+"&code="+code+"&average="+average+"&limit="+str(limit)
        results = requests.get(api_url, headers=headers)
        device_measurements = results.json()
        if results.status_code  == 200: 
            for i in range(0, len(results.json())):              
                #print('[!] Inserting - ', device_measurements[i])
                mongo.db.device_hourly_measurements.insert(device_measurements[i])

    def save_clarity_device_daily_measurements(self,average,code,startTime, limit ):
        """
         saves the daily measurements for the specified clarity device to airqo_analytics mongodb
        
        """
        headers = {'x-api-key': Config.CLARITY_API_KEY}
        api_url=  Config.CLARITY_API_BASE_URL + "/measurements?startTime="+startTime+"&code="+code+"&average="+average+"&limit="+str(limit)
        results = requests.get(api_url, headers=headers)
        device_measurements = results.json()
        if results.status_code  == 200: 
            for i in range(0, len(results.json())):            
                mongo.db.device_daily_measurements.insert(device_measurements[i])

    def save_clarity_raw_device_measurements(self, device_code):
        """
        gets all raw data for a device and saves it to MongoDB
        """
        base_url = 'https://clarity-data-api.clarity.io/v1/measurements?'

        results_list = []
        api_url = base_url + 'code=' + device_code
        headers = {'x-api-key': 'qJ2INQDcuMhnTdnIi6ofYX5X4vl2YYG4k2VmwUOy', 'Accept-Encoding': 'gzip'}
        results = requests.get(api_url, headers=headers)
        json_results = results.json()

        if results.status_code  == 200: 
            results_list.extend(json_results)
        
            while len(json_results) != 0:#should stop when no result is returned
                endtime_string = results_list[-1]['time'] #getting the datetime of the last returned row
                endtime_date = self.str_to_date(endtime_string)
                next_start_time = endtime_date - timedelta(seconds=1)
                api_url = base_url+'endTime='+self.date_to_str(next_start_time)+'&code='+device_code
                results = requests.get(api_url, headers=headers)
                json_results = results.json()
                if len(json_results)==0:
                    print ('Download Ended')
                else:
                    results_list.extend(json_results)
        self.insert_data_mongo(results_list)

    def str_to_date(self, st):
        """
        Converts a string to datetime
        """
        return datetime.strptime(st, '%Y-%m-%dT%H:%M:%S.%fZ')

    def date_to_str(self,date):
        """
        Converts datetime to string
        """
        return datetime.strftime(date,'%Y-%m-%dT%H:%M:%S.%fZ')

    def insert_data_mongo(self,data):
        """
        Inserts list of json objects into MongoDB
        """
        client = MongoClient('mongodb+srv://lillian:fosho@cluster0-99jha.gcp.mongodb.net/test?retryWrites=true&w=majority')
        db=client['airqo_analytics']
        for i in data:
            db['device_raw_measurements'].insert_one(i)

    def get_last_time(self,device_code):
        """
        Gets the time of the latest record in the MongoDB
        """
        client = MongoClient("mongodb+srv://lillian:fosho@cluster0-99jha.gcp.mongodb.net/test?retryWrites=true&w=majority")  
        db=client['airqo_analytics']
    
        query = {'deviceCode': device_code}
        last_record = list(db.device_raw_measurements.find(query).sort([('time', -1)]).limit(1))
        last_time = last_record[0]['time']
        return last_time

    def update_clarity_data(self,device_code):
        '''
        Gets new data for a specific device and inserts into MongoDB
        '''
        base_url = 'https://clarity-data-api.clarity.io/v1/measurements?'
        last_time = self.get_last_time(device_code)
        endtime = self.date_to_str(datetime.now())
        results_list = []
        api_url = base_url+'startTime='+last_time+ '&endTime='+endtime+'&code='+device_code
        headers = {'x-api-key': 'qJ2INQDcuMhnTdnIi6ofYX5X4vl2YYG4k2VmwUOy', 'Accept-Encoding': 'gzip'}
        results = requests.get(api_url, headers=headers)
        json_results = results.json()
    
        if results.status_code  == 200: 
            if len(json_results)<500:
                results_list.extend(json_results)
            else:
                results_list.extend(json_results)
                while len(json_results) != 0:#should stop when no result is returned
                    endtime_string = results_list[-1]['time'] #getting the datetime of the last returned row
                    endtime_date = self.str_to_date(endtime_string)
                    next_endtime = endtime_date - timedelta(seconds=1)
                    api_url = base_url+'startTime='+last_time+ '&endTime='+next_endtime+'&code='+device_code
                    results = requests.get(api_url, headers=headers)
                    json_results = results.json()
                    if results.status_code==200:
                        results_list.extend(json_results)
        self.insert_data_mongo(results_list)




if __name__ == '__main__':
    clarity_object = ClarityApi()
    clarity_object.save_clarity_devices()

               
           
    
  