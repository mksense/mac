#include <Uberdust.h>

#include <XbeeRadio.h>
#include <XBee.h>
#include <stdio.h>
#include <string.h>
#include <SoftwareSerial.h>


// Create the xbee object
XBeeRadio xbee = XBeeRadio(); 

// Create reusable response objects for responses we expect to handle 
XBeeRadioResponse response = XBeeRadioResponse();  

// Allocate two bytes for to hold a 32-bit analog reading
uint8_t payload[] = { 
  102, 0,  0, 0, 0, 0};

// 16-bit addressing: Enter address of remote XBee, typically the coordinator
Tx16Request tx = Tx16Request(0xffff, payload, sizeof(payload));

uint8_t tmpPayload[20];
Tx16Request tmpTx = Tx16Request(0xffff, tmpPayload, sizeof(tmpPayload)); 

TxStatusResponse txStatus = TxStatusResponse();

uint8_t lampPins[] = { 2, 3, 4, 5, 6};
uint8_t relayCheckPin = A4;
uint8_t numOfRelays = 0;

uint8_t lampStatuses[5] = { 0, 0, 0, 0, 0};

uint8_t pirPin = 9;
uint8_t heaterPin = 10;
uint8_t securityPin = 11;
uint8_t sensorsCheckPin = 12;
uint8_t ledPin = 13;
uint8_t tempPin = A0;
uint8_t lightPin = A1;
uint8_t methanePin = A2;
uint8_t carbonPin = A3;
bool sensorsExist = false;

uint8_t pirStatus;
uint8_t securityStatus;
int tempValue=0;
int lightValue=0;
int methaneValue=0;
int carbonValue=0;


Uberdust uber = Uberdust();

void setup()
{

  xbee.initialize_xbee_module();
  // setup xbee 
  xbee.begin(38400);
  xbee.init();

  uber.setup(&xbee, &tx);

  numOfRelays = getNumOfRelays();
  for(int i=0; i< numOfRelays; i++)
  {
    pinMode(lampPins[i], OUTPUT);   
    setLamp(i, LOW);
  }
  delay(1000);
  uber.blinkLED(numOfRelays, 200*numOfRelays);
  delay(1000);

  pinMode(sensorsCheckPin, INPUT);
  digitalWrite(sensorsCheckPin, HIGH);
  sensorsExist = !digitalRead(sensorsCheckPin);

  if(sensorsExist)
  {
    pinMode(pirPin, INPUT);
    digitalWrite(pirPin, HIGH);
    pinMode(heaterPin, OUTPUT);
    pinMode(securityPin, INPUT);
    digitalWrite(securityPin, HIGH);
    uber.blinkLED(1, 500);
  } 

    sendCapabilities();
}

void loop()
{
  static unsigned long ledTimestamp = 0;
  if(millis() - ledTimestamp > 5000)
  {
    uber.blinkLED(1,100);
    ledTimestamp = millis();
  }

  if(numOfRelays)
    checkLamps();

  if(sensorsExist)
    checkSensors();
    
  periodicCapabilities();
}

//Subroutine, which sends sensor's data
void send_data(int sensor_type, int sensor_val)
{
  // initialize bla_pointer pointer for reading 32 bit sensors values
  uint8_t * bla_pointer;

  payload[1] = sensor_type;
  bla_pointer = (uint8_t*) &sensor_val; //ldr_val
  payload[2] = *bla_pointer;
  bla_pointer++;
  payload[3] = *bla_pointer;
  bla_pointer++;
  payload[4] = 0; //*bla_pointer;
  bla_pointer++;
  payload[5] = 0; //*bla_pointer;


  send_data();

}

void send_data(void)
{
  xbee.sendAndCheck(tx,112);
  delay(100);  
}

void periodicCapabilities()
{
  static unsigned long capabTimestamp = 0;
  if(millis() - capabTimestamp > 60000)
  {
    digitalWrite(13, HIGH);
    sendCapabilities();
    digitalWrite(13, LOW);
    delay(100);
    capabTimestamp = millis();
  }
}
void sendCapabilities(void)
{
  int messageSize = 3;
  messageSize += numOfRelays;
  if(sensorsExist)
    messageSize += 4;
  tmpPayload[0] = 102;
  tmpPayload[1] = 100;
  tmpPayload[2] = messageSize - 3;
  int count = 3;
  for(int i = 0; i < numOfRelays; i++)
  {
    tmpPayload[count++] = 14;
  }
  
  if(sensorsExist)
  {
    tmpPayload[count++] = 1;
    tmpPayload[count++] = 6;
    tmpPayload[count++] = 7;
    tmpPayload[count++] = 16;
  }

  xbee.sendAndCheck(tmpTx,112);  
}

