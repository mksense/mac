#include <XbeeRadio.h>
#include <XBee.h>

#include <Uberdust.h>

#define ZONE_NAME "zone"

// Create the xbee object
XBeeRadio xbee = XBeeRadio(); 

// Create reusable response objects for responses we expect to handle 
XBeeRadioResponse response = XBeeRadioResponse();  

// Allocate two bytes for to hold a 32-bit analog reading
uint8_t payload[] = { 
  102, 0,  0, 0, 0, 0};

// 16-bit addressing: Enter address of remote XBee, typically the coordinator
Tx16Request tx = Tx16Request(0xffff, payload, sizeof(payload));

TxStatusResponse txStatus = TxStatusResponse();

uint8_t lampPins[] = { 2, 3};
uint8_t lampStatuses[] = { 0, 0};

uint8_t numOfRelays = 2;

uint8_t pirPin = 9;
uint8_t pirStatus = LOW;

uint8_t proxPin = A3;
uint8_t proxStatus = LOW;

uint8_t floodPin = 11;
uint8_t floodStatus = LOW;

uint8_t modePin = 8;
uint8_t modeStatus = LOW;

uint8_t ledPin = 13;


Uberdust uber = Uberdust();

void setup()
{

  xbee.initialize_xbee_module();
  // setup xbee 
  xbee.begin(38400);
  xbee.init();

  uber.setup(&xbee, &tx);

  setupRelays();
  
  delay(1000);
  uber.blinkLED(numOfRelays, 200*numOfRelays);
  delay(1000);

  sendCapabilities();
}

void loop()
{
  periodicBlink();

  if(numOfRelays)
    checkLamps();

  periodicCapabilities();
}

void periodicBlink()
{
  static unsigned long ledTimestamp = 0;
  if(millis() - ledTimestamp > 5000)
  {
    uber.blinkLED(1,100);
    ledTimestamp = millis();
  }
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

  for(int i = 0; i < numOfRelays; i++)
  {
    uber.sendValue("report", String(ZONE_NAME)+(i+1));
  }

}

void setupRelays(void)
{
//  numOfRelays = getNumOfRelays();
  for(int i=0; i< numOfRelays; i++)
  {
    pinMode(lampPins[i], OUTPUT);   
    setLamp(i, LOW);
  }
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
  uber.sendValue(String(ZONE_NAME)+(lamp+1), lampStatuses[lamp]);
}

void reportAllLamps(void)
{
  for(int i = 0; i < numOfRelays; i++)
    reportLamp(i);
}

void checkSensors(void)
{
  checkPir();
}

void checkPir(void)
{
  static unsigned long pirTimestamp = 0;
  if(millis() - pirTimestamp > 500)
  {

    int newPirStatus = digitalRead(pirPin); // read the value from the sensor
    if(newPirStatus != pirStatus || !newPirStatus)
    {

      uber.sendValue("pir", !newPirStatus);
      if(newPirStatus)
      {
        uber.sendValue("pir", !newPirStatus);
        uber.sendValue("pir", !newPirStatus);
      }

    }
    pirStatus = newPirStatus;
    pirTimestamp = millis();
  }
}

