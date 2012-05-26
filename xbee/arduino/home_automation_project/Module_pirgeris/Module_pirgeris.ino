#include <XbeeRadio.h>
#include <XBee.h>

#include <Uberdust.h>

#define ZONE_NAME "zone"
#define PROX_DISTANCE 35.0

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
  initializePins();
  
  delay(1000);
  uber.blinkLED(numOfRelays, 200*numOfRelays);
  delay(1000);

  sendCapabilities();
}

void loop()
{
  periodicBlink();
  doCheckSensors();
  
  doCheckMode();
  if(modeStatus)
  {
    if(numOfRelays)
      checkLamps();
    checkSensors();
  }
  else
  {
    static unsigned long valveTimestamp = 0;
    if(proxStatus && !floodStatus)
    {
      digitalWrite(lampPins[1], HIGH);
      valveTimestamp = millis();
    }
    else
    {
      if(millis() - valveTimestamp > 1000)
        digitalWrite(lampPins[1], LOW);
    }
    
    static unsigned long moveTimestamp = 0;
    if(pirStatus)
    {
      digitalWrite(lampPins[0], HIGH);
      moveTimestamp = millis();
    }
    else
    {
      if(millis() - moveTimestamp > 60000)
        digitalWrite(lampPins[0], LOW);
    }
  }
  periodicCapabilities();
}

void initializePins(void)
{
  pinMode(ledPin, OUTPUT);
  
  pinMode(pirPin, INPUT);
  digitalWrite(pirPin, HIGH);
  
  pinMode(floodPin, INPUT);
  digitalWrite(floodPin, HIGH);

  pinMode(modePin, INPUT);
  digitalWrite(modePin, HIGH);
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
  uber.sendValue("report", "proximity");
  uber.sendValue("report", "pir");
  uber.sendValue("report", "flood");
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

void doCheckSensors(void)
{
  doCheckFlood();
  doCheckPir();
  doCheckProximity();
}

void checkSensors(void)
{
  checkFlood();
  checkPir();
  checkProximity();
}

void doCheckPir(void)
{
  static unsigned long pirTimestamp = 0;
  if(millis() - pirTimestamp > 500)
  {
    pirStatus = !digitalRead(pirPin); // read the value from the sensor
    pirTimestamp = millis();
  }
}

void checkPir(void)
{

  static uint8_t oldPirStatus = 0;
  if(oldPirStatus != pirStatus)
  {
    uber.sendValue("pir", pirStatus);
    if(oldPirStatus)
    {
      uber.sendValue("pir", pirStatus);
      uber.sendValue("pir", pirStatus);
    }
    oldPirStatus = pirStatus;
  }
  
  static unsigned long pirTimestamp = 0;
  if(millis() - pirTimestamp > 1000)
  {
    if(pirStatus)
      uber.sendValue("pir", pirStatus);
    pirTimestamp = millis();
  }
}


void doCheckProximity()
{
  static unsigned long proxTimestamp = 0;
  if(millis() - proxTimestamp > 30)
  {
    static uint8_t count = 0;
    static float data[5];
    float volts = analogRead(proxPin)*0.0048828125;  // value from sensor * (5/1024) - if running 3.3.volts then change 5 to 3.3
    data[count++] = 65*pow(volts, -1.10)*0.254;
    if(count == 5) count = 0;
    
    int value_count = 0;
    for(int i = 0; i < 5; i++)
    {
      if(data[i] > 3.0 && data[i] < PROX_DISTANCE)
        value_count++;
    }
    if(value_count == 5)
      proxStatus = HIGH;
    else
      proxStatus = LOW;
      
      proxTimestamp = millis();
  }
}  

void checkProximity(void)
{
  static uint8_t oldProxStatus = 0;
  if(proxStatus != oldProxStatus)
  {
    uber.sendValue("proximity", proxStatus);
    oldProxStatus = proxStatus;
  }
  
  static unsigned long proxTimestamp = 0;
  if(millis() - proxTimestamp > 60000)
  {
    uber.sendValue("proximity", proxStatus);
    proxTimestamp = millis();
  }
}

void doCheckFlood(void)
{
  static unsigned long floodTimestamp = 0;
  if(millis() - floodTimestamp > 500)
  {
    floodStatus = !digitalRead(floodPin); // read the value from the sensor
    floodTimestamp = millis();
  }
}

void checkFlood(void)
{
  static uint8_t oldFloodStatus = 0;
  if(floodStatus != oldFloodStatus)
  {
    uber.sendValue("flood", floodStatus);
    oldFloodStatus = floodStatus;
  }
  
  static unsigned long floodTimestamp = 0;
  if(millis() - floodTimestamp > 60000)
  {
    uber.sendValue("flood", floodStatus);
    floodTimestamp = millis();
  }
}

void doCheckMode(void)
{
  static unsigned long modeTimestamp = 0;
  if(millis() - modeTimestamp > 500)
  {
    modeStatus = digitalRead(modePin); // read the value from the sensor
    modeTimestamp = millis();
  }
}