uint8_t getNumOfRelays(void)
{
  uint8_t relays[] ={0, 0, 0, 0, 0, 0};
  for(int i = 0; i < 10; i++)
  {
    relays[getNumOfRels()]++;
  }
  int num = 0;
  
  for(int i = 1 ; i < 6; i++)
  {
    if(relays[i] > relays[i-1])
      num = i;
  }
  return num;
}

uint8_t getNumOfRels(void)
{
  int value = analogRead(relayCheckPin);
  delay(10);
  int relNum = 0;
  int distance[5];
  int thresholds[] = {
    0, 342, 512, 614, 683, 732   };
  for(int i = 0; i< 6; i++)
  {
    thresholds[i] < value? distance[i] = value - thresholds[i] : distance[i] = thresholds[i] - value;
    //    Serial.print(thresholds[i], DEC);
    //    Serial.print("\t");
    //    Serial.println(distance[i], DEC);
  }

  for(int i = 1; i< 6; i++)
    if(distance[i] < distance[i-1]) relNum = i;

  return relNum;
}

void checkLamps(void)
{
  const int inactiveMins = 5;
  static unsigned long lastTimestamp = 0;
  if(xbee.checkForData(112))
  {

    xbee.getResponse(response);
    if(response.getData(0) == 1)
    {
      lastTimestamp = millis();
      int lamp = response.getData(1);
      int value = response.getData(2);
      if(lamp > 0 && lamp <= numOfRelays)
      {
        setLamp(lamp-1, value);
        reportLamp(lamp-1);
      }
      else if(lamp == 0xff)
      {
        setAllLamps(value);
        reportAllLamps();
      }
      uber.blinkLED(2,100);
    }
  }

  if(millis() - lastTimestamp > inactiveMins * 60000)
  {
    setAllLamps(LOW);
    reportAllLamps();
    lastTimestamp = millis();
  }

  static unsigned long reportTimestamp = 0;
  if(millis() - reportTimestamp > 60000)
  {
    reportAllLamps();
    reportTimestamp = millis();
  }

}

void setLamp(int lamp, int value)
{
  lampStatuses[lamp] = value;
  digitalWrite(lampPins[lamp], lampStatuses[lamp]);
}

void setAllLamps(int value)
{
  for(int i = 0; i < numOfRelays ; i++)
    setLamp(i, value);
}

void reportLamp(int lamp)
{
//  payload[1] = 14;
//  payload[2] = lamp+1;
//  payload[3] = lampStatuses[lamp];
//  send_data();
  uber.sendValue(String("lamp")+lamp+1, lampStatuses[lamp]);
}

void reportAllLamps(void)
{
  for(int i = 0; i < numOfRelays; i++)
    reportLamp(i);
}

void checkSensors(void)
{
  checkPir();
  checkLight();
  checkTemp();
  checkMethane();
}

void checkPir(void)
{
  static unsigned long pirTimestamp = 0;
  if(millis() - pirTimestamp > 500)
  {

    int newPirStatus = digitalRead(pirPin); // read the value from the sensor
    if(newPirStatus != pirStatus || !newPirStatus)
    {
      //      send_data(7, newPirStatus);
      payload[1] = 7;
      payload[2] = !newPirStatus;
      payload[3] = !newPirStatus;
      payload[4] = !newPirStatus;
      payload[5] = !newPirStatus;
      send_data();
      if(newPirStatus)
      {
        send_data();
        send_data();
      }

    }
    pirStatus = newPirStatus;
    pirTimestamp = millis();
  }
}

void checkLight(void)
{
  // for light sensor

  static unsigned long lightTimestamp = 0;
  if(millis() - lightTimestamp > 3 * 60000)
  {
    lightValue = analogRead(lightPin);  // read the value from the sensor
    send_data(1, lightValue);
    lightTimestamp = millis();
  }

}

void checkTemp(void)
{
  // for temp sensor

  static unsigned long tempTimestamp = 0;
  if(millis() - tempTimestamp > 3 * 60000)
  {
    uint8_t value = analogRead(tempPin);  // read the value from the sensor
    tempValue = map(value, 0, 1024, 0, 5000)/10;  
    payload[1] = 16;
    payload[2] = tempValue;
    payload[3] = tempValue;
    payload[4] = tempValue;
    payload[5] = tempValue;
    send_data(16, tempValue);
    tempTimestamp = millis();
  }
}

void checkMethane(void)
{
  // for temp sensor

  static unsigned long methaneTimestamp = 0;
  if(millis() - methaneTimestamp > 3 * 60000)
  {
    methaneValue = analogRead(methanePin);  // read the value from the sensor
    send_data(6, methaneValue);
    methaneTimestamp = millis();
  }
}


